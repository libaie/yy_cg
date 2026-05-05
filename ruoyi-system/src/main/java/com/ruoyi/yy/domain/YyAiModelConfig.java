package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;

public class YyAiModelConfig extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String modelCode;
    private String modelName;
    private String provider;
    private String endpoint;
    private String apiKey;
    private String capabilities;
    private Integer isMultimodal;
    private Integer maxTokens;
    private Integer isEnabled;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModelCode() { return modelCode; }
    public void setModelCode(String modelCode) { this.modelCode = modelCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getCapabilities() { return capabilities; }
    public void setCapabilities(String capabilities) { this.capabilities = capabilities; }
    public Integer getIsMultimodal() { return isMultimodal; }
    public void setIsMultimodal(Integer isMultimodal) { this.isMultimodal = isMultimodal; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
