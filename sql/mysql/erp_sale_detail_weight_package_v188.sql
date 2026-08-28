-- 用途：补齐销售管理明细的重量/包装数字段和字段定义
-- 日期：2026-08-18

DROP PROCEDURE IF EXISTS add_erp_sale_detail_column_if_missing;

DELIMITER $$
CREATE PROCEDURE add_erp_sale_detail_column_if_missing(
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

CALL add_erp_sale_detail_column_if_missing('erp_sale_quote_items', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量''');
CALL add_erp_sale_detail_column_if_missing('erp_sale_quote_items', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

CALL add_erp_sale_detail_column_if_missing('erp_sale_cart_items', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量''');
CALL add_erp_sale_detail_column_if_missing('erp_sale_cart_items', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

CALL add_erp_sale_detail_column_if_missing('erp_sale_order_items', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量''');
CALL add_erp_sale_detail_column_if_missing('erp_sale_order_items', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

CALL add_erp_sale_detail_column_if_missing('erp_sale_out_items', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

CALL add_erp_sale_detail_column_if_missing('erp_sale_return_items', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量''');
CALL add_erp_sale_detail_column_if_missing('erp_sale_return_items', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

CALL add_erp_sale_detail_column_if_missing('erp_sale_price_adjust_item', 'weight',
    '`weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''重量''');
CALL add_erp_sale_detail_column_if_missing('erp_sale_price_adjust_item', 'package_qty',
    '`package_qty` INT DEFAULT NULL COMMENT ''包装数''');

DROP PROCEDURE IF EXISTS add_erp_sale_detail_column_if_missing;

UPDATE `erp_sale_quote_items` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`package_qty` IS NULL);

UPDATE `erp_sale_cart_items` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`package_qty` IS NULL);

UPDATE `erp_sale_order_items` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`package_qty` IS NULL);

UPDATE `erp_sale_out_items` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`unit_weight` = COALESCE(i.`unit_weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`),
    i.`total_weight` = CASE
        WHEN i.`total_weight` IS NULL AND COALESCE(i.`unit_weight`, p.`weight`) IS NOT NULL AND i.`count` IS NOT NULL
        THEN COALESCE(i.`unit_weight`, p.`weight`) * i.`count`
        ELSE i.`total_weight`
    END
WHERE i.`deleted` = b'0'
  AND (i.`unit_weight` IS NULL OR i.`package_qty` IS NULL OR i.`total_weight` IS NULL);

UPDATE `erp_sale_out` o
JOIN (
    SELECT `out_id`, SUM(COALESCE(`total_weight`, 0)) AS `total_weight`
      FROM `erp_sale_out_items`
     WHERE `deleted` = b'0'
     GROUP BY `out_id`
) s ON s.`out_id` = o.`id`
SET o.`total_weight` = s.`total_weight`
WHERE o.`deleted` = b'0'
  AND o.`total_weight` IS NULL;

UPDATE `erp_sale_return_items` i
JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`package_qty` IS NULL);

UPDATE `erp_sale_price_adjust_item` i
LEFT JOIN `erp_sale_out_items` oi ON oi.`id` = i.`sale_out_item_id` AND oi.`deleted` = b'0'
LEFT JOIN `erp_product` p ON p.`id` = i.`product_id` AND p.`deleted` = b'0'
SET i.`weight` = COALESCE(i.`weight`, oi.`unit_weight`, p.`weight`),
    i.`package_qty` = COALESCE(i.`package_qty`, oi.`package_qty`, p.`package_qty`)
WHERE i.`deleted` = b'0'
  AND (i.`weight` IS NULL OR i.`package_qty` IS NULL);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_sale_quote', 'item_weight', '重量', 'detail_item', 462, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_quote', 'item_packageQty', '包装数', 'detail_item', 464, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_order', 'item_weight', '重量', 'detail_item', 432, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_order', 'item_packageQty', '包装数', 'detail_item', 434, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_cart', 'item_weight', '重量', 'detail_item', 502, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_cart', 'item_packageQty', '包装数', 'detail_item', 504, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_out', 'item_packageQty', '包装数', 'detail_item', 555, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_return', 'item_productUnitName', '单位', 'detail_item', 415, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_return', 'item_weight', '重量', 'detail_item', 416, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_return', 'item_packageQty', '包装数', 'detail_item', 418, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_price_adjust', 'item_unit', '单位', 'detail_item', 455, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_price_adjust', 'item_weight', '重量', 'detail_item', 456, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_price_adjust', 'item_packageQty', '包装数', 'detail_item', 458, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `id` = `id`;
