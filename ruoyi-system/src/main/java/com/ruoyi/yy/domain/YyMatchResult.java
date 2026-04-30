package com.ruoyi.yy.domain;

import com.ruoyi.yy.constant.MatchMethod;
import java.math.BigDecimal;

/**
 * 单个匹配策略的结果
 */
public class YyMatchResult {

    private boolean matched;
    private Long drugId;
    private String drugCode;
    private BigDecimal confidence;
    private MatchMethod matchMethod;
    private String reason;

    public YyMatchResult() {}

    public static YyMatchResult success(Long drugId, String drugCode, BigDecimal confidence, MatchMethod method, String reason) {
        YyMatchResult r = new YyMatchResult();
        r.matched = true;
        r.drugId = drugId;
        r.drugCode = drugCode;
        r.confidence = confidence;
        r.matchMethod = method;
        r.reason = reason;
        return r;
    }

    public static YyMatchResult failure(String reason) {
        YyMatchResult r = new YyMatchResult();
        r.matched = false;
        r.reason = reason;
        return r;
    }

    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public MatchMethod getMatchMethod() { return matchMethod; }
    public void setMatchMethod(MatchMethod matchMethod) { this.matchMethod = matchMethod; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
