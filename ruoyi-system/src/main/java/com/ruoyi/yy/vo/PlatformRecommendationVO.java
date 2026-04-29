package com.ruoyi.yy.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台推荐VO
 */
@Data
public class PlatformRecommendationVO {
    
    /** SKU ID */
    private String skuId;
    
    /** 商品名称 */
    private String productName;
    
    /** 推荐平台编码 */
    private String recommendedPlatform;
    
    /** 推荐平台名称 */
    private String recommendedPlatformName;
    
    /** 推荐价格 */
    private BigDecimal recommendedPrice;
    
    /** 推荐到手价 */
    private BigDecimal totalCost;
    
    /** 推荐原因 */
    private String reason;
    
    /** 节省金额 */
    private BigDecimal savedAmount;
    
    /** 节省百分比 */
    private BigDecimal savedPercent;
    
    /** 所有平台比价结果 */
    private List<PriceComparisonVO> allPlatforms;
    
    /** 最优活动 */
    private String bestActivity;
    
    /** 建议订购量 */
    private Integer suggestedOrderQty;
}
