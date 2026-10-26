-- 采购管理多位小数统一迁移：既有 DECIMAL 数值列安全扩展到 DECIMAL(24,6)
-- 幂等；只修改已存在的 DECIMAL/NUMERIC 列，不新增字段、不改数据。

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_purchase_decimal_precision_v294$$
CREATE PROCEDURE upgrade_purchase_decimal_precision_v294()
BEGIN
    DECLARE finished INT DEFAULT 0;
    DECLARE v_table_name VARCHAR(64);
    DECLARE v_column_name VARCHAR(64);
    DECLARE v_is_nullable VARCHAR(3);
    DECLARE v_column_default LONGTEXT;
    DECLARE v_column_comment LONGTEXT;
    DECLARE v_column_type VARCHAR(255);
    DECLARE v_extra VARCHAR(255);
    DECLARE v_risk_count INT DEFAULT 0;
    DECLARE v_overflow_count BIGINT DEFAULT 0;
    DECLARE v_error_message VARCHAR(255);
    DECLARE v_sql LONGTEXT;

    DECLARE decimal_columns CURSOR FOR
        SELECT table_name, column_name, is_nullable, column_default,
               column_comment, column_type, extra
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name IN (
              'erp_purchase_order', 'erp_purchase_order_items',
              'erp_purchase_in', 'erp_purchase_in_items',
              'erp_purchase_return', 'erp_purchase_return_items',
              'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
              'erp_purchase_invoice', 'erp_purchase_invoice_item',
              'erp_purchase_invoice_source_allocation'
          )
          AND data_type IN ('decimal', 'numeric')
          AND (numeric_precision <> 24 OR numeric_scale <> 6)
        ORDER BY table_name, ordinal_position;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

    SELECT COUNT(*) INTO v_risk_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name IN (
          'erp_purchase_order', 'erp_purchase_order_items',
          'erp_purchase_in', 'erp_purchase_in_items',
          'erp_purchase_return', 'erp_purchase_return_items',
          'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
          'erp_purchase_invoice', 'erp_purchase_invoice_item',
          'erp_purchase_invoice_source_allocation'
      )
      AND data_type IN ('decimal', 'numeric')
      AND (numeric_scale > 6 OR extra <> '');

    IF v_risk_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'v294: decimal column exceeds DECIMAL(24,6) capacity; run precheck and review first';
    END IF;

    OPEN decimal_columns;
    alter_loop: LOOP
        FETCH decimal_columns INTO v_table_name, v_column_name, v_is_nullable,
            v_column_default, v_column_comment, v_column_type, v_extra;
        IF finished = 1 THEN
            LEAVE alter_loop;
        END IF;

        SET @v_overflow_count = 0;
        SET v_sql = CONCAT(
            'SELECT COUNT(*) INTO @v_overflow_count FROM `',
            REPLACE(v_table_name, '`', '``'), '` WHERE ABS(`',
            REPLACE(v_column_name, '`', '``'), '`) >= 1000000000000000000'
        );
        SET @v_sql = v_sql;
        PREPARE stmt FROM @v_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET v_overflow_count = @v_overflow_count;
        IF v_overflow_count > 0 THEN
            SET v_error_message = CONCAT('v294: ', v_table_name, '.', v_column_name,
                                         ' has value outside DECIMAL(24,6) range');
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = v_error_message;
        END IF;

        SET v_sql = CONCAT(
            'ALTER TABLE `', REPLACE(v_table_name, '`', '``'),
            '` MODIFY COLUMN `', REPLACE(v_column_name, '`', '``'),
            '` DECIMAL(24,6)',
            IF(LOCATE('unsigned', LOWER(v_column_type)) > 0, ' UNSIGNED', ''),
            IF(v_is_nullable = 'YES', ' NULL', ' NOT NULL'),
            IF(v_column_default IS NULL, '',
               CONCAT(' DEFAULT ''', REPLACE(v_column_default, '''', ''''''), '''')),
            IF(v_column_comment IS NULL OR v_column_comment = '', '',
               CONCAT(' COMMENT ''', REPLACE(v_column_comment, '''', ''''''), ''''))
        );
        SET @v_sql = v_sql;
        PREPARE stmt FROM @v_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE decimal_columns;
END$$

CALL upgrade_purchase_decimal_precision_v294()$$
DROP PROCEDURE IF EXISTS upgrade_purchase_decimal_precision_v294$$

DELIMITER ;

SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
      'erp_purchase_order', 'erp_purchase_order_items',
      'erp_purchase_in', 'erp_purchase_in_items',
      'erp_purchase_return', 'erp_purchase_return_items',
      'erp_purchase_price_adjust', 'erp_purchase_price_adjust_item',
      'erp_purchase_invoice', 'erp_purchase_invoice_item',
      'erp_purchase_invoice_source_allocation'
  )
  AND data_type IN ('decimal', 'numeric')
ORDER BY table_name, ordinal_position;

