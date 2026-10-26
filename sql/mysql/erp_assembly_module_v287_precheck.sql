-- v287 precheck：组装配方与套件组装单迁移前只读检查。
-- 目标：确认对象不存在或可安全复用；本脚本不修改任何数据。

SELECT DATABASE() AS target_database, NOW() AS checked_at;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN ('erp_assembly_recipe', 'erp_assembly_recipe_item',
                     'erp_assembly_order', 'erp_assembly_order_item');

SELECT table_name, column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'erp_stock_record' AND column_name = 'source_record_id')
       OR table_name IN ('erp_assembly_recipe', 'erp_assembly_recipe_item',
                         'erp_assembly_order', 'erp_assembly_order_item'))
ORDER BY table_name, ordinal_position;

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS indexed_columns
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'erp_stock_record'
  AND column_name = 'source_record_id'
GROUP BY index_name
ORDER BY index_name;

SELECT id, name, permission, type, parent_id, path, component, deleted
FROM system_menu
WHERE deleted = b'0'
  AND (permission LIKE 'erp:assembly-%'
       OR (name IN ('模块管理', '套件组装单') AND path IN ('assembly-recipe', 'assembly-order')))
ORDER BY id;

SELECT dict_type, value, label, COUNT(*) AS duplicate_count
FROM system_dict_data
WHERE deleted = b'0'
  AND dict_type = 'erp_stock_record_biz_type'
  AND value IN ('100', '101', '102', '103')
GROUP BY dict_type, value, label;

SELECT permission, COUNT(*) AS duplicate_count
FROM system_menu
WHERE deleted = b'0' AND permission IN (
  'erp:assembly-recipe:query', 'erp:assembly-recipe:create',
  'erp:assembly-recipe:update', 'erp:assembly-recipe:delete',
  'erp:assembly-order:query', 'erp:assembly-order:create',
  'erp:assembly-order:update', 'erp:assembly-order:delete',
  'erp:assembly-order:complete', 'erp:assembly-order:void'
)
GROUP BY permission
HAVING COUNT(*) > 1;

SELECT id, name, parent_id, path, component
FROM system_menu
WHERE deleted = b'0' AND type = 1 AND name = '库存管理' AND path = 'stock';
