package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 平台活动对象 yy_platform_activity
 */
@Data
public class YyPlatformActivity {
    
    /** 主键ID */
    private Long id;
    
    /** 平台编码 */
    private String platformCode;
    
    /** 平台名称 */
    private String platformName;
    
    /** 活动编码（唯一） */
    private String activityCode;
    
    /** 活动名称 */
    private String activityName;
    
    /** 活动类型 */
    private String activityType;
    
    /** 活动描述 */
    private String activityDesc;
    
    /** 活动规则详情（JSON） */
    private String activityRules;
    
    /** 适用范围 */
    private String applyScope;
    
    /** 范围值（分类ID或商品ID列表） */
    private String scopeValue;
    
    /** 适用客户业态列表（JSON） */
    private String customerTypes;
    
    /** 活动开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    
    /** 活动结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    
    /** 状态：0禁用 1启用 */
    private Integer isActive;
    
    /** 优先级 */
    private Integer priority;
    
    /** 创建者 */
    private String createdBy;
    
    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    
    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
