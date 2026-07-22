-- ERP 应收/应付账款旧核销入口下线 v119
-- 收款单、付款单是唯一的核销入口；保留历史核销流水、字段和旧接口用于兼容。
-- 可重复执行，仅处理两个明确的旧按钮权限，不影响其它菜单或角色授权。

UPDATE system_role_menu rm
INNER JOIN system_menu m ON m.id = rm.menu_id
SET rm.deleted = b'1',
    rm.updater = 'system',
    rm.update_time = NOW()
WHERE rm.deleted = b'0'
  AND m.permission IN (
    'erp:receivable-account:writeoff',
    'erp:payable-account:writeoff'
  );

UPDATE system_menu
SET deleted = b'1',
    updater = 'system',
    update_time = NOW()
WHERE deleted = b'0'
  AND permission IN (
    'erp:receivable-account:writeoff',
    'erp:payable-account:writeoff'
  );

-- 执行后应返回 0 行；新核销与反核销权限不会被本脚本处理。
SELECT id, name, permission
FROM system_menu
WHERE deleted = b'0'
  AND permission IN (
    'erp:receivable-account:writeoff',
    'erp:payable-account:writeoff'
  );
