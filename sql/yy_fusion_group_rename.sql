-- ========================================
-- 融合分组表字段重命名
-- 2026-04-14: generic_name -> common_name
-- ========================================

-- 方式一：直接修改原表（如果数据量小）
ALTER TABLE `yy_product_fusion_group` 
CHANGE COLUMN `generic_name` `common_name` VARCHAR(200) DEFAULT NULL COMMENT '药品通用名（展示用）';

-- 方式二：如果数据量大，建议备份后重建
-- CREATE TABLE `yy_product_fusion_group_new` LIKE `yy_product_fusion_group`;
-- ALTER TABLE `yy_product_fusion_group_new` CHANGE COLUMN `generic_name` `common_name` VARCHAR(200) DEFAULT NULL COMMENT '药品通用名（展示用）';
-- INSERT INTO `yy_product_fusion_group_new` SELECT * FROM `yy_product_fusion_group`;
-- RENAME TABLE `yy_product_fusion_group` TO `yy_product_fusion_group_backup`, `yy_product_fusion_group_new` TO `yy_product_fusion_group`;
