-- ========================================
-- C端平台行情分析模块 - 数据库表设计（增量开发）
-- 创建日期: 2026-04-21
-- 说明：存储C端平台（美团、京东、天猫、拼多多）的行情数据
-- ========================================

-- ========================================
-- 1. C端平台数据表
-- 用途：存储C端平台的商品销售数据
-- ========================================
DROP TABLE IF EXISTS `yy_cplatform_data`;
CREATE TABLE `yy_cplatform_data` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `product_name` VARCHAR(500) NOT NULL COMMENT '商品名称',
  `common_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名',
  `brand_name` VARCHAR(200) DEFAULT NULL COMMENT '品牌',
  `specification` VARCHAR(200) DEFAULT NULL COMMENT '规格',
  `barcode` VARCHAR(64) DEFAULT NULL COMMENT '条码',
  
  -- C端平台信息
  `cplatform` VARCHAR(50) NOT NULL COMMENT 'C端平台：meituan/jd/tmall/pdd',
  `cplatform_product_id` VARCHAR(128) DEFAULT NULL COMMENT 'C端平台商品ID',
  `cplatform_url` VARCHAR(500) DEFAULT NULL COMMENT '商品链接',
  
  -- 销售数据
  `daily_sales` INT DEFAULT 0 COMMENT '日销量',
  `weekly_sales` INT DEFAULT 0 COMMENT '近7天销量',
  `monthly_sales` INT DEFAULT 0 COMMENT '月销量',
  `total_sales` INT DEFAULT 0 COMMENT '总销量',
  
  -- 价格数据
  `current_price` DECIMAL(10,2) DEFAULT NULL COMMENT '当前售价',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
  `discount_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '折扣率',
  
  -- 店铺信息
  `shop_name` VARCHAR(200) DEFAULT NULL COMMENT '店铺名称',
  `shop_rating` DECIMAL(3,2) DEFAULT NULL COMMENT '店铺评分',
  
  -- 评价数据
  `review_count` INT DEFAULT 0 COMMENT '评价数',
  `positive_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '好评率',
  
  -- 区域数据
  `region_code` VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  `region_name` VARCHAR(100) DEFAULT NULL COMMENT '区域名称',
  
  -- 元数据
  `collected_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  `data_source` VARCHAR(50) DEFAULT 'crawler' COMMENT '数据来源：crawler/api',
  
  PRIMARY KEY (`id`),
  INDEX `idx_cplatform` (`cplatform`),
  INDEX `idx_product_name` (`product_name`(100)),
  INDEX `idx_common_name` (`common_name`),
  INDEX `idx_barcode` (`barcode`),
  INDEX `idx_collected_at` (`collected_at`),
  INDEX `idx_daily_sales` (`daily_sales`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端平台数据表';

-- ========================================
-- 2. C端热销榜单表
-- 用途：存储C端平台热销排行
-- ========================================
DROP TABLE IF EXISTS `yy_cplatform_hot_ranking`;
CREATE TABLE `yy_cplatform_hot_ranking` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `product_name` VARCHAR(500) NOT NULL COMMENT '商品名称',
  `common_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名',
  
  -- 榜单信息
  `cplatform` VARCHAR(50) NOT NULL COMMENT 'C端平台',
  `ranking_type` VARCHAR(20) NOT NULL COMMENT '排行类型：daily/weekly/monthly',
  `ranking_date` DATE NOT NULL COMMENT '排行日期',
  `rank_position` INT NOT NULL COMMENT '排名位置',
  `category` VARCHAR(100) DEFAULT NULL COMMENT '分类',
  
  -- 销售数据
  `sales_quantity` INT DEFAULT 0 COMMENT '销量',
  `sales_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '销售额',
  `growth_rate` DECIMAL(10,2) DEFAULT NULL COMMENT '增长率',
  
  -- 价格数据
  `avg_price` DECIMAL(10,2) DEFAULT NULL COMMENT '平均售价',
  `price_range` VARCHAR(50) DEFAULT NULL COMMENT '价格区间',
  
  -- 元数据
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ranking` (`cplatform`, `ranking_type`, `ranking_date`, `rank_position`),
  INDEX `idx_ranking_date` (`ranking_date`),
  INDEX `idx_common_name` (`common_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端热销榜单表';

-- ========================================
-- 3. C端价格对比表
-- 用途：同一商品在不同C端平台的价格对比
-- ========================================
DROP TABLE IF EXISTS `yy_cplatform_price_compare`;
CREATE TABLE `yy_cplatform_price_compare` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `product_name` VARCHAR(500) NOT NULL COMMENT '商品名称',
  `common_name` VARCHAR(200) DEFAULT NULL COMMENT '通用名',
  `barcode` VARCHAR(64) DEFAULT NULL COMMENT '条码',
  
  -- 各平台价格
  `meituan_price` DECIMAL(10,2) DEFAULT NULL COMMENT '美团价格',
  `jd_price` DECIMAL(10,2) DEFAULT NULL COMMENT '京东价格',
  `tmall_price` DECIMAL(10,2) DEFAULT NULL COMMENT '天猫价格',
  `pdd_price` DECIMAL(10,2) DEFAULT NULL COMMENT '拼多多价格',
  
  -- 价格分析
  `min_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最低价',
  `max_price` DECIMAL(10,2) DEFAULT NULL COMMENT '最高价',
  `avg_price` DECIMAL(10,2) DEFAULT NULL COMMENT '平均价',
  `price_diff_percent` DECIMAL(10,2) DEFAULT NULL COMMENT '价差百分比',
  `cheapest_platform` VARCHAR(50) DEFAULT NULL COMMENT '最低价平台',
  
  -- 元数据
  `collected_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  
  PRIMARY KEY (`id`),
  INDEX `idx_common_name` (`common_name`),
  INDEX `idx_barcode` (`barcode`),
  INDEX `idx_collected_at` (`collected_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='C端价格对比表';

-- ========================================
-- 完成提示
-- ========================================
SELECT 'C端平台行情分析模块数据库表设计完成！' AS message;
SELECT '已创建表：yy_cplatform_data, yy_cplatform_hot_ranking, yy_cplatform_price_compare' AS tables;
