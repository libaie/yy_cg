DROP TABLE IF EXISTS yy_user_event;
CREATE TABLE yy_user_event (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  user_id BIGINT(20) DEFAULT NULL COMMENT '用户ID，NULL=未登录',
  event_type VARCHAR(64) NOT NULL COMMENT 'page_view/search/compare/ai_chat/subscribe_click/register/login',
  event_data JSON DEFAULT NULL COMMENT '事件附加数据',
  session_id VARCHAR(64) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(512) DEFAULT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_event_type (event_type, created_at),
  KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为事件';
