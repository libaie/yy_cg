package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 比价记录对象 yy_price_comparison
 */
@Data
public class YyPriceComparison {
    
    /** 主键ID */
    private Long id;
    
    /** 商品ID */
    private String productId;
    
    /** SKU ID */
    private String skuId;
    
    /** 数据来源平台 */
    private String sourcePlatform;
    
    /** 基础价格（原价） */
    private BigDecimal basePrice;
    
    /** 当前售价 */
    private BigDecimal currentPrice;
    
    /** 活动类型 */
    private String activityType;
    
    /** 活动价格 */
    private BigDecimal activityPrice;
    
    /** 活动名称 */
    private String activityName;
    
    /** 活动规则（JSON） */
    private String activityRules;
    
    /** 最小起订量 */
    private Integer minOrderQty;
    
    /** 最大订购量 */
    private Integer maxOrderQty;
    
    /** 可用库存 */
    private Integer stockQuantity;
    
    /** 运费 */
    private BigDecimal freightAmount;
    
    /** 免邮门槛 */
    private BigDecimal freeShippingThreshold;
    
    /** 预计配送天数 */
    private Integer deliveryDays;
    
    /** 配送区域 */
    private String deliveryArea;
    
    /** 客户业态 */
    private String customerType;
    
    /** 价格生效开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validFrom;
    
    /** 价格生效结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date validUntil;
    
    /** 原始数据快照（JSON） */
    private String rawData;
    
    /** 采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;
    
    /** 同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncedAt;

    /** 规格（非持久化，AI分析用） */
    private transient String specification;

    /** 厂家（非持久化，AI分析用） */
    private transient String manufacturer;
}
