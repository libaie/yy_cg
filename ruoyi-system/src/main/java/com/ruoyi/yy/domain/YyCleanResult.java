package com.ruoyi.yy.domain;

import java.util.List;
import java.util.Map;

/**
 * 数据清洗结果
 */
public class YyCleanResult {

    private int totalProcessed;
    private int autoFixed;
    private int needsReview;
    private List<Map<String, Object>> suggestions;

    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }
    public int getAutoFixed() { return autoFixed; }
    public void setAutoFixed(int autoFixed) { this.autoFixed = autoFixed; }
    public int getNeedsReview() { return needsReview; }
    public void setNeedsReview(int needsReview) { this.needsReview = needsReview; }
    public List<Map<String, Object>> getSuggestions() { return suggestions; }
    public void setSuggestions(List<Map<String, Object>> suggestions) { this.suggestions = suggestions; }
}
