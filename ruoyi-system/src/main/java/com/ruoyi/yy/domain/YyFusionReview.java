package com.ruoyi.yy.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 融合审核队列 yy_fusion_review
 */
public class YyFusionReview extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long snapshotId;
    private String candidateDrugIds;   // JSON array
    private String matchScores;        // JSON array
    private String aiSuggestion;
    private String status;             // pending/approved/rejected
    private Long reviewerId;
    private String reviewNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reviewedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }
    public String getCandidateDrugIds() { return candidateDrugIds; }
    public void setCandidateDrugIds(String candidateDrugIds) { this.candidateDrugIds = candidateDrugIds; }
    public String getMatchScores() { return matchScores; }
    public void setMatchScores(String matchScores) { this.matchScores = matchScores; }
    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
}
