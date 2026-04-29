package com.ruoyi.yy.domain;

import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 平台字段映射配置对象 yy_field_mapping
 */
public class YyFieldMapping extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long platformId;

    @Excel(name = "标准字段名")
    private String standardField;

    @Excel(name = "平台字段名")
    private String platformField;

    @Excel(name = "数据类型")
    private String fieldType;

    @Excel(name = "是否必填")
    private Integer isRequired;

    @Excel(name = "排序")
    private Integer sortOrder;

    @Excel(name = "状态")
    private Integer status;

    /** 备注 */
    @Excel(name = "备注")
    private String remark;

    /** 数据入口路径，如 data.wholesales */
    @Excel(name = "数据入口路径")
    private String entryPath;

    /** 非数据库字段：平台名称（用于列表显示） */
    private String platformName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlatformId() { return platformId; }
    public void setPlatformId(Long platformId) { this.platformId = platformId; }

    public String getStandardField() { return standardField; }
    public void setStandardField(String standardField) { this.standardField = standardField; }

    public String getPlatformField() { return platformField; }
    public void setPlatformField(String platformField) { this.platformField = platformField; }

    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }

    public Integer getIsRequired() { return isRequired; }
    public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getPlatformName() { return platformName; }
    public void setPlatformName(String platformName) { this.platformName = platformName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getEntryPath() { return entryPath; }
    public void setEntryPath(String entryPath) { this.entryPath = entryPath; }
}
