-- Sale out print template selectable fields (v235).
-- Safe to execute repeatedly. Existing field labels, order and field permissions are preserved.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module`, seed.`field_key`, seed.`field_label`, seed.`field_group`, seed.`sort`,
       '1', NOW(), '1', NOW(), b'0', seed.`tenant_id`
FROM (
    SELECT 'erp_sale_out' AS `module`, 'logisticsCompany' AS `field_key`, '物流公司' AS `field_label`,
           'main_form' AS `field_group`, 38 AS `sort`, 1 AS `tenant_id`
    UNION ALL SELECT 'erp_sale_out', 'customerAddress', '客户地址', 'main_form', 39, 1
    UNION ALL SELECT 'erp_sale_out', 'customerPhone', '客户电话', 'main_form', 40, 1
    UNION ALL SELECT 'erp_sale_out', 'previousReceivable', '此前应收', 'main_form', 41, 1
    UNION ALL SELECT 'erp_sale_out', 'currentDebt', '本次欠款', 'main_form', 42, 1
    UNION ALL SELECT 'erp_sale_out', 'totalDebt', '总欠款金额', 'main_form', 43, 1
    UNION ALL SELECT 'erp_sale_out', 'totalAmount', '合计总金额', 'main_form', 44, 1
    UNION ALL SELECT 'erp_sale_out', 'totalAmountUpper', '大写金额', 'main_form', 45, 1
    UNION ALL SELECT 'erp_sale_out', 'item_seq', '序号', 'detail_item', 1, 1
    UNION ALL SELECT 'erp_sale_out', 'billerName', '开单员', 'system_info', 901, 1
    UNION ALL SELECT 'erp_sale_out', 'pickerName', '拣货人', 'system_info', 902, 1
    UNION ALL SELECT 'erp_sale_out', 'checkerName', '验货人', 'system_info', 903, 1
    UNION ALL SELECT 'erp_sale_out', 'creatorName', '制单人', 'system_info', 900, 1
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` existing
    WHERE existing.`module` = seed.`module`
      AND existing.`field_key` = seed.`field_key`
      AND existing.`tenant_id` = seed.`tenant_id`
      AND existing.`deleted` = b'0'
);
