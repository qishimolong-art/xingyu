CREATE TABLE IF NOT EXISTS `system_role_form_data_scope` (
  `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `role_id`             BIGINT       NOT NULL COMMENT '角色编号',
  `form_key`            VARCHAR(64)  NOT NULL COMMENT '表单标识，对应 system_field_definition.module',
  `data_scope`          TINYINT      NOT NULL COMMENT '数据范围，参见 DataScopeEnum',
  `data_scope_dept_ids` VARCHAR(512) NOT NULL DEFAULT '[]' COMMENT '自定义部门编号列表，JSON 数组',
  `tenant_id`           BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  `creator`             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`             VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '更新者',
  `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_form` (`role_id`, `form_key`, `tenant_id`, `deleted`)
) ENGINE=InnoDB COMMENT='角色表单级数据权限';
