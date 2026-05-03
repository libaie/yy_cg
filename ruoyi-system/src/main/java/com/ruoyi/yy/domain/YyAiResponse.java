package com.ruoyi.yy.domain;

/**
 * AI响应对象
 */
public class YyAiResponse {

    private boolean success;
    private String content;
    private String model;
    private int promptTokens;
    private int completionTokens;
    private long latencyMs;
    private String errorMessage;

    public YyAiResponse() {}

    public static YyAiResponse ok(String content, String model, int promptTokens, int completionTokens, long latencyMs) {
        YyAiResponse r = new YyAiResponse();
        r.success = true;
        r.content = content;
        r.model = model;
        r.promptTokens = promptTokens;
        r.completionTokens = completionTokens;
        r.latencyMs = latencyMs;
        return r;
    }

    public static YyAiResponse fail(String errorMessage) {
        YyAiResponse r = new YyAiResponse();
        r.success = false;
        r.errorMessage = errorMessage;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
