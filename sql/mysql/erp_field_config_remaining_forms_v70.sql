-- ERP field config remaining form seeds v70.
-- Scope: customer, sale_order, sale_out, sale_price_adjust.
-- Safe to execute repeatedly. This script only inserts missing active tenant-1 rows
-- and does not overwrite user-edited labels, required flags, visible flags, or sort.

DROP PROCEDURE IF EXISTS add_erp_field_config_visible_v70;

DELIMITER //
CREATE PROCEDURE add_erp_field_config_visible_v70()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_field_config'
          AND COLUMN_NAME = 'visible'
    ) THEN
        ALTER TABLE `erp_field_config`
            ADD COLUMN `visible` BIT(1) NOT NULL DEFAULT b'1' COMMENT '是否显示'
            AFTER `required`;
    END IF;
END //
DELIMITER ;

CALL add_erp_field_config_visible_v70();
DROP PROCEDURE IF EXISTS add_erp_field_config_visible_v70;

INSERT INTO `erp_field_config`
(`module_key`, `field_name`, `field_label`, `required`, `visible`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.`module_key`, seed.`field_name`, seed.`field_label`, seed.`required`, b'1', seed.`sort`, '1', NOW(), '1', NOW(), b'0', 1
FROM (
    SELECT 'customer' module_key, 'code' field_name, '客户编码' field_label, b'0' required, 10 sort UNION ALL
    SELECT 'customer', 'name', '客户名称', b'1', 30 UNION ALL
    SELECT 'customer', 'shortName', '简称', b'0', 40 UNION ALL
    SELECT 'customer', 'relationType', '往来类别', b'1', 60 UNION ALL
    SELECT 'customer', 'areaId', '区域', b'0', 70 UNION ALL
    SELECT 'customer', 'customerType', '客户类型', b'0', 80 UNION ALL
    SELECT 'customer', 'groupCustomer', '集团客户', b'0', 100 UNION ALL
    SELECT 'customer', 'enterpriseMatchStatus', '企业匹配', b'0', 110 UNION ALL
    SELECT 'customer', 'contact', '联系人', b'0', 200 UNION ALL
    SELECT 'customer', 'telephone', '电话', b'0', 210 UNION ALL
    SELECT 'customer', 'mobile', '手机', b'1', 220 UNION ALL
    SELECT 'customer', 'financeTelephone', '财务联系电话', b'0', 240 UNION ALL
    SELECT 'customer', 'addressAreaId', '地址', b'0', 250 UNION ALL
    SELECT 'customer', 'detailAddress', '详细地址', b'0', 260 UNION ALL
    SELECT 'customer', 'postCode', '邮政编码', b'0', 270 UNION ALL
    SELECT 'customer', 'accountName', '账户', b'0', 300 UNION ALL
    SELECT 'customer', 'settleMethod', '结算方式', b'1', 310 UNION ALL
    SELECT 'customer', 'settleLocked', '结算锁定', b'0', 320 UNION ALL
    SELECT 'customer', 'invoiceType', '开票类型', b'0', 330 UNION ALL
    SELECT 'customer', 'minOrderAmount', '起订金额', b'0', 340 UNION ALL
    SELECT 'customer', 'freightType', '运费类型', b'0', 350 UNION ALL
    SELECT 'customer', 'commissionRate', '佣金比例', b'0', 360 UNION ALL
    SELECT 'customer', 'commissionEnabled', '启用佣金', b'0', 370 UNION ALL
    SELECT 'customer', 'unifiedCreditCode', '统一信用代码', b'0', 400 UNION ALL
    SELECT 'customer', 'taxNo', '纳税人识别号', b'0', 410 UNION ALL
    SELECT 'customer', 'bankName', '开户行', b'0', 420 UNION ALL
    SELECT 'customer', 'invoiceBankName', '开票银行', b'0', 430 UNION ALL
    SELECT 'customer', 'invoiceBankAccount', '开票银行账号', b'0', 440 UNION ALL
    SELECT 'customer', 'invoiceAddress', '开票地址', b'0', 450 UNION ALL
    SELECT 'customer', 'invoiceTelephone', '开票电话', b'0', 460 UNION ALL
    SELECT 'customer', 'invoiceCompany', '开票单位', b'0', 470 UNION ALL
    SELECT 'customer', 'bankAccount', '银行账号', b'0', 480 UNION ALL
    SELECT 'customer', 'bankAddress', '开户地址', b'0', 490 UNION ALL
    SELECT 'customer', 'saleUserId', '所属业务员', b'0', 500 UNION ALL
    SELECT 'customer', 'developerUserId', '所属开发员', b'0', 510 UNION ALL
    SELECT 'customer', 'deptId', '所属部门', b'0', 520 UNION ALL
    SELECT 'customer', 'priceLevel', '价格级别', b'1', 530 UNION ALL
    SELECT 'customer', 'priceLocked', '价格锁定', b'0', 540 UNION ALL
    SELECT 'customer', 'creditLimit', '白条授信额度', b'0', 590 UNION ALL
    SELECT 'customer', 'logistics', '是否物流', b'0', 700 UNION ALL
    SELECT 'customer', 'transportMethod', '运输方式', b'0', 710 UNION ALL
    SELECT 'customer', 'routeId', '线路', b'0', 720 UNION ALL
    SELECT 'customer', 'freightExplainId', '运费说明', b'0', 730 UNION ALL
    SELECT 'customer', 'wubiCode', '五笔码', b'0', 800 UNION ALL
    SELECT 'customer', 'pinyinCode', '拼音码', b'0', 810 UNION ALL
    SELECT 'customer', 'memberCode', '会员编码', b'0', 820 UNION ALL
    SELECT 'customer', 'platformCode', '平台唯一码', b'0', 830 UNION ALL
    SELECT 'customer', 'status', '状态', b'0', 840 UNION ALL
    SELECT 'customer', 'remark', '备注', b'0', 850 UNION ALL

    SELECT 'sale_order', 'no', '订单单号', b'0', 10 UNION ALL
    SELECT 'sale_order', 'orderTime', '订单时间', b'1', 20 UNION ALL
    SELECT 'sale_order', 'customerId', '客户', b'1', 30 UNION ALL
    SELECT 'sale_order', 'orderType', '订单类型', b'0', 40 UNION ALL
    SELECT 'sale_order', 'saleUserId', '销售人员', b'0', 50 UNION ALL
    SELECT 'sale_order', 'deptId', '所属部门', b'0', 60 UNION ALL
    SELECT 'sale_order', 'remark', '备注', b'0', 70 UNION ALL
    SELECT 'sale_order', 'fileUrl', '附件', b'0', 80 UNION ALL
    SELECT 'sale_order', 'items', '销售产品清单', b'0', 90 UNION ALL
    SELECT 'sale_order', 'discountPercent', '优惠率', b'0', 100 UNION ALL
    SELECT 'sale_order', 'discountPrice', '付款优惠', b'0', 110 UNION ALL
    SELECT 'sale_order', 'feeAmount', '费用', b'0', 120 UNION ALL
    SELECT 'sale_order', 'totalPrice', '优惠后金额', b'0', 130 UNION ALL
    SELECT 'sale_order', 'accountId', '结算账户', b'0', 140 UNION ALL
    SELECT 'sale_order', 'depositPrice', '收取订金', b'0', 150 UNION ALL

    SELECT 'sale_out', 'no', '销售单号', b'0', 10 UNION ALL
    SELECT 'sale_out', 'sourceNo', '来源单号', b'0', 20 UNION ALL
    SELECT 'sale_out', 'outTime', '出库时间', b'0', 30 UNION ALL
    SELECT 'sale_out', 'status', '状态', b'0', 40 UNION ALL
    SELECT 'sale_out', 'settleStatus', '结算状态', b'0', 50 UNION ALL
    SELECT 'sale_out', 'customerName', '客户名称', b'0', 60 UNION ALL
    SELECT 'sale_out', 'customerCode', '客户编码', b'0', 70 UNION ALL
    SELECT 'sale_out', 'priority', '优先级', b'0', 80 UNION ALL
    SELECT 'sale_out', 'orderType', '订单类型', b'0', 90 UNION ALL
    SELECT 'sale_out', 'deliveryMethod', '送货方式', b'0', 100 UNION ALL
    SELECT 'sale_out', 'receiverName', '收货人', b'0', 110 UNION ALL
    SELECT 'sale_out', 'receiverPhone', '收货电话', b'0', 120 UNION ALL
    SELECT 'sale_out', 'logisticsCompany', '物流公司', b'0', 130 UNION ALL
    SELECT 'sale_out', 'logisticsNo', '物流单号', b'0', 140 UNION ALL
    SELECT 'sale_out', 'deliveryNo', '配送单号', b'0', 150 UNION ALL
    SELECT 'sale_out', 'shipper', '发货方', b'0', 160 UNION ALL
    SELECT 'sale_out', 'senderName', '发货人', b'0', 170 UNION ALL
    SELECT 'sale_out', 'thirdPartyNo', '第三方单号', b'0', 180 UNION ALL
    SELECT 'sale_out', 'thirdPartyUpstreamNo', '第三方上游单号', b'0', 190 UNION ALL
    SELECT 'sale_out', 'insuranceCompany', '保险公司', b'0', 200 UNION ALL
    SELECT 'sale_out', 'vin', 'VIN', b'0', 210 UNION ALL
    SELECT 'sale_out', 'settleMethod', '结算方式', b'0', 220 UNION ALL
    SELECT 'sale_out', 'freight', '运费', b'0', 230 UNION ALL
    SELECT 'sale_out', 'freightType', '运费类型', b'0', 235 UNION ALL
    SELECT 'sale_out', 'billType', '票据类型', b'0', 240 UNION ALL
    SELECT 'sale_out', 'billNo', '票据号', b'0', 250 UNION ALL
    SELECT 'sale_out', 'invoiceAmount', '开票金额', b'0', 260 UNION ALL
    SELECT 'sale_out', 'reductionAmount', '减收金额', b'0', 270 UNION ALL
    SELECT 'sale_out', 'afterReductionAmount', '减后金额', b'0', 280 UNION ALL
    SELECT 'sale_out', 'billAmount', '票据金额', b'0', 290 UNION ALL
    SELECT 'sale_out', 'cancelCount', '取消数量', b'0', 300 UNION ALL
    SELECT 'sale_out', 'cancelAmount', '取消金额', b'0', 310 UNION ALL
    SELECT 'sale_out', 'afterCancelAmount', '取消后金额', b'0', 320 UNION ALL
    SELECT 'sale_out', 'feeAmount', '费用', b'0', 330 UNION ALL
    SELECT 'sale_out', 'auditorName', '审核人', b'0', 340 UNION ALL
    SELECT 'sale_out', 'saleUserName', '业务员', b'0', 350 UNION ALL
    SELECT 'sale_out', 'approveTime', '审核时间', b'0', 360 UNION ALL
    SELECT 'sale_out', 'printTime', '打印时间', b'0', 370 UNION ALL
    SELECT 'sale_out', 'confirmTime', '确认时间', b'0', 380 UNION ALL
    SELECT 'sale_out', 'sourceCreateTime', '来源单制单日期', b'0', 390 UNION ALL
    SELECT 'sale_out', 'totalWeight', '总重', b'0', 400 UNION ALL
    SELECT 'sale_out', 'printCount', '打印次数', b'0', 410 UNION ALL
    SELECT 'sale_out', 'remark', '备注', b'0', 420 UNION ALL
    SELECT 'sale_out', 'internalNote', '内部说明', b'0', 430 UNION ALL

    SELECT 'sale_price_adjust', 'no', '调价单号', b'0', 10 UNION ALL
    SELECT 'sale_price_adjust', 'adjustDate', '调价日期', b'1', 20 UNION ALL
    SELECT 'sale_price_adjust', 'customerId', '客户', b'1', 30 UNION ALL
    SELECT 'sale_price_adjust', 'adjustUserId', '调价人', b'1', 40 UNION ALL
    SELECT 'sale_price_adjust', 'settleMethod', '结算方式', b'1', 50 UNION ALL
    SELECT 'sale_price_adjust', 'deliveryMethod', '送货方式', b'1', 60 UNION ALL
    SELECT 'sale_price_adjust', 'logisticsCompany', '物流公司', b'0', 70 UNION ALL
    SELECT 'sale_price_adjust', 'adjustType', '销售调价类型', b'0', 80 UNION ALL
    SELECT 'sale_price_adjust', 'deptId', '部门', b'0', 90 UNION ALL
    SELECT 'sale_price_adjust', 'remark', '备注', b'0', 100 UNION ALL
    SELECT 'sale_price_adjust', 'items', '调价产品清单', b'0', 110 UNION ALL
    SELECT 'sale_price_adjust', 'totalOriginalPrice', '调价前金额', b'0', 120 UNION ALL
    SELECT 'sale_price_adjust', 'totalAdjustedPrice', '调价后金额', b'0', 130 UNION ALL
    SELECT 'sale_price_adjust', 'totalAdjustPrice', '调价差额', b'0', 140 UNION ALL
    SELECT 'sale_price_adjust', 'sourceNo', '销售单号', b'0', 150 UNION ALL
    SELECT 'sale_price_adjust', 'status', '状态', b'0', 160 UNION ALL
    SELECT 'sale_price_adjust', 'creatorName', '创建人', b'0', 170 UNION ALL
    SELECT 'sale_price_adjust', 'createTime', '创建日期', b'0', 180 UNION ALL
    SELECT 'sale_price_adjust', 'updaterName', '修改人', b'0', 190 UNION ALL
    SELECT 'sale_price_adjust', 'updateTime', '修改日期', b'0', 200 UNION ALL
    SELECT 'sale_price_adjust', 'adjustUserName', '调价人', b'0', 210
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_field_config` cfg
    WHERE cfg.`tenant_id` = 1
      AND cfg.`module_key` = seed.`module_key`
      AND cfg.`field_name` = seed.`field_name`
      AND cfg.`deleted` = b'0'
);

SELECT `module_key`, COUNT(*) AS `field_count`
FROM `erp_field_config`
WHERE `tenant_id` = 1
  AND `deleted` = b'0'
  AND `module_key` IN ('customer', 'sale_order', 'sale_out', 'sale_price_adjust')
GROUP BY `module_key`
ORDER BY `module_key`;
