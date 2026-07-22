-- ERP base option business type split (v82).
-- Splits supplier/customer relation category options into independent types.
-- Requires erp_base_data_code_v80.sql before this script.

SET @tenant_id := 1;

-- Pre-check:
-- SELECT type, name, code, sort, status, deleted, tenant_id
-- FROM erp_base_data
-- WHERE deleted = b'0'
--   AND tenant_id = @tenant_id
--   AND type IN ('category', 'supplier_category', 'customer_category')
-- ORDER BY type, sort, id;

-- Supplier category used to read the shared legacy category type. Copy active
-- legacy rows first so existing supplier custom categories remain selectable.
INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'supplier_category', legacy.`name`, legacy.`code`, legacy.`sort`, legacy.`status`,
       'ERP基础选项业务专用类型拆分', '1', NOW(), '1', NOW(), b'0', legacy.`tenant_id`
FROM `erp_base_data` legacy
WHERE legacy.`type` = 'category'
  AND legacy.`tenant_id` = @tenant_id
  AND legacy.`deleted` = b'0'
  AND legacy.`name` <> '客户'
  AND (legacy.`code` IS NULL OR legacy.`code` <> 'customer')
  AND NOT EXISTS (
      SELECT 1
      FROM `erp_base_data` existing
      WHERE existing.`type` = 'supplier_category'
        AND existing.`name` = legacy.`name`
        AND existing.`tenant_id` = legacy.`tenant_id`
        AND existing.`deleted` = b'0'
  );

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'supplier_category' AS type, '供应商' AS name, 'supplier' AS code, 50 AS sort, 'ERP基础选项业务专用类型拆分' AS remark
    UNION ALL SELECT 'supplier_category', '既是客户又是供应商', 'customer_supplier', 60, 'ERP基础选项业务专用类型拆分'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`name` = seed.name
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

-- Customer relation type is a numeric field and only supports these stable
-- code mappings in the current frontend: customer -> 1, customer_supplier -> 2.
INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'customer_category' AS type, '客户' AS name, 'customer' AS code, 40 AS sort, 'ERP基础选项业务专用类型拆分' AS remark
    UNION ALL SELECT 'customer_category', '既是客户又是供应商', 'customer_supplier', 60, 'ERP基础选项业务专用类型拆分'
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
    SELECT 'supplier_category' AS type, '供应商' AS name, 'supplier' AS code
    UNION ALL SELECT 'supplier_category', '既是客户又是供应商', 'customer_supplier'
    UNION ALL SELECT 'customer_category', '客户', 'customer'
    UNION ALL SELECT 'customer_category', '既是客户又是供应商', 'customer_supplier'
) seed ON target.`type` = seed.type
      AND target.`name` = seed.name
      AND target.`tenant_id` = @tenant_id
      AND target.`deleted` = b'0'
   SET target.`code` = seed.code,
       target.`updater` = '1',
       target.`update_time` = NOW()
 WHERE target.`code` IS NULL OR target.`code` = '';

-- Post-check:
-- SELECT type, name, code, sort, status, deleted, tenant_id
-- FROM erp_base_data
-- WHERE deleted = b'0'
--   AND tenant_id = @tenant_id
--   AND type IN ('category', 'supplier_category', 'customer_category')
-- ORDER BY type, sort, id;
