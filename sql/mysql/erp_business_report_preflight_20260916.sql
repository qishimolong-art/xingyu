-- 经营报表公共底座上线预检（MySQL 8），只读，2026-09-16。
-- 不执行迁移、不授角色、不启用新核算。请在目标环境由授权运维账号运行并保存各结果集。
-- 依赖现有 system_menu/system_role_menu/system_tenant_package；核算新表缺失只报状态。
-- 预检 PASS 不是端到端验收；运行时配置、字段/数据权限、登录缓存必须另测。
-- 禁止将本文件所在目录整目录执行。旧 v175/v202/v207/v220 都不是纯新增入口脚本。

-- 1. 当前数据库与版本，确认目标环境；空 DATABASE 应先选定库。
SELECT DATABASE() AS target_schema, VERSION() AS mysql_version;

-- 2. 现有报表叶、真实父节点和路由。MISSING 是待核对，并不授权自动补建。
WITH expected AS (
SELECT 'erp/sale/report/index' AS component
UNION ALL SELECT 'erp/purchase/report/index'
UNION ALL SELECT 'erp/stock/report/index'
UNION ALL SELECT 'erp/stock/record/index'
UNION ALL SELECT 'erp/stock/transfer-ledger/index'
UNION ALL SELECT 'erp/finance/receivable/report/index'
UNION ALL SELECT 'erp/finance/payable/report/index'
UNION ALL SELECT 'erp/report/system/index'
UNION ALL SELECT 'erp/report/account-fund/index'
UNION ALL SELECT 'erp/report/expense-income/index'
UNION ALL SELECT 'erp/report/financial-statement/index'
UNION ALL SELECT 'erp/report/fixed-asset/index'
UNION ALL SELECT 'erp/report/audit-trace-performance/index'
UNION ALL SELECT 'erp/report/financial/report-template/index'
UNION ALL SELECT 'erp/report/financial/balance-sheet/index'
UNION ALL SELECT 'erp/report/financial/income-statement/index'
UNION ALL SELECT 'erp/report/financial/cash-flow/index'
)
SELECT e.component, COUNT(m.id) AS active_matches,
       CASE WHEN COUNT(m.id)=0 THEN 'MISSING' WHEN COUNT(m.id)=1 THEN 'PRESENT' ELSE 'DUPLICATE_REVIEW' END AS check_status,
       GROUP_CONCAT(CONCAT(m.id, ':',m.name, ':parent=',m.parent_id, ':path=',m.path) ORDER BY m.id SEPARATOR ' | ') AS menu_locations
FROM expected e LEFT JOIN system_menu m ON m.component=e.component AND m.type=2 AND m.deleted=b'0'
GROUP BY e.component ORDER BY e.component;

-- 3. 查询/导出/成本确认按钮存在性；不能仅因存在就视为用户已授权。
WITH expected AS (
SELECT 'erp:sale-report:query' AS permission
UNION ALL SELECT 'erp:sale-report:export'
UNION ALL SELECT 'erp:purchase-report:query'
UNION ALL SELECT 'erp:purchase-report:export'
UNION ALL SELECT 'erp:system-report:query'
UNION ALL SELECT 'erp:system-report:export'
UNION ALL SELECT 'erp:stock:query'
UNION ALL SELECT 'erp:stock:export'
UNION ALL SELECT 'erp:stock-record:query'
UNION ALL SELECT 'erp:stock-record:export'
UNION ALL SELECT 'erp:sale-report-v2:query'
UNION ALL SELECT 'erp:sale-report-v2:export'
UNION ALL SELECT 'erp:purchase-report-v2:query'
UNION ALL SELECT 'erp:purchase-report-v2:export'
UNION ALL SELECT 'erp:report-stock-opening:confirm'
)
SELECT e.permission, COUNT(m.id) AS active_matches,
       CASE WHEN COUNT(m.id)=0 THEN 'MISSING' WHEN COUNT(m.id)>1 THEN 'DUPLICATE_REVIEW'
            WHEN MIN(m.type)<>3 THEN 'TYPE_REVIEW' WHEN MAX(m.status)<>0 THEN 'DISABLED_REVIEW' ELSE 'PRESENT' END AS check_status,
       GROUP_CONCAT(CONCAT(m.id, ':parent=',m.parent_id) ORDER BY m.id) AS menu_ids
FROM expected e LEFT JOIN system_menu m ON m.permission=e.permission AND m.deleted=b'0'
GROUP BY e.permission ORDER BY e.permission;

-- 4. 当前经营报表树（最多16层）；不猜绝对URL拼接规则，不返回金额或业务数据。
WITH RECURSIVE report_tree AS (
 SELECT m.id,m.parent_id,m.name,m.type,m.path,m.component,m.permission,m.status,m.visible,0 AS depth
 FROM system_menu m JOIN system_menu p ON p.id=m.parent_id AND p.deleted=b'0'
 WHERE m.deleted=b'0' AND m.path='business-report' AND p.path='/erp' AND p.parent_id=0
 UNION ALL
 SELECT c.id,c.parent_id,c.name,c.type,c.path,c.component,c.permission,c.status,c.visible,t.depth+1
 FROM system_menu c JOIN report_tree t ON c.parent_id=t.id WHERE c.deleted=b'0' AND t.depth<16
)
SELECT * FROM report_tree ORDER BY depth,parent_id,id;

-- 5. 当前报表子树中“角色有子节点但缺直接父节点”的清单。
-- 缺父级只是人工核对项，不自动扩大原角色权限；结果带tenant避免跨租户混淆。
WITH RECURSIVE report_ids AS (
 SELECT m.id,m.parent_id,0 AS depth FROM system_menu m JOIN system_menu p ON p.id=m.parent_id AND p.deleted=b'0'
 WHERE m.deleted=b'0' AND m.path='business-report' AND p.path='/erp' AND p.parent_id=0
 UNION ALL
 SELECT c.id,c.parent_id,t.depth+1 FROM system_menu c JOIN report_ids t ON c.parent_id=t.id
 WHERE c.deleted=b'0' AND t.depth<16
)
SELECT DISTINCT rm.tenant_id,rm.role_id,t.id AS granted_menu_id,t.parent_id AS missing_parent_id
FROM report_ids t JOIN system_role_menu rm ON rm.menu_id=t.id AND rm.deleted=b'0'
WHERE t.parent_id<>0 AND NOT EXISTS (
 SELECT 1 FROM system_role_menu parent_grant WHERE parent_grant.tenant_id=rm.tenant_id
   AND parent_grant.role_id=rm.role_id AND parent_grant.menu_id=t.parent_id AND parent_grant.deleted=b'0'
) ORDER BY rm.tenant_id,rm.role_id,t.id;

-- 6. 租户套餐格式，不在预检时JSON_APPEND/自动授予任何菜单。
SELECT id AS package_id,
 CASE WHEN menu_ids IS NULL OR JSON_VALID(menu_ids)=0 THEN 'INVALID_JSON'
      WHEN JSON_TYPE(menu_ids)<>'ARRAY' THEN 'NOT_ARRAY' ELSE 'VALID_ARRAY' END AS check_status
FROM system_tenant_package WHERE deleted=b'0' ORDER BY id;

-- 7. 结构存在性仅由information_schema读取，不SELECT未部署业务表。
WITH expected AS (
SELECT 'erp_sale_cart_transfer_link' AS table_name
UNION ALL SELECT 'erp_stock_dual_cost_balance'
UNION ALL SELECT 'erp_stock_dual_cost_posting'
UNION ALL SELECT 'erp_stock_dual_cost_origin'
UNION ALL SELECT 'erp_business_report_item_snapshot'
)
SELECT e.table_name,CASE WHEN t.table_name IS NULL THEN 'NOT_INSTALLED' ELSE 'PRESENT_STRUCTURE_NOT_VERIFIED' END AS check_status
FROM expected e LEFT JOIN information_schema.tables t ON t.table_schema=DATABASE() AND t.table_name=e.table_name
ORDER BY e.table_name;

-- 8. 游标列/两个触发器只做清单。PRESENT不是触发器定义、顺序或水位正确的证明。
SELECT table_name,column_name,column_type,is_nullable,column_default FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='erp_stock' AND column_name='legacy_record_cursor_id';
SELECT trigger_name,event_object_table,event_manipulation,action_timing,action_order
FROM information_schema.triggers WHERE trigger_schema=DATABASE()
 AND trigger_name IN ('erp_stock_record_cursor_ai','erp_stock_record_cursor_bu') ORDER BY trigger_name;



