package com.ruoyi.yy.domain;

import java.math.BigDecimal;

/**
 * 候选药品，绑定 drugId 和匹配分数，避免并行列表索引不一致的风险。
 */
public class YyCandidateDrug {

    private Long drugId;
    private BigDecimal score;

    public YyCandidateDrug() {}

    public YyCandidateDrug(Long drugId, BigDecimal score) {
        this.drugId = drugId;
        this.score = score;
    }

    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}
