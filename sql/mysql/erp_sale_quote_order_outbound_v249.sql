-- 报价订单“下订 / 出库转待审核手推车”按钮名称调整。
-- 仅更新两个既有权限菜单的展示名称，不新增、删除或重分配任何角色权限。

UPDATE `system_menu`
SET `name` = '报价订单下订/反下订',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'erp:sale-quote:approve'
  AND `deleted` = b'0'
  AND `name` <> '报价订单下订/反下订';

UPDATE `system_menu`
SET `name` = '报价订单出库',
    `updater` = '1',
    `update_time` = NOW()
WHERE `permission` = 'erp:sale-quote:convert-cart'
  AND `deleted` = b'0'
  AND `name` <> '报价订单出库';

SELECT `id`, `name`, `permission`, `type`, `parent_id`, `status`, `deleted`
FROM `system_menu`
WHERE `permission` IN ('erp:sale-quote:approve', 'erp:sale-quote:convert-cart')
ORDER BY `permission`, `id`;
