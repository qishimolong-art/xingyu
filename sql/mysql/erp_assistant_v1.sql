-- Apply to isolated test environment first. No role grants or business-table changes.
CREATE TABLE IF NOT EXISTS erp_assistant_conversation (
 id varchar(36) NOT NULL PRIMARY KEY,
 tenant_id bigint NOT NULL, user_id bigint NOT NULL,
 title varchar(80) NOT NULL, deleted bit NOT NULL DEFAULT b'0',
 create_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 update_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_owner (tenant_id,user_id,deleted,update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS erp_assistant_message (
 id varchar(36) NOT NULL PRIMARY KEY, conversation_id varchar(36) NOT NULL,
 tenant_id bigint NOT NULL, user_id bigint NOT NULL,
 question varchar(2000) NOT NULL, plan_json text NULL,
 status varchar(32) NOT NULL, deleted bit NOT NULL DEFAULT b'0',
 create_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_conversation (tenant_id,user_id,conversation_id,deleted,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS erp_assistant_audit (
 id varchar(36) NOT NULL PRIMARY KEY, message_id varchar(36) NOT NULL,
 tenant_id bigint NOT NULL, user_id bigint NOT NULL,
 metric varchar(32) NULL, knowledge_version varchar(80) NOT NULL,
 status varchar(32) NOT NULL, condition_summary varchar(1000) NULL, elapsed_ms bigint NOT NULL DEFAULT 0,
 tokens int NOT NULL DEFAULT 0, deleted bit NOT NULL DEFAULT b'0',
 create_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 KEY idx_audit_owner (tenant_id,user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ERP child menu. Precheck exactly one enabled ERP root and at most one active
-- assistant menu before execution. No role grants or unrelated menu updates.
INSERT INTO system_menu (name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater,deleted)
SELECT '智能问数','erp:assistant:query',2,99,erp.id,'assistant','lucide:message-square','erp/assistant/index','ErpAssistant',0,b'1',b'1',b'1','assistant-migration','assistant-migration',b'0'
FROM system_menu erp
WHERE erp.parent_id=0 AND erp.path='/erp' AND erp.type=1 AND erp.status=0 AND erp.deleted=b'0'
AND (SELECT COUNT(*) FROM system_menu root WHERE root.parent_id=0 AND root.path='/erp' AND root.type=1 AND root.status=0 AND root.deleted=b'0')=1
AND NOT EXISTS (SELECT 1 FROM system_menu WHERE permission='erp:assistant:query' AND deleted=b'0');

-- Preserve the original menu ID and role memberships when moving the v1 root menu.
UPDATE system_menu assistant
JOIN system_menu erp ON erp.parent_id=0 AND erp.path='/erp' AND erp.type=1 AND erp.status=0 AND erp.deleted=b'0'
SET assistant.parent_id=erp.id, assistant.path='assistant', assistant.updater='assistant-migration'
WHERE assistant.permission='erp:assistant:query' AND assistant.deleted=b'0'
AND assistant.type=2 AND assistant.component='erp/assistant/index'
AND assistant.parent_id=0 AND assistant.path='/erp/assistant';
