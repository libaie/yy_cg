package com.ruoyi.yy.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 价格趋势VO
 */
@Data
public class PriceTrendVO {
    
    /** 日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date date;
    
    /** 平台编码 */
    private String platformCode;
    
    /** 基础价格 */
    private BigDecimal basePrice;
    
    /** 活动价格 */
    private BigDecimal activityPrice;
    
    /** 到手价（含运费） */
    private BigDecimal totalCost;
    
    /** 活动类型 */
    private String activityType;
    
    /** 采集时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date collectedAt;
}
