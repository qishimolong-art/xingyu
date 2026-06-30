-- Customer archive basic option seed data (v80).
-- Requires erp_base_data_code_v80.sql first.
-- Scope:
-- 1. category: customer relation type.
-- 2. settle_method, invoice_type, freight_type, delivery_method: customer form options.
--
-- The script is idempotent. It inserts missing active rows and only fills an
-- empty code for the exact target type/name rows.

SET @tenant_id := 1;

-- Pre-check:
-- SELECT type, name, code, sort, status, deleted, tenant_id
-- FROM erp_base_data
-- WHERE type IN ('category', 'settle_method', 'invoice_type', 'freight_type', 'delivery_method')
--   AND tenant_id = @tenant_id
-- ORDER BY type, sort, id;

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'category' AS type, '客户' AS name, 'customer' AS code, 40 AS sort, 'ERP客户档案基础选项初始化' AS remark
    UNION ALL SELECT 'category', '既是客户又是供应商', 'customer_supplier', 60, 'ERP客户档案基础选项初始化'

    UNION ALL SELECT 'settle_method', '现金', 'cash', 10, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'settle_method', '挂账', 'credit', 20, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'settle_method', '汇款', 'remittance', 30, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'settle_method', '网上支付', 'online_payment', 40, 'ERP客户档案基础选项初始化'

    UNION ALL SELECT 'invoice_type', '收据', 'receipt', 10, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'invoice_type', '普通发票', 'normal_invoice', 30, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'invoice_type', '增值税发票', 'vat_invoice', 40, 'ERP客户档案基础选项初始化'

    UNION ALL SELECT 'freight_type', '代客户付', 'pay_for_customer', 40, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '其他', 'other', 50, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '我方自付', 'self_pay', 60, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '客户自付', 'customer_pay', 70, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '送货补贴', 'delivery_subsidy', 80, 'ERP客户档案基础选项初始化'

    UNION ALL SELECT 'delivery_method', '客户自提', 'customer_pickup', 10, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'delivery_method', '配送服务', 'delivery_service', 20, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'delivery_method', '物流托运', 'logistics_consignment', 30, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'delivery_method', '送货上门', 'door_delivery', 50, 'ERP客户档案基础选项初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

UPDATE `erp_base_data` target
JOIN (
    SELECT 'category' AS type, '客户' AS name, 'customer' AS code
    UNION ALL SELECT 'category', '既是客户又是供应商', 'customer_supplier'
    UNION ALL SELECT 'settle_method', '现金', 'cash'
    UNION ALL SELECT 'settle_method', '挂账', 'credit'
    UNION ALL SELECT 'settle_method', '汇款', 'remittance'
    UNION ALL SELECT 'settle_method', '网上支付', 'online_payment'
    UNION ALL SELECT 'invoice_type', '收据', 'receipt'
    UNION ALL SELECT 'invoice_type', '普通发票', 'normal_invoice'
    UNION ALL SELECT 'invoice_type', '增值税发票', 'vat_invoice'
    UNION ALL SELECT 'freight_type', '代客户付', 'pay_for_customer'
    UNION ALL SELECT 'freight_type', '其他', 'other'
    UNION ALL SELECT 'freight_type', '我方自付', 'self_pay'
    UNION ALL SELECT 'freight_type', '客户自付', 'customer_pay'
    UNION ALL SELECT 'freight_type', '送货补贴', 'delivery_subsidy'
    UNION ALL SELECT 'delivery_method', '客户自提', 'customer_pickup'
    UNION ALL SELECT 'delivery_method', '配送服务', 'delivery_service'
    UNION ALL SELECT 'delivery_method', '物流托运', 'logistics_consignment'
    UNION ALL SELECT 'delivery_method', '送货上门', 'door_delivery'
) seed ON target.`type` = seed.type
      AND target.`name` = seed.name
      AND target.`tenant_id` = @tenant_id
      AND target.`deleted` = b'0'
   SET target.`code` = seed.code,
       target.`updater` = '1',
       target.`update_time` = NOW()
 WHERE target.`code` IS NULL OR target.`code` = '';

-- Post-check:
-- SELECT type, name, code
-- FROM erp_base_data
-- WHERE deleted = b'0'
--   AND tenant_id = @tenant_id
--   AND type IN ('category', 'settle_method', 'invoice_type', 'freight_type', 'delivery_method')
-- ORDER BY type, sort, id;
