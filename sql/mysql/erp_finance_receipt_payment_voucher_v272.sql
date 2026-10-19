-- v272 收款单手工凭证号（MySQL 8）。
-- 仅新增可空业务字段及字段权限/字段配置，不回填历史数据，不修改角色现有权限。
-- 执行前先运行同名 precheck 脚本，并将下方目标库改为已核对的数据库名。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET @receipt_payment_voucher_schema = 'ruoyi-vue-pro';

DROP PROCEDURE IF EXISTS migrate_receipt_payment_voucher_schema_v272;
DELIMITER $$
CREATE PROCEDURE migrate_receipt_payment_voucher_schema_v272()
BEGIN
  IF @receipt_payment_voucher_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@receipt_payment_voucher_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: target database changed';
  END IF;
  IF VERSION() < '8.0' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: MySQL 8 is required';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'system_field_definition')
     OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema = DATABASE() AND table_name = 'erp_field_config') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: required receipt or field configuration table is missing';
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
        AND column_name = 'payment_voucher_no'
        AND (data_type <> 'varchar' OR character_maximum_length <> 64 OR is_nullable <> 'YES')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: existing payment_voucher_no column is incompatible';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
      WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt'
        AND column_name = 'payment_voucher_no') THEN
    ALTER TABLE erp_finance_receipt
      ADD COLUMN payment_voucher_no VARCHAR(64) NULL COMMENT '凭证号（客户付款编号）'
      AFTER receipt_price;
  END IF;
END$$
DELIMITER ;
CALL migrate_receipt_payment_voucher_schema_v272();
DROP PROCEDURE migrate_receipt_payment_voucher_schema_v272;

DROP PROCEDURE IF EXISTS migrate_receipt_payment_voucher_config_v272;
DELIMITER $$
CREATE PROCEDURE migrate_receipt_payment_voucher_config_v272()
BEGIN
  DECLARE got_lock INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    IF got_lock = 1 THEN
      DO RELEASE_LOCK('erp_receipt_payment_voucher_v272');
    END IF;
    RESIGNAL;
  END;

  IF @receipt_payment_voucher_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@receipt_payment_voucher_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: target database changed before config migration';
  END IF;
  IF EXISTS (SELECT tenant_id
      FROM system_field_definition
      WHERE module = 'erp_finance_receipt' AND field_key = 'paymentVoucherNo' AND deleted = b'0'
      GROUP BY tenant_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: duplicate active field definitions exist';
  END IF;
  IF EXISTS (SELECT tenant_id
      FROM erp_field_config
      WHERE module_key = 'erp_finance_receipt' AND field_name = 'paymentVoucherNo' AND deleted = b'0'
      GROUP BY tenant_id HAVING COUNT(*) > 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: duplicate active field configs exist';
  END IF;

  SELECT GET_LOCK('erp_receipt_payment_voucher_v272', 10) INTO got_lock;
  IF got_lock IS NULL OR got_lock <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v272: migration lock unavailable';
  END IF;
  START TRANSACTION;

  INSERT INTO system_field_definition
    (module, field_key, field_label, field_group, sort, creator, create_time,
     updater, update_time, deleted, tenant_id)
  SELECT DISTINCT source.module, 'paymentVoucherNo', '凭证号', 'main_form', 111,
         '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM system_field_definition source
  WHERE source.module = 'erp_finance_receipt' AND source.deleted = b'0'
    AND NOT EXISTS (SELECT 1 FROM system_field_definition current
      WHERE current.module = source.module
        AND current.field_key = 'paymentVoucherNo'
        AND current.tenant_id = source.tenant_id
        AND current.deleted = b'0');

  INSERT INTO erp_field_config
    (module_key, field_name, field_label, required, visible, sort, field_source,
     physical_column, field_type, field_group, component_type, max_length,
     list_visible, searchable, readonly, creator, create_time, updater, update_time,
     deleted, tenant_id)
  SELECT DISTINCT source.module_key, 'paymentVoucherNo', '凭证号', b'0', b'1', 65, 'SYSTEM',
         'payment_voucher_no', 'VARCHAR', 'base_info', 'Input', 64,
         b'1', b'1', b'0', '1', NOW(), '1', NOW(), b'0', source.tenant_id
  FROM erp_field_config source
  WHERE source.module_key = 'erp_finance_receipt' AND source.deleted = b'0'
    AND NOT EXISTS (SELECT 1 FROM erp_field_config current
      WHERE current.module_key = source.module_key
        AND current.field_name = 'paymentVoucherNo'
        AND current.tenant_id = source.tenant_id
        AND current.deleted = b'0');

  COMMIT;
  DO RELEASE_LOCK('erp_receipt_payment_voucher_v272');
END$$
DELIMITER ;
CALL migrate_receipt_payment_voucher_config_v272();
DROP PROCEDURE migrate_receipt_payment_voucher_config_v272;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'erp_finance_receipt'
  AND column_name = 'payment_voucher_no';

SELECT COUNT(*) receipt_count,
       SUM(payment_voucher_no IS NULL) null_payment_voucher_count,
       SUM(payment_voucher_no IS NOT NULL) non_null_payment_voucher_count
FROM erp_finance_receipt
WHERE deleted = b'0';

SELECT tenant_id, field_key, field_label, field_group, sort, COUNT(*) row_count
FROM system_field_definition
WHERE module = 'erp_finance_receipt'
  AND field_key = 'paymentVoucherNo'
  AND deleted = b'0'
GROUP BY tenant_id, field_key, field_label, field_group, sort
ORDER BY tenant_id;

SELECT tenant_id, field_name, field_label, required, visible, sort,
       physical_column, field_type, component_type, max_length,
       list_visible, searchable, readonly, COUNT(*) row_count
FROM erp_field_config
WHERE module_key = 'erp_finance_receipt'
  AND field_name = 'paymentVoucherNo'
  AND deleted = b'0'
GROUP BY tenant_id, field_name, field_label, required, visible, sort,
         physical_column, field_type, component_type, max_length,
         list_visible, searchable, readonly
ORDER BY tenant_id;

