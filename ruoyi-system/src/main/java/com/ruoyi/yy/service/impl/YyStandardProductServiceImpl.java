package com.ruoyi.yy.service.impl;

import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.mapper.YyStandardProductMapper;
import com.ruoyi.yy.service.IYyStandardProductService;
import com.ruoyi.yy.vo.PriceComparisonVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 标准商品 Service 实现
 */
@Service
public class YyStandardProductServiceImpl implements IYyStandardProductService {

    @Autowired
    private YyStandardProductMapper yyStandardProductMapper;

    @Override
    public YyStandardProduct selectYyStandardProductById(Long id) {
        return yyStandardProductMapper.selectYyStandardProductById(id);
    }

    @Override
    public List<YyStandardProduct> selectYyStandardProductList(YyStandardProduct yyStandardProduct) {
        return yyStandardProductMapper.selectYyStandardProductList(yyStandardProduct);
    }

    @Override
    public List<YyStandardProduct> selectByFusionGroupId(Long fusionGroupId) {
        return yyStandardProductMapper.selectByFusionGroupId(fusionGroupId);
    }

    @Override
    public int insertYyStandardProduct(YyStandardProduct yyStandardProduct) {
        return yyStandardProductMapper.insertYyStandardProduct(yyStandardProduct);
    }

    @Override
    public int updateYyStandardProduct(YyStandardProduct yyStandardProduct) {
        return yyStandardProductMapper.updateYyStandardProduct(yyStandardProduct);
    }

    @Override
    public int deleteYyStandardProductByIds(Long[] ids) {
        return yyStandardProductMapper.deleteYyStandardProductByIds(ids);
    }

    @Override
    public int deleteYyStandardProductById(Long id) {
        return yyStandardProductMapper.deleteYyStandardProductById(id);
    }

    // ========== 比价功能（增量扩展） ==========

    @Override
    public List<YyStandardProduct> selectBySkuId(String skuId, String sourcePlatform) {
        if (sourcePlatform != null && !sourcePlatform.isEmpty()) {
            // 查询单个平台
            YyStandardProduct product = yyStandardProductMapper.selectBySkuId(sourcePlatform, skuId);
            return product != null ? Collections.singletonList(product) : Collections.emptyList();
        } else {
            // 查询所有平台
            return yyStandardProductMapper.selectAllPlatformsBySkuId(skuId);
        }
    }

    @Override
    public List<YyStandardProduct> selectByFusionKey(String fusionKey) {
        return yyStandardProductMapper.selectByFusionKey(fusionKey);
    }

    @Override
    public List<YyStandardProduct> selectByCommonName(String commonName) {
        return yyStandardProductMapper.selectByCommonName(commonName);
    }

    @Override
    public List<PriceComparisonVO> comparePrices(String productId, String customerType) {
        // 查询各平台价格
        List<YyStandardProduct> products;
        
        // 判断是SKU ID还是通用名
        if (productId.length() <= 20) {
            // 假设SKU ID长度不超过20
            products = yyStandardProductMapper.selectAllPlatformsBySkuId(productId);
        } else {
            // 可能是融合键
            products = yyStandardProductMapper.selectByFusionKey(productId);
        }
        
        if (products.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 按平台分组，取每个平台最新记录
        Map<String, YyStandardProduct> latestByPlatform = new HashMap<>();
        for (YyStandardProduct product : products) {
            String platform = product.getSourcePlatform();
            YyStandardProduct existing = latestByPlatform.get(platform);
            if (existing == null || product.getCollectedAt().after(existing.getCollectedAt())) {
                latestByPlatform.put(platform, product);
            }
        }
        
        // 转换为VO并计算到手价
        List<PriceComparisonVO> voList = new ArrayList<>();
        BigDecimal maxTotalCost = BigDecimal.ZERO;
        
        for (YyStandardProduct product : latestByPlatform.values()) {
            PriceComparisonVO vo = convertToPriceComparisonVO(product);
            voList.add(vo);
            
            if (vo.getTotalCost().compareTo(maxTotalCost) > 0) {
                maxTotalCost = vo.getTotalCost();
            }
        }
        
        // 按到手价排序
        voList.sort(Comparator.comparing(PriceComparisonVO::getTotalCost));
        
        // 计算节省金额和排名
        final BigDecimal maxCost = maxTotalCost;
        for (int i = 0; i < voList.size(); i++) {
            PriceComparisonVO vo = voList.get(i);
            vo.setRank(i + 1);
            vo.setIsBestPrice(i == 0);
            
            BigDecimal savedAmount = maxCost.subtract(vo.getTotalCost());
            vo.setSavedAmount(savedAmount);
            
            if (maxCost.compareTo(BigDecimal.ZERO) > 0) {
                vo.setSavedPercent(
                    savedAmount.divide(maxCost, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                );
            }
        }
        
        return voList;
    }

    @Override
    public Map<String, List<PriceComparisonVO>> batchComparePrices(List<String> productIds, String customerType) {
        Map<String, List<PriceComparisonVO>> result = new HashMap<>();
        
        for (String productId : productIds) {
            List<PriceComparisonVO> comparison = comparePrices(productId, customerType);
            if (!comparison.isEmpty()) {
                result.put(productId, comparison);
            }
        }
        
        return result;
    }

    @Override
    public Map<String, Object> recommendPlatform(String productId, String customerType) {
        List<PriceComparisonVO> comparisonList = comparePrices(productId, customerType);
        
        Map<String, Object> result = new HashMap<>();
        
        if (comparisonList.isEmpty()) {
            result.put("recommended", false);
            result.put("reason", "未找到比价数据");
            return result;
        }
        
        PriceComparisonVO best = comparisonList.get(0);
        
        result.put("recommended", true);
        result.put("productId", productId);
        result.put("productName", best.getProductName());
        result.put("recommendedPlatform", best.getPlatformCode());
        result.put("recommendedPlatformName", best.getPlatformName());
        result.put("recommendedPrice", best.getCurrentPrice());
        result.put("totalCost", best.getTotalCost());
        result.put("savedAmount", best.getSavedAmount());
        result.put("allPlatforms", comparisonList);
        
        // 生成推荐原因
        StringBuilder reason = new StringBuilder();
        if (best.getIsBestPrice()) {
            reason.append("价格最低");
        }
        if (best.getActivityType() != null && !"none".equals(best.getActivityType())) {
            reason.append("，参与").append(best.getActivityType()).append("活动");
        }
        if (Boolean.TRUE.equals(best.getIsFreeShipping())) {
            reason.append("，包邮");
        }
        result.put("reason", reason.toString());
        
        return result;
    }

    // ========== 私有方法 ==========

    /**
     * 将 YyStandardProduct 转换为 PriceComparisonVO
     */
    private PriceComparisonVO convertToPriceComparisonVO(YyStandardProduct product) {
        PriceComparisonVO vo = new PriceComparisonVO();
        
        // 商品信息
        vo.setSkuId(product.getSkuId());
        vo.setProductName(product.getProductName());
        vo.setCommonName(product.getCommonName());
        vo.setSpecification(product.getSpecification());
        vo.setManufacturer(product.getManufacturer());
        
        // 平台信息
        vo.setPlatformCode(product.getSourcePlatform());
        // TODO: 从平台配置表获取平台名称
        vo.setPlatformName(product.getSourcePlatform());
        
        // 价格信息
        vo.setBasePrice(product.getPriceRetail());
        vo.setCurrentPrice(product.getPriceCurrent());
        vo.setActivityPrice(product.getPriceAssemble());
        
        // 判断活动类型
        if (product.getPriceAssemble() != null && product.getPriceAssemble().compareTo(product.getPriceCurrent()) < 0) {
            vo.setActivityType("拼团");
        }
        
        // 采购条件
        vo.setMinOrderQty(product.getMinOrderQty());
        vo.setStockQuantity(product.getStockQuantity());
        
        // 物流信息
        vo.setFreightAmount(product.getFreightAmount() != null ? product.getFreightAmount() : BigDecimal.ZERO);
        vo.setFreeShippingThreshold(product.getFreeShippingThreshold());
        
        // 计算到手价
        BigDecimal price = vo.getActivityPrice() != null ? vo.getActivityPrice() : vo.getCurrentPrice();
        BigDecimal freight = vo.getFreightAmount();
        vo.setTotalCost(price.add(freight));
        
        // 判断是否包邮
        boolean isFreeShipping = freight.compareTo(BigDecimal.ZERO) == 0 ||
            (vo.getFreeShippingThreshold() != null && price.compareTo(vo.getFreeShippingThreshold()) >= 0);
        vo.setIsFreeShipping(isFreeShipping);
        
        // 时间信息
        vo.setCollectedAt(product.getCollectedAt());
        
        return vo;
    }
}
