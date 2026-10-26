-- v293：套件组装正反向状态流程、执行轮次、审计字段及独立按钮权限。
-- 状态：10 草稿，15 已确认，18 装配中，20 已完成，30 历史已作废。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS `erp_add_column_v293`;
DROP PROCEDURE IF EXISTS `erp_migrate_assembly_workflow_v293`;

DELIMITER $$

CREATE PROCEDURE `erp_add_column_v293`(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @v293_ddl = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN `',
                           p_column_name, '` ', p_definition);
    PREPARE v293_stmt FROM @v293_ddl;
    EXECUTE v293_stmt;
    DEALLOCATE PREPARE v293_stmt;
  END IF;
END$$

CREATE PROCEDURE `erp_migrate_assembly_workflow_v293`()
BEGIN
  IF DATABASE() IS NULL OR DATABASE() = '' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 未选择目标数据库';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.tables
      WHERE table_schema = DATABASE()
        AND table_name IN ('erp_assembly_order', 'erp_assembly_order_item',
                           'erp_stock_record', 'erp_warehouse_move',
                           'system_menu', 'system_role_menu', 'system_tenant_package')) <> 7 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 组装、库存、移货或权限表不完整';
  END IF;
  IF EXISTS (
    SELECT 1 FROM erp_assembly_order
    WHERE deleted = b'0' AND status NOT IN (10, 15, 18, 20, 30)
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 存在无法识别的套件组装状态';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND component = 'erp/assembly/order/index') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 套件组装单菜单缺失或重复';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0' AND permission = 'erp:assembly-order:complete') <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 完成装配权限缺失或重复';
  END IF;
  IF (SELECT COUNT(*) FROM system_menu
      WHERE deleted = b'0'
        AND permission IN ('erp:assembly-order:void', 'erp:assembly-order:reverse-assembly')) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 作废/反装配权限缺失或重复';
  END IF;
  IF EXISTS (
    SELECT 1 FROM erp_warehouse_move
    WHERE deleted = b'0' AND source_type = 30 AND source_id IS NOT NULL
    GROUP BY tenant_id, source_id HAVING SUM(status = 20) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 同一组装单存在多张有效自动移货单';
  END IF;
  IF EXISTS (
    SELECT 1 FROM system_menu
    WHERE deleted = b'0'
      AND permission IN ('erp:assembly-order:void', 'erp:assembly-order:reverse-assembly')
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v293: 作废与反装配权限同时存在，无法安全原位迁移';
  END IF;

  CALL erp_add_column_v293('erp_assembly_order', 'execution_revision',
       'int NOT NULL DEFAULT 0 COMMENT ''当前装配执行轮次'' AFTER `status`');
  CALL erp_add_column_v293('erp_assembly_order', 'confirm_user_id',
       'bigint NULL COMMENT ''确认装配人'' AFTER `execution_revision`');
  CALL erp_add_column_v293('erp_assembly_order', 'confirm_time',
       'datetime NULL COMMENT ''确认装配时间'' AFTER `confirm_user_id`');
  CALL erp_add_column_v293('erp_assembly_order', 'reverse_confirm_user_id',
       'bigint NULL COMMENT ''反确认装配人'' AFTER `confirm_time`');
  CALL erp_add_column_v293('erp_assembly_order', 'reverse_confirm_time',
       'datetime NULL COMMENT ''反确认装配时间'' AFTER `reverse_confirm_user_id`');
  CALL erp_add_column_v293('erp_assembly_order', 'start_user_id',
       'bigint NULL COMMENT ''开始装配人'' AFTER `reverse_confirm_time`');
  CALL erp_add_column_v293('erp_assembly_order', 'start_time',
       'datetime NULL COMMENT ''开始装配时间'' AFTER `start_user_id`');
  CALL erp_add_column_v293('erp_assembly_order', 'cancel_start_user_id',
       'bigint NULL COMMENT ''取消开始装配人'' AFTER `start_time`');
  CALL erp_add_column_v293('erp_assembly_order', 'cancel_start_time',
       'datetime NULL COMMENT ''取消开始装配时间'' AFTER `cancel_start_user_id`');
  CALL erp_add_column_v293('erp_assembly_order', 'reverse_assembly_user_id',
       'bigint NULL COMMENT ''反装配人'' AFTER `complete_time`');
  CALL erp_add_column_v293('erp_assembly_order', 'reverse_assembly_time',
       'datetime NULL COMMENT ''反装配时间'' AFTER `reverse_assembly_user_id`');

  ALTER TABLE erp_assembly_order
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 10
      COMMENT '状态：10草稿，15已确认，18装配中，20已完成，30历史已作废';

  UPDATE erp_assembly_order
  SET execution_revision = 1
  WHERE deleted = b'0' AND status IN (20, 30) AND execution_revision = 0;
END$$

DELIMITER ;

CALL `erp_migrate_assembly_workflow_v293`();
DROP PROCEDURE IF EXISTS `erp_migrate_assembly_workflow_v293`;
DROP PROCEDURE IF EXISTS `erp_add_column_v293`;

START TRANSACTION;

SET @assembly_order_menu_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND component = 'erp/assembly/order/index'
  ORDER BY id DESC LIMIT 1
);

SET @legacy_void_menu_id := (
  SELECT id FROM system_menu
  WHERE deleted = b'0' AND parent_id = @assembly_order_menu_id
    AND permission = 'erp:assembly-order:void'
  ORDER BY id DESC LIMIT 1
);

UPDATE system_menu
SET name = '套件组装单反装配',
    permission = 'erp:assembly-order:reverse-assembly',
    updater = '1', update_time = NOW()
WHERE id = @legacy_void_menu_id AND deleted = b'0';

DROP TEMPORARY TABLE IF EXISTS tmp_assembly_workflow_permissions_v293;
CREATE TEMPORARY TABLE tmp_assembly_workflow_permissions_v293 (
  menu_name varchar(64) NOT NULL,
  permission_code varchar(128) NOT NULL,
  sort_no int NOT NULL,
  source_permission varchar(128) NOT NULL,
  UNIQUE KEY uk_tmp_assembly_workflow_permission (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO tmp_assembly_workflow_permissions_v293 VALUES
('套件组装单确认装配', 'erp:assembly-order:confirm', 5, 'erp:assembly-order:complete'),
('套件组装单反确认装配', 'erp:assembly-order:reverse-confirm', 6, 'erp:assembly-order:reverse-assembly'),
('套件组装单开始装配', 'erp:assembly-order:start', 7, 'erp:assembly-order:complete'),
('套件组装单取消开始', 'erp:assembly-order:cancel-start', 8, 'erp:assembly-order:reverse-assembly'),
('套件组装单反装配', 'erp:assembly-order:reverse-assembly', 10, 'erp:assembly-order:void');

INSERT INTO system_menu
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT (@assembly_permission_base_v293 := @assembly_permission_base_v293 + 1),
       p.menu_name, p.permission_code, 3, p.sort_no, @assembly_order_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM tmp_assembly_workflow_permissions_v293 p
CROSS JOIN (SELECT @assembly_permission_base_v293 := COALESCE(MAX(id), 0) FROM system_menu) seed
WHERE @assembly_order_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM system_menu m
    WHERE m.deleted = b'0' AND m.permission = p.permission_code
  )
ORDER BY p.sort_no;

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT source_grant.role_id, target_menu.id, '1', NOW(), '1', NOW(), b'0', source_grant.tenant_id
FROM tmp_assembly_workflow_permissions_v293 p
INNER JOIN system_menu source_menu
  ON source_menu.deleted = b'0' AND source_menu.permission = p.source_permission
INNER JOIN system_role_menu source_grant
  ON source_grant.menu_id = source_menu.id AND source_grant.deleted = b'0'
INNER JOIN system_menu target_menu
  ON target_menu.deleted = b'0' AND target_menu.permission = p.permission_code
WHERE NOT EXISTS (
  SELECT 1 FROM system_role_menu existing
  WHERE existing.role_id = source_grant.role_id
    AND existing.menu_id = target_menu.id
    AND existing.tenant_id = source_grant.tenant_id
    AND existing.deleted = b'0'
);

SET @confirm_menu_id := (SELECT id FROM system_menu WHERE deleted = b'0' AND permission = 'erp:assembly-order:confirm' LIMIT 1);
SET @reverse_confirm_menu_id := (SELECT id FROM system_menu WHERE deleted = b'0' AND permission = 'erp:assembly-order:reverse-confirm' LIMIT 1);
SET @start_menu_id := (SELECT id FROM system_menu WHERE deleted = b'0' AND permission = 'erp:assembly-order:start' LIMIT 1);
SET @cancel_start_menu_id := (SELECT id FROM system_menu WHERE deleted = b'0' AND permission = 'erp:assembly-order:cancel-start' LIMIT 1);
SET @reverse_assembly_menu_id := (SELECT id FROM system_menu WHERE deleted = b'0' AND permission = 'erp:assembly-order:reverse-assembly' LIMIT 1);

UPDATE system_tenant_package
SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', CAST(@confirm_menu_id AS UNSIGNED)),
    updater = '1', update_time = NOW()
WHERE deleted = b'0' AND JSON_VALID(menu_ids)
  AND JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$')
  AND @confirm_menu_id IS NOT NULL
  AND NOT JSON_CONTAINS(menu_ids, CAST(@confirm_menu_id AS CHAR), '$');
UPDATE system_tenant_package
SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', CAST(@reverse_confirm_menu_id AS UNSIGNED)),
    updater = '1', update_time = NOW()
WHERE deleted = b'0' AND JSON_VALID(menu_ids)
  AND JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$')
  AND @reverse_confirm_menu_id IS NOT NULL
  AND NOT JSON_CONTAINS(menu_ids, CAST(@reverse_confirm_menu_id AS CHAR), '$');
UPDATE system_tenant_package
SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', CAST(@start_menu_id AS UNSIGNED)),
    updater = '1', update_time = NOW()
WHERE deleted = b'0' AND JSON_VALID(menu_ids)
  AND JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$')
  AND @start_menu_id IS NOT NULL
  AND NOT JSON_CONTAINS(menu_ids, CAST(@start_menu_id AS CHAR), '$');
UPDATE system_tenant_package
SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', CAST(@cancel_start_menu_id AS UNSIGNED)),
    updater = '1', update_time = NOW()
WHERE deleted = b'0' AND JSON_VALID(menu_ids)
  AND JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$')
  AND @cancel_start_menu_id IS NOT NULL
  AND NOT JSON_CONTAINS(menu_ids, CAST(@cancel_start_menu_id AS CHAR), '$');
UPDATE system_tenant_package
SET menu_ids = JSON_ARRAY_APPEND(menu_ids, '$', CAST(@reverse_assembly_menu_id AS UNSIGNED)),
    updater = '1', update_time = NOW()
WHERE deleted = b'0' AND JSON_VALID(menu_ids)
  AND JSON_CONTAINS(menu_ids, CAST(@assembly_order_menu_id AS CHAR), '$')
  AND @reverse_assembly_menu_id IS NOT NULL
  AND NOT JSON_CONTAINS(menu_ids, CAST(@reverse_assembly_menu_id AS CHAR), '$');

DROP TEMPORARY TABLE IF EXISTS tmp_assembly_workflow_permissions_v293;
COMMIT;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'erp_assembly_order'
  AND column_name IN ('status', 'execution_revision', 'confirm_user_id', 'confirm_time',
                      'reverse_confirm_user_id', 'reverse_confirm_time', 'start_user_id', 'start_time',
                      'cancel_start_user_id', 'cancel_start_time',
                      'reverse_assembly_user_id', 'reverse_assembly_time')
ORDER BY ordinal_position;

SELECT id, name, permission, parent_id, sort
FROM system_menu
WHERE deleted = b'0' AND permission IN (
  'erp:assembly-order:confirm', 'erp:assembly-order:reverse-confirm',
  'erp:assembly-order:start', 'erp:assembly-order:cancel-start',
  'erp:assembly-order:complete', 'erp:assembly-order:reverse-assembly')
ORDER BY sort, id;

