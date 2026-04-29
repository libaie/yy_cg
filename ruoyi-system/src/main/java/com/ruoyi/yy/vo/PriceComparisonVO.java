package com.ruoyi.yy.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 比价结果VO
 */
@Data
public class PriceComparisonVO {
    
    // ========== 商品信息 ==========
    /** SKU ID */
    private String skuId;
    
    /** 商品名称 */
    private String productName;
    
    /** 通用名 */
    private String commonName;
    
    /** 规格 */
    private String specification;
    
    /** 生产厂家 */
    private String manufacturer;
    
    // ========== 平台信息 ==========
    /** 平台编码 */
    private String platformCode;
    
    /** 平台名称 */
    private String platformName;
    
    // ========== 价格信息 ==========
    /** 基础价格 */
    private BigDecimal basePrice;
    
    /** 当前售价 */
    private BigDecimal currentPrice;
    
    /** 活动类型 */
    private String activityType;
    
    /** 活动价格 */
    private BigDecimal activityPrice;
    
    /** 活动名称 */
    private String activityName;
    
    /** 活动规则详情 */
    private Map<String, Object> activityRules;
    
    // ========== 采购条件 ==========
    /** 最小起订量 */
    private Integer minOrderQty;
    
    /** 可用库存 */
    private Integer stockQuantity;
    
    // ========== 物流信息 ==========
    /** 运费 */
    private BigDecimal freightAmount;
    
    /** 免邮门槛 */
    private BigDecimal freeShippingThreshold;
    
    /** 配送天数 */
    private Integer deliveryDays;
    
    // ========== 计算结果 ==========
    /** 到手价（含运费） */
    private BigDecimal totalCost;
    
    /** 是否包邮 */
    private Boolean isFreeShipping;
    
    /** 节省金额（与最高价对比） */
    private BigDecimal savedAmount;
    
    /** 节省百分比 */
    private BigDecimal savedPercent;
    
    /** 是否最优价格 */
    private Boolean isBestPrice;
    
    /** 排名 */
    private Integer rank;
    
    // ========== 时间信息 ==========
    /** 采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;
    
    /** 价格有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validUntil;
}
