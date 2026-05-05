package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

import lombok.Data;

/**
 * 标准商品对象 yy_standard_product v2.0
 *
 * @author fdAgent
 */
@Data
public class YyStandardProduct extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    // ========== 核心标识 ==========
    
    /** 商品ID（平台内部统一标识） */
    private String productId;
    
    /** SKU ID（具体规格商品标识） */
    private String skuId;
    
    /** 数据来源平台 */
    private String sourcePlatform;

    /** 数据来源API: hot/search/flash_kill */
    private String sourceApi;

    // ========== 溯源 ==========
    
    /** 原始数据快照 */
    private String rawDataPayload;

    // ========== 基础信息 ==========
    
    /** 商品条码（如69码） */
    private String barcode;
    
    /** 商品名称（主标题） */
    private String productName;
    
    /** 通用名称（药品法定名） */
    private String commonName;
    
    /** 品牌名称 */
    private String brandName;
    
    /** 生产厂家 */
    private String manufacturer;
    
    /** 批准文号 */
    private String approvalNumber;

    // ========== 分类 ==========
    
    /** 分类ID */
    private String categoryId;
    
    /** 分类名称 */
    private String categoryName;

    // ========== 规格 ==========
    
    /** 商品规格 */
    private String specification;
    
    /** 销售单位 */
    private String unit;
    
    /** 中包装/件装量 */
    private String packingRatio;

    // ========== 状态与库存 ==========
    
    /** 商品状态（1上架 0下架） */
    private String productStatus;
    
    /** 总库存量 */
    private Integer stockQuantity;
    
    /** 分仓库存列表 */
    private String warehouseStock;

    // ========== 图片 ==========
    
    /** 商品主图URL列表 */
    private String mainImages;

    // ========== 限购 ==========
    
    /** 最小起订量 */
    private Integer minOrderQty;
    
    /** 最大订购量 */
    private Integer maxOrderQty;

    // ========== 日期 ==========
    
    /** 生产日期 */
    private String productionDate;
    
    /** 有效期至 */
    private String expirationDate;
    
    /** 保质期 */
    private String shelfLife;

    // ========== 医药专属 ==========
    
    /** 是否处方药（0否 1是） */
    private Integer isPrescriptionDrug;
    
    /** 医保类型 */
    private String medicareType;
    
    /** 追溯码状态（0无 1有） */
    private Integer traceabilityCodeStatus;

    // ========== 销售 ==========
    
    /** 销量 */
    private Integer salesVolume;
    
    /** 店铺/供应商名称 */
    private String shopName;

    // ========== 价格 ==========
    
    /** 建议零售价/标价 */
    private BigDecimal priceRetail;
    
    /** 当前基础供货价 */
    private BigDecimal priceCurrent;
    
    /** 阶梯价规则列表 */
    private String priceStepRules;
    
    /** 拼团/活动底价 */
    private BigDecimal priceAssemble;

    // ========== 物流与税务 ==========
    
    /** 是否含税（0否 1是） */
    private Integer isTaxIncluded;
    
    /** 基础运费 */
    private BigDecimal freightAmount;
    
    /** 免邮门槛 */
    private BigDecimal freeShippingThreshold;

    // ========== 标签与活动 ==========
    
    /** 商品标签 */
    private String tags;
    
    /** 营销短标签 */
    private String marketingTags;
    
    /** 复杂活动明细列表 */
    private String activityDetails;
    
    /** 限购与起批规则 */
    private String purchaseLimits;

    // ========== 融合相关 ==========
    
    /** 所属融合分组ID */
    private Long fusionGroupId;
    
    /** 融合键 */
    private String fusionKey;

    // ========== 降级标记 ==========

    /**
     * 降级标记：true 表示该数据来自缓存回退而非实时采集。
     * transient —— 不持久化到数据库，仅用于向前端传递信号。
     */
    private transient boolean degraded = false;

    // ========== 时间 ==========

    /** 采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;

    /** 同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncedAt;
}
