-- 智能问数混合编排 v2。
-- 先在开发／测试库执行。仅扩展问数审计表和增加管理员审计按钮，不修改业务数据、不自动授权角色。

SET @assistant_schema = DATABASE();

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema=@assistant_schema AND table_name='erp_assistant_audit' AND column_name='route_type'),
  'SELECT 1',
  'ALTER TABLE erp_assistant_audit ADD COLUMN route_type varchar(32) NULL AFTER tokens'
);
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema=@assistant_schema AND table_name='erp_assistant_audit' AND column_name='tool_name'),
  'SELECT 1',
  'ALTER TABLE erp_assistant_audit ADD COLUMN tool_name varchar(128) NULL AFTER route_type'
);
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema=@assistant_schema AND table_name='erp_assistant_audit' AND column_name='sql_fingerprint'),
  'SELECT 1',
  'ALTER TABLE erp_assistant_audit ADD COLUMN sql_fingerprint char(64) NULL AFTER tool_name'
);
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema=@assistant_schema AND table_name='erp_assistant_audit' AND column_name='query_trace'),
  'SELECT 1',
  'ALTER TABLE erp_assistant_audit ADD COLUMN query_trace mediumtext NULL AFTER sql_fingerprint'
);
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

SET @ddl = IF(
  EXISTS (SELECT 1 FROM information_schema.columns
          WHERE table_schema=@assistant_schema AND table_name='erp_assistant_audit' AND column_name='row_count'),
  'SELECT 1',
  'ALTER TABLE erp_assistant_audit ADD COLUMN row_count int NULL AFTER query_trace'
);
PREPARE assistant_stmt FROM @ddl; EXECUTE assistant_stmt; DEALLOCATE PREPARE assistant_stmt;

-- 只有且仅有一个现存智能问数菜单时才插入按钮，避免挂到错误菜单。
INSERT INTO system_menu
  (name,permission,type,sort,parent_id,path,icon,component,component_name,status,
   visible,keep_alive,always_show,creator,updater,deleted)
SELECT
  '查看问数审计','erp:assistant:audit',3,1,assistant.id,'','','','',0,
  b'1',b'1',b'1','assistant-hybrid-v2','assistant-hybrid-v2',b'0'
FROM system_menu assistant
WHERE assistant.permission='erp:assistant:query' AND assistant.deleted=b'0'
  AND (SELECT COUNT(*) FROM system_menu
       WHERE permission='erp:assistant:query' AND deleted=b'0')=1
  AND NOT EXISTS (SELECT 1 FROM system_menu
                  WHERE permission='erp:assistant:audit' AND deleted=b'0');

-- 执行后核验：应返回 5 个扩展列、1 个审计权限；角色权限数量不应因本脚本改变。
SELECT column_name,column_type,is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit'
  AND column_name IN ('route_type','tool_name','sql_fingerprint','query_trace','row_count')
ORDER BY ordinal_position;

SELECT id,name,permission,type,parent_id,deleted
FROM system_menu
WHERE permission IN ('erp:assistant:query','erp:assistant:audit')
ORDER BY permission,id;
