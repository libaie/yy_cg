-- 字段映射规则引擎 V2
DROP TABLE IF EXISTS yy_field_mapping_rule;
CREATE TABLE yy_field_mapping_rule (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  platform_id BIGINT(20) NOT NULL COMMENT '平台ID',
  api_code VARCHAR(32) DEFAULT NULL COMMENT 'API编码，NULL=全局默认',
  standard_field VARCHAR(64) NOT NULL COMMENT '标准字段名',
  source_paths TEXT COMMENT '源字段路径数组 JSON: ["data.price","data.wholesalePrice"]',
  transform_type VARCHAR(32) DEFAULT 'none' COMMENT '转换类型: none/number/date/strip',
  transform_config TEXT COMMENT '转换参数 JSON',
  value_map TEXT COMMENT '值映射 JSON: {"有货":1,"缺货":0}',
  is_required TINYINT(1) DEFAULT 0,
  default_value VARCHAR(256) DEFAULT NULL,
  validation TEXT COMMENT '校验规则 JSON: {"min":0,"pattern":"^\\d+$"}',
  sort_order INT DEFAULT 0,
  is_enabled TINYINT(1) DEFAULT 1,
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT '',
  PRIMARY KEY (id),
  KEY idx_platform_api (platform_id, api_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段映射规则引擎';

-- 迁移现有 yy_field_mapping 到新表
INSERT INTO yy_field_mapping_rule (platform_id, api_code, standard_field, source_paths, transform_type, is_required, sort_order, is_enabled)
SELECT platform_id, NULL, standard_field, JSON_ARRAY(platform_field), 'none', is_required, sort_order, status
FROM yy_field_mapping WHERE standard_field IS NOT NULL AND platform_field IS NOT NULL;
