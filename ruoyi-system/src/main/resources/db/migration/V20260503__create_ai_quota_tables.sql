-- V20260503__create_ai_quota_tables.sql

CREATE TABLE IF NOT EXISTS yy_ai_quota_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tier_level INT NOT NULL COMMENT '会员等级 (0=regular, 1=gold, 2=platinum, 3=diamond)',
    daily_chat_limit INT DEFAULT 10 COMMENT '每日对话次数 (-1=无限)',
    daily_tool_limit INT DEFAULT 3 COMMENT '每日快捷功能次数 (-1=无限)',
    max_tokens_per_req INT DEFAULT 800 COMMENT '单次请求最大 token 数',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用 AI 功能',
    create_by VARCHAR(64) DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) DEFAULT '',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tier_level (tier_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 功能配额配置';

CREATE TABLE IF NOT EXISTS yy_ai_usage_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    usage_type VARCHAR(20) NOT NULL COMMENT 'chat/tool',
    tool_name VARCHAR(50) DEFAULT NULL COMMENT 'advisor/insight/drug_qa/recommend',
    tokens_used INT DEFAULT 0 COMMENT '消耗 token 数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_date (user_id, create_time),
    KEY idx_usage_type (usage_type, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 功能用量日志';

INSERT INTO yy_ai_quota_config (tier_level, daily_chat_limit, daily_tool_limit, max_tokens_per_req, enabled) VALUES
(0, 10, 3, 800, 1),
(1, 50, -1, 800, 1),
(2, -1, -1, 1200, 1),
(3, -1, -1, 1600, 1);
