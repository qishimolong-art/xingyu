-- Sale out print template main fields (v212).
-- Safe to execute repeatedly. Existing field labels and order are preserved.

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module`, seed.`field_key`, seed.`field_label`, seed.`field_group`, seed.`sort`,
       '1', NOW(), '1', NOW(), b'0', seed.`tenant_id`
FROM (
    SELECT 'erp_sale_out' AS `module`, 'customerCode' AS `field_key`, '客户编码' AS `field_label`,
           'main_form' AS `field_group`, 32 AS `sort`, 1 AS `tenant_id`
    UNION ALL SELECT 'erp_sale_out', 'settleStatus', '结算状态', 'main_form', 33, 1
    UNION ALL SELECT 'erp_sale_out', 'status', '状态', 'main_form', 34, 1
    UNION ALL SELECT 'erp_sale_out', 'receiverName', '收货人', 'main_form', 36, 1
    UNION ALL SELECT 'erp_sale_out', 'receiverPhone', '收货人电话', 'main_form', 37, 1
    UNION ALL SELECT 'erp_sale_out', 'logisticsCompany', '物流公司', 'main_form', 38, 1
    UNION ALL SELECT 'erp_sale_out', 'shipper', '发货方', 'main_form', 39, 1
    UNION ALL SELECT 'erp_sale_out', 'senderName', '发货人', 'main_form', 45, 1
    UNION ALL SELECT 'erp_sale_out', 'settleMethod', '结算方式', 'main_form', 62, 1
    UNION ALL SELECT 'erp_sale_out', 'freight', '运费', 'main_form', 116, 1
    UNION ALL SELECT 'erp_sale_out', 'billType', '票据类型', 'main_form', 117, 1
    UNION ALL SELECT 'erp_sale_out', 'billNo', '票据号', 'main_form', 118, 1
    UNION ALL SELECT 'erp_sale_out', 'reductionAmount', '减收金额', 'main_form', 119, 1
    UNION ALL SELECT 'erp_sale_out', 'afterReductionAmount', '减后金额', 'main_form', 120, 1
    UNION ALL SELECT 'erp_sale_out', 'billAmount', '票据金额', 'main_form', 121, 1
    UNION ALL SELECT 'erp_sale_out', 'cancelCount', '取消数量', 'main_form', 122, 1
    UNION ALL SELECT 'erp_sale_out', 'cancelAmount', '取消金额', 'main_form', 123, 1
    UNION ALL SELECT 'erp_sale_out', 'afterCancelAmount', '取消后金额', 'main_form', 124, 1
    UNION ALL SELECT 'erp_sale_out', 'feeAmount', '额外费用', 'main_form', 125, 1
    UNION ALL SELECT 'erp_sale_out', 'totalWeight', '总重', 'main_form', 126, 1
    UNION ALL SELECT 'erp_sale_out', 'printCount', '打印次数', 'main_form', 127, 1
    UNION ALL SELECT 'erp_sale_out', 'sourceCreateTime', '来源单制单日期', 'main_form', 128, 1
    UNION ALL SELECT 'erp_sale_out', 'sourceCreatorName', '来源单制单人', 'main_form', 129, 1
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_field_definition` existing
    WHERE existing.`module` = seed.`module`
      AND existing.`field_key` = seed.`field_key`
      AND existing.`tenant_id` = seed.`tenant_id`
      AND existing.`deleted` = b'0'
);
