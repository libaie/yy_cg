-- 价格历史时间序列表（支持 30/90/365 天趋势分析）
DROP TABLE IF EXISTS yy_price_history;
CREATE TABLE yy_price_history (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  source_platform VARCHAR(32) NOT NULL COMMENT '来源平台',
  sku_id VARCHAR(128) NOT NULL COMMENT 'SKU ID',
  product_name VARCHAR(256) DEFAULT NULL COMMENT '商品名',
  specification VARCHAR(128) DEFAULT NULL COMMENT '规格',
  manufacturer VARCHAR(128) DEFAULT NULL COMMENT '厂家',
  price_current DECIMAL(10,2) DEFAULT NULL COMMENT '当前售价',
  price_retail DECIMAL(10,2) DEFAULT NULL COMMENT '市场零售价',
  price_assemble DECIMAL(10,2) DEFAULT NULL COMMENT '拼单价',
  stock_quantity INT DEFAULT NULL COMMENT '库存数量',
  freight_amount DECIMAL(10,2) DEFAULT NULL COMMENT '运费',
  shop_name VARCHAR(128) DEFAULT NULL COMMENT '店铺名',
  collected_at DATETIME NOT NULL COMMENT '采集时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, collected_at),
  KEY idx_platform_sku_time (source_platform, sku_id, collected_at),
  KEY idx_collected_at (collected_at),
  KEY idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格历史时间序列表'
PARTITION BY RANGE (TO_DAYS(collected_at)) (
  PARTITION p_history VALUES LESS THAN MAXVALUE
);

-- 月度分区管理存储过程
DROP PROCEDURE IF EXISTS sp_manage_price_partitions;
DELIMITER $$
CREATE PROCEDURE sp_manage_price_partitions()
BEGIN
  DECLARE v_partition_name VARCHAR(64);
  DECLARE v_partition_date DATE;
  DECLARE v_ttl_date DATE;
  DECLARE done INT DEFAULT FALSE;
  DECLARE cur CURSOR FOR
    SELECT PARTITION_NAME FROM INFORMATION_SCHEMA.PARTITIONS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'yy_price_history'
    AND PARTITION_NAME LIKE 'p_%' AND PARTITION_NAME != 'p_history';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  -- 创建下月分区（提前）
  SET v_partition_date = DATE_ADD(LAST_DAY(CURDATE()), INTERVAL 1 DAY);
  SET v_partition_name = CONCAT('p_', DATE_FORMAT(v_partition_date, '%Y%m'));
  SET @sql = CONCAT('ALTER TABLE yy_price_history REORGANIZE PARTITION p_history INTO (',
    'PARTITION ', v_partition_name, ' VALUES LESS THAN (TO_DAYS(''', DATE_ADD(v_partition_date, INTERVAL 1 MONTH), ''')),',
    'PARTITION p_history VALUES LESS THAN MAXVALUE)');
  PREPARE stmt FROM @sql;
  EXECUTE stmt;
  DEALLOCATE PREPARE stmt;

  -- 删除 90 天前的分区（数据 TTL）
  SET v_ttl_date = DATE_SUB(CURDATE(), INTERVAL 90 DAY);
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO v_partition_name;
    IF done THEN LEAVE read_loop; END IF;
    SET v_partition_date = STR_TO_DATE(CONCAT(SUBSTRING(v_partition_name, 3), '01'), '%Y%m%d');
    IF v_partition_date < v_ttl_date THEN
      SET @drop_sql = CONCAT('ALTER TABLE yy_price_history DROP PARTITION IF EXISTS ', v_partition_name);
      PREPARE drop_stmt FROM @drop_sql;
      EXECUTE drop_stmt;
      DEALLOCATE PREPARE drop_stmt;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

-- 每月 1 号凌晨 2 点自动执行
DROP EVENT IF EXISTS evt_price_partition_manage;
CREATE EVENT evt_price_partition_manage
ON SCHEDULE EVERY 1 MONTH STARTS CONCAT(DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-'), '01 02:00:00')
DO CALL sp_manage_price_partitions();
