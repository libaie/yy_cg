-- ========================================
-- 热销预测模块 - 数据库表设计（增量开发）
-- 创建日期: 2026-04-21
-- 说明：基于现有 yy_standard_product 表扩展
-- ========================================

-- ========================================
-- 1. 销售历史记录表
-- 用途：存储商品的历史销量数据，用于预测分析
-- 说明：从现有 yy_standard_product 表的 sales_volume 字段定期采集
-- ========================================
DROP TABLE IF EXISTS `yy_sales_history`;
CREATE TABLE `yy_sales_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识（关联现有表）
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键（关联现有表）',
  
  -- 销售数据
  `sale_date` DATE NOT NULL COMMENT '销售日期',
  `sale_quantity` INT DEFAULT 0 COMMENT '当日销量',
  `sale_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '当日销售额',
  `order_count` INT DEFAULT 0 COMMENT '订单数',
  `customer_count` INT DEFAULT 0 COMMENT '购买客户数',
  
  -- 库存数据
  `stock_quantity` INT DEFAULT 0 COMMENT '库存量',
  
  -- 区域维度
  `region_code` VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  `region_name` VARCHAR(100) DEFAULT NULL COMMENT '区域名称',
  
  -- 元数据
  `collected_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '采集时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_platform_date` (`sku_id`, `source_platform`, `sale_date`),
  INDEX `idx_fusion_key` (`fusion_key`),
  INDEX `idx_sale_date` (`sale_date`),
  INDEX `idx_region` (`region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售历史记录表';

-- ========================================
-- 2. 销售预测结果表
-- 用途：存储预测结果
-- ========================================
DROP TABLE IF EXISTS `yy_sales_forecast`;
CREATE TABLE `yy_sales_forecast` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键',
  
  -- 预测维度
  `forecast_type` VARCHAR(20) NOT NULL COMMENT '预测类型：15day/30day',
  `forecast_date` DATE NOT NULL COMMENT '预测目标日期',
  `region_code` VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  
  -- 预测结果
  `predicted_quantity` INT NOT NULL COMMENT '预测销量',
  `predicted_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '预测销售额',
  `confidence_level` DECIMAL(5,2) DEFAULT NULL COMMENT '置信度（0-100）',
  
  -- 预测区间
  `quantity_lower` INT DEFAULT NULL COMMENT '预测下限',
  `quantity_upper` INT DEFAULT NULL COMMENT '预测上限',
  
  -- 风险评估
  `stock_risk` VARCHAR(20) DEFAULT 'normal' COMMENT '库存风险：low/normal/high/critical',
  `suggested_order_qty` INT DEFAULT NULL COMMENT '建议补货量',
  
  -- 算法信息
  `algorithm` VARCHAR(50) DEFAULT 'arima' COMMENT '预测算法',
  `model_version` VARCHAR(20) DEFAULT '1.0' COMMENT '模型版本',
  
  -- 元数据
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_forecast` (`sku_id`, `source_platform`, `forecast_type`, `forecast_date`),
  INDEX `idx_fusion_key` (`fusion_key`),
  INDEX `idx_forecast_date` (`forecast_date`),
  INDEX `idx_stock_risk` (`stock_risk`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售预测结果表';

-- ========================================
-- 3. 缺货预警表
-- 用途：记录缺货风险预警
-- ========================================
DROP TABLE IF EXISTS `yy_stock_alert`;
CREATE TABLE `yy_stock_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键',
  
  -- 预警信息
  `alert_type` VARCHAR(20) NOT NULL COMMENT '预警类型：low_stock/stockout_risk/stockout',
  `alert_level` VARCHAR(20) NOT NULL COMMENT '预警级别：warning/critical',
  `alert_message` VARCHAR(500) DEFAULT NULL COMMENT '预警消息',
  
  -- 库存状态
  `current_stock` INT DEFAULT 0 COMMENT '当前库存',
  `daily_sales` DECIMAL(10,2) DEFAULT 0 COMMENT '日均销量',
  `days_remaining` INT DEFAULT 0 COMMENT '剩余天数',
  
  -- 建议
  `suggested_order_qty` INT DEFAULT NULL COMMENT '建议补货量',
  `suggested_platform` VARCHAR(50) DEFAULT NULL COMMENT '建议采购平台',
  
  -- 状态
  `status` VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending/notified/resolved/ignored',
  `resolved_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `resolved_by` VARCHAR(64) DEFAULT NULL COMMENT '处理人',
  
  -- 元数据
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  INDEX `idx_sku_platform` (`sku_id`, `source_platform`),
  INDEX `idx_fusion_key` (`fusion_key`),
  INDEX `idx_alert_level` (`alert_level`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺货预警表';

-- ========================================
-- 4. 热销榜单表
-- 用途：存储热销商品排行
-- ========================================
DROP TABLE IF EXISTS `yy_hot_product_ranking`;
CREATE TABLE `yy_hot_product_ranking` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  `fusion_key` VARCHAR(64) DEFAULT NULL COMMENT '融合键',
  
  -- 排行信息
  `ranking_type` VARCHAR(20) NOT NULL COMMENT '排行类型：daily/weekly/monthly',
  `ranking_date` DATE NOT NULL COMMENT '排行日期',
  `rank_position` INT NOT NULL COMMENT '排名位置',
  `category_id` VARCHAR(64) DEFAULT NULL COMMENT '分类ID',
  `region_code` VARCHAR(50) DEFAULT NULL COMMENT '区域编码',
  
  -- 销售数据
  `sale_quantity` INT DEFAULT 0 COMMENT '销量',
  `sale_amount` DECIMAL(12,2) DEFAULT 0.00 COMMENT '销售额',
  `growth_rate` DECIMAL(10,2) DEFAULT NULL COMMENT '增长率（%）',
  
  -- 元数据
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ranking` (`sku_id`, `source_platform`, `ranking_type`, `ranking_date`, `region_code`),
  INDEX `idx_ranking_date` (`ranking_date`),
  INDEX `idx_rank_position` (`rank_position`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热销榜单表';

-- ========================================
-- 完成提示
-- ========================================
SELECT '热销预测模块数据库表设计完成！' AS message;
SELECT '已创建表：yy_sales_history, yy_sales_forecast, yy_stock_alert, yy_hot_product_ranking' AS tables;
