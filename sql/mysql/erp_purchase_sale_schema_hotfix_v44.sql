-- ERP purchase/sale schema hotfix v44.
-- Purpose: align purchase return / price adjust / invoice and sale document
-- main tables with current MyBatis-Plus DO fields.
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

-- purchase return columns used by ErpPurchaseReturnDO
CALL add_erp_column_if_missing('erp_purchase_return', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_purchase_return', 'refund_price', '`refund_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''refund price''');
CALL add_erp_column_if_missing('erp_purchase_return', 'other_price', '`other_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''other price''');
CALL add_erp_column_if_missing('erp_purchase_return', 'return_type', '`return_type` VARCHAR(64) DEFAULT NULL COMMENT ''return type''');
CALL add_erp_column_if_missing('erp_purchase_return', 'purchaser', '`purchaser` VARCHAR(64) DEFAULT NULL COMMENT ''purchaser''');
CALL add_erp_column_if_missing('erp_purchase_return', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_purchase_return', 'transport_method', '`transport_method` VARCHAR(64) DEFAULT NULL COMMENT ''transport method''');
CALL add_erp_column_if_missing('erp_purchase_return', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_purchase_return', 'package_count', '`package_count` INT DEFAULT 0 COMMENT ''package count''');
CALL add_erp_column_if_missing('erp_purchase_return', 'freight_amount', '`freight_amount` DECIMAL(24,6) DEFAULT 0 COMMENT ''freight amount''');
CALL add_erp_column_if_missing('erp_purchase_return', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_purchase_return', 'doc_source', '`doc_source` VARCHAR(64) DEFAULT NULL COMMENT ''doc source''');
CALL add_erp_column_if_missing('erp_purchase_return', 'factory_order_no', '`factory_order_no` VARCHAR(128) DEFAULT NULL COMMENT ''factory order no''');
CALL add_erp_column_if_missing('erp_purchase_return', 'maker', '`maker` VARCHAR(64) DEFAULT NULL COMMENT ''maker''');
CALL add_erp_column_if_missing('erp_purchase_return', 'dept', '`dept` VARCHAR(64) DEFAULT NULL COMMENT ''dept''');
CALL add_erp_column_if_missing('erp_purchase_return', 'shipping_area', '`shipping_area` VARCHAR(64) DEFAULT NULL COMMENT ''shipping area''');
CALL add_erp_column_if_missing('erp_purchase_return', 'warehouse_type', '`warehouse_type` VARCHAR(64) DEFAULT NULL COMMENT ''warehouse type''');
CALL add_erp_column_if_missing('erp_purchase_return', 'freight_type', '`freight_type` VARCHAR(64) DEFAULT NULL COMMENT ''freight type''');
CALL add_erp_column_if_missing('erp_purchase_return', 'logistics_no', '`logistics_no` VARCHAR(128) DEFAULT NULL COMMENT ''logistics no''');
CALL add_erp_column_if_missing('erp_purchase_return', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_purchase_return', 'order_method', '`order_method` VARCHAR(64) DEFAULT NULL COMMENT ''order method''');
CALL add_erp_column_if_missing('erp_purchase_return', 'return_mode', '`return_mode` INT DEFAULT NULL COMMENT ''return mode''');
CALL add_erp_column_if_missing('erp_purchase_return', 'tax_rate', '`tax_rate` DECIMAL(10,2) DEFAULT NULL COMMENT ''tax rate''');
CALL add_erp_column_if_missing('erp_purchase_return', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_purchase_return', 'handler', '`handler` BIGINT DEFAULT NULL COMMENT ''handler''');

-- purchase price adjust columns used by ErpPurchasePriceAdjustDO
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'no', '`no` VARCHAR(64) DEFAULT NULL COMMENT ''no''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'status', '`status` INT DEFAULT 10 COMMENT ''status''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'adjust_time', '`adjust_time` DATETIME DEFAULT NULL COMMENT ''adjust time''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'supplier_id', '`supplier_id` BIGINT DEFAULT NULL COMMENT ''supplier id''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'adjuster', '`adjuster` BIGINT DEFAULT NULL COMMENT ''adjuster''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'adjust_type', '`adjust_type` INT DEFAULT 10 COMMENT ''adjust type''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'remark', '`remark` VARCHAR(500) DEFAULT NULL COMMENT ''remark''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'total_adjust_price', '`total_adjust_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''total adjust price''');
CALL add_erp_column_if_missing('erp_purchase_price_adjust', 'approve_time', '`approve_time` DATETIME DEFAULT NULL COMMENT ''approve time''');

-- purchase invoice columns used by ErpPurchaseInvoiceDO
CALL add_erp_column_if_missing('erp_purchase_invoice', 'no', '`no` VARCHAR(64) DEFAULT NULL COMMENT ''no''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'status', '`status` INT DEFAULT 10 COMMENT ''status''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'supplier_id', '`supplier_id` BIGINT DEFAULT NULL COMMENT ''supplier id''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'invoice_date', '`invoice_date` DATE DEFAULT NULL COMMENT ''invoice date''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'invoice_no', '`invoice_no` VARCHAR(128) DEFAULT NULL COMMENT ''invoice no''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'invoice_count', '`invoice_count` INT DEFAULT 0 COMMENT ''invoice count''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'tax_exclusive_amount', '`tax_exclusive_amount` DECIMAL(24,6) DEFAULT 0 COMMENT ''tax exclusive amount''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'tax_amount', '`tax_amount` DECIMAL(24,6) DEFAULT 0 COMMENT ''tax amount''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'total_amount', '`total_amount` DECIMAL(24,6) DEFAULT 0 COMMENT ''total amount''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'handler_id', '`handler_id` BIGINT DEFAULT NULL COMMENT ''handler id''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'remark', '`remark` VARCHAR(500) DEFAULT NULL COMMENT ''remark''');
CALL add_erp_column_if_missing('erp_purchase_invoice', 'file_url', '`file_url` VARCHAR(512) DEFAULT NULL COMMENT ''file url''');

-- sale order columns used by ErpSaleOrderDO
CALL add_erp_column_if_missing('erp_sale_order', 'sale_user_id', '`sale_user_id` BIGINT DEFAULT NULL COMMENT ''sale user id''');
CALL add_erp_column_if_missing('erp_sale_order', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_order', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_sale_order', 'deposit_price', '`deposit_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''deposit price''');
CALL add_erp_column_if_missing('erp_sale_order', 'out_count', '`out_count` DECIMAL(24,6) DEFAULT 0 COMMENT ''out count''');
CALL add_erp_column_if_missing('erp_sale_order', 'return_count', '`return_count` DECIMAL(24,6) DEFAULT 0 COMMENT ''return count''');
CALL add_erp_column_if_missing('erp_sale_order', 'order_type', '`order_type` INT DEFAULT 1 COMMENT ''order type''');

-- sale out columns used by ErpSaleOutDO
CALL add_erp_column_if_missing('erp_sale_out', 'sale_user_id', '`sale_user_id` BIGINT DEFAULT NULL COMMENT ''sale user id''');
CALL add_erp_column_if_missing('erp_sale_out', 'source_type', '`source_type` INT DEFAULT NULL COMMENT ''source type''');
CALL add_erp_column_if_missing('erp_sale_out', 'source_id', '`source_id` BIGINT DEFAULT NULL COMMENT ''source id''');
CALL add_erp_column_if_missing('erp_sale_out', 'source_no', '`source_no` VARCHAR(64) DEFAULT NULL COMMENT ''source no''');
CALL add_erp_column_if_missing('erp_sale_out', 'receipt_price', '`receipt_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''receipt price''');
CALL add_erp_column_if_missing('erp_sale_out', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'other_price', '`other_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''other price''');
CALL add_erp_column_if_missing('erp_sale_out', 'adjusted', '`adjusted` TINYINT(1) DEFAULT 0 COMMENT ''adjusted''');
CALL add_erp_column_if_missing('erp_sale_out', 'adjust_source_out_id', '`adjust_source_out_id` BIGINT DEFAULT NULL COMMENT ''adjust source out id''');
CALL add_erp_column_if_missing('erp_sale_out', 'adjust_new_out_id', '`adjust_new_out_id` BIGINT DEFAULT NULL COMMENT ''adjust new out id''');
CALL add_erp_column_if_missing('erp_sale_out', 'adjust_price_adjust_id', '`adjust_price_adjust_id` BIGINT DEFAULT NULL COMMENT ''adjust price adjust id''');
CALL add_erp_column_if_missing('erp_sale_out', 'settle_status', '`settle_status` TINYINT DEFAULT 0 COMMENT ''settle status''');
CALL add_erp_column_if_missing('erp_sale_out', 'order_type', '`order_type` VARCHAR(64) DEFAULT NULL COMMENT ''order type''');
CALL add_erp_column_if_missing('erp_sale_out', 'extra_fee', '`extra_fee` DECIMAL(24,6) DEFAULT NULL COMMENT ''extra fee''');
CALL add_erp_column_if_missing('erp_sale_out', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_sale_out', 'sign_status', '`sign_status` TINYINT DEFAULT 0 COMMENT ''sign status''');
CALL add_erp_column_if_missing('erp_sale_out', 'sign_image_url', '`sign_image_url` VARCHAR(512) DEFAULT NULL COMMENT ''sign image url''');
CALL add_erp_column_if_missing('erp_sale_out', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_sale_out', 'shipper', '`shipper` VARCHAR(128) DEFAULT NULL COMMENT ''shipper''');
CALL add_erp_column_if_missing('erp_sale_out', 'receiver_name', '`receiver_name` VARCHAR(128) DEFAULT NULL COMMENT ''receiver name''');
CALL add_erp_column_if_missing('erp_sale_out', 'receiver_phone', '`receiver_phone` VARCHAR(32) DEFAULT NULL COMMENT ''receiver phone''');
CALL add_erp_column_if_missing('erp_sale_out', 'delivery_no', '`delivery_no` VARCHAR(128) DEFAULT NULL COMMENT ''delivery no''');
CALL add_erp_column_if_missing('erp_sale_out', 'logistics_no', '`logistics_no` VARCHAR(128) DEFAULT NULL COMMENT ''logistics no''');
CALL add_erp_column_if_missing('erp_sale_out', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_sale_out', 'sender_name', '`sender_name` VARCHAR(128) DEFAULT NULL COMMENT ''sender name''');
CALL add_erp_column_if_missing('erp_sale_out', 'insurance_company', '`insurance_company` VARCHAR(128) DEFAULT NULL COMMENT ''insurance company''');
CALL add_erp_column_if_missing('erp_sale_out', 'third_party_no', '`third_party_no` VARCHAR(128) DEFAULT NULL COMMENT ''third party no''');
CALL add_erp_column_if_missing('erp_sale_out', 'third_party_upstream_no', '`third_party_upstream_no` VARCHAR(128) DEFAULT NULL COMMENT ''third party upstream no''');
CALL add_erp_column_if_missing('erp_sale_out', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_sale_out', 'invoice_amount', '`invoice_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''invoice amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'reduction_amount', '`reduction_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''reduction amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'after_reduction_amount', '`after_reduction_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''after reduction amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'bill_amount', '`bill_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''bill amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'freight', '`freight` DECIMAL(24,6) DEFAULT NULL COMMENT ''freight''');
CALL add_erp_column_if_missing('erp_sale_out', 'bill_type', '`bill_type` VARCHAR(64) DEFAULT NULL COMMENT ''bill type''');
CALL add_erp_column_if_missing('erp_sale_out', 'bill_no', '`bill_no` VARCHAR(128) DEFAULT NULL COMMENT ''bill no''');
CALL add_erp_column_if_missing('erp_sale_out', 'cancel_count', '`cancel_count` DECIMAL(24,6) DEFAULT NULL COMMENT ''cancel count''');
CALL add_erp_column_if_missing('erp_sale_out', 'cancel_amount', '`cancel_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''cancel amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'after_cancel_amount', '`after_cancel_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''after cancel amount''');
CALL add_erp_column_if_missing('erp_sale_out', 'auditor_id', '`auditor_id` BIGINT DEFAULT NULL COMMENT ''auditor id''');
CALL add_erp_column_if_missing('erp_sale_out', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_out', 'total_weight', '`total_weight` DECIMAL(24,6) DEFAULT NULL COMMENT ''total weight''');
CALL add_erp_column_if_missing('erp_sale_out', 'approve_time', '`approve_time` DATETIME DEFAULT NULL COMMENT ''approve time''');
CALL add_erp_column_if_missing('erp_sale_out', 'print_time', '`print_time` DATETIME DEFAULT NULL COMMENT ''print time''');
CALL add_erp_column_if_missing('erp_sale_out', 'confirm_time', '`confirm_time` DATETIME DEFAULT NULL COMMENT ''confirm time''');
CALL add_erp_column_if_missing('erp_sale_out', 'source_create_time', '`source_create_time` DATETIME DEFAULT NULL COMMENT ''source create time''');
CALL add_erp_column_if_missing('erp_sale_out', 'internal_note', '`internal_note` VARCHAR(512) DEFAULT NULL COMMENT ''internal note''');
CALL add_erp_column_if_missing('erp_sale_out', 'vin', '`vin` VARCHAR(64) DEFAULT NULL COMMENT ''vin''');
CALL add_erp_column_if_missing('erp_sale_out', 'print_count', '`print_count` INT DEFAULT 0 COMMENT ''print count''');
CALL add_erp_column_if_missing('erp_sale_out', 'return_status', '`return_status` TINYINT DEFAULT 0 COMMENT ''return status''');

-- sale return columns used by ErpSaleReturnDO
CALL add_erp_column_if_missing('erp_sale_return', 'sale_user_id', '`sale_user_id` BIGINT DEFAULT NULL COMMENT ''sale user id''');
CALL add_erp_column_if_missing('erp_sale_return', 'return_mode', '`return_mode` INT DEFAULT NULL COMMENT ''return mode''');
CALL add_erp_column_if_missing('erp_sale_return', 'source_out_id', '`source_out_id` BIGINT DEFAULT NULL COMMENT ''source out id''');
CALL add_erp_column_if_missing('erp_sale_return', 'source_out_no', '`source_out_no` VARCHAR(64) DEFAULT NULL COMMENT ''source out no''');
CALL add_erp_column_if_missing('erp_sale_return', 'refund_price', '`refund_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''refund price''');
CALL add_erp_column_if_missing('erp_sale_return', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_sale_return', 'other_price', '`other_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''other price''');
CALL add_erp_column_if_missing('erp_sale_return', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_return', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_sale_return', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_sale_return', 'bill_no', '`bill_no` VARCHAR(128) DEFAULT NULL COMMENT ''bill no''');
CALL add_erp_column_if_missing('erp_sale_return', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_sale_return', 'reduction_amount', '`reduction_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''reduction amount''');
CALL add_erp_column_if_missing('erp_sale_return', 'freight_type', '`freight_type` VARCHAR(64) DEFAULT NULL COMMENT ''freight type''');
CALL add_erp_column_if_missing('erp_sale_return', 'freight_amount', '`freight_amount` DECIMAL(24,6) DEFAULT NULL COMMENT ''freight amount''');
CALL add_erp_column_if_missing('erp_sale_return', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_sale_return', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_sale_return', 'vehicle_no', '`vehicle_no` VARCHAR(64) DEFAULT NULL COMMENT ''vehicle no''');
CALL add_erp_column_if_missing('erp_sale_return', 'branch_store', '`branch_store` VARCHAR(128) DEFAULT NULL COMMENT ''branch store''');
CALL add_erp_column_if_missing('erp_sale_return', 'branch_store_enabled', '`branch_store_enabled` TINYINT(1) DEFAULT 0 COMMENT ''branch store enabled''');
CALL add_erp_column_if_missing('erp_sale_return', 'purchase_area', '`purchase_area` VARCHAR(64) DEFAULT NULL COMMENT ''purchase area''');
CALL add_erp_column_if_missing('erp_sale_return', 'business_type', '`business_type` VARCHAR(64) DEFAULT NULL COMMENT ''business type''');
CALL add_erp_column_if_missing('erp_sale_return', 'order_method', '`order_method` VARCHAR(64) DEFAULT NULL COMMENT ''order method''');
CALL add_erp_column_if_missing('erp_sale_return', 'developer_user_id', '`developer_user_id` BIGINT DEFAULT NULL COMMENT ''developer user id''');

-- sale quote columns used by ErpSaleQuoteDO
CALL add_erp_column_if_missing('erp_sale_quote', 'sale_user_id', '`sale_user_id` BIGINT DEFAULT NULL COMMENT ''sale user id''');
CALL add_erp_column_if_missing('erp_sale_quote', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_quote', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_sale_quote', 'other_price', '`other_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''other price''');
CALL add_erp_column_if_missing('erp_sale_quote', 'source_type', '`source_type` INT DEFAULT NULL COMMENT ''source type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'source_id', '`source_id` BIGINT DEFAULT NULL COMMENT ''source id''');
CALL add_erp_column_if_missing('erp_sale_quote', 'source_no', '`source_no` VARCHAR(64) DEFAULT NULL COMMENT ''source no''');
CALL add_erp_column_if_missing('erp_sale_quote', 'order_type', '`order_type` VARCHAR(64) DEFAULT NULL COMMENT ''order type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_sale_quote', 'prepayment', '`prepayment` TINYINT(1) DEFAULT 0 COMMENT ''prepayment''');
CALL add_erp_column_if_missing('erp_sale_quote', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_sale_quote', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_sale_quote', 'proxy_delivery', '`proxy_delivery` TINYINT(1) DEFAULT 0 COMMENT ''proxy delivery''');
CALL add_erp_column_if_missing('erp_sale_quote', 'delivery_address', '`delivery_address` VARCHAR(512) DEFAULT NULL COMMENT ''delivery address''');
CALL add_erp_column_if_missing('erp_sale_quote', 'allowance_price', '`allowance_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''allowance price''');
CALL add_erp_column_if_missing('erp_sale_quote', 'ticket_no', '`ticket_no` VARCHAR(64) DEFAULT NULL COMMENT ''ticket no''');
CALL add_erp_column_if_missing('erp_sale_quote', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'freight_type', '`freight_type` VARCHAR(64) DEFAULT NULL COMMENT ''freight type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'freight_amount', '`freight_amount` DECIMAL(24,6) DEFAULT 0 COMMENT ''freight amount''');
CALL add_erp_column_if_missing('erp_sale_quote', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_sale_quote', 'receiver_name', '`receiver_name` VARCHAR(128) DEFAULT NULL COMMENT ''receiver name''');
CALL add_erp_column_if_missing('erp_sale_quote', 'receiver_phone', '`receiver_phone` VARCHAR(32) DEFAULT NULL COMMENT ''receiver phone''');
CALL add_erp_column_if_missing('erp_sale_quote', 'price_type', '`price_type` VARCHAR(64) DEFAULT NULL COMMENT ''price type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'branch_delivery', '`branch_delivery` VARCHAR(64) DEFAULT NULL COMMENT ''branch delivery''');
CALL add_erp_column_if_missing('erp_sale_quote', 'expected_delivery_time', '`expected_delivery_time` DATETIME DEFAULT NULL COMMENT ''expected delivery time''');
CALL add_erp_column_if_missing('erp_sale_quote', 'billing_method', '`billing_method` VARCHAR(64) DEFAULT NULL COMMENT ''billing method''');
CALL add_erp_column_if_missing('erp_sale_quote', 'vehicle_plate_no', '`vehicle_plate_no` VARCHAR(32) DEFAULT NULL COMMENT ''vehicle plate no''');
CALL add_erp_column_if_missing('erp_sale_quote', 'business_type', '`business_type` VARCHAR(64) DEFAULT NULL COMMENT ''business type''');
CALL add_erp_column_if_missing('erp_sale_quote', 'developer_user_id', '`developer_user_id` BIGINT DEFAULT NULL COMMENT ''developer user id''');
CALL add_erp_column_if_missing('erp_sale_quote', 'vin', '`vin` VARCHAR(64) DEFAULT NULL COMMENT ''vin''');
CALL add_erp_column_if_missing('erp_sale_quote', 'internal_remark', '`internal_remark` VARCHAR(512) DEFAULT NULL COMMENT ''internal remark''');

-- sale cart columns used by ErpSaleCartDO
CALL add_erp_column_if_missing('erp_sale_cart', 'no', '`no` VARCHAR(64) DEFAULT NULL COMMENT ''no''');
CALL add_erp_column_if_missing('erp_sale_cart', 'status', '`status` INT DEFAULT 10 COMMENT ''status''');
CALL add_erp_column_if_missing('erp_sale_cart', 'customer_id', '`customer_id` BIGINT DEFAULT NULL COMMENT ''customer id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'account_id', '`account_id` BIGINT DEFAULT NULL COMMENT ''account id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'sale_user_id', '`sale_user_id` BIGINT DEFAULT NULL COMMENT ''sale user id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'cart_time', '`cart_time` DATETIME DEFAULT NULL COMMENT ''cart time''');
CALL add_erp_column_if_missing('erp_sale_cart', 'first_audit_user_id', '`first_audit_user_id` BIGINT DEFAULT NULL COMMENT ''first audit user id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'first_audit_time', '`first_audit_time` DATETIME DEFAULT NULL COMMENT ''first audit time''');
CALL add_erp_column_if_missing('erp_sale_cart', 'final_audit_user_id', '`final_audit_user_id` BIGINT DEFAULT NULL COMMENT ''final audit user id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'final_audit_time', '`final_audit_time` DATETIME DEFAULT NULL COMMENT ''final audit time''');
CALL add_erp_column_if_missing('erp_sale_cart', 'fee_amount', '`fee_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT ''fee amount''');
CALL add_erp_column_if_missing('erp_sale_cart', 'other_price', '`other_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''other price''');
CALL add_erp_column_if_missing('erp_sale_cart', 'source_type', '`source_type` INT DEFAULT NULL COMMENT ''source type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'source_id', '`source_id` BIGINT DEFAULT NULL COMMENT ''source id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'source_no', '`source_no` VARCHAR(64) DEFAULT NULL COMMENT ''source no''');
CALL add_erp_column_if_missing('erp_sale_cart', 'business_type', '`business_type` VARCHAR(64) DEFAULT NULL COMMENT ''business type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'order_type', '`order_type` VARCHAR(64) DEFAULT NULL COMMENT ''order type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'billing_method', '`billing_method` VARCHAR(64) DEFAULT NULL COMMENT ''billing method''');
CALL add_erp_column_if_missing('erp_sale_cart', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_sale_cart', 'invoice_type', '`invoice_type` VARCHAR(64) DEFAULT NULL COMMENT ''invoice type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_sale_cart', 'freight_type', '`freight_type` VARCHAR(64) DEFAULT NULL COMMENT ''freight type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'priority', '`priority` VARCHAR(64) DEFAULT NULL COMMENT ''priority''');
CALL add_erp_column_if_missing('erp_sale_cart', 'price_type', '`price_type` VARCHAR(64) DEFAULT NULL COMMENT ''price type''');
CALL add_erp_column_if_missing('erp_sale_cart', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');
CALL add_erp_column_if_missing('erp_sale_cart', 'developer_user_id', '`developer_user_id` BIGINT DEFAULT NULL COMMENT ''developer user id''');
CALL add_erp_column_if_missing('erp_sale_cart', 'contact_person', '`contact_person` VARCHAR(128) DEFAULT NULL COMMENT ''contact person''');
CALL add_erp_column_if_missing('erp_sale_cart', 'contact_phone', '`contact_phone` VARCHAR(64) DEFAULT NULL COMMENT ''contact phone''');
CALL add_erp_column_if_missing('erp_sale_cart', 'delivery_address', '`delivery_address` VARCHAR(512) DEFAULT NULL COMMENT ''delivery address''');
CALL add_erp_column_if_missing('erp_sale_cart', 'delivery_date', '`delivery_date` DATETIME DEFAULT NULL COMMENT ''delivery date''');
CALL add_erp_column_if_missing('erp_sale_cart', 'tax_rate', '`tax_rate` DECIMAL(24,6) DEFAULT NULL COMMENT ''tax rate''');
CALL add_erp_column_if_missing('erp_sale_cart', 'total_freight', '`total_freight` DECIMAL(24,6) DEFAULT NULL COMMENT ''total freight''');
CALL add_erp_column_if_missing('erp_sale_cart', 'payment_date', '`payment_date` DATETIME DEFAULT NULL COMMENT ''payment date''');
CALL add_erp_column_if_missing('erp_sale_cart', 'business_entity', '`business_entity` VARCHAR(128) DEFAULT NULL COMMENT ''business entity''');
CALL add_erp_column_if_missing('erp_sale_cart', 'order_method', '`order_method` VARCHAR(64) DEFAULT NULL COMMENT ''order method''');
CALL add_erp_column_if_missing('erp_sale_cart', 'source_type2', '`source_type2` VARCHAR(64) DEFAULT NULL COMMENT ''source type 2''');
CALL add_erp_column_if_missing('erp_sale_cart', 'remark2', '`remark2` VARCHAR(512) DEFAULT NULL COMMENT ''remark 2''');

-- sale price adjust columns used by ErpSalePriceAdjustDO
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'no', '`no` VARCHAR(64) DEFAULT NULL COMMENT ''no''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'status', '`status` INT DEFAULT 10 COMMENT ''status''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'adjust_date', '`adjust_date` DATETIME DEFAULT NULL COMMENT ''adjust date''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'customer_id', '`customer_id` BIGINT DEFAULT NULL COMMENT ''customer id''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'adjust_user_id', '`adjust_user_id` BIGINT DEFAULT NULL COMMENT ''adjust user id''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'adjust_type', '`adjust_type` INT DEFAULT 10 COMMENT ''adjust type''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'remark', '`remark` VARCHAR(500) DEFAULT NULL COMMENT ''remark''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'total_adjust_price', '`total_adjust_price` DECIMAL(24,6) DEFAULT 0 COMMENT ''total adjust price''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'original_sale_out_id', '`original_sale_out_id` BIGINT DEFAULT NULL COMMENT ''original sale out id''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'original_sale_out_no', '`original_sale_out_no` VARCHAR(64) DEFAULT NULL COMMENT ''original sale out no''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'new_sale_out_id', '`new_sale_out_id` BIGINT DEFAULT NULL COMMENT ''new sale out id''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'new_sale_out_no', '`new_sale_out_no` VARCHAR(64) DEFAULT NULL COMMENT ''new sale out no''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'settle_method', '`settle_method` VARCHAR(64) DEFAULT NULL COMMENT ''settle method''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'delivery_method', '`delivery_method` VARCHAR(64) DEFAULT NULL COMMENT ''delivery method''');
CALL add_erp_column_if_missing('erp_sale_price_adjust', 'logistics_company', '`logistics_company` VARCHAR(128) DEFAULT NULL COMMENT ''logistics company''');

-- sale config columns used by ErpSaleConfigDO
CALL add_erp_column_if_missing('erp_sale_config', 'dept_id', '`dept_id` BIGINT DEFAULT NULL COMMENT ''dept id''');

DROP PROCEDURE IF EXISTS add_erp_column_if_missing;
