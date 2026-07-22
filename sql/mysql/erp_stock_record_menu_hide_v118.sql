-- ============================================================
-- 隐藏库存进出流水明细账独立菜单入口
-- 说明：
--   1. 仅隐藏菜单 id=2593，不删除菜单、不禁用路由、不改按钮权限。
--   2. 保留 erp:stock-record:query / erp:stock-record:export，供库存页明细弹窗查询和导出。
--   3. 执行后需刷新菜单缓存或重新登录。
-- ============================================================

UPDATE `system_menu`
SET `visible` = b'0',
    `updater` = '1',
    `update_time` = NOW()
WHERE `id` = 2593
  AND `component` = 'erp/stock/record/index'
  AND `component_name` = 'ErpStockRecord'
  AND `deleted` = b'0';
