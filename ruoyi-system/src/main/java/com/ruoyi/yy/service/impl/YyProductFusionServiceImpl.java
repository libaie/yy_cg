package com.ruoyi.yy.service.impl;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.yy.domain.YyFusionResult;
import com.ruoyi.yy.domain.YyProductFusionGroup;
import com.ruoyi.yy.domain.YyProductSnapshot;
import com.ruoyi.yy.domain.YyStandardProduct;
import com.ruoyi.yy.dto.MappedProductDTO;
import com.ruoyi.yy.mapper.YyStandardProductMapper;
import com.ruoyi.yy.service.IYyProductFusionGroupService;
import com.ruoyi.yy.service.IYyProductFusionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 商品融合服务实现
 *
 * 负责融合键生成、融合分组管理、标准商品入库以及融合分组的聚合信息更新。
 * 从 DataFusionServiceImpl 中提取，使得融合逻辑可以独立复用。
 *
 * @author fdAgent
 */
@Service
public class YyProductFusionServiceImpl implements IYyProductFusionService {

    private static final Logger log = LoggerFactory.getLogger(YyProductFusionServiceImpl.class);

    @Autowired
    private IYyProductFusionGroupService fusionGroupService;

    @Autowired
    private YyFusionEngineImpl fusionEngine;

    @Autowired
    private YyStandardProductMapper standardProductMapper;

    // ========== Fuse (delegate to fusion engine) ==========

    @Override
    public YyFusionResult fuse(YyProductSnapshot snapshot) {
        return fusionEngine.fuse(snapshot);
    }

    // ========== Get-or-create group + upsert standard product ==========

    @Override
    public YyProductFusionGroup getOrCreateGroup(MappedProductDTO dto, String platformCode) {
        // 1. 生成融合键
        String fusionKey = generateFusionKey(dto);

        // 2. 查询已有分组
        YyProductFusionGroup group = fusionGroupService.selectByFusionKey(fusionKey);
        if (group == null) {
            // 3. 创建新分组
            group = new YyProductFusionGroup();
            group.setFusionKey(fusionKey);
            group.setCommonName(dto.getCommonName());
            group.setSpecification(dto.getSpecification());
            group.setManufacturer(dto.getManufacturer());
            group.setApprovalNumber(dto.getApprovalNumber());
            group.setStatus(1);
            fusionGroupService.insertYyProductFusionGroup(group);
        }

        // 4. 构建并 upsert 标准商品
        YyStandardProduct product = buildStandardProduct(dto, platformCode, fusionKey, group.getId());
        standardProductMapper.upsertYyStandardProduct(product);

        return group;
    }

    // ========== Update aggregation ==========

    @Override
    public void updateAggregation(Long groupId) {
        List<YyStandardProduct> products = standardProductMapper.selectByFusionGroupId(groupId);
        if (products.isEmpty()) {
            return;
        }

        YyProductFusionGroup group = fusionGroupService.selectYyProductFusionGroupById(groupId);
        if (group == null) {
            return;
        }

        // 计算平台数
        int platformCount = (int) products.stream()
                .map(YyStandardProduct::getSourcePlatform)
                .filter(Objects::nonNull)
                .distinct().count();
        group.setPlatformCount(platformCount);

        // 计算最低价
        BigDecimal minPrice = products.stream()
                .map(YyStandardProduct::getPriceCurrent)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        group.setMinPrice(minPrice);

        // 计算总库存
        int totalStock = products.stream()
                .mapToInt(p -> p.getStockQuantity() != null ? p.getStockQuantity() : 0)
                .sum();
        group.setTotalStock(totalStock);

        fusionGroupService.updateYyProductFusionGroup(group);
    }

    // ========== Private helpers ==========

    /**
     * 生成融合键：MD5(通用名 + 规格 + 厂家 + 批准文号)
     */
    private String generateFusionKey(MappedProductDTO dto) {
        String raw = normalizeForFusionKey(dto.getCommonName())
                + "|" + normalizeForFusionKey(dto.getSpecification())
                + "|" + normalizeForFusionKey(dto.getManufacturer())
                + "|" + Optional.ofNullable(dto.getApprovalNumber()).orElse("");

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate fusion key for: " + raw, e);
        }
    }

    /**
     * 融合键字段标准化
     */
    private String normalizeForFusionKey(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String s = value.trim();
        s = s.replace("×", "*").replace("x", "*").replace("X", "*");
        s = s.replaceAll("(有限公司|有限责任公司|股份公司|股份有限公司|集团|药业|制药|医药)$", "");
        s = s.replaceAll("[()（）\\[\\]【】]", "");
        s = s.replaceAll("\\s+", "");
        return s;
    }

    /**
     * 从 MappedProductDTO 构建 YyStandardProduct
     */
    private YyStandardProduct buildStandardProduct(MappedProductDTO dto, String platformCode,
                                                    String fusionKey, Long fusionGroupId) {
        YyStandardProduct product = new YyStandardProduct();

        // 核心标识
        product.setProductId(dto.getProductId());
        product.setSkuId(dto.getSkuId());
        product.setSourcePlatform(platformCode);
        product.setSourceApi(dto.getSourceApi());

        // 溯源
        product.setRawDataPayload(dto.getRawDataPayload());

        // 基础信息
        product.setBarcode(dto.getBarcode());
        product.setProductName(dto.getProductName());
        product.setCommonName(dto.getCommonName());
        product.setBrandName(dto.getBrandName());
        product.setManufacturer(dto.getManufacturer());
        product.setApprovalNumber(dto.getApprovalNumber());

        // 分类
        product.setCategoryId(dto.getCategoryId());
        product.setCategoryName(dto.getCategoryName());

        // 规格
        product.setSpecification(dto.getSpecification());
        product.setUnit(dto.getUnit());
        product.setPackingRatio(dto.getPackingRatio());

        // 状态与库存
        product.setProductStatus(dto.getProductStatus());
        product.setStockQuantity(dto.getStockQuantity());
        product.setWarehouseStock(dto.getWarehouseStock());

        // 图片
        product.setMainImages(dto.getMainImages());

        // 限购
        product.setMinOrderQty(dto.getMinOrderQty());
        product.setMaxOrderQty(dto.getMaxOrderQty());

        // 日期 (MappedProductDTO uses Date, YyStandardProduct uses String)
        product.setProductionDate(formatDate(dto.getProductionDate()));
        product.setExpirationDate(formatDate(dto.getExpirationDate()));
        product.setShelfLife(dto.getShelfLife());

        // 医药专属
        product.setIsPrescriptionDrug(dto.getIsPrescriptionDrug());
        product.setMedicareType(dto.getMedicareType());
        product.setTraceabilityCodeStatus(dto.getTraceabilityCodeStatus());

        // 销售
        product.setSalesVolume(dto.getSalesVolume());
        product.setShopName(dto.getShopName());

        // 价格
        product.setPriceRetail(dto.getPriceRetail());
        product.setPriceCurrent(dto.getPriceCurrent());
        product.setPriceStepRules(dto.getPriceStepRules());
        product.setPriceAssemble(dto.getPriceAssemble());

        // 物流与税务
        product.setIsTaxIncluded(dto.getIsTaxIncluded());
        product.setFreightAmount(dto.getFreightAmount());
        product.setFreeShippingThreshold(dto.getFreeShippingThreshold());

        // 标签与活动 (MappedProductDTO uses List<String>, YyStandardProduct uses String)
        product.setTags(jsonify(dto.getTags()));
        product.setMarketingTags(jsonify(dto.getMarketingTags()));
        product.setActivityDetails(dto.getActivityDetails());
        product.setPurchaseLimits(dto.getPurchaseLimits());

        // 融合相关
        product.setFusionGroupId(fusionGroupId);
        product.setFusionKey(fusionKey);

        // 时间
        if (dto.getCollectedAt() != null) {
            product.setCollectedAt(dto.getCollectedAt());
        } else {
            product.setCollectedAt(new Date());
        }
        product.setSyncedAt(dto.getSyncedAt());

        return product;
    }

    /**
     * 将 List 序列化为 JSON 数组字符串，若为 null 则返回 null
     */
    private String jsonify(List<String> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(items);
    }

    /**
     * 将 Date 格式化为 "yyyy-MM-dd" 字符串，若为 null 则返回 null
     */
    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }
}
