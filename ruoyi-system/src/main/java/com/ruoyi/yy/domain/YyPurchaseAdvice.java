package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AI采购建议
 */
public class YyPurchaseAdvice {

    private String summary;
    private String bestPlatform;
    private BigDecimal bestPrice;
    private BigDecimal totalSaving;
    private List<String> tips;
    private Map<String, Object> detail;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getBestPlatform() { return bestPlatform; }
    public void setBestPlatform(String bestPlatform) { this.bestPlatform = bestPlatform; }
    public BigDecimal getBestPrice() { return bestPrice; }
    public void setBestPrice(BigDecimal bestPrice) { this.bestPrice = bestPrice; }
    public BigDecimal getTotalSaving() { return totalSaving; }
    public void setTotalSaving(BigDecimal totalSaving) { this.totalSaving = totalSaving; }
    public List<String> getTips() { return tips; }
    public void setTips(List<String> tips) { this.tips = tips; }
    public Map<String, Object> getDetail() { return detail; }
    public void setDetail(Map<String, Object> detail) { this.detail = detail; }
}
