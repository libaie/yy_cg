package com.ruoyi.yy.domain;

import java.util.Map;

/**
 * AI请求对象
 */
public class YyAiRequest {

    private String scene;           // match/advisor/evaluator/search/cleaner
    private String systemPrompt;
    private String userPrompt;
    private String model;           // qwen-turbo/qwen-plus/qwen-max
    private double temperature;
    private int maxTokens;
    private Map<String, Object> extra;

    public YyAiRequest() {
        this.model = "qwen-turbo";
        this.temperature = 0.1;
        this.maxTokens = 1000;
    }

    public YyAiRequest(String scene, String userPrompt) {
        this();
        this.scene = scene;
        this.userPrompt = userPrompt;
    }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getUserPrompt() { return userPrompt; }
    public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public Map<String, Object> getExtra() { return extra; }
    public void setExtra(Map<String, Object> extra) { this.extra = extra; }
}
