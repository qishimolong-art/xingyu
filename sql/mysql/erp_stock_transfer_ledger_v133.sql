-- ERP 调拨出入库台账（v133）
--
-- 内容：
--   1. 在“调拨管理”下新增调拨出入库台账页面及查询、导出权限。
--   2. 为已有调拨出库或调拨入库权限的角色增量授予台账权限。
--   3. 注册查询区、日汇总、钻取明细及导出共用字段权限。
--   4. 增加台账日期和关联查询索引。
--
-- 安全：
--   - 不删除、不替换原调拨菜单、按钮、角色或字段权限。
--   - 所有插入均带有效数据 NOT EXISTS 判断，可重复执行。
--   - 角色权限仅增量插入，不执行覆盖式更新。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

START TRANSACTION;

SET @transfer_management_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 1
    AND (`name` = '调拨管理' OR `path` = 'transfer-management')
  ORDER BY `id` DESC
  LIMIT 1
);

SET @transfer_ledger_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 2
    AND (`component` = 'erp/stock/transfer-ledger/index'
      OR `path` = 'transfer-ledger')
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31360, '调拨出入库台账', '', 2, 4, @transfer_management_menu_id, 'transfer-ledger', 'ep:data-analysis',
       'erp/stock/transfer-ledger/index', 'ErpStockTransferLedger',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @transfer_management_menu_id IS NOT NULL
  AND @transfer_ledger_menu_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 31360);

SET @transfer_ledger_menu_id := (
  SELECT `id`
  FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `type` = 2
    AND (`component` = 'erp/stock/transfer-ledger/index'
      OR `path` = 'transfer-ledger')
  ORDER BY `id` DESC
  LIMIT 1
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31361, '调拨出入库台账查询', 'erp:stock-transfer-ledger:query', 3, 1,
       @transfer_ledger_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @transfer_ledger_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'erp:stock-transfer-ledger:query'
  )
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 31361);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 31362, '调拨出入库台账导出', 'erp:stock-transfer-ledger:export', 3, 2,
       @transfer_ledger_menu_id, '', '', '', NULL,
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @transfer_ledger_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `system_menu`
    WHERE `deleted` = b'0'
      AND `permission` = 'erp:stock-transfer-ledger:export'
  )
  AND NOT EXISTS (SELECT 1 FROM `system_menu` WHERE `id` = 31362);

SET @transfer_ledger_query_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` = 'erp:stock-transfer-ledger:query'
  ORDER BY `id` DESC LIMIT 1
);
SET @transfer_ledger_export_menu_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND `permission` = 'erp:stock-transfer-ledger:export'
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT target_permission.`role_id`, target_permission.`menu_id`,
       '1', NOW(), '1', NOW(), b'0', target_permission.`tenant_id`
FROM (
  SELECT DISTINCT role_menu.`role_id`, role_menu.`tenant_id`,
         @transfer_ledger_menu_id AS `menu_id`
  FROM `system_role_menu` role_menu
  INNER JOIN `system_menu` owned_menu
          ON owned_menu.`id` = role_menu.`menu_id`
         AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND (
      owned_menu.`permission` LIKE 'erp:stock-transfer-out:%'
      OR owned_menu.`permission` LIKE 'erp:stock-transfer-in:%'
      OR owned_menu.`component` IN (
        'erp/stock/transfer-out/index',
        'erp/stock/transfer-in/index'
      )
    )
  UNION
  SELECT DISTINCT role_menu.`role_id`, role_menu.`tenant_id`,
         @transfer_ledger_query_menu_id AS `menu_id`
  FROM `system_role_menu` role_menu
  INNER JOIN `system_menu` owned_menu
          ON owned_menu.`id` = role_menu.`menu_id`
         AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND owned_menu.`permission` IN (
      'erp:stock-transfer-out:query',
      'erp:stock-transfer-in:query'
    )
  UNION
  SELECT DISTINCT role_menu.`role_id`, role_menu.`tenant_id`,
         @transfer_ledger_export_menu_id AS `menu_id`
  FROM `system_role_menu` role_menu
  INNER JOIN `system_menu` owned_menu
          ON owned_menu.`id` = role_menu.`menu_id`
         AND owned_menu.`deleted` = b'0'
  WHERE role_menu.`deleted` = b'0'
    AND owned_menu.`permission` IN (
      'erp:stock-transfer-out:export',
      'erp:stock-transfer-in:export'
    )
) target_permission
WHERE target_permission.`menu_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `system_role_menu` existing
    WHERE existing.`role_id` = target_permission.`role_id`
      AND existing.`menu_id` = target_permission.`menu_id`
      AND existing.`tenant_id` = target_permission.`tenant_id`
      AND existing.`deleted` = b'0'
  );

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_stock_transfer_ledger', field_seed.`field_key`, field_seed.`field_label`,
       field_seed.`field_group`, field_seed.`sort`,
       '1', NOW(), '1', NOW(), b'0', module_tenant.`tenant_id`
FROM (
  SELECT DISTINCT `tenant_id`
  FROM `system_field_definition`
  WHERE `deleted` = b'0'
    AND `module` IN ('erp_stock_transfer_out', 'erp_stock_transfer_in')
) module_tenant
CROSS JOIN (
  SELECT 'query_moveTime' AS `field_key`, '查询-业务日期' AS `field_label`, 'query' AS `field_group`, 100 AS `sort`
  UNION ALL SELECT 'query_fromDeptId', '查询-调出部门', 'query', 110
  UNION ALL SELECT 'query_toDeptId', '查询-调入部门', 'query', 120
  UNION ALL SELECT 'query_fromWarehouseId', '查询-调出仓库', 'query', 130
  UNION ALL SELECT 'query_toWarehouseId', '查询-调入仓库', 'query', 140
  UNION ALL SELECT 'query_productId', '查询-产品', 'query', 150
  UNION ALL SELECT 'query_transferOutNo', '查询-调拨出库单号', 'query', 160
  UNION ALL SELECT 'query_transferInNo', '查询-调拨入库单号', 'query', 170
  UNION ALL SELECT 'query_sourceNo', '查询-来源单号', 'query', 180
  UNION ALL SELECT 'query_status', '查询-审核状态', 'query', 190
  UNION ALL SELECT 'query_matchStatus', '查询-匹配状态', 'query', 200
  UNION ALL SELECT 'query_exceptionCode', '查询-异常类型', 'query', 210
  UNION ALL SELECT 'query_keyword', '查询-关键词', 'query', 220
  UNION ALL SELECT 'report_businessDate', '业务日期', 'report_col', 1000
  UNION ALL SELECT 'report_transferOutDocumentCount', '调拨出库单数', 'report_col', 1010
  UNION ALL SELECT 'report_transferOutCount', '调拨出库数量', 'report_col', 1020
  UNION ALL SELECT 'report_transferInDocumentCount', '调拨入库单数', 'report_col', 1030
  UNION ALL SELECT 'report_transferInCount', '调拨入库数量', 'report_col', 1040
  UNION ALL SELECT 'report_differenceCount', '数量差异', 'report_col', 1050
  UNION ALL SELECT 'report_pendingGroupCount', '待审核组数', 'report_col', 1060
  UNION ALL SELECT 'report_completedGroupCount', '已完成组数', 'report_col', 1070
  UNION ALL SELECT 'report_abnormalGroupCount', '异常组数', 'report_col', 1080
  UNION ALL SELECT 'report_ledgerStatus', '台账状态', 'report_col', 1090
  UNION ALL SELECT 'report_matchStatus', '匹配状态', 'report_detail', 1990
  UNION ALL SELECT 'report_transferOutNo', '调拨出库单号', 'report_detail', 2000
  UNION ALL SELECT 'report_transferInNo', '调拨入库单号', 'report_detail', 2010
  UNION ALL SELECT 'report_sourceNo', '来源单号', 'report_detail', 2020
  UNION ALL SELECT 'report_fromDeptName', '调出部门', 'report_detail', 2030
  UNION ALL SELECT 'report_toDeptName', '调入部门', 'report_detail', 2040
  UNION ALL SELECT 'report_productCode', '产品编码', 'report_detail', 2050
  UNION ALL SELECT 'report_productName', '产品名称', 'report_detail', 2060
  UNION ALL SELECT 'report_fromWarehouseName', '调出仓库', 'report_detail', 2070
  UNION ALL SELECT 'report_toWarehouseName', '调入仓库', 'report_detail', 2080
  UNION ALL SELECT 'report_batchNo', '批次号', 'report_detail', 2090
  UNION ALL SELECT 'report_exceptionReason', '异常原因', 'report_detail', 2100
) field_seed
WHERE NOT EXISTS (
  SELECT 1
  FROM `system_field_definition` existing
  WHERE existing.`module` = 'erp_stock_transfer_ledger'
    AND existing.`field_key` = field_seed.`field_key`
    AND existing.`tenant_id` = module_tenant.`tenant_id`
    AND existing.`deleted` = b'0'
);

COMMIT;

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_ledger_index_if_missing_v133;

DELIMITER $$

CREATE PROCEDURE add_erp_stock_transfer_ledger_index_if_missing_v133(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexColumns VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('ALTER TABLE `', tableName, '` ADD INDEX `', indexName, '` ', indexColumns);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

CALL add_erp_stock_transfer_ledger_index_if_missing_v133(
    'erp_stock_move',
    'idx_stock_transfer_ledger_date',
    '(`tenant_id`, `transfer_direction`, `move_time`, `deleted`)'
);
CALL add_erp_stock_transfer_ledger_index_if_missing_v133(
    'erp_stock_move',
    'idx_stock_transfer_ledger_related',
    '(`tenant_id`, `related_move_id`, `transfer_direction`, `deleted`)'
);

DROP PROCEDURE IF EXISTS add_erp_stock_transfer_ledger_index_if_missing_v133;

-- 部署后请刷新菜单缓存并重新登录，确认新菜单、按钮和字段权限生效。
