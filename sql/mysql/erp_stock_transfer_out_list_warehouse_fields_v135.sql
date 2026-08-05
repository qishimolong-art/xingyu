-- 调拨出库单列表仓库字段权限（v135）
--
-- 新增“调出仓库、调入仓库”列表字段定义。
-- 安全说明：
--   1. 仅为已经存在调拨出库字段定义的租户补充字段；
--   2. 不删除、不覆盖已有字段定义和角色字段权限；
--   3. 重复执行不会产生重复的有效字段定义。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_stock_transfer_out', field_seed.`field_key`, field_seed.`field_label`, 'list_col',
       field_seed.`sort`, '1', NOW(), '1', NOW(), b'0', module_tenant.`tenant_id`
FROM (
    SELECT DISTINCT `tenant_id`
    FROM `system_field_definition`
    WHERE `module` = 'erp_stock_transfer_out'
      AND `deleted` = b'0'
) module_tenant
CROSS JOIN (
    SELECT 'col_fromWarehouseNames' AS `field_key`, '调出仓库' AS `field_label`, 415 AS `sort`
    UNION ALL
    SELECT 'col_toWarehouseNames', '调入仓库', 418
) field_seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` existing
    WHERE existing.`module` = 'erp_stock_transfer_out'
      AND existing.`field_key` = field_seed.`field_key`
      AND existing.`tenant_id` = module_tenant.`tenant_id`
      AND existing.`deleted` = b'0'
);

SELECT `tenant_id`, `field_key`, `field_label`, `field_group`, `sort`
FROM `system_field_definition`
WHERE `module` = 'erp_stock_transfer_out'
  AND `field_key` IN ('col_fromWarehouseNames', 'col_toWarehouseNames')
  AND `deleted` = b'0'
ORDER BY `tenant_id`, `sort`;
