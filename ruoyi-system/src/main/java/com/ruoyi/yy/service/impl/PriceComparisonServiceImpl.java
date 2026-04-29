package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.yy.domain.YyPriceComparison;
import com.ruoyi.yy.domain.YyPlatformActivity;
import com.ruoyi.yy.dto.PriceComparisonDTO;
import com.ruoyi.yy.mapper.YyPriceComparisonMapper;
import com.ruoyi.yy.mapper.YyPlatformActivityMapper;
import com.ruoyi.yy.service.IPriceComparisonService;
import com.ruoyi.yy.service.IYyPlatformService;
import com.ruoyi.yy.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 多平台比价服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceComparisonServiceImpl implements IPriceComparisonService {
    
    private final YyPriceComparisonMapper priceComparisonMapper;
    private final YyPlatformActivityMapper platformActivityMapper;
    private final IYyPlatformService platformService;
    
    // ========== 商品比价 ==========
    
    @Override
    public List<PriceComparisonVO> comparePrices(String skuId, String customerType) {
        // 查询各平台最新价格
        List<YyPriceComparison> priceList = priceComparisonMapper.selectLatestPricesBySku(skuId, customerType);
        
        if (priceList.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 转换为VO并计算到手价
        List<PriceComparisonVO> voList = new ArrayList<>();
        BigDecimal maxTotalCost = BigDecimal.ZERO;
        
        for (YyPriceComparison price : priceList) {
            PriceComparisonVO vo = convertToVO(price);
            voList.add(vo);
            
            if (vo.getTotalCost().compareTo(maxTotalCost) > 0) {
                maxTotalCost = vo.getTotalCost();
            }
        }
        
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
    public Map<String, List<PriceComparisonVO>> batchComparePrices(List<String> skuIds, String customerType) {
        Map<String, List<PriceComparisonVO>> result = new HashMap<>();
        
        // 批量查询
        List<YyPriceComparison> priceList = priceComparisonMapper.selectBySkuIds(skuIds, customerType);
        
        // 按SKU分组
        Map<String, List<YyPriceComparison>> groupedBySku = priceList.stream()
            .collect(Collectors.groupingBy(YyPriceComparison::getSkuId));
        
        // 对每个SKU进行比价计算
        for (Map.Entry<String, List<YyPriceComparison>> entry : groupedBySku.entrySet()) {
            String skuId = entry.getKey();
            List<YyPriceComparison> skuPrices = entry.getValue();
            
            // 按平台分组，取每个平台最新记录
            Map<String, YyPriceComparison> latestByPlatform = new HashMap<>();
            for (YyPriceComparison price : skuPrices) {
                String platform = price.getSourcePlatform();
                YyPriceComparison existing = latestByPlatform.get(platform);
                if (existing == null || price.getCollectedAt().after(existing.getCollectedAt())) {
                    latestByPlatform.put(platform, price);
                }
            }
            
            // 转换并排序
            List<PriceComparisonVO> voList = new ArrayList<>();
            BigDecimal maxTotalCost = BigDecimal.ZERO;
            
            for (YyPriceComparison price : latestByPlatform.values()) {
                PriceComparisonVO vo = convertToVO(price);
                voList.add(vo);
                
                if (vo.getTotalCost().compareTo(maxTotalCost) > 0) {
                    maxTotalCost = vo.getTotalCost();
                }
            }
            
            // 按总价排序
            voList.sort(Comparator.comparing(PriceComparisonVO::getTotalCost));
            
            // 计算节省金额和排名
            final BigDecimal maxCost = maxTotalCost;
            for (int i = 0; i < voList.size(); i++) {
                PriceComparisonVO vo = voList.get(i);
                vo.setRank(i + 1);
                vo.setIsBestPrice(i == 0);
                vo.setSavedAmount(maxCost.subtract(vo.getTotalCost()));
            }
            
            result.put(skuId, voList);
        }
        
        return result;
    }
    
    @Override
    public List<PriceComparisonVO> comparePricesByCondition(PriceComparisonDTO dto) {
        // TODO: 实现复杂条件查询
        // 这里简化处理，实际应该根据dto构建查询条件
        return comparePrices(dto.getSkuId(), dto.getCustomerType());
    }
    
    // ========== 价格计算 ==========
    
    @Override
    public BigDecimal calculateActivityPrice(BigDecimal basePrice, String activityType, String activityRules, 
                                              Integer orderQty, String customerType) {
        if (activityType == null || "none".equals(activityType)) {
            return basePrice;
        }
        
        JSONObject rules = JSON.parseObject(activityRules);
        
        switch (activityType) {
            case "拼团":
                return calculateGroupBuyPrice(basePrice, rules, orderQty);
            case "凑单":
            case "满减":
                // 满减需要订单总金额，这里返回原价，由调用方处理
                return basePrice;
            case "会员价":
                return calculateMemberPrice(basePrice, rules, customerType);
            case "限时折扣":
                return calculateDiscountPrice(basePrice, rules);
            case "专享价":
                return rules.getBigDecimal("special_price");
            default:
                return basePrice;
        }
    }
    
    @Override
    public BigDecimal calculateFullReduction(BigDecimal totalAmount, String platformCode) {
        // 查询平台有效的满减活动
        List<YyPlatformActivity> activities = platformActivityMapper.selectActiveByPlatformAndType(
            platformCode, "凑单"
        );
        
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        for (YyPlatformActivity activity : activities) {
            JSONObject rules = JSON.parseObject(activity.getActivityRules());
            BigDecimal threshold = rules.getBigDecimal("threshold");
            
            if (totalAmount.compareTo(threshold) >= 0) {
                String type = rules.getString("type");
                BigDecimal discount = rules.getBigDecimal("discount");
                
                if ("fixed".equals(type)) {
                    totalDiscount = totalDiscount.add(discount);
                } else if ("percent".equals(type)) {
                    // 折扣率，如0.1表示打9折（减10%）
                    totalDiscount = totalDiscount.add(
                        totalAmount.multiply(discount).setScale(2, RoundingMode.HALF_UP)
                    );
                }
            }
        }
        
        return totalDiscount;
    }
    
    @Override
    public BigDecimal calculateGroupBuyPrice(BigDecimal basePrice, String activityRules, Integer currentGroupSize) {
        JSONObject rules = JSON.parseObject(activityRules);
        Integer requiredSize = rules.getInteger("group_size");
        BigDecimal discountRate = rules.getBigDecimal("discount_rate");
        
        if (currentGroupSize != null && currentGroupSize >= requiredSize) {
            // 已成团
            return basePrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
        }
        
        // 未成团，返回原价
        return basePrice;
    }
    
    // ========== 推荐与优化 ==========
    
    @Override
    public PlatformRecommendationVO recommendPlatform(String skuId, String customerType, String regionCode) {
        List<PriceComparisonVO> comparisonList = comparePrices(skuId, customerType);
        
        if (comparisonList.isEmpty()) {
            return null;
        }
        
        PriceComparisonVO best = comparisonList.get(0);
        
        PlatformRecommendationVO recommendation = new PlatformRecommendationVO();
        recommendation.setSkuId(skuId);
        recommendation.setProductName(best.getProductName());
        recommendation.setRecommendedPlatform(best.getPlatformCode());
        recommendation.setRecommendedPlatformName(best.getPlatformName());
        recommendation.setRecommendedPrice(best.getCurrentPrice());
        recommendation.setTotalCost(best.getTotalCost());
        recommendation.setAllPlatforms(comparisonList);
        
        // 生成推荐原因
        StringBuilder reason = new StringBuilder();
        if (best.getIsBestPrice()) {
            reason.append("价格最低");
        }
        if (best.getActivityType() != null && !"none".equals(best.getActivityType())) {
            reason.append("，参与").append(best.getActivityType()).append("活动");
        }
        if (best.getIsFreeShipping()) {
            reason.append("，包邮");
        }
        if (best.getDeliveryDays() != null && best.getDeliveryDays() <= 2) {
            reason.append("，配送快");
        }
        recommendation.setReason(reason.toString());
        
        // 计算节省金额
        if (comparisonList.size() > 1) {
            BigDecimal maxCost = comparisonList.get(comparisonList.size() - 1).getTotalCost();
            recommendation.setSavedAmount(maxCost.subtract(best.getTotalCost()));
        }
        
        return recommendation;
    }
    
    @Override
    public PurchasePlanVO generatePurchasePlan(List<String> skuIds, String customerType, BigDecimal budget) {
        PurchasePlanVO plan = new PurchasePlanVO();
        plan.setPlanId(UUID.randomUUID().toString().replace("-", ""));
        plan.setCustomerType(customerType);
        
        // 批量比价
        Map<String, List<PriceComparisonVO>> comparisonMap = batchComparePrices(skuIds, customerType);
        
        List<PurchasePlanVO.PurchaseItem> items = new ArrayList<>();
        Map<String, List<PurchasePlanVO.PurchaseItem>> platformItems = new HashMap<>();
        
        BigDecimal totalProductAmount = BigDecimal.ZERO;
        BigDecimal totalFreightAmount = BigDecimal.ZERO;
        BigDecimal totalDiscountAmount = BigDecimal.ZERO;
        
        // 为每个商品选择最优平台
        for (String skuId : skuIds) {
            List<PriceComparisonVO> comparisonList = comparisonMap.get(skuId);
            if (comparisonList == null || comparisonList.isEmpty()) {
                continue;
            }
            
            PriceComparisonVO best = comparisonList.get(0);
            
            PurchasePlanVO.PurchaseItem item = new PurchasePlanVO.PurchaseItem();
            item.setSkuId(skuId);
            item.setProductName(best.getProductName());
            item.setPlatformCode(best.getPlatformCode());
            item.setPlatformName(best.getPlatformName());
            item.setUnitPrice(best.getCurrentPrice());
            item.setQuantity(1); // 默认数量为1
            item.setSubtotal(best.getCurrentPrice());
            item.setActivityType(best.getActivityType());
            item.setFreight(best.getFreightAmount());
            item.setDeliveryDays(best.getDeliveryDays());
            
            items.add(item);
            
            // 按平台分组
            platformItems.computeIfAbsent(best.getPlatformCode(), k -> new ArrayList<>())
                .add(item);
            
            // 累计金额
            totalProductAmount = totalProductAmount.add(best.getCurrentPrice());
            totalFreightAmount = totalFreightAmount.add(best.getFreightAmount());
        }
        
        plan.setItems(items);
        plan.setPlatformItems(platformItems);
        plan.setTotalProductAmount(totalProductAmount);
        plan.setTotalFreightAmount(totalFreightAmount);
        plan.setFinalAmount(totalProductAmount.add(totalFreightAmount).subtract(totalDiscountAmount));
        
        // 生成优化建议
        List<String> tips = new ArrayList<>();
        if (platformItems.size() > 1) {
            tips.add("采购涉及" + platformItems.size() + "个平台，可尝试凑单减少运费");
        }
        plan.setOptimizationTips(tips);
        
        return plan;
    }
    
    @Override
    public PurchasePlanVO optimizePurchasePlan(PurchasePlanVO plan) {
        // TODO: 实现采购优化算法（考虑拼团、凑单等）
        return plan;
    }
    
    // ========== 数据查询 ==========
    
    @Override
    public List<PriceTrendVO> getPriceTrend(String skuId, String platformCode, int days) {
        List<Map<String, Object>> trendData = priceComparisonMapper.selectPriceTrend(skuId, platformCode, days);
        
        return trendData.stream().map(data -> {
            PriceTrendVO vo = new PriceTrendVO();
            vo.setDate((Date) data.get("date"));
            vo.setPlatformCode((String) data.get("platformCode"));
            vo.setBasePrice((BigDecimal) data.get("basePrice"));
            vo.setActivityPrice((BigDecimal) data.get("activityPrice"));
            vo.setTotalCost((BigDecimal) data.get("totalCost"));
            vo.setActivityType((String) data.get("activityType"));
            vo.setCollectedAt((Date) data.get("collectedAt"));
            return vo;
        }).collect(Collectors.toList());
    }
    
    @Override
    public List<PlatformActivityVO> getPlatformActivities(String platformCode, boolean activeOnly) {
        List<YyPlatformActivity> activities;
        
        if (activeOnly) {
            activities = platformActivityMapper.selectActive(platformCode);
        } else {
            activities = platformActivityMapper.selectAll(platformCode);
        }
        
        return activities.stream().map(this::convertToActivityVO).collect(Collectors.toList());
    }
    
    @Override
    public PriceComparisonStatsVO getComparisonStats(String customerType) {
        Map<String, Object> stats = priceComparisonMapper.selectComparisonStats(customerType);
        
        PriceComparisonStatsVO vo = new PriceComparisonStatsVO();
        vo.setCustomerType(customerType);
        vo.setTotalProducts((Integer) stats.get("totalProducts"));
        vo.setPlatformCount((Integer) stats.get("platformCount"));
        vo.setAvgSavedAmount((BigDecimal) stats.get("avgSavedAmount"));
        
        return vo;
    }
    
    // ========== 数据采集 ==========
    
    @Override
    public Map<String, Object> collectPriceData(String platformCode, List<String> skuIds) {
        // TODO: 实现数据采集逻辑
        // 这里应该调用Chrome扩展或爬虫采集数据
        return Collections.emptyMap();
    }
    
    @Override
    @Transactional
    public Map<String, Object> importPriceComparisons(List<PriceComparisonVO> comparisonList) {
        int successCount = 0;
        int failCount = 0;
        
        for (PriceComparisonVO vo : comparisonList) {
            try {
                YyPriceComparison entity = convertToEntity(vo);
                entity.setCollectedAt(new Date());
                priceComparisonMapper.insertYyPriceComparison(entity);
                successCount++;
            } catch (Exception e) {
                log.error("导入比价数据失败: {}", vo.getSkuId(), e);
                failCount++;
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("totalCount", comparisonList.size());
        
        return result;
    }
    
    // ========== 私有方法 ==========
    
    private PriceComparisonVO convertToVO(YyPriceComparison entity) {
        PriceComparisonVO vo = new PriceComparisonVO();
        vo.setSkuId(entity.getSkuId());
        vo.setPlatformCode(entity.getSourcePlatform());
        vo.setBasePrice(entity.getBasePrice());
        vo.setCurrentPrice(entity.getCurrentPrice());
        vo.setActivityType(entity.getActivityType());
        vo.setActivityPrice(entity.getActivityPrice());
        vo.setActivityName(entity.getActivityName());
        vo.setMinOrderQty(entity.getMinOrderQty());
        vo.setStockQuantity(entity.getStockQuantity());
        vo.setFreightAmount(entity.getFreightAmount());
        vo.setFreeShippingThreshold(entity.getFreeShippingThreshold());
        vo.setDeliveryDays(entity.getDeliveryDays());
        vo.setCollectedAt(entity.getCollectedAt());
        vo.setValidUntil(entity.getValidUntil());
        
        // 计算到手价
        BigDecimal price = entity.getActivityPrice() != null ? entity.getActivityPrice() : entity.getCurrentPrice();
        BigDecimal freight = entity.getFreightAmount() != null ? entity.getFreightAmount() : BigDecimal.ZERO;
        vo.setTotalCost(price.add(freight));
        
        // 判断是否包邮
        boolean isFreeShipping = freight.compareTo(BigDecimal.ZERO) == 0 ||
            (entity.getFreeShippingThreshold() != null && price.compareTo(entity.getFreeShippingThreshold()) >= 0);
        vo.setIsFreeShipping(isFreeShipping);
        
        return vo;
    }
    
    private YyPriceComparison convertToEntity(PriceComparisonVO vo) {
        YyPriceComparison entity = new YyPriceComparison();
        entity.setSkuId(vo.getSkuId());
        entity.setSourcePlatform(vo.getPlatformCode());
        entity.setBasePrice(vo.getBasePrice());
        entity.setCurrentPrice(vo.getCurrentPrice());
        entity.setActivityType(vo.getActivityType());
        entity.setActivityPrice(vo.getActivityPrice());
        entity.setActivityName(vo.getActivityName());
        entity.setMinOrderQty(vo.getMinOrderQty());
        entity.setStockQuantity(vo.getStockQuantity());
        entity.setFreightAmount(vo.getFreightAmount());
        entity.setFreeShippingThreshold(vo.getFreeShippingThreshold());
        entity.setDeliveryDays(vo.getDeliveryDays());
        entity.setValidUntil(vo.getValidUntil());
        return entity;
    }
    
    private PlatformActivityVO convertToActivityVO(YyPlatformActivity entity) {
        PlatformActivityVO vo = new PlatformActivityVO();
        vo.setId(entity.getId());
        vo.setPlatformCode(entity.getPlatformCode());
        vo.setPlatformName(entity.getPlatformName());
        vo.setActivityCode(entity.getActivityCode());
        vo.setActivityName(entity.getActivityName());
        vo.setActivityType(entity.getActivityType());
        vo.setActivityDesc(entity.getActivityDesc());
        vo.setStartTime(entity.getStartTime());
        vo.setEndTime(entity.getEndTime());
        vo.setIsActive(entity.getIsActive());
        vo.setPriority(entity.getPriority());
        
        // 判断是否有效
        Date now = new Date();
        vo.setIsValid(entity.getStartTime().before(now) && entity.getEndTime().after(now));
        
        // 计算剩余天数
        long diff = entity.getEndTime().getTime() - now.getTime();
        vo.setRemainingDays(diff / (1000 * 60 * 60 * 24));
        
        return vo;
    }
    
    private BigDecimal calculateMemberPrice(BigDecimal basePrice, JSONObject rules, String customerType) {
        // 根据会员等级计算折扣
        BigDecimal discountRate = rules.getBigDecimal("level_1"); // 默认一级会员
        
        // TODO: 根据实际会员等级查询折扣率
        
        return basePrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateDiscountPrice(BigDecimal basePrice, JSONObject rules) {
        BigDecimal discountRate = rules.getBigDecimal("discount_rate");
        return basePrice.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
    }
}
