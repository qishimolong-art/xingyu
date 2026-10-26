-- v289 只读预检：模块业务字段、人工费成本快照。
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database,
       VERSION() AS mysql_version;

SELECT table_name,
       table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_recipe', 'erp_assembly_order', 'erp_product')
ORDER BY table_name;

SELECT COUNT(*) AS active_recipe_missing_product_identity
FROM erp_assembly_recipe r
LEFT JOIN erp_product p
       ON p.id = r.product_id
      AND p.tenant_id = r.tenant_id
WHERE r.deleted = b'0'
  AND (p.id IS NULL OR TRIM(COALESCE(p.code, '')) = '' OR TRIM(COALESCE(p.name, '')) = '');

SELECT r.tenant_id,
       p.code AS proposed_module_code,
       COUNT(*) AS duplicate_count
FROM erp_assembly_recipe r
JOIN erp_product p
     ON p.id = r.product_id
    AND p.tenant_id = r.tenant_id
WHERE r.deleted = b'0'
GROUP BY r.tenant_id, p.code
HAVING COUNT(*) > 1;

SELECT r.id AS recipe_id,
       r.no AS internal_recipe_no,
       r.product_id,
       p.code AS proposed_module_code,
       p.name AS proposed_module_name,
       r.status
FROM erp_assembly_recipe r
LEFT JOIN erp_product p
       ON p.id = r.product_id
      AND p.tenant_id = r.tenant_id
WHERE r.deleted = b'0'
ORDER BY r.id;

SELECT status,
       COUNT(*) AS order_count,
       COALESCE(SUM(total_cost), 0) AS original_total_cost
FROM erp_assembly_order
WHERE deleted = b'0'
GROUP BY status
ORDER BY status;

SELECT table_name,
       column_name,
       column_type,
       is_nullable,
       column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_recipe', 'erp_assembly_order')
  AND column_name IN (
    'purpose', 'module_code', 'module_name', 'labor_cost_per_set', 'active_module_code',
    'recipe_module_code', 'recipe_module_name', 'material_cost', 'labor_cost'
  )
ORDER BY table_name, ordinal_position;
