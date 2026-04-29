-- ========================================
-- 多平台比价系统 - 数据库表设计
-- 创建日期: 2026-04-21
-- 阶段: 第一阶段
-- ========================================

-- ========================================
-- 1. 比价历史记录表
-- 用途：存储各平台商品的价格数据，支持历史比价分析
-- ========================================
DROP TABLE IF EXISTS `yy_price_comparison`;
CREATE TABLE `yy_price_comparison` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识（关联标准商品）
  `product_id` VARCHAR(128) DEFAULT NULL COMMENT '商品ID',
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  `source_platform` VARCHAR(50) NOT NULL COMMENT '数据来源平台',
  
  -- 价格信息
  `base_price` DECIMAL(10,2) NOT NULL COMMENT '基础价格（原价）',
  `current_price` DECIMAL(10,2) NOT NULL COMMENT '当前售价',
  
  -- 活动信息
  `activity_type` VARCHAR(50) DEFAULT NULL COMMENT '活动类型：none/拼团/凑单/会员价/专享价',
  `activity_price` DECIMAL(10,2) DEFAULT NULL COMMENT '活动价格',
  `activity_name` VARCHAR(200) DEFAULT NULL COMMENT '活动名称',
  `activity_rules` JSON DEFAULT NULL COMMENT '活动规则（满减门槛、折扣率等）',
  
  -- 采购条件
  `min_order_qty` INT DEFAULT 1 COMMENT '最小起订量',
  `max_order_qty` INT DEFAULT NULL COMMENT '最大订购量',
  `stock_quantity` INT DEFAULT 0 COMMENT '可用库存',
  
  -- 物流信息
  `freight_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '运费',
  `free_shipping_threshold` DECIMAL(10,2) DEFAULT NULL COMMENT '免邮门槛',
  `delivery_days` INT DEFAULT NULL COMMENT '预计配送天数',
  `delivery_area` VARCHAR(100) DEFAULT NULL COMMENT '配送区域',
  
  -- 客户业态（单体/连锁/终端/批发）
  `customer_type` VARCHAR(50) DEFAULT 'single' COMMENT '客户业态：single/chain/clinic/wholesale',
  
  -- 有效期
  `valid_from` DATETIME DEFAULT NULL COMMENT '价格生效开始时间',
  `valid_until` DATETIME DEFAULT NULL COMMENT '价格生效结束时间',
  
  -- 溯源
  `raw_data` JSON DEFAULT NULL COMMENT '原始数据快照',
  `collected_at` DATETIME NOT NULL COMMENT '采集时间',
  `synced_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '同步时间',
  
  PRIMARY KEY (`id`),
  INDEX `idx_product_platform` (`product_id`, `source_platform`),
  INDEX `idx_sku_platform` (`sku_id`, `source_platform`),
  INDEX `idx_collected_at` (`collected_at`),
  INDEX `idx_customer_type` (`customer_type`),
  INDEX `idx_valid_until` (`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比价历史记录表';

-- ========================================
-- 2. 平台活动配置表
-- 用途：存储各平台的活动规则，支持活动识别和价格计算
-- ========================================
DROP TABLE IF EXISTS `yy_platform_activity`;
CREATE TABLE `yy_platform_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 平台信息
  `platform_code` VARCHAR(50) NOT NULL COMMENT '平台编码',
  `platform_name` VARCHAR(100) DEFAULT NULL COMMENT '平台名称',
  
  -- 活动信息
  `activity_code` VARCHAR(100) NOT NULL COMMENT '活动编码（唯一）',
  `activity_name` VARCHAR(200) NOT NULL COMMENT '活动名称',
  `activity_type` VARCHAR(50) NOT NULL COMMENT '活动类型：拼团/凑单/满减/会员价/专享价/限时折扣',
  `activity_desc` TEXT DEFAULT NULL COMMENT '活动描述',
  
  -- 活动规则
  `activity_rules` JSON NOT NULL COMMENT '活动规则详情',
  -- 规则示例：
  -- 拼团：{"group_size": 3, "group_price": 85.00, "original_price": 100.00}
  -- 凑单：{"threshold": 500, "discount": 50, "type": "fixed"}  // 满500减50
  -- 凑单：{"threshold": 300, "discount": 0.1, "type": "percent"}  // 满300打9折
  -- 会员价：{"level_1": 0.95, "level_2": 0.90, "level_3": 0.85}
  
  -- 适用范围
  `apply_scope` VARCHAR(50) DEFAULT 'all' COMMENT '适用范围：all/category/product',
  `scope_value` VARCHAR(500) DEFAULT NULL COMMENT '范围值（分类ID或商品ID列表）',
  `customer_types` JSON DEFAULT NULL COMMENT '适用客户业态列表',
  
  -- 时间范围
  `start_time` DATETIME NOT NULL COMMENT '活动开始时间',
  `end_time` DATETIME NOT NULL COMMENT '活动结束时间',
  
  -- 状态
  `is_active` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `priority` INT DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  
  -- 元数据
  `created_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_activity` (`platform_code`, `activity_code`),
  INDEX `idx_platform_type` (`platform_code`, `activity_type`),
  INDEX `idx_time_range` (`start_time`, `end_time`),
  INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台活动配置表';

-- ========================================
-- 3. 比价结果缓存表（可选，用于性能优化）
-- 用途：缓存常用商品的比价结果，减少实时计算
-- ========================================
DROP TABLE IF EXISTS `yy_price_comparison_cache`;
CREATE TABLE `yy_price_comparison_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  
  -- 商品标识
  `product_id` VARCHAR(128) DEFAULT NULL COMMENT '商品ID',
  `sku_id` VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  
  -- 客户业态
  `customer_type` VARCHAR(50) NOT NULL COMMENT '客户业态',
  
  -- 比价结果
  `comparison_result` JSON NOT NULL COMMENT '比价结果详情',
  -- 结果示例：
  -- {
  --   "platforms": [
  --     {"platform": "ysbang", "price": 85.00, "activity": "拼团", "total_cost": 90.00},
  --     {"platform": "ykd", "price": 88.00, "activity": null, "total_cost": 93.00}
  --   ],
  --   "cheapest_platform": "ysbang",
  --   "cheapest_price": 85.00,
  --   "price_diff_percent": 3.5
  -- }
  
  -- 最优推荐
  `recommended_platform` VARCHAR(50) DEFAULT NULL COMMENT '推荐平台',
  `recommended_reason` VARCHAR(500) DEFAULT NULL COMMENT '推荐原因',
  
  -- 缓存控制
  `cache_key` VARCHAR(255) NOT NULL COMMENT '缓存键（product_id:sku_id:customer_type）',
  `expires_at` DATETIME NOT NULL COMMENT '过期时间',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cache_key` (`cache_key`),
  INDEX `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='比价结果缓存表';

-- ========================================
-- 4. 插入示例活动数据
-- ========================================
INSERT INTO `yy_platform_activity` (
  `platform_code`, `platform_name`, `activity_code`, `activity_name`, 
  `activity_type`, `activity_desc`, `activity_rules`, 
  `apply_scope`, `start_time`, `end_time`, `is_active`, `priority`
) VALUES 
-- 药师帮拼团活动
('ysbang', '药师帮', 'ys_2026_group_buy', '春季药品拼团节', '拼团', 
 '3人成团，享85折优惠', 
 '{"group_size": 3, "discount_rate": 0.85, "original_price_field": "price_retail"}',
 'all', '2026-04-01 00:00:00', '2026-04-30 23:59:59', 1, 10),

-- 药师帮满减活动
('ysbang', '药师帮', 'ys_2026_full_reduction', '采购满减', '凑单',
 '满500减50',
 '{"threshold": 500, "discount": 50, "type": "fixed"}',
 'all', '2026-04-01 00:00:00', '2026-04-30 23:59:59', 1, 5),

-- 药京多满减活动
('ykd', '药京多', 'ykd_2026_spring', '春季大促', '凑单',
 '满300减30',
 '{"threshold": 300, "discount": 30, "type": "fixed"}',
 'all', '2026-04-01 00:00:00', '2026-04-30 23:59:59', 1, 8),

-- 1药城会员价
('1yc', '1药城', '1yc_member_price', '会员专享价', '会员价',
 '黄金会员95折，铂金会员9折，钻石会员85折',
 '{"level_1": 0.95, "level_2": 0.90, "level_3": 0.85}',
 'all', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1, 3);

-- ========================================
-- 5. 创建视图：实时比价查询
-- ========================================
CREATE OR REPLACE VIEW `v_price_comparison_latest` AS
SELECT 
    pc.id,
    pc.product_id,
    pc.sku_id,
    pc.source_platform,
    pc.current_price,
    pc.activity_type,
    pc.activity_price,
    pc.min_order_qty,
    pc.freight_amount,
    pc.free_shipping_threshold,
    pc.delivery_days,
    pc.customer_type,
    pc.collected_at,
    -- 计算到手价（含运费）
    CASE 
        WHEN pc.activity_price IS NOT NULL THEN pc.activity_price + pc.freight_amount
        ELSE pc.current_price + pc.freight_amount
    END AS total_cost,
    -- 是否包邮
    CASE 
        WHEN pc.free_shipping_threshold IS NOT NULL AND pc.activity_price >= pc.free_shipping_threshold THEN 1
        WHEN pc.freight_amount = 0 THEN 1
        ELSE 0
    END AS is_free_shipping
FROM yy_price_comparison pc
WHERE pc.id IN (
    SELECT MAX(id) 
    FROM yy_price_comparison 
    GROUP BY sku_id, source_platform, customer_type
);

-- ========================================
-- 6. 创建存储过程：获取商品比价结果
-- ========================================
DELIMITER //

CREATE PROCEDURE `sp_get_price_comparison`(
    IN p_sku_id VARCHAR(128),
    IN p_customer_type VARCHAR(50),
    IN p_region_code VARCHAR(50) DEFAULT NULL
)
BEGIN
    SELECT 
        pc.source_platform,
        p.platform_name,
        pc.current_price,
        pc.activity_type,
        pc.activity_price,
        pc.activity_name,
        pc.min_order_qty,
        pc.freight_amount,
        pc.delivery_days,
        -- 计算到手价
        COALESCE(pc.activity_price, pc.current_price) + pc.freight_amount AS total_cost,
        -- 计算节省金额（与最高价对比）
        (
            SELECT MAX(COALESCE(activity_price, current_price) + freight_amount)
            FROM yy_price_comparison 
            WHERE sku_id = p_sku_id AND customer_type = p_customer_type
        ) - (COALESCE(pc.activity_price, pc.current_price) + pc.freight_amount) AS saved_amount,
        -- 是否最优
        CASE 
            WHEN pc.id = (
                SELECT id FROM yy_price_comparison 
                WHERE sku_id = p_sku_id AND customer_type = p_customer_type
                ORDER BY (COALESCE(activity_price, current_price) + freight_amount) ASC 
                LIMIT 1
            ) THEN 1
            ELSE 0
        END AS is_best_price
    FROM yy_price_comparison pc
    LEFT JOIN yy_platform p ON pc.source_platform = p.platform_code
    WHERE pc.sku_id = p_sku_id 
      AND pc.customer_type = p_customer_type
      AND (p_region_code IS NULL OR pc.delivery_area LIKE CONCAT('%', p_region_code, '%'))
      AND (pc.valid_until IS NULL OR pc.valid_until > NOW())
    ORDER BY total_cost ASC;
END //

DELIMITER ;

-- ========================================
-- 完成提示
-- ========================================
SELECT '比价系统数据库表设计完成！' AS message;
SELECT '已创建表：yy_price_comparison, yy_platform_activity, yy_price_comparison_cache' AS tables;
SELECT '已创建视图：v_price_comparison_latest' AS views;
SELECT '已创建存储过程：sp_get_price_comparison' AS procedures;
