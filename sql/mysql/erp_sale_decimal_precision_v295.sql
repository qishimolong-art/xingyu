-- 销售管理及库存成本链路多位小数统一迁移：既有 DECIMAL 数值列安全扩展到 DECIMAL(24,6)
-- 幂等；只修改已存在的 DECIMAL/NUMERIC 列，不新增字段、不改数据。

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_sale_decimal_precision_v295$$
CREATE PROCEDURE upgrade_sale_decimal_precision_v295()
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
          AND ((table_name = 'erp_sale_price_adjust' AND column_name = 'total_adjust_price')
          OR (table_name = 'erp_sale_price_adjust_item' AND column_name IN ('old_price', 'new_price', 'adjust_price'))
          OR (table_name = 'erp_stock' AND column_name = 'cost_amount')
          OR (table_name = 'erp_stock_record' AND column_name IN ('total_price', 'cost_amount')))
          AND data_type IN ('decimal', 'numeric')
          AND (numeric_precision <> 24 OR numeric_scale <> 6)
        ORDER BY table_name, ordinal_position;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

    SELECT COUNT(*) INTO v_risk_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND ((table_name = 'erp_sale_price_adjust' AND column_name = 'total_adjust_price')
          OR (table_name = 'erp_sale_price_adjust_item' AND column_name IN ('old_price', 'new_price', 'adjust_price'))
          OR (table_name = 'erp_stock' AND column_name = 'cost_amount')
          OR (table_name = 'erp_stock_record' AND column_name IN ('total_price', 'cost_amount')))
      AND (numeric_scale > 6 OR extra <> '' OR data_type NOT IN ('decimal', 'numeric'));

    IF v_risk_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'v295: decimal column exceeds DECIMAL(24,6) capacity; run precheck and review first';
    END IF;

    SELECT COUNT(*) INTO v_risk_count FROM information_schema.columns
    WHERE table_schema = DATABASE() AND ((table_name = 'erp_sale_price_adjust' AND column_name = 'total_adjust_price')
          OR (table_name = 'erp_sale_price_adjust_item' AND column_name IN ('old_price', 'new_price', 'adjust_price'))
          OR (table_name = 'erp_stock' AND column_name = 'cost_amount')
          OR (table_name = 'erp_stock_record' AND column_name IN ('total_price', 'cost_amount')));
    IF v_risk_count <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v295: expected exactly seven existing columns';
    END IF;

    SELECT COUNT(*) INTO v_overflow_count FROM (
      SELECT total_adjust_price AS v FROM erp_sale_price_adjust
      UNION ALL SELECT old_price FROM erp_sale_price_adjust_item
      UNION ALL SELECT new_price FROM erp_sale_price_adjust_item
      UNION ALL SELECT adjust_price FROM erp_sale_price_adjust_item
      UNION ALL SELECT cost_amount FROM erp_stock
      UNION ALL SELECT total_price FROM erp_stock_record
      UNION ALL SELECT cost_amount FROM erp_stock_record
    ) values_to_check WHERE ABS(v) >= 1000000000000000000 OR v <> ROUND(v, 6);
    IF v_overflow_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v295: historical value exceeds DECIMAL(24,6) capacity';
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
            SET v_error_message = CONCAT('v295: ', v_table_name, '.', v_column_name,
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

CALL upgrade_sale_decimal_precision_v295()$$
DROP PROCEDURE IF EXISTS upgrade_sale_decimal_precision_v295$$

DELIMITER ;

SELECT table_name, column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'erp_sale_price_adjust' AND column_name = 'total_adjust_price')
          OR (table_name = 'erp_sale_price_adjust_item' AND column_name IN ('old_price', 'new_price', 'adjust_price'))
          OR (table_name = 'erp_stock' AND column_name = 'cost_amount')
          OR (table_name = 'erp_stock_record' AND column_name IN ('total_price', 'cost_amount')))
  AND data_type IN ('decimal', 'numeric')
ORDER BY table_name, ordinal_position;
