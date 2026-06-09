-- 库存调拨单：新增“销售单”来源选择字段的字段权限定义
INSERT INTO system_field_definition (
    module, field_key, field_label, field_group, sort,
    creator, create_time, updater, update_time, deleted, tenant_id
)
VALUES
    ('erp_stock_move', 'sourceOutNo', '销售单', 'main_form', 35, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
    field_label = VALUES(field_label),
    field_group = VALUES(field_group),
    sort = VALUES(sort),
    deleted = b'0';
