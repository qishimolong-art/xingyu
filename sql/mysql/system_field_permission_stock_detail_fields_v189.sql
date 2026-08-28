-- 用途：补齐库存明细批次号、包装数、重量字段权限定义
-- 日期：2026-08-18

DROP PROCEDURE IF EXISTS add_stock_field_definition_if_missing_v189;

DELIMITER $$
CREATE PROCEDURE add_stock_field_definition_if_missing_v189(
    IN p_module VARCHAR(100),
    IN p_field_key VARCHAR(100),
    IN p_field_label VARCHAR(100),
    IN p_field_group VARCHAR(50),
    IN p_sort INT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
         FROM `system_field_definition`
         WHERE `module` COLLATE utf8mb4_general_ci = p_module COLLATE utf8mb4_general_ci
           AND `field_key` COLLATE utf8mb4_general_ci = p_field_key COLLATE utf8mb4_general_ci
           AND `deleted` = b'0'
           AND `tenant_id` = 1
    ) THEN
        INSERT INTO `system_field_definition`
        (`module`, `field_key`, `field_label`, `field_group`, `sort`,
         `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
        VALUES
        (p_module, p_field_key, p_field_label, p_field_group, p_sort,
         '1', NOW(), '1', NOW(), b'0', 1);
    END IF;
END$$
DELIMITER ;

CALL add_stock_field_definition_if_missing_v189('erp_stock_in', 'item_batchNo', '批次号', 'detail_item', 241);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in', 'item_packageQty', '包装数', 'detail_item', 242);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in', 'item_weight', '单重', 'detail_item', 243);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in', 'item_totalWeight', '总重', 'detail_item', 244);

CALL add_stock_field_definition_if_missing_v189('erp_stock_out', 'item_batchNo', '批次号', 'detail_item', 241);
CALL add_stock_field_definition_if_missing_v189('erp_stock_out', 'item_packageQty', '包装数', 'detail_item', 242);
CALL add_stock_field_definition_if_missing_v189('erp_stock_out', 'item_weight', '单重', 'detail_item', 243);
CALL add_stock_field_definition_if_missing_v189('erp_stock_out', 'item_totalWeight', '总重', 'detail_item', 244);

CALL add_stock_field_definition_if_missing_v189('erp_stock_move', 'item_batchNo', '批次号', 'detail_item', 251);
CALL add_stock_field_definition_if_missing_v189('erp_stock_move', 'item_packageQty', '包装数', 'detail_item', 252);
CALL add_stock_field_definition_if_missing_v189('erp_stock_move', 'item_weight', '单重', 'detail_item', 253);
CALL add_stock_field_definition_if_missing_v189('erp_stock_move', 'item_totalWeight', '总重', 'detail_item', 254);

CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_out', 'item_batchNo', '批次号', 'detail_item', 251);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_out', 'item_packageQty', '包装数', 'detail_item', 252);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_out', 'item_weight', '单重', 'detail_item', 253);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_out', 'item_totalWeight', '总重', 'detail_item', 254);

CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_in', 'item_batchNo', '批次号', 'detail_item', 251);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_in', 'item_packageQty', '包装数', 'detail_item', 252);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_in', 'item_weight', '单重', 'detail_item', 253);
CALL add_stock_field_definition_if_missing_v189('erp_stock_transfer_in', 'item_totalWeight', '总重', 'detail_item', 254);

CALL add_stock_field_definition_if_missing_v189('erp_stock_check', 'item_packageQty', '包装数', 'detail_item', 242);
CALL add_stock_field_definition_if_missing_v189('erp_stock_check', 'item_weight', '单重', 'detail_item', 243);
CALL add_stock_field_definition_if_missing_v189('erp_stock_check', 'item_totalWeight', '总重', 'detail_item', 244);

CALL add_stock_field_definition_if_missing_v189('erp_warehouse_move', 'item_batchNo', '批次号', 'detail_item', 251);
CALL add_stock_field_definition_if_missing_v189('erp_warehouse_move', 'item_packageQty', '包装数', 'detail_item', 252);
CALL add_stock_field_definition_if_missing_v189('erp_warehouse_move', 'item_weight', '单重', 'detail_item', 253);
CALL add_stock_field_definition_if_missing_v189('erp_warehouse_move', 'item_totalWeight', '总重', 'detail_item', 254);

CALL add_stock_field_definition_if_missing_v189('erp_stock_in_bill', 'item_productUnitName', '单位', 'detail_item', 241);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in_bill', 'item_packageQty', '包装数', 'detail_item', 242);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in_bill', 'item_weight', '单重', 'detail_item', 243);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in_bill', 'item_totalWeight', '总重', 'detail_item', 244);
CALL add_stock_field_definition_if_missing_v189('erp_stock_in_bill', 'item_batchNo', '批次号', 'detail_item', 245);

CALL add_stock_field_definition_if_missing_v189('erp_stock_out_bill', 'item_packageQty', '包装数', 'detail_item', 242);
CALL add_stock_field_definition_if_missing_v189('erp_stock_out_bill', 'item_weight', '单重', 'detail_item', 243);
CALL add_stock_field_definition_if_missing_v189('erp_stock_out_bill', 'item_totalWeight', '总重', 'detail_item', 244);

CALL add_stock_field_definition_if_missing_v189('erp_stock_record', 'packageQty', '包装数', 'report', 365);
CALL add_stock_field_definition_if_missing_v189('erp_stock_record', 'weight', '单重', 'report', 366);
CALL add_stock_field_definition_if_missing_v189('erp_stock_record', 'totalWeight', '总重', 'report', 367);

DROP PROCEDURE IF EXISTS add_stock_field_definition_if_missing_v189;
