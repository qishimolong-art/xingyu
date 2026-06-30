-- Customer archive remaining base options seed and migration (v81).
-- Requires erp_base_data.code from erp_base_data_code_v80.sql.
--
-- Scope:
-- 1. Seed customer_type and price_level with stable codes for Integer fields.
-- 2. Migrate SALES_AREA / ROUTE / FREIGHT_EXPLAIN from erp_sale_config into erp_base_data.
-- 3. Update erp_customer area_id / route_id / freight_explain_id from old sale_config ids
--    to new erp_base_data ids by tenant + stable code mapping.

SET @tenant_id := 1;

-- Pre-check: old sale config rows that will be migrated.
-- SELECT id, config_type, code, name, tenant_id, deleted
-- FROM erp_sale_config
-- WHERE config_type IN ('SALES_AREA', 'ROUTE', 'FREIGHT_EXPLAIN')
--   AND tenant_id = @tenant_id
--   AND deleted = b'0'
-- ORDER BY config_type, sort, id;

-- Pre-check: current customer references to old sale config ids.
-- SELECT COUNT(*) AS customer_area_old_ref_count
-- FROM erp_customer c
-- JOIN erp_sale_config sc ON sc.id = c.area_id
--  AND sc.config_type = 'SALES_AREA'
--  AND sc.tenant_id = c.tenant_id
--  AND sc.deleted = b'0'
-- WHERE c.tenant_id = @tenant_id
--   AND c.deleted = b'0';

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'customer_type' AS type, '车主' AS name, 'car_owner' AS code, 10 AS sort, 'ERP客户档案基础选项初始化' AS remark
    UNION ALL SELECT 'customer_type', '修理厂', 'repair_factory', 20, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'customer_type', '终端客户', 'terminal_customer', 30, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'customer_type', '既是供应商又是客户', 'customer_supplier', 40, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'customer_type', '供应商', 'supplier', 50, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'customer_type', '门店', 'store', 60, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'customer_type', '经销商', 'dealer', 70, 'ERP客户档案基础选项初始化'

    UNION ALL SELECT 'price_level', '备用价', 'spare_price', 10, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'price_level', '参考价', 'reference_price', 20, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'price_level', '零售价', 'retail_price', 30, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'price_level', '批发价', 'wholesale_price', 40, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'price_level', '最后一次采购入库价格', 'last_purchase_in_price', 50, 'ERP客户档案基础选项初始化'
    UNION ALL SELECT 'price_level', '库存成本价', 'stock_cost_price', 60, 'ERP客户档案基础选项初始化'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`code` = seed.code
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT mapped.base_type, sc.name, sc.code, sc.sort, sc.status,
       CONCAT('从销售配置迁移：', sc.config_type), '1', NOW(), '1', NOW(), b'0', sc.tenant_id
FROM `erp_sale_config` sc
JOIN (
    SELECT 'SALES_AREA' AS config_type, 'sales_area' AS base_type
    UNION ALL SELECT 'ROUTE', 'route'
    UNION ALL SELECT 'FREIGHT_EXPLAIN', 'freight_explain'
) mapped ON mapped.config_type = sc.config_type
WHERE sc.tenant_id = @tenant_id
  AND sc.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `erp_base_data` existing
      WHERE existing.`type` = mapped.base_type
        AND existing.`code` = sc.code
        AND existing.`tenant_id` = sc.tenant_id
        AND existing.`deleted` = b'0'
  );

UPDATE `erp_customer` c
JOIN `erp_sale_config` sc ON sc.id = c.area_id
    AND sc.config_type = 'SALES_AREA'
    AND sc.tenant_id = c.tenant_id
    AND sc.deleted = b'0'
JOIN `erp_base_data` bd ON bd.type = 'sales_area'
    AND bd.code = sc.code
    AND bd.tenant_id = c.tenant_id
    AND bd.deleted = b'0'
SET c.area_id = bd.id,
    c.updater = '1',
    c.update_time = NOW()
WHERE c.tenant_id = @tenant_id
  AND c.deleted = b'0'
  AND c.area_id IS NOT NULL;

UPDATE `erp_customer` c
JOIN `erp_sale_config` sc ON sc.id = c.route_id
    AND sc.config_type = 'ROUTE'
    AND sc.tenant_id = c.tenant_id
    AND sc.deleted = b'0'
JOIN `erp_base_data` bd ON bd.type = 'route'
    AND bd.code = sc.code
    AND bd.tenant_id = c.tenant_id
    AND bd.deleted = b'0'
SET c.route_id = bd.id,
    c.updater = '1',
    c.update_time = NOW()
WHERE c.tenant_id = @tenant_id
  AND c.deleted = b'0'
  AND c.route_id IS NOT NULL;

UPDATE `erp_customer` c
JOIN `erp_sale_config` sc ON sc.id = c.freight_explain_id
    AND sc.config_type = 'FREIGHT_EXPLAIN'
    AND sc.tenant_id = c.tenant_id
    AND sc.deleted = b'0'
JOIN `erp_base_data` bd ON bd.type = 'freight_explain'
    AND bd.code = sc.code
    AND bd.tenant_id = c.tenant_id
    AND bd.deleted = b'0'
SET c.freight_explain_id = bd.id,
    c.updater = '1',
    c.update_time = NOW()
WHERE c.tenant_id = @tenant_id
  AND c.deleted = b'0'
  AND c.freight_explain_id IS NOT NULL;

-- Post-check: managed customer options.
-- SELECT type, name, code, sort, status, tenant_id, deleted
-- FROM erp_base_data
-- WHERE tenant_id = @tenant_id
--   AND deleted = b'0'
--   AND type IN ('customer_type', 'price_level', 'sales_area', 'route', 'freight_explain')
-- ORDER BY type, sort, id;

-- Post-check: remaining old references after migration.
-- SELECT c.id, c.area_id, c.route_id, c.freight_explain_id
-- FROM erp_customer c
-- LEFT JOIN erp_sale_config area_sc ON area_sc.id = c.area_id
--  AND area_sc.config_type = 'SALES_AREA'
--  AND area_sc.tenant_id = c.tenant_id
--  AND area_sc.deleted = b'0'
-- LEFT JOIN erp_sale_config route_sc ON route_sc.id = c.route_id
--  AND route_sc.config_type = 'ROUTE'
--  AND route_sc.tenant_id = c.tenant_id
--  AND route_sc.deleted = b'0'
-- LEFT JOIN erp_sale_config freight_sc ON freight_sc.id = c.freight_explain_id
--  AND freight_sc.config_type = 'FREIGHT_EXPLAIN'
--  AND freight_sc.tenant_id = c.tenant_id
--  AND freight_sc.deleted = b'0'
-- WHERE c.tenant_id = @tenant_id
--   AND c.deleted = b'0'
--   AND (area_sc.id IS NOT NULL OR route_sc.id IS NOT NULL OR freight_sc.id IS NOT NULL);
