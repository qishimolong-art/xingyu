-- ERP 应付管理 v31
-- 1. 新增应付管理业务表
-- 2. 新增财务管理下“应付管理”菜单和权限

CREATE TABLE IF NOT EXISTS `erp_payable_other` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL COMMENT '单据编号（QTFK前缀）',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10=未审核 20=已审核 30=已反审',
  `biz_time` date NOT NULL COMMENT '开单日期',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `voucher_no` varchar(64) NOT NULL COMMENT '凭证号',
  `settled_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '已结金额（已付款）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `payable_amount` decimal(24,6) NOT NULL COMMENT '应付金额（支持正负数）',
  `project` varchar(128) DEFAULT NULL COMMENT '调账项目',
  `source_type` varchar(64) DEFAULT '调账' COMMENT '来源类型',
  `handler_id` bigint DEFAULT NULL COMMENT '经手人用户ID',
  `remark` varchar(512) NOT NULL COMMENT '调账原因备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 应付调账单';

CREATE TABLE IF NOT EXISTS `erp_payable_expense` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `no` varchar(64) NOT NULL COMMENT '单据编号（FYZF前缀）',
  `status` tinyint NOT NULL DEFAULT 10 COMMENT '10=未审核 20=已审核 30=已反审',
  `biz_time` date NOT NULL COMMENT '单据日期',
  `settle_method` varchar(64) NOT NULL COMMENT '结算方式',
  `account_id` bigint NOT NULL COMMENT '结算账户ID',
  `voucher_no` varchar(64) DEFAULT NULL COMMENT '凭证号',
  `expense_type` varchar(64) NOT NULL COMMENT '费用类型（差旅/办公/招待等）',
  `total_amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '总金额（汇总明细）',
  `dept_id` bigint DEFAULT NULL COMMENT '申请部门ID',
  `handler_id` bigint NOT NULL COMMENT '申请人用户ID',
  `party` varchar(128) DEFAULT NULL COMMENT '收款对象（单位/个人）',
  `related_biz` varchar(256) DEFAULT NULL COMMENT '相关业务描述',
  `doc_type` varchar(64) DEFAULT '正常单据' COMMENT '单据类型',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 费用支付单';

CREATE TABLE IF NOT EXISTS `erp_payable_expense_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expense_id` bigint NOT NULL COMMENT '费用支付单ID',
  `item_name` varchar(128) NOT NULL COMMENT '费用项目名称',
  `amount` decimal(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `invoice_no` varchar(64) DEFAULT NULL COMMENT '发票号',
  `party` varchar(128) DEFAULT NULL COMMENT '收款对象',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `biz_date` date DEFAULT NULL COMMENT '发生日期',
  `handler_id` bigint DEFAULT NULL COMMENT '申请人/业务员ID',
  `qty` int DEFAULT 1 COMMENT '单据数量',
  `expense_category` varchar(64) DEFAULT NULL COMMENT '费用分类',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `file_url` varchar(512) DEFAULT NULL COMMENT '附件URL',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_expense_id` (`expense_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 费用支付单明细';

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31100, '应付管理', '', 2, 4, 2645, 'payable', 'ep:wallet-filled',
 '', 'ErpPayable',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31114, '应付账款', '', 2, 1, 31100, 'account', 'ep:histogram',
 'erp/finance/payable/account/index', 'ErpPayableAccount',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31101, '应付账款查询', 'erp:payable-account:query', 3, 1, 31114, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31102, '其他应付', '', 2, 2, 31100, 'other', 'ep:document',
 'erp/finance/payable/other/index', 'ErpPayableOther',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31103, '其他应付查询', 'erp:payable-other:query', 3, 1, 31102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31104, '其他应付创建', 'erp:payable-other:create', 3, 2, 31102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31105, '其他应付修改', 'erp:payable-other:update', 3, 3, 31102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31106, '其他应付删除', 'erp:payable-other:delete', 3, 4, 31102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31107, '其他应付审核', 'erp:payable-other:update-status', 3, 5, 31102, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31108, '费用支付', '', 2, 3, 31100, 'expense', 'ep:credit-card',
 'erp/finance/payable/expense/index', 'ErpPayableExpense',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31109, '费用支付查询', 'erp:payable-expense:query', 3, 1, 31108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31110, '费用支付创建', 'erp:payable-expense:create', 3, 2, 31108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31111, '费用支付修改', 'erp:payable-expense:update', 3, 3, 31108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31112, '费用支付删除', 'erp:payable-expense:delete', 3, 4, 31108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31113, '费用支付审核', 'erp:payable-expense:update-status', 3, 5, 31108, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31100, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31114, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31101, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31102, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31103, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31104, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31105, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31106, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31107, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31108, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31109, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31110, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31111, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31112, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31113, '1', NOW(), '1', NOW(), b'0', 1);

-- 已执行过旧版 SQL 的库，可补执行以下修正语句：
UPDATE `system_menu`
SET `component` = '',
    `component_name` = 'ErpPayable'
WHERE `id` = 31100;

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31114, '应付账款', '', 2, 1, 31100, 'account', 'ep:histogram',
 'erp/finance/payable/account/index', 'ErpPayableAccount',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

UPDATE `system_menu`
SET `parent_id` = 31114
WHERE `id` = 31101;

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31114, '1', NOW(), '1', NOW(), b'0', 1);
