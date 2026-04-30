package com.ruoyi.yy.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * 融合引擎的最终结果
 */
public class YyFusionResult {

    private boolean matched;
    private Long drugId;
    private String drugCode;
    private BigDecimal confidence;
    private String matchMethod;
    private String reason;
    private boolean needsReview;
    private List<YyCandidateDrug> candidateDrugs;

    public YyFusionResult() {}

    public static YyFusionResult matched(Long drugId, String drugCode, BigDecimal confidence,
                                        String matchMethod, String reason, boolean needsReview) {
        YyFusionResult r = new YyFusionResult();
        r.matched = true;
        r.drugId = drugId;
        r.drugCode = drugCode;
        r.confidence = confidence;
        r.matchMethod = matchMethod;
        r.reason = reason;
        r.needsReview = needsReview;
        return r;
    }

    public static YyFusionResult noMatch(List<YyCandidateDrug> candidateDrugs) {
        YyFusionResult r = new YyFusionResult();
        r.matched = false;
        r.candidateDrugs = candidateDrugs;
        return r;
    }

    // Getters and setters
    public boolean isMatched() { return matched; }
    public void setMatched(boolean matched) { this.matched = matched; }
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public String getMatchMethod() { return matchMethod; }
    public void setMatchMethod(String matchMethod) { this.matchMethod = matchMethod; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public boolean isNeedsReview() { return needsReview; }
    public void setNeedsReview(boolean needsReview) { this.needsReview = needsReview; }
    public List<YyCandidateDrug> getCandidateDrugs() { return candidateDrugs; }
    public void setCandidateDrugs(List<YyCandidateDrug> candidateDrugs) { this.candidateDrugs = candidateDrugs; }
}
