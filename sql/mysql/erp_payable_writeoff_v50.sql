-- ERP 应付账款核销 v50
-- 1. 新增应付账款核销记录表
-- 2. 新增应付账款核销菜单权限

CREATE TABLE IF NOT EXISTS `erp_payable_writeoff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `supplier_id` bigint NOT NULL COMMENT '供应商编号',
  `biz_type` int DEFAULT NULL COMMENT '业务类型',
  `biz_id` bigint DEFAULT NULL COMMENT '业务单据编号',
  `biz_no` varchar(64) DEFAULT NULL COMMENT '业务单据号',
  `write_off_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '核销金额',
  `remark` varchar(512) DEFAULT NULL COMMENT '核销备注',
  `write_off_time` datetime NOT NULL COMMENT '核销时间',
  `operator_user_id` bigint DEFAULT NULL COMMENT '核销人',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_supplier_time` (`supplier_id`, `write_off_time`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 应付账款核销记录';

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31115, '应付账款核销', 'erp:payable-account:writeoff', 3, 2, 31114, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31115, '1', NOW(), '1', NOW(), b'0', 1);
