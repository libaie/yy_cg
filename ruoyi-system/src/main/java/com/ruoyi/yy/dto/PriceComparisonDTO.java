package com.ruoyi.yy.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 比价查询DTO
 */
@Data
public class PriceComparisonDTO {
    
    /** SKU ID */
    private String skuId;
    
    /** 商品名称（模糊搜索） */
    private String productName;
    
    /** 通用名 */
    private String commonName;
    
    /** 条码 */
    private String barcode;
    
    /** 批准文号 */
    private String approvalNumber;
    
    /** 平台编码列表 */
    private List<String> platformCodes;
    
    /** 客户业态：single/chain/clinic/wholesale */
    private String customerType;
    
    /** 区域编码 */
    private String regionCode;
    
    /** 是否只看有活动的商品 */
    private Boolean hasActivity;
    
    /** 价格区间-最低价 */
    private BigDecimal minPrice;
    
    /** 价格区间-最高价 */
    private BigDecimal maxPrice;
    
    /** 排序字段：price/delivery_days/saved_amount */
    private String sortBy;
    
    /** 排序方向：asc/desc */
    private String sortOrder;
    
    /** 页码 */
    private Integer pageNum;
    
    /** 每页数量 */
    private Integer pageSize;
}
