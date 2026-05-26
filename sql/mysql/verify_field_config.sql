-- 验证字段配置功能部署情况

-- 1. 检查表是否创建
SELECT 'erp_field_config表' as 检查项,
       CASE WHEN COUNT(*) > 0 THEN '✅ 已创建' ELSE '❌ 未创建' END as 状态
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'ruoyi-vue-pro' AND TABLE_NAME = 'erp_field_config';

-- 2. 检查初始化数据
SELECT '字段配置数据' as 检查项,
       CONCAT('✅ 共 ', COUNT(*), ' 条记录') as 状态
FROM erp_field_config
WHERE tenant_id = 0;

-- 3. 按模块统计
SELECT module_key as 模块,
       COUNT(*) as 字段总数,
       SUM(CASE WHEN required = b'1' THEN 1 ELSE 0 END) as 必填字段数
FROM erp_field_config
WHERE tenant_id = 0
GROUP BY module_key
ORDER BY module_key;

-- 4. 检查菜单是否创建
SELECT '字段配置菜单' as 检查项,
       CASE WHEN COUNT(*) = 4 THEN '✅ 已创建（4条）' ELSE CONCAT('❌ 仅 ', COUNT(*), ' 条') END as 状态
FROM system_menu
WHERE id IN (2960, 2961, 2962, 2963) AND deleted = b'0';

-- 5. 检查权限授权
SELECT '超管权限授权' as 检查项,
       CASE WHEN COUNT(*) = 4 THEN '✅ 已授权（4条）' ELSE CONCAT('❌ 仅 ', COUNT(*), ' 条') END as 状态
FROM system_role_menu
WHERE role_id = 1 AND menu_id IN (2960, 2961, 2962, 2963) AND deleted = b'0';
