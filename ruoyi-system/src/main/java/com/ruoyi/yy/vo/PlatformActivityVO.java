package com.ruoyi.yy.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 平台活动VO
 */
@Data
public class PlatformActivityVO {
    
    /** 活动ID */
    private Long id;
    
    /** 平台编码 */
    private String platformCode;
    
    /** 平台名称 */
    private String platformName;
    
    /** 活动编码 */
    private String activityCode;
    
    /** 活动名称 */
    private String activityName;
    
    /** 活动类型 */
    private String activityType;
    
    /** 活动描述 */
    private String activityDesc;
    
    /** 活动规则 */
    private Map<String, Object> activityRules;
    
    /** 适用范围 */
    private String applyScope;
    
    /** 适用客户业态 */
    private List<String> customerTypes;
    
    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    /** 状态 */
    private Integer isActive;
    
    /** 优先级 */
    private Integer priority;
    
    /** 是否有效（时间范围内） */
    private Boolean isValid;
    
    /** 剩余天数 */
    private Long remainingDays;
}
