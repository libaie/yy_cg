-- 添加数据来源API标记列
ALTER TABLE yy_standard_product 
ADD COLUMN `source_api` VARCHAR(50) DEFAULT NULL COMMENT '数据来源API: hot/search/flash_kill' 
AFTER `source_platform`;
