package com.ruoyi.yy.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 比价统计VO
 */
@Data
public class PriceComparisonStatsVO {
    
    /** 客户业态 */
    private String customerType;
    
    // ========== 商品统计 ==========
    /** 比价商品总数 */
    private Integer totalProducts;
    
    /** 有活动的商品数 */
    private Integer productWithActivity;
    
    /** 活动商品占比 */
    private BigDecimal activityPercent;
    
    // ========== 平台统计 ==========
    /** 覆盖平台数 */
    private Integer platformCount;
    
    /** 平台价格分布 */
    private Map<String, PlatformStats> platformStats;
    
    // ========== 优惠统计 ==========
    /** 平均节省金额 */
    private BigDecimal avgSavedAmount;
    
    /** 平均节省百分比 */
    private BigDecimal avgSavedPercent;
    
    /** 最大节省金额 */
    private BigDecimal maxSavedAmount;
    
    // ========== 活动统计 ==========
    /** 有效活动数 */
    private Integer activeActivityCount;
    
    /** 活动类型分布 */
    private Map<String, Integer> activityTypeDistribution;
    
    // ========== 排行榜 ==========
    /** 最省钱商品TOP10 */
    private List<SavedProductRank> topSavedProducts;
    
    /** 最优平台TOP3 */
    private List<PlatformRank> topPlatforms;
    
    /**
     * 平台统计
     */
    @Data
    public static class PlatformStats {
        private String platformCode;
        private String platformName;
        private Integer productCount;
        private BigDecimal avgPrice;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private Integer activityCount;
    }
    
    /**
     * 节省商品排行
     */
    @Data
    public static class SavedProductRank {
        private String skuId;
        private String productName;
        private BigDecimal savedAmount;
        private BigDecimal savedPercent;
        private String bestPlatform;
    }
    
    /**
     * 平台排行
     */
    @Data
    public static class PlatformRank {
        private String platformCode;
        private String platformName;
        private Integer winCount; // 最优价格次数
        private BigDecimal avgSavedPercent;
    }
}
