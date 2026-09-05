-- ERP 应收管理 v30
-- 1. 新增应收管理相关单据表
-- 2. 新增财务管理下“应收管理”菜单

CREATE TABLE IF NOT EXISTS `erp_receivable_other` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 10,
  `biz_time` date NOT NULL,
  `customer_id` bigint NOT NULL,
  `voucher_no` varchar(64) DEFAULT NULL,
  `settled_amount` decimal(24,6) DEFAULT 0,
  `dept_id` bigint DEFAULT NULL,
  `receivable_amount` decimal(24,6) DEFAULT 0,
  `project` varchar(128) DEFAULT NULL,
  `source_type` varchar(64) DEFAULT NULL,
  `handler_id` bigint DEFAULT NULL,
  `receivable_type` varchar(64) DEFAULT NULL,
  `cost_amount` decimal(24,6) DEFAULT 0,
  `remark` varchar(512) DEFAULT NULL,
  `is_paper_note` bit(1) DEFAULT b'0',
  `paper_note_desc` varchar(512) DEFAULT NULL,
  `source_no` varchar(64) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他应收';

CREATE TABLE IF NOT EXISTS `erp_receivable_other_income` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 10,
  `biz_time` datetime NOT NULL,
  `settle_method` varchar(64) NOT NULL,
  `account_id` bigint NOT NULL,
  `voucher_no` varchar(64) DEFAULT NULL,
  `income_type` varchar(64) NOT NULL,
  `total_amount` decimal(24,6) NOT NULL DEFAULT 0,
  `dept_id` bigint DEFAULT NULL,
  `handler_id` bigint NOT NULL,
  `party` varchar(128) DEFAULT NULL,
  `related_biz` varchar(256) DEFAULT NULL,
  `doc_type` varchar(64) DEFAULT '正常单据',
  `remark` varchar(512) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他收入';

CREATE TABLE IF NOT EXISTS `erp_receivable_other_income_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `income_id` bigint NOT NULL,
  `item_name` varchar(128) NOT NULL,
  `amount` decimal(24,6) NOT NULL DEFAULT 0,
  `invoice_no` varchar(64) DEFAULT NULL,
  `party` varchar(128) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `dept_id` bigint DEFAULT NULL,
  `biz_date` datetime DEFAULT NULL,
  `handler_id` bigint DEFAULT NULL,
  `qty` int DEFAULT 1,
  `freight_type` varchar(64) DEFAULT NULL,
  `remark` varchar(512) DEFAULT NULL,
  `file_url` varchar(512) DEFAULT NULL,
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_income_id` (`income_id`),
  KEY `idx_customer_id` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他收入明细';

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3100, '应收管理', '', 2, 3, 2645, 'receivable', 'ep:money',
 '', 'ErpReceivable',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3101, '应收账款', '', 2, 1, 3100, 'account', 'ep:histogram',
 'erp/finance/receivable/account/index', 'ErpReceivableAccount',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31014, '应收账款查询', 'erp:receivable-account:query', 3, 1, 3101, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3102, '其他应收', '', 2, 2, 3100, 'other-receivable', 'ep:document',
 'erp/finance/receivable/other-receivable/index', 'ErpReceivableOther',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3103, '其他应收查询', 'erp:receivable-other:query', 3, 1, 3102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3104, '其他应收创建', 'erp:receivable-other:create', 3, 2, 3102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3105, '其他应收修改', 'erp:receivable-other:update', 3, 3, 3102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3106, '其他应收删除', 'erp:receivable-other:delete', 3, 4, 3102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3107, '其他应收审核', 'erp:receivable-other:update-status', 3, 5, 3102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3108, '其他收入', '', 2, 3, 3100, 'other-income', 'ep:wallet',
 'erp/finance/receivable/other-income/index', 'ErpReceivableOtherIncome',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3109, '其他收入查询', 'erp:receivable-other-income:query', 3, 1, 3108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3110, '其他收入创建', 'erp:receivable-other-income:create', 3, 2, 3108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3111, '其他收入修改', 'erp:receivable-other-income:update', 3, 3, 3108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3112, '其他收入删除', 'erp:receivable-other-income:delete', 3, 4, 3108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3113, '其他收入审核', 'erp:receivable-other-income:update-status', 3, 5, 3108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 3100, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3101, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31014, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3102, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3103, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3104, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3105, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3106, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3107, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3108, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3109, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3110, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3111, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3112, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3113, '1', NOW(), '1', NOW(), b'0', 1);

-- 已执行过旧版 SQL 的库，可补执行以下修正语句：
UPDATE `system_menu`
SET `component` = '',
    `component_name` = 'ErpReceivable'
WHERE `id` = 3100;

UPDATE `system_menu`
SET `name` = '应收账款',
    `permission` = '',
    `type` = 2,
    `sort` = 1,
    `parent_id` = 3100,
    `path` = 'account',
    `icon` = 'ep:histogram',
    `component` = 'erp/finance/receivable/account/index',
    `component_name` = 'ErpReceivableAccount'
WHERE `id` = 3101;

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31014, '应收账款查询', 'erp:receivable-account:query', 3, 1, 3101, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31014, '1', NOW(), '1', NOW(), b'0', 1);
