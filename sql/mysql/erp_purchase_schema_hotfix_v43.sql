-- ERP purchase schema hotfix v43.
-- Purpose: align purchase order / purchase inbound tables with current DO fields.
-- MySQL 5.7 / 8.0 compatible. Safe to execute repeatedly.

DROP PROCEDURE IF EXISTS add_erp_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_column_if_missing(
    IN tableName VARCHAR(64),
    IN columnName VARCHAR(64),
    IN columnSql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND COLUMN_NAME = columnName
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN ', columnSql);
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- purchase order columns used by ErpPurchaseOrderDO
CALL add_erp_column_if_missing('erp_purchase_order', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_purchase_order', 'deposit_price', '`deposit_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''deposit price''');
CALL add_erp_column_if_missing('erp_purchase_order', 'in_count', '`in_count` DECIMAL(24,6) DEFAULT 0 COMMENT ''in count''');
CALL add_erp_column_if_missing('erp_purchase_order', 'return_count', '`return_count` DECIMAL(24,6) DEFAULT 0 COMMENT ''return count''');
CALL add_erp_column_if_missing('erp_purchase_order', 'arrival_date', '`arrival_date` DATETIME DEFAULT NULL COMMENT ''arrival date''');
CALL add_erp_column_if_missing('erp_purchase_order', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_purchase_order', 'purchase_type', '`purchase_type` VARCHAR(64) DEFAULT NULL COMMENT ''purchase type''');
CALL add_erp_column_if_missing('erp_purchase_order', 'order_formula', '`order_formula` VARCHAR(128) DEFAULT NULL COMMENT ''order formula''');
CALL add_erp_column_if_missing('erp_purchase_order', 'send_date', '`send_date` DATETIME DEFAULT NULL COMMENT ''send date''');
CALL add_erp_column_if_missing('erp_purchase_order', 'latest_arrival_date', '`latest_arrival_date` DATETIME DEFAULT NULL COMMENT ''latest arrival date''');
CALL add_erp_column_if_missing('erp_purchase_order', 'sale_date_from', '`sale_date_from` DATETIME DEFAULT NULL COMMENT ''sale date from''');
CALL add_erp_column_if_missing('erp_purchase_order', 'sale_date_to', '`sale_date_to` DATETIME DEFAULT NULL COMMENT ''sale date to''');
CALL add_erp_column_if_missing('erp_purchase_order', 'factory_order_no', '`factory_order_no` VARCHAR(128) DEFAULT NULL COMMENT ''factory order no''');
CALL add_erp_column_if_missing('erp_purchase_order', 'receive_address', '`receive_address` VARCHAR(256) DEFAULT NULL COMMENT ''receive address''');
CALL add_erp_column_if_missing('erp_purchase_order', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_purchase_order', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_purchase_order', 'purchaser', '`purchaser` BIGINT DEFAULT NULL COMMENT ''purchaser''');
CALL add_erp_column_if_missing('erp_purchase_order', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_purchase_order', 'order_date', '`order_date` DATETIME DEFAULT NULL COMMENT ''order date''');
CALL add_erp_column_if_missing('erp_purchase_order', 'purchase_cycle', '`purchase_cycle` INT DEFAULT NULL COMMENT ''purchase cycle''');
CALL add_erp_column_if_missing('erp_purchase_order', 'order_company', '`order_company` VARCHAR(128) DEFAULT NULL COMMENT ''order company''');
CALL add_erp_column_if_missing('erp_purchase_order', 'tax_percent', '`tax_percent` DECIMAL(24,2) DEFAULT NULL COMMENT ''tax percent''');
CALL add_erp_column_if_missing('erp_purchase_order', 'document_type', '`document_type` VARCHAR(32) DEFAULT NULL COMMENT ''document type''');
CALL add_erp_column_if_missing('erp_purchase_order', 'latest_order_date', '`latest_order_date` DATETIME DEFAULT NULL COMMENT ''latest order date''');

-- purchase inbound columns used by ErpPurchaseInDO
CALL add_erp_column_if_missing('erp_purchase_in', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_purchase_in', 'purchaser', '`purchaser` VARCHAR(64) DEFAULT NULL COMMENT ''purchaser''');
CALL add_erp_column_if_missing('erp_purchase_in', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_purchase_in', 'transport_method', '`transport_method` VARCHAR(64) DEFAULT NULL COMMENT ''transport method''');
CALL add_erp_column_if_missing('erp_purchase_in', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_purchase_in', 'purchase_area', '`purchase_area` VARCHAR(64) DEFAULT NULL COMMENT ''purchase area''');
CALL add_erp_column_if_missing('erp_purchase_in', 'accountant', '`accountant` VARCHAR(64) DEFAULT NULL COMMENT ''accountant''');
CALL add_erp_column_if_missing('erp_purchase_in', 'float_rate', '`float_rate` DECIMAL(8,4) DEFAULT 1.0000 COMMENT ''float rate''');
CALL add_erp_column_if_missing('erp_purchase_in', 'package_count', '`package_count` INT DEFAULT 0 COMMENT ''package count''');
CALL add_erp_column_if_missing('erp_purchase_in', 'factory_order_no', '`factory_order_no` VARCHAR(128) DEFAULT NULL COMMENT ''factory order no''');
CALL add_erp_column_if_missing('erp_purchase_in', 'order_method', '`order_method` VARCHAR(64) DEFAULT NULL COMMENT ''order method''');
CALL add_erp_column_if_missing('erp_purchase_in', 'freight_type1', '`freight_type1` VARCHAR(64) DEFAULT NULL COMMENT ''freight type 1''');
CALL add_erp_column_if_missing('erp_purchase_in', 'freight_type2', '`freight_type2` VARCHAR(64) DEFAULT NULL COMMENT ''freight type 2''');
CALL add_erp_column_if_missing('erp_purchase_in', 'freight_object1', '`freight_object1` VARCHAR(64) DEFAULT NULL COMMENT ''freight object 1''');
CALL add_erp_column_if_missing('erp_purchase_in', 'freight_object2', '`freight_object2` VARCHAR(64) DEFAULT NULL COMMENT ''freight object 2''');
CALL add_erp_column_if_missing('erp_purchase_in', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_purchase_in', 'handler', '`handler` VARCHAR(64) DEFAULT NULL COMMENT ''handler''');
CALL add_erp_column_if_missing('erp_purchase_in', 'tax_rate', '`tax_rate` DECIMAL(8,4) DEFAULT NULL COMMENT ''tax rate''');
CALL add_erp_column_if_missing('erp_purchase_in', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_purchase_in', 'purchase_discount', '`purchase_discount` DECIMAL(20,4) DEFAULT 0.0000 COMMENT ''purchase discount''');
CALL add_erp_column_if_missing('erp_purchase_in', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_purchase_in', 'unloader', '`unloader` VARCHAR(64) DEFAULT NULL COMMENT ''unloader''');
CALL add_erp_column_if_missing('erp_purchase_in', 'float_record', '`float_record` VARCHAR(128) DEFAULT NULL COMMENT ''float record''');
CALL add_erp_column_if_missing('erp_purchase_in', 'receive_unit', '`receive_unit` VARCHAR(128) DEFAULT NULL COMMENT ''receive unit''');
CALL add_erp_column_if_missing('erp_purchase_in', 'total_freight1', '`total_freight1` DECIMAL(20,4) DEFAULT 0.0000 COMMENT ''total freight 1''');
CALL add_erp_column_if_missing('erp_purchase_in', 'total_freight2', '`total_freight2` DECIMAL(20,4) DEFAULT 0.0000 COMMENT ''total freight 2''');
CALL add_erp_column_if_missing('erp_purchase_in', 'payment_date', '`payment_date` DATETIME DEFAULT NULL COMMENT ''payment date''');
CALL add_erp_column_if_missing('erp_purchase_in', 'has_invoice', '`has_invoice` TINYINT(1) DEFAULT 0 COMMENT ''has invoice''');
CALL add_erp_column_if_missing('erp_purchase_in', 'business_entity', '`business_entity` VARCHAR(128) DEFAULT NULL COMMENT ''business entity''');
CALL add_erp_column_if_missing('erp_purchase_in', 'adjusted', '`adjusted` BIT(1) DEFAULT b''0'' COMMENT ''adjusted''');

DROP PROCEDURE IF EXISTS add_erp_column_if_missing;
