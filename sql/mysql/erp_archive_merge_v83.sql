-- ERP archive merge feature (v83)
-- Adds merge flags for supplier/customer/product and merge button permissions.
-- Safety:
--   - No DELETE/TRUNCATE.
--   - Permission script is append-only for role grants.
--   - Existing active menu permissions are not overwritten.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_archive_merge_column_if_missing;

DELIMITER //
CREATE PROCEDURE add_erp_archive_merge_column_if_missing(
  IN p_table_name varchar(64),
  IN p_column_name varchar(64),
  IN p_column_definition varchar(1000)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND column_name = p_column_name
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table_name, '` ADD COLUMN ', p_column_definition);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END//
DELIMITER ;

CALL add_erp_archive_merge_column_if_missing('erp_supplier', 'merged_flag',
  '`merged_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已合并'' AFTER `disabled_time`');
CALL add_erp_archive_merge_column_if_missing('erp_supplier', 'merged_target_id',
  '`merged_target_id` bigint DEFAULT NULL COMMENT ''合并目标供应商编号'' AFTER `merged_flag`');
CALL add_erp_archive_merge_column_if_missing('erp_supplier', 'merged_by',
  '`merged_by` bigint DEFAULT NULL COMMENT ''合并操作人'' AFTER `merged_target_id`');
CALL add_erp_archive_merge_column_if_missing('erp_supplier', 'merged_time',
  '`merged_time` datetime DEFAULT NULL COMMENT ''合并时间'' AFTER `merged_by`');

CALL add_erp_archive_merge_column_if_missing('erp_customer', 'merged_flag',
  '`merged_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已合并'' AFTER `disabled_time`');
CALL add_erp_archive_merge_column_if_missing('erp_customer', 'merged_target_id',
  '`merged_target_id` bigint DEFAULT NULL COMMENT ''合并目标客户编号'' AFTER `merged_flag`');
CALL add_erp_archive_merge_column_if_missing('erp_customer', 'merged_by',
  '`merged_by` bigint DEFAULT NULL COMMENT ''合并操作人'' AFTER `merged_target_id`');
CALL add_erp_archive_merge_column_if_missing('erp_customer', 'merged_time',
  '`merged_time` datetime DEFAULT NULL COMMENT ''合并时间'' AFTER `merged_by`');

CALL add_erp_archive_merge_column_if_missing('erp_product', 'merged_flag',
  '`merged_flag` bit(1) NOT NULL DEFAULT b''0'' COMMENT ''是否已合并'' AFTER `disabled_time`');
CALL add_erp_archive_merge_column_if_missing('erp_product', 'merged_target_id',
  '`merged_target_id` bigint DEFAULT NULL COMMENT ''合并目标配件编号'' AFTER `merged_flag`');
CALL add_erp_archive_merge_column_if_missing('erp_product', 'merged_by',
  '`merged_by` bigint DEFAULT NULL COMMENT ''合并操作人'' AFTER `merged_target_id`');
CALL add_erp_archive_merge_column_if_missing('erp_product', 'merged_time',
  '`merged_time` datetime DEFAULT NULL COMMENT ''合并时间'' AFTER `merged_by`');

DROP PROCEDURE IF EXISTS add_erp_archive_merge_column_if_missing;

CREATE TABLE IF NOT EXISTS `erp_archive_merge_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `archive_type` varchar(32) NOT NULL COMMENT '档案类型：supplier/customer/product',
  `source_id` bigint NOT NULL COMMENT '被合并档案编号',
  `keep_id` bigint NOT NULL COMMENT '保留档案编号',
  `affected_tables` text DEFAULT NULL COMMENT '影响表及行数',
  `affected_rows` int NOT NULL DEFAULT 0 COMMENT '影响行数',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_archive_source` (`archive_type`, `source_id`),
  KEY `idx_archive_keep` (`archive_type`, `keep_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP基础档案合并日志';

DROP TEMPORARY TABLE IF EXISTS tmp_erp_archive_merge_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_archive_merge_parent;

CREATE TEMPORARY TABLE tmp_erp_archive_merge_permissions (
  name varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  sort int NOT NULL,
  query_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  update_perm varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_archive_merge_permissions
(name, permission, sort, query_perm, update_perm)
VALUES
('供应商合并', 'erp:supplier:merge', 7, 'erp:supplier:query', 'erp:supplier:update'),
('客户合并', 'erp:customer:merge', 7, 'erp:customer:query', 'erp:customer:update'),
('配件合并', 'erp:product:merge', 7, 'erp:product:query', 'erp:product:update');

CREATE TEMPORARY TABLE tmp_erp_archive_merge_parent (
  permission varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL PRIMARY KEY,
  parent_id bigint NOT NULL
) ENGINE = MEMORY DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO tmp_erp_archive_merge_parent (permission, parent_id)
SELECT source.permission, MIN(candidate.parent_id) AS parent_id
FROM tmp_erp_archive_merge_permissions source
JOIN system_menu candidate
  ON (
      candidate.permission COLLATE utf8mb4_unicode_ci = source.query_perm COLLATE utf8mb4_unicode_ci
   OR candidate.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
  )
 AND candidate.deleted = b'0'
 AND candidate.parent_id > 0
GROUP BY source.permission;

INSERT INTO system_menu
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT source.name,
       source.permission,
       3,
       source.sort,
       parent.parent_id,
       '',
       '',
       '',
       NULL,
       0,
       b'1',
       b'1',
       b'1',
       '1',
       NOW(),
       '1',
       NOW(),
       b'0'
FROM tmp_erp_archive_merge_permissions source
JOIN tmp_erp_archive_merge_parent parent
  ON parent.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_menu exists_menu
    WHERE exists_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
      AND exists_menu.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       merge_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM tmp_erp_archive_merge_permissions source
JOIN system_menu merge_menu
  ON merge_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND merge_menu.deleted = b'0'
JOIN system_role_menu owned_role
  ON owned_role.deleted = b'0'
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission COLLATE utf8mb4_unicode_ci = source.update_perm COLLATE utf8mb4_unicode_ci
WHERE NOT EXISTS (
    SELECT 1
    FROM system_role_menu target
    WHERE target.role_id = owned_role.role_id
      AND target.menu_id = merge_menu.id
      AND target.tenant_id = owned_role.tenant_id
      AND target.deleted = b'0'
);

INSERT INTO system_role_menu
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       merge_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN tmp_erp_archive_merge_permissions source ON 1 = 1
JOIN system_menu merge_menu
  ON merge_menu.permission COLLATE utf8mb4_unicode_ci = source.permission COLLATE utf8mb4_unicode_ci
 AND merge_menu.deleted = b'0'
WHERE super_role.code COLLATE utf8mb4_unicode_ci = 'super_admin' COLLATE utf8mb4_unicode_ci
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu target
      WHERE target.role_id = super_role.id
        AND target.menu_id = merge_menu.id
        AND target.tenant_id = super_role.tenant_id
        AND target.deleted = b'0'
  );

DROP TEMPORARY TABLE IF EXISTS tmp_erp_archive_merge_permissions;
DROP TEMPORARY TABLE IF EXISTS tmp_erp_archive_merge_parent;
