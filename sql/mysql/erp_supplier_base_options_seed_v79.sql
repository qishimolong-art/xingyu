-- Supplier archive basic option seed data.
-- Scope:
-- 1. category: current supplier form default/options.
-- 2. freight_type: new configurable freight type option.
--
-- The script is idempotent. It inserts missing active rows only and does not
-- overwrite data maintained from the Basic Data > Basic Options page.

SET @tenant_id := 1;

-- Pre-check:
-- SELECT type, name, sort, status, deleted, tenant_id
-- FROM erp_base_data
-- WHERE type IN ('category', 'freight_type')
--   AND tenant_id = @tenant_id
-- ORDER BY type, sort, id;

INSERT INTO `erp_base_data`
(`type`, `name`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'category' AS type, '供应商' AS name, 50 AS sort, 'ERP供应商档案基础选项初始化' AS remark
    UNION ALL SELECT 'category', '既是客户又是供应商', 60, 'ERP供应商档案基础选项初始化'

    UNION ALL SELECT 'freight_type', '卖方承担', 10, 'ERP供应商档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '买方承担', 20, 'ERP供应商档案基础选项初始化'
    UNION ALL SELECT 'freight_type', '运费对半', 30, 'ERP供应商档案基础选项初始化'
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
--   AND type IN ('category', 'freight_type')
-- GROUP BY type;
