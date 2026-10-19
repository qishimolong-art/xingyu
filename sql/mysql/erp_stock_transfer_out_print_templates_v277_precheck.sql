-- 调拨出库单打印模板接入前检查（v277）。
-- 目标：本地配置指向的开发库 ruoyi-vue-pro，租户 1。
-- 只读检查，不修改任何数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS database_name, @@hostname AS database_host, CURRENT_USER() AS database_user;

SELECT COUNT(*) AS print_template_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name = 'erp_print_template';

SELECT id, name, status
FROM system_tenant
WHERE id = 1
  AND deleted = b'0';

SELECT id, module_key, name, defaulted + 0 AS defaulted, status, tenant_id,
       CHAR_LENGTH(template_json) AS template_json_length,
       JSON_VALID(template_json) AS template_json_valid,
       paper_config
FROM erp_print_template
WHERE module_key = 'stock_transfer_out'
  AND tenant_id = 1
  AND deleted = b'0'
ORDER BY defaulted DESC, id;

SELECT name, COUNT(*) AS duplicate_count
FROM erp_print_template
WHERE module_key = 'stock_transfer_out'
  AND tenant_id = 1
  AND deleted = b'0'
  AND name IN ('调拨出库单-无价', '调拨出库单-带价')
GROUP BY name
HAVING COUNT(*) > 1;

