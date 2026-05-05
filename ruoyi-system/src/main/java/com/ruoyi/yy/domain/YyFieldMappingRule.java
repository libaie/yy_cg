package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class YyFieldMappingRule extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long platformId;
    private String apiCode;
    private String standardField;
    private String sourcePaths;
    private String transformType;
    private String transformConfig;
    private String valueMap;
    private Integer isRequired;
    private String defaultValue;
    private String validation;
    private Integer sortOrder;
    private Integer isEnabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlatformId() { return platformId; }
    public void setPlatformId(Long platformId) { this.platformId = platformId; }
    public String getApiCode() { return apiCode; }
    public void setApiCode(String apiCode) { this.apiCode = apiCode; }
    public String getStandardField() { return standardField; }
    public void setStandardField(String standardField) { this.standardField = standardField; }
    public String getSourcePaths() { return sourcePaths; }
    public void setSourcePaths(String sourcePaths) { this.sourcePaths = sourcePaths; }
    public String getTransformType() { return transformType; }
    public void setTransformType(String transformType) { this.transformType = transformType; }
    public String getTransformConfig() { return transformConfig; }
    public void setTransformConfig(String transformConfig) { this.transformConfig = transformConfig; }
    public String getValueMap() { return valueMap; }
    public void setValueMap(String valueMap) { this.valueMap = valueMap; }
    public Integer getIsRequired() { return isRequired; }
    public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public String getValidation() { return validation; }
    public void setValidation(String validation) { this.validation = validation; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }
}
