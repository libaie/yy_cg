-- ============================================
-- AI配额配置表
-- ============================================
DROP TABLE IF EXISTS yy_ai_quota_config;
CREATE TABLE yy_ai_quota_config (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  tier_level INT DEFAULT 0 COMMENT '会员等级: 0=普通 1=黄金 2=铂金 3=钻石',
  daily_chat_limit INT DEFAULT 10 COMMENT '每日对话次数限制(-1=无限)',
  daily_tool_limit INT DEFAULT 5 COMMENT '每日快捷工具次数限制(-1=无限)',
  max_tokens_per_req INT DEFAULT 800 COMMENT '单次请求最大Token',
  enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_tier_level (tier_level)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI配额配置表';

-- 默认配额数据
INSERT INTO yy_ai_quota_config (tier_level, daily_chat_limit, daily_tool_limit, max_tokens_per_req, enabled) VALUES
(0, 5,  3,  800,  1),
(1, 15, 10, 1200,  1),
(2, 30, 20, 1200,  1),
(3, -1, -1, 1600,  1);

-- ============================================
-- AI使用日志表
-- ============================================
DROP TABLE IF EXISTS yy_ai_usage_log;
CREATE TABLE yy_ai_usage_log (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT(20) NOT NULL COMMENT '用户ID',
  usage_type VARCHAR(32) NOT NULL COMMENT '使用类型: chat/tool',
  tool_name VARCHAR(64) DEFAULT '' COMMENT '工具名称: advisor/insight/drug_qa/recommend',
  tokens_used INT DEFAULT 0 COMMENT '消耗Token数',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_type_time (user_id, usage_type, create_time),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI使用日志表';

-- ============================================
-- AI提示词模板表
-- ============================================
DROP TABLE IF EXISTS yy_ai_prompt_template;
CREATE TABLE yy_ai_prompt_template (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  template_code VARCHAR(64) NOT NULL COMMENT '模板编码',
  template_name VARCHAR(128) DEFAULT '' COMMENT '模板名称',
  scene VARCHAR(32) DEFAULT '' COMMENT '场景: advisor/insight/drug_qa/recommend/chat',
  system_prompt TEXT COMMENT '系统提示词',
  user_prompt_template TEXT COMMENT '用户提示词模板',
  model VARCHAR(64) DEFAULT 'deepseek-chat' COMMENT '推荐模型',
  temperature DOUBLE DEFAULT 0.7 COMMENT '温度参数',
  max_tokens INT DEFAULT 800 COMMENT '最大Token',
  is_enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_template_code (template_code)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI提示词模板表';
