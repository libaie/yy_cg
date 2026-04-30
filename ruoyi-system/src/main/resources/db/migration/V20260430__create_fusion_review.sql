CREATE TABLE IF NOT EXISTS yy_fusion_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    snapshot_id BIGINT NOT NULL COMMENT '待匹配的商品快照ID',
    candidate_drug_ids JSON DEFAULT NULL COMMENT '候选药品ID列表',
    match_scores JSON DEFAULT NULL COMMENT '各候选的匹配分数',
    ai_suggestion VARCHAR(500) DEFAULT NULL COMMENT 'AI建议',
    status VARCHAR(20) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    reviewer_id BIGINT DEFAULT NULL COMMENT '审核人',
    review_note VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    reviewed_at DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='融合审核队列';
