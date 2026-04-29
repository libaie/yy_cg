-- ========================================
-- 标准商品表 v2.0 - 全字段重构
-- 创建日期: 2026-04-14
-- ========================================

DROP TABLE IF EXISTS `yy_standard_product`;
CREATE TABLE `yy_standard_product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 核心标识
  `product_id` VARCHAR(128) DEFAULT NULL COMMENT '商品ID（平台内部统一标识）',
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID（具体规格商品标识）',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  
  -- 溯源
  `raw_data_payload` JSON DEFAULT NULL COMMENT '原始数据快照',
  
  -- 基础信息
  `barcode` VARCHAR(64) DEFAULT NULL COMMENT '商品条码（如69码）',
  `product_name` VARCHAR(500) DEFAULT NULL COMMENT '商品名称（主标题）',
  `common_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名称（药品法定名）',
  `brand_name` VARCHAR(200) DEFAULT NULL COMMENT '品牌名称',
  `manufacturer` VARCHAR(200) DEFAULT NULL COMMENT '生产厂家',
  `approval_number` VARCHAR(100) DEFAULT NULL COMMENT '批准文号',
  
  -- 分类
  `category_id` VARCHAR(64) DEFAULT NULL COMMENT '分类ID',
  `category_name` VARCHAR(200) DEFAULT NULL COMMENT '分类名称',
  
  -- 规格
  `specification` VARCHAR(200) DEFAULT NULL COMMENT '商品规格',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '销售单位',
  `packing_ratio` VARCHAR(100) DEFAULT NULL COMMENT '中包装/件装量',
  
  -- 限购
  `min_order_qty` INT DEFAULT 1 COMMENT '最小起订量',
  `max_order_qty` INT DEFAULT NULL COMMENT '最大订购量',
  
  -- 状态与库存
  `product_status` VARCHAR(20) DEFAULT '1' COMMENT '商品状态（如：1上架 0下架 在售等）',
  `stock_quantity` INT DEFAULT 0 COMMENT '总库存量',
  `warehouse_stock` JSON DEFAULT NULL COMMENT '分仓库存列表',
  
  -- 图片
  `main_images` JSON DEFAULT NULL COMMENT '商品主图URL列表',
  
  -- 限购
  `min_order_qty` INT DEFAULT 1 COMMENT '最小起订量',
  `max_order_qty` INT DEFAULT NULL COMMENT '最大订购量',
  
  -- 日期
  `production_date` VARCHAR(20) DEFAULT NULL COMMENT '生产日期',
  `expiration_date` VARCHAR(20) DEFAULT NULL COMMENT '有效期至',
  `shelf_life` VARCHAR(50) DEFAULT NULL COMMENT '保质期',
  
  -- 医药专属
  `is_prescription_drug` TINYINT DEFAULT 0 COMMENT '是否处方药（0否 1是）',
  `medicare_type` VARCHAR(100) DEFAULT NULL COMMENT '医保类型',
  `traceability_code_status` TINYINT DEFAULT 0 COMMENT '追溯码状态（0无 1有）',
  
  -- 销售
  `sales_volume` INT DEFAULT 0 COMMENT '销量',
  `shop_name` VARCHAR(200) DEFAULT NULL COMMENT '店铺/供应商名称',
  
  -- 价格
  `price_retail` DECIMAL(10,2) DEFAULT NULL COMMENT '建议零售价/标价',
  `price_current` DECIMAL(10,2) DEFAULT NULL COMMENT '当前基础供货价',
  `price_step_rules` JSON DEFAULT NULL COMMENT '阶梯价规则列表',
  `price_assemble` DECIMAL(10,2) DEFAULT NULL COMMENT '拼团/活动底价',
  
  -- 物流与税务
  `is_tax_included` TINYINT DEFAULT 0 COMMENT '是否含税（0否 1是）',
  `freight_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '基础运费',
  `free_shipping_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '免邮门槛',
  
  -- 标签与活动
  `tags` JSON DEFAULT NULL COMMENT '商品标签',
  `marketing_tags` JSON DEFAULT NULL COMMENT '营销短标签',
  `activity_details` JSON DEFAULT NULL COMMENT '复杂活动明细列表',
  `purchase_limits` JSON DEFAULT NULL COMMENT '限购与起批规则',
  
  -- 融合相关
  `fusion_group_id` BIGINT DEFAULT NULL COMMENT '所属融合分组ID',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键',
  
  -- 时间
  `collected_at` DATETIME DEFAULT NULL COMMENT '采集时间',
  `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_sku` (`source_platform`, `sku_id`),
  INDEX `idx_fusion_key` (`fusion_key`),
  INDEX `idx_common_name` (`common_name`),
  INDEX `idx_approval_number` (`approval_number`),
  INDEX `idx_barcode` (`barcode`),
  INDEX `idx_collected_at` (`collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准商品表v2';
