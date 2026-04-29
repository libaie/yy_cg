-- ========================================
-- 医药采购平台数据融合表结构
-- 创建日期: 2026-04-12
-- ========================================

-- 1. 字段映射配置表
-- 用于配置各平台字段到标准字段的映射关系
DROP TABLE IF EXISTS `yy_field_mapping`;
CREATE TABLE `yy_field_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` BIGINT NOT NULL COMMENT '所属平台ID，关联yy_platform表',
  `standard_field` VARCHAR(50) NOT NULL COMMENT '标准字段名（如 price, generic_name）',
  `platform_field` VARCHAR(100) NOT NULL COMMENT '平台字段名（如 y_price, md_price）',
  `field_type` VARCHAR(20) DEFAULT 'string' COMMENT '数据类型：string/number/decimal/date',
  `is_required` TINYINT DEFAULT 0 COMMENT '是否必填：0否 1是',
  `sort_order` INT DEFAULT 0 COMMENT '排序权重',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `remark` VARCHAR(200) DEFAULT NULL COMMENT '备注',
  `entry_path` VARCHAR(200) DEFAULT NULL COMMENT '数据入口路径，如 data.wholesales',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_standard` (`platform_id`, `standard_field`),
  INDEX `idx_platform_id` (`platform_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台字段映射配置表';

-- 已有数据库升级：加 entry_path 字段
-- ALTER TABLE `yy_field_mapping` ADD COLUMN `entry_path` VARCHAR(200) DEFAULT NULL COMMENT '数据入口路径' AFTER `remark`;

-- 2. 商品融合分组表
-- 同一药品在不同平台的聚合分组
DROP TABLE IF EXISTS `yy_product_fusion_group`;
CREATE TABLE `yy_product_fusion_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `fusion_key` VARCHAR(64) NOT NULL COMMENT '融合键：MD5(通用名+规格+厂家+批准文号)',
  `generic_name` VARCHAR(200) DEFAULT NULL COMMENT '药品通用名（展示用）',
  `specification` VARCHAR(200) DEFAULT NULL COMMENT '规格（展示用）',
  `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家（展示用）',
  `approval_number` VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
  `platform_count` INT DEFAULT 0 COMMENT '有货平台数',
  `min_price` DECIMAL(10,2) DEFAULT NULL COMMENT '全平台最低价',
  `min_price_platform_id` BIGINT DEFAULT NULL COMMENT '最低价所属平台ID',
  `total_stock` INT DEFAULT 0 COMMENT '全平台总库存',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1正常 0下架',
  `last_updated` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fusion_key` (`fusion_key`),
  INDEX `idx_min_price` (`min_price`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品融合分组表';

-- 3. 标准商品表
-- 接收各平台已解密、已映射后的标准化商品数据
DROP TABLE IF EXISTS `yy_standard_product`;
CREATE TABLE `yy_standard_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `platform_id` BIGINT NOT NULL COMMENT '来源平台ID',
  `platform_sku_id` VARCHAR(128) NOT NULL COMMENT '平台侧商品唯一标识',
  
  -- 标准字段
  `generic_name` VARCHAR(200) DEFAULT NULL COMMENT '药品通用名',
  `specification` VARCHAR(200) DEFAULT NULL COMMENT '规格',
  `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
  `brand_name` VARCHAR(200) DEFAULT NULL COMMENT '品牌名',
  `approval_number` VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
  
  -- 交易信息
  `current_price` DECIMAL(10,2) DEFAULT NULL COMMENT '当前起步价',
  `min_price` DECIMAL(10,2) DEFAULT NULL COMMENT '阶梯最低价',
  `stock` INT DEFAULT 0 COMMENT '当前库存',
  `min_order_qty` INT DEFAULT 1 COMMENT '最小起订量',
  `valid_until` VARCHAR(50) DEFAULT NULL COMMENT '有效期至',
  `supplier` VARCHAR(200) DEFAULT NULL COMMENT '供应商/店铺名',
  `product_url` VARCHAR(500) DEFAULT NULL COMMENT '商品详情页链接',
  `product_image` VARCHAR(500) DEFAULT NULL COMMENT '商品主图',
  
  -- 融合相关
  `fusion_group_id` BIGINT DEFAULT NULL COMMENT '所属融合分组ID',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键',
  
  -- 溯源
  `raw_data` JSON DEFAULT NULL COMMENT '平台原始响应数据（解密后完整保留）',
  `collected_at` DATETIME DEFAULT NULL COMMENT '采集时间',
  `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_sku` (`platform_id`, `platform_sku_id`),
  INDEX `idx_fusion_key` (`fusion_key`),
  INDEX `idx_generic_name` (`generic_name`),
  INDEX `idx_approval_number` (`approval_number`),
  INDEX `idx_collected_at` (`collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准商品表';
