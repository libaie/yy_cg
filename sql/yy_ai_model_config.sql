-- AI模型配置表
DROP TABLE IF EXISTS yy_ai_model_config;
CREATE TABLE yy_ai_model_config (
  id BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  model_code VARCHAR(64) NOT NULL COMMENT '模型标识，如 deepseek-chat',
  model_name VARCHAR(128) DEFAULT '' COMMENT '模型名称，如 DeepSeek Chat',
  provider VARCHAR(32) DEFAULT 'custom' COMMENT '供应商: deepseek/openai/dashscope/custom',
  endpoint VARCHAR(512) NOT NULL COMMENT 'API地址',
  api_key VARCHAR(256) DEFAULT '' COMMENT 'API密钥',
  capabilities VARCHAR(256) DEFAULT 'text' COMMENT '能力标签，逗号分隔: text,image,vision,video,audio',
  is_multimodal TINYINT(1) DEFAULT 0 COMMENT '是否多模态',
  max_tokens INT DEFAULT 4096 COMMENT '最大Token数',
  is_enabled TINYINT(1) DEFAULT 1 COMMENT '启用状态',
  sort_order INT DEFAULT 0 COMMENT '排序',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  UNIQUE KEY uk_model_code (model_code)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- 初始数据
INSERT INTO yy_ai_model_config (model_code, model_name, provider, endpoint, capabilities, is_multimodal, max_tokens, is_enabled, sort_order) VALUES
('deepseek-chat',      'DeepSeek Chat',     'deepseek',  'https://api.deepseek.com/v1/chat/completions',      'text',              0,  8192,  1, 0),
('deepseek-reasoner',  'DeepSeek Reasoner', 'deepseek',  'https://api.deepseek.com/v1/chat/completions',      'text',              0, 65536,  1, 1);

-- 菜单数据 (如有需要，取消注释并调整 parent_id)
-- INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, perms, menu_type, visible, status)
-- VALUES ('AI模型配置', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE path='yy') AS t), 10,
--         'aiModel', 'yy/aiModel/index', 'yy:admin:ai:quota', 'C', '0', '0');
