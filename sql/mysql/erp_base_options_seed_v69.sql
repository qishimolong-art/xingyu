-- ERP basic option seed data for fields switched to erp_base_data.
-- Scope:
-- 1. invoice_type: purchase invoice, purchase return, sale return
-- 2. settle_method: purchase return, sale return, sale price adjust
-- 3. delivery_method: sale return, sale price adjust
--
-- The script is idempotent. It inserts missing active rows only and does not
-- overwrite data maintained from the Basic Data > Basic Options page.

SET @tenant_id := 1;

-- Pre-check:
-- SELECT type, name, sort, status, deleted, tenant_id
-- FROM erp_base_data
-- WHERE type IN ('invoice_type', 'settle_method', 'delivery_method')
--   AND tenant_id = @tenant_id
-- ORDER BY type, sort, id;

INSERT INTO `erp_base_data`
(`type`, `name`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'settle_method' AS type, '现金' AS name, 10 AS sort, 'ERP基础选项初始化' AS remark
    UNION ALL SELECT 'settle_method', '挂账', 20, 'ERP基础选项初始化'
    UNION ALL SELECT 'settle_method', '汇款', 30, 'ERP基础选项初始化'
    UNION ALL SELECT 'settle_method', '网上支付', 40, 'ERP基础选项初始化'

    UNION ALL SELECT 'invoice_type', '收据', 10, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '收据和不开票', 20, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '普通发票', 30, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '增值税发票', 40, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '增值税专用发票', 50, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '增值税普通发票', 60, 'ERP基础选项初始化'
    UNION ALL SELECT 'invoice_type', '不开票', 70, 'ERP基础选项初始化'

    UNION ALL SELECT 'delivery_method', '客户自提', 10, 'ERP基础选项初始化'
    UNION ALL SELECT 'delivery_method', '配送服务', 20, 'ERP基础选项初始化'
    UNION ALL SELECT 'delivery_method', '配送服务物流托运', 30, 'ERP基础选项初始化'
    UNION ALL SELECT 'delivery_method', '物流配送', 40, 'ERP基础选项初始化'
    UNION ALL SELECT 'delivery_method', '送货上门', 50, 'ERP基础选项初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

-- Post-check:
-- SELECT type, COUNT(*) AS cnt
-- FROM erp_base_data
-- WHERE deleted = b'0'
--   AND tenant_id = @tenant_id
--   AND type IN ('invoice_type', 'settle_method', 'delivery_method')
-- GROUP BY type;
