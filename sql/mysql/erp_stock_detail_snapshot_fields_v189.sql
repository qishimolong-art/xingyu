-- 用途：补齐库存明细批次号、单位、包装数、重量快照字段
-- 日期：2026-08-18

DROP PROCEDURE IF EXISTS add_erp_stock_detail_column_if_missing_v189;

DELIMITER $$
CREATE PROCEDURE add_erp_stock_detail_column_if_missing_v189(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = p_table_name
           AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_item', 'batch_no',
    '`batch_no` VARCHAR(128) DEFAULT NULL COMMENT ''批次号'' AFTER `product_unit_id`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'' AFTER `batch_no`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_item', 'batch_no',
    '`batch_no` VARCHAR(128) DEFAULT NULL COMMENT ''批次号'' AFTER `product_unit_id`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'' AFTER `batch_no`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_move_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'' AFTER `product_unit_id`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_move_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_move_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_check_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'' AFTER `batch_no`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_check_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_check_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_warehouse_move_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数'' AFTER `product_unit_id`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_bill_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_in_bill_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_bill_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_out_bill_item', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''总重'' AFTER `weight`');

CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_record', 'product_unit_id',
    '`product_unit_id` BIGINT DEFAULT NULL COMMENT ''产品单位编号快照'' AFTER `batch_no`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_record', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数快照'' AFTER `product_unit_id`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_record', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''单重快照'' AFTER `package_qty`');
CALL add_erp_stock_detail_column_if_missing_v189('erp_stock_record', 'total_weight',
    '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''本次业务总重'' AFTER `weight`');

DROP PROCEDURE IF EXISTS add_erp_stock_detail_column_if_missing_v189;

UPDATE `erp_stock_in_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`product_unit_id` = COALESCE(i.`product_unit_id`, p.`unit_id`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`),
    i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`product_unit_id` IS NULL OR i.`package_qty` IS NULL OR i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_stock_out_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`product_unit_id` = COALESCE(i.`product_unit_id`, p.`unit_id`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`),
    i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`product_unit_id` IS NULL OR i.`package_qty` IS NULL OR i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_stock_move_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`product_unit_id` = COALESCE(i.`product_unit_id`, p.`unit_id`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`),
    i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`product_unit_id` IS NULL OR i.`package_qty` IS NULL OR i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_stock_check_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`product_unit_id` = COALESCE(i.`product_unit_id`, p.`unit_id`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`),
    i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`product_unit_id` IS NULL OR i.`package_qty` IS NULL OR i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_warehouse_move_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND i.`package_qty` IS NULL;

UPDATE `erp_stock_in_bill_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_stock_out_bill_item` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
            THEN COALESCE(i.`weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_stock_record` r
JOIN `erp_product` p ON p.`id` = r.`product_id` AND p.`deleted` = b'0'
SET r.`product_unit_id` = COALESCE(r.`product_unit_id`, p.`unit_id`),
    r.`package_qty` = COALESCE(r.`package_qty`, p.`package_qty`),
    r.`weight` = COALESCE(r.`weight`, p.`weight`),
    r.`total_weight` = CASE
        WHEN r.`total_weight` IS NULL AND COALESCE(r.`weight`, p.`weight`) IS NOT NULL AND r.`count` IS NOT NULL
            THEN COALESCE(r.`weight`, p.`weight`) * r.`count`
        ELSE r.`total_weight`
    END
WHERE r.`deleted` = b'0'
  AND (r.`product_unit_id` IS NULL OR r.`package_qty` IS NULL OR r.`weight` IS NULL OR r.`total_weight` IS NULL);
