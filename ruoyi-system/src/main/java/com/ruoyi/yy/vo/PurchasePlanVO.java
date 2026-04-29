package com.ruoyi.yy.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 采购方案VO
 */
@Data
public class PurchasePlanVO {
    
    /** 方案ID */
    private String planId;
    
    /** 客户业态 */
    private String customerType;
    
    /** 采购商品列表 */
    private List<PurchaseItem> items;
    
    /** 分平台采购详情 */
    private Map<String, List<PurchaseItem>> platformItems;
    
    // ========== 金额汇总 ==========
    /** 商品总金额 */
    private BigDecimal totalProductAmount;
    
    /** 总运费 */
    private BigDecimal totalFreightAmount;
    
    /** 活动优惠总额 */
    private BigDecimal totalDiscountAmount;
    
    /** 最终总金额 */
    private BigDecimal finalAmount;
    
    /** 节省总金额 */
    private BigDecimal totalSavedAmount;
    
    /** 节省百分比 */
    private BigDecimal savedPercent;
    
    // ========== 建议 ==========
    /** 推荐采购平台组合 */
    private List<String> recommendedPlatforms;
    
    /** 优化建议 */
    private List<String> optimizationTips;
    
    /** 拼团建议 */
    private GroupBuySuggestion groupBuySuggestion;
    
    /** 凑单建议 */
    private FullReductionSuggestion fullReductionSuggestion;
    
    /**
     * 采购商品明细
     */
    @Data
    public static class PurchaseItem {
        /** SKU ID */
        private String skuId;
        
        /** 商品名称 */
        private String productName;
        
        /** 平台编码 */
        private String platformCode;
        
        /** 平台名称 */
        private String platformName;
        
        /** 单价 */
        private BigDecimal unitPrice;
        
        /** 数量 */
        private Integer quantity;
        
        /** 小计 */
        private BigDecimal subtotal;
        
        /** 活动类型 */
        private String activityType;
        
        /** 运费 */
        private BigDecimal freight;
        
        /** 配送天数 */
        private Integer deliveryDays;
    }
    
    /**
     * 拼团建议
     */
    @Data
    public static class GroupBuySuggestion {
        /** 是否可拼团 */
        private Boolean available;
        
        /** 拼团商品 */
        private List<String> skuIds;
        
        /** 拼团平台 */
        private String platformCode;
        
        /** 需要人数 */
        private Integer requiredGroupSize;
        
        /** 当前人数 */
        private Integer currentGroupSize;
        
        /** 拼团节省金额 */
        private BigDecimal savedAmount;
    }
    
    /**
     * 凑单建议
     */
    @Data
    public static class FullReductionSuggestion {
        /** 是否可凑单 */
        private Boolean available;
        
        /** 当前金额 */
        private BigDecimal currentAmount;
        
        /** 凑单门槛 */
        private BigDecimal threshold;
        
        /** 还需金额 */
        private BigDecimal amountNeeded;
        
        /** 优惠金额 */
        private BigDecimal discountAmount;
        
        /** 建议凑单商品 */
        private List<String> suggestedSkuIds;
    }
}
