-- ERP 收付款单后续核销 v118
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS add_erp_finance_writeoff_column_if_missing;
DROP PROCEDURE IF EXISTS add_erp_finance_writeoff_index_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_finance_writeoff_column_if_missing(
    IN tableName VARCHAR(64), IN columnName VARCHAR(64), IN columnSql TEXT
)
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = columnName) THEN
        SET @ddl = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE add_erp_finance_writeoff_index_if_missing(
    IN tableName VARCHAR(64), IN indexName VARCHAR(64), IN indexSql TEXT
)
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.TABLES
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = indexName) THEN
        SET @ddl = CONCAT('CREATE INDEX `', indexName, '` ON `', tableName, '` ', indexSql);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'write_off_status',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''核销状态：0待生效、1已生效、2已撤销'' AFTER `receipt_price`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'write_off_time',
    'DATETIME NULL COMMENT ''核销生效时间'' AFTER `write_off_status`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'write_off_user_id',
    'BIGINT NULL COMMENT ''核销操作人'' AFTER `write_off_time`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'reverse_time',
    'DATETIME NULL COMMENT ''撤销时间'' AFTER `write_off_user_id`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'reverse_user_id',
    'BIGINT NULL COMMENT ''撤销操作人'' AFTER `reverse_time`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_receipt_item', 'reverse_reason',
    'VARCHAR(500) NULL COMMENT ''撤销原因'' AFTER `reverse_user_id`');

CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'write_off_status',
    'TINYINT NOT NULL DEFAULT 0 COMMENT ''核销状态：0待生效、1已生效、2已撤销'' AFTER `payment_price`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'write_off_time',
    'DATETIME NULL COMMENT ''核销生效时间'' AFTER `write_off_status`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'write_off_user_id',
    'BIGINT NULL COMMENT ''核销操作人'' AFTER `write_off_time`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'reverse_time',
    'DATETIME NULL COMMENT ''撤销时间'' AFTER `write_off_user_id`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'reverse_user_id',
    'BIGINT NULL COMMENT ''撤销操作人'' AFTER `reverse_time`');
CALL add_erp_finance_writeoff_column_if_missing('erp_finance_payment_item', 'reverse_reason',
    'VARCHAR(500) NULL COMMENT ''撤销原因'' AFTER `reverse_user_id`');

CALL add_erp_finance_writeoff_index_if_missing('erp_finance_receipt_item',
    'idx_receipt_writeoff_status', '(`receipt_id`, `write_off_status`, `deleted`)');
CALL add_erp_finance_writeoff_index_if_missing('erp_finance_receipt_item',
    'idx_receipt_biz_writeoff', '(`tenant_id`, `biz_type`, `biz_id`, `write_off_status`, `deleted`)');
CALL add_erp_finance_writeoff_index_if_missing('erp_finance_payment_item',
    'idx_payment_writeoff_status', '(`payment_id`, `write_off_status`, `deleted`)');
CALL add_erp_finance_writeoff_index_if_missing('erp_finance_payment_item',
    'idx_payment_biz_writeoff', '(`tenant_id`, `biz_type`, `biz_id`, `write_off_status`, `deleted`)');

DROP PROCEDURE IF EXISTS add_erp_finance_writeoff_column_if_missing;
DROP PROCEDURE IF EXISTS add_erp_finance_writeoff_index_if_missing;

-- 历史明细：已审核主单下的明细生效；草稿主单明细保持待生效。
UPDATE erp_finance_receipt_item i
INNER JOIN erp_finance_receipt r
        ON r.id = i.receipt_id AND r.tenant_id = i.tenant_id AND r.deleted = 0
SET i.write_off_status = 1,
    i.write_off_time = COALESCE(i.write_off_time, r.update_time),
    i.write_off_user_id = COALESCE(i.write_off_user_id, CAST(NULLIF(r.updater, '') AS UNSIGNED))
WHERE i.deleted = 0 AND i.write_off_status = 0 AND r.status = 20;

UPDATE erp_finance_payment_item i
INNER JOIN erp_finance_payment p
        ON p.id = i.payment_id AND p.tenant_id = i.tenant_id AND p.deleted = 0
SET i.write_off_status = 1,
    i.write_off_time = COALESCE(i.write_off_time, p.update_time),
    i.write_off_user_id = COALESCE(i.write_off_user_id, CAST(NULLIF(p.updater, '') AS UNSIGNED))
WHERE i.deleted = 0 AND i.write_off_status = 0 AND p.status = 20;

-- 按有效资金核销流水重算业务单据级已收、已付金额。
UPDATE erp_sale_out b
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.receipt_price) AS amount
    FROM erp_finance_receipt_item i
    INNER JOIN erp_finance_receipt r
            ON r.id = i.receipt_id AND r.tenant_id = i.tenant_id AND r.deleted = 0 AND r.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 21
    GROUP BY i.tenant_id, i.biz_id
) x ON x.tenant_id = b.tenant_id AND x.biz_id = b.id
SET b.receipt_price = COALESCE(x.amount, 0)
WHERE b.deleted = 0;

UPDATE erp_sale_return b
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.receipt_price) AS amount
    FROM erp_finance_receipt_item i
    INNER JOIN erp_finance_receipt r
            ON r.id = i.receipt_id AND r.tenant_id = i.tenant_id AND r.deleted = 0 AND r.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 22
    GROUP BY i.tenant_id, i.biz_id
) x ON x.tenant_id = b.tenant_id AND x.biz_id = b.id
SET b.refund_price = ABS(COALESCE(x.amount, 0))
WHERE b.deleted = 0;

UPDATE erp_purchase_in b
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.payment_price) AS amount
    FROM erp_finance_payment_item i
    INNER JOIN erp_finance_payment p
            ON p.id = i.payment_id AND p.tenant_id = i.tenant_id AND p.deleted = 0 AND p.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 11
    GROUP BY i.tenant_id, i.biz_id
) x ON x.tenant_id = b.tenant_id AND x.biz_id = b.id
SET b.payment_price = COALESCE(x.amount, 0)
WHERE b.deleted = 0;

UPDATE erp_purchase_return b
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.payment_price) AS amount
    FROM erp_finance_payment_item i
    INNER JOIN erp_finance_payment p
            ON p.id = i.payment_id AND p.tenant_id = i.tenant_id AND p.deleted = 0 AND p.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 12
    GROUP BY i.tenant_id, i.biz_id
) x ON x.tenant_id = b.tenant_id AND x.biz_id = b.id
SET b.refund_price = ABS(COALESCE(x.amount, 0))
WHERE b.deleted = 0;

-- 只新增权限，不替换任何现有菜单或角色权限。
INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '收款单核销', 'erp:finance-receipt:writeoff', 3, 7, p.id, '', '', '', NULL,
       0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM system_menu p
WHERE p.deleted = b'0' AND p.component_name = 'ErpFinanceReceipt'
  AND NOT EXISTS (SELECT 1 FROM system_menu m
                  WHERE m.deleted = b'0' AND m.permission = 'erp:finance-receipt:writeoff')
LIMIT 1;

INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '收款核销撤销', 'erp:finance-receipt:writeoff-reverse', 3, 8, p.id, '', '', '', NULL,
       0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM system_menu p
WHERE p.deleted = b'0' AND p.component_name = 'ErpFinanceReceipt'
  AND NOT EXISTS (SELECT 1 FROM system_menu m
                  WHERE m.deleted = b'0' AND m.permission = 'erp:finance-receipt:writeoff-reverse')
LIMIT 1;

INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '付款单核销', 'erp:finance-payment:writeoff', 3, 7, p.id, '', '', '', NULL,
       0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM system_menu p
WHERE p.deleted = b'0' AND p.component_name = 'ErpFinancePayment'
  AND NOT EXISTS (SELECT 1 FROM system_menu m
                  WHERE m.deleted = b'0' AND m.permission = 'erp:finance-payment:writeoff')
LIMIT 1;

INSERT INTO system_menu
    (name, permission, type, sort, parent_id, path, icon, component, component_name,
     status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
SELECT '付款核销撤销', 'erp:finance-payment:writeoff-reverse', 3, 8, p.id, '', '', '', NULL,
       0, b'1', b'1', b'1', 'system', NOW(), 'system', NOW(), b'0'
FROM system_menu p
WHERE p.deleted = b'0' AND p.component_name = 'ErpFinancePayment'
  AND NOT EXISTS (SELECT 1 FROM system_menu m
                  WHERE m.deleted = b'0' AND m.permission = 'erp:finance-payment:writeoff-reverse')
LIMIT 1;

-- 已拥有对应审批权限的角色自动获得核销与撤销权限；使用 NOT EXISTS，绝不覆盖其它授权。
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT rm.role_id, target.id, 'system', NOW(), 'system', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu source ON source.id = rm.menu_id AND source.deleted = b'0'
INNER JOIN system_menu target ON target.deleted = b'0'
       AND target.permission IN ('erp:finance-receipt:writeoff', 'erp:finance-receipt:writeoff-reverse')
WHERE rm.deleted = b'0' AND source.permission = 'erp:finance-receipt:update-status'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.deleted = b'0' AND existing.role_id = rm.role_id
                    AND existing.menu_id = target.id AND existing.tenant_id = rm.tenant_id);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT rm.role_id, target.id, 'system', NOW(), 'system', NOW(), b'0', rm.tenant_id
FROM system_role_menu rm
INNER JOIN system_menu source ON source.id = rm.menu_id AND source.deleted = b'0'
INNER JOIN system_menu target ON target.deleted = b'0'
       AND target.permission IN ('erp:finance-payment:writeoff', 'erp:finance-payment:writeoff-reverse')
WHERE rm.deleted = b'0' AND source.permission = 'erp:finance-payment:update-status'
  AND NOT EXISTS (SELECT 1 FROM system_role_menu existing
                  WHERE existing.deleted = b'0' AND existing.role_id = rm.role_id
                    AND existing.menu_id = target.id AND existing.tenant_id = rm.tenant_id);

-- 迁移结果核查：只输出异常，不删除或覆盖异常流水。
SELECT 'receipt_over_allocated' AS anomaly_type, r.tenant_id, r.id AS document_id,
       r.total_price, COALESCE(SUM(CASE WHEN i.write_off_status = 1 THEN i.receipt_price ELSE 0 END), 0) AS allocated_price
FROM erp_finance_receipt r
LEFT JOIN erp_finance_receipt_item i
       ON i.receipt_id = r.id AND i.tenant_id = r.tenant_id AND i.deleted = 0
WHERE r.deleted = 0
GROUP BY r.tenant_id, r.id, r.total_price
HAVING allocated_price < 0 OR allocated_price > r.total_price;

SELECT 'payment_over_allocated' AS anomaly_type, p.tenant_id, p.id AS document_id,
       p.total_price, COALESCE(SUM(CASE WHEN i.write_off_status = 1 THEN i.payment_price ELSE 0 END), 0) AS allocated_price
FROM erp_finance_payment p
LEFT JOIN erp_finance_payment_item i
       ON i.payment_id = p.id AND i.tenant_id = p.tenant_id AND i.deleted = 0
WHERE p.deleted = 0
GROUP BY p.tenant_id, p.id, p.total_price
HAVING allocated_price < 0 OR allocated_price > p.total_price;
