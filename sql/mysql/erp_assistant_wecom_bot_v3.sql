-- 智能问数企业微信机器人绑定表。仅新增问数通道数据，不修改业务、菜单、角色或权限数据。
CREATE TABLE IF NOT EXISTS erp_assistant_wecom_binding (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  tenant_id bigint NOT NULL COMMENT '租户编号',
  corp_id varchar(64) NOT NULL COMMENT '企业微信企业 ID',
  bot_id varchar(128) NOT NULL COMMENT '智能机器人 BotID',
  wecom_user_id varchar(128) NOT NULL COMMENT '企业微信成员 UserId',
  user_id bigint NOT NULL COMMENT 'ERP 管理用户 ID',
  mobile varchar(32) NOT NULL COMMENT '绑定时手机号快照',
  conversation_id varchar(36) NULL COMMENT '当前问数会话 ID',
  bind_time datetime(6) NOT NULL COMMENT '绑定时间',
  last_active_time datetime(6) NOT NULL COMMENT '最后活跃时间',
  creator varchar(64) NULL DEFAULT '' COMMENT '创建者',
  create_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updater varchar(64) NULL DEFAULT '' COMMENT '更新者',
  update_time datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  deleted bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_assistant_wecom_user (tenant_id,bot_id,wecom_user_id,deleted),
  UNIQUE KEY uk_assistant_erp_user (tenant_id,bot_id,user_id,deleted),
  KEY idx_assistant_wecom_active (tenant_id,bot_id,last_active_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能问数企业微信用户绑定';

SELECT table_name,index_name,non_unique,GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_name
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='erp_assistant_wecom_binding'
GROUP BY table_name,index_name,non_unique
ORDER BY index_name;
