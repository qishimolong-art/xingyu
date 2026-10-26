-- 采购调价明细备注幂等迁移（v281）。
-- 新增可空 remark 字段，不回填既有业务数据；补齐各已有采购调价配置租户的字段权限定义。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_erp_purchase_price_adjust_item_remark_v281;
DELIMITER //
CREATE PROCEDURE migrate_erp_purchase_price_adjust_item_remark_v281()
BEGIN
    DECLARE required_table_count INT DEFAULT 0;
    DECLARE remark_column_count INT DEFAULT 0;
    DECLARE remark_column_valid INT DEFAULT 0;
    DECLARE active_definition_duplicates INT DEFAULT 0;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v281: no target database selected';
    END IF;

    SELECT COUNT(*) INTO required_table_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_type = 'BASE TABLE'
      AND table_name IN ('erp_purchase_price_adjust_item', 'system_field_definition');
    IF required_table_count <> 2 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v281: required tables are missing';
    END IF;

    SELECT COUNT(*) INTO active_definition_duplicates
    FROM (
        SELECT tenant_id
        FROM system_field_definition
        WHERE module = 'erp_purchase_price_adjust'
          AND field_key = 'item_remark'
          AND deleted = b'0'
        GROUP BY tenant_id
        HAVING COUNT(*) > 1
    ) duplicate_definition;
    IF active_definition_duplicates <> 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v281: duplicate active item_remark field definitions exist';
    END IF;

    SELECT COUNT(*) INTO remark_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_purchase_price_adjust_item'
      AND column_name = 'remark';
    IF remark_column_count = 0 THEN
        ALTER TABLE erp_purchase_price_adjust_item
          ADD COLUMN remark VARCHAR(500) NULL DEFAULT NULL COMMENT '明细备注'
          AFTER warehouse_position;
    ELSE
        SELECT COUNT(*) INTO remark_column_valid
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'erp_purchase_price_adjust_item'
          AND column_name = 'remark'
          AND data_type = 'varchar'
          AND character_maximum_length = 500
          AND is_nullable = 'YES';
        IF remark_column_valid <> 1 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v281: existing remark column is incompatible';
        END IF;
    END IF;

    START TRANSACTION;
    INSERT INTO system_field_definition
        (module, field_key, field_label, field_group, sort, creator, create_time,
         updater, update_time, deleted, tenant_id)
    SELECT 'erp_purchase_price_adjust', 'item_remark', '明细备注', 'detail_item', 360,
           '1', NOW(), '1', NOW(), b'0', tenant_scope.tenant_id
    FROM (
        SELECT DISTINCT tenant_id
        FROM system_field_definition
        WHERE module = 'erp_purchase_price_adjust' AND deleted = b'0'
        UNION
        SELECT 1
        WHERE NOT EXISTS (
            SELECT 1 FROM system_field_definition
            WHERE module = 'erp_purchase_price_adjust' AND deleted = b'0'
        )
    ) tenant_scope
    ON DUPLICATE KEY UPDATE
        field_label = VALUES(field_label),
        field_group = VALUES(field_group),
        sort = VALUES(sort),
        updater = VALUES(updater),
        update_time = VALUES(update_time);

    SELECT COUNT(*) INTO active_definition_duplicates
    FROM (
        SELECT tenant_id
        FROM system_field_definition
        WHERE module = 'erp_purchase_price_adjust'
          AND field_key = 'item_remark'
          AND deleted = b'0'
        GROUP BY tenant_id
        HAVING COUNT(*) > 1
    ) duplicate_definition;
    IF active_definition_duplicates <> 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v281: field definition post-check failed';
    END IF;
    COMMIT;

    SELECT column_name, column_type, is_nullable, column_comment
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_purchase_price_adjust_item'
      AND column_name = 'remark';
    SELECT tenant_id, field_key, field_label, field_group, sort, deleted
    FROM system_field_definition
    WHERE module = 'erp_purchase_price_adjust'
      AND field_key = 'item_remark'
      AND deleted = b'0'
    ORDER BY tenant_id;
END //
DELIMITER ;

CALL migrate_erp_purchase_price_adjust_item_remark_v281();
DROP PROCEDURE IF EXISTS migrate_erp_purchase_price_adjust_item_remark_v281;
