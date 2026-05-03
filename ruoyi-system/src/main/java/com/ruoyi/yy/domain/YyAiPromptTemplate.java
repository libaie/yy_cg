package com.ruoyi.yy.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;

/**
 * AI Prompt模板 yy_ai_prompt_template
 */
public class YyAiPromptTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String templateCode;
    private String templateName;
    private String scene;
    private String systemPrompt;
    private String userPromptTemplate;
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserPromptTemplate() { return userPromptTemplate; }
    public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
