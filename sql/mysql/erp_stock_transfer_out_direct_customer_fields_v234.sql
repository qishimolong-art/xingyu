-- 调拨出库单直发客户字段定义
-- 仅补充字段权限/打印字段定义，不变更业务数据。

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_stock_transfer_out', 'directCustomerName', '直发客户', 'main_form', 65,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_stock_transfer_out'
      AND `field_key` = 'directCustomerName'
      AND `deleted` = b'0'
);

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_stock_transfer_out', 'col_directCustomerName', '直发客户', 'list_col', 405,
       '1', NOW(), '1', NOW(), b'0', 1
WHERE NOT EXISTS (
    SELECT 1 FROM `system_field_definition`
    WHERE `module` = 'erp_stock_transfer_out'
      AND `field_key` = 'col_directCustomerName'
      AND `deleted` = b'0'
);
