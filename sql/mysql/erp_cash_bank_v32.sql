-- ERP 现金银行 v32
-- 1. 扩展 erp_account
-- 2. 新增 erp_finance_transfer
-- 3. 初始化现金银行菜单、权限、角色授权
-- 4. 初始化字典 erp_account_type

DROP PROCEDURE IF EXISTS add_column_if_not_exists;
DELIMITER $$
CREATE PROCEDURE add_column_if_not_exists(
  IN p_table VARCHAR(64),
  IN p_col VARCHAR(64),
  IN p_def TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = p_table
      AND COLUMN_NAME = p_col
  ) THEN
    SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_def);
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL add_column_if_not_exists(
  'erp_account',
  'account_type',
  '`account_type` TINYINT NOT NULL DEFAULT 1 COMMENT ''账户类型：1-银行账户 2-现金账户 3-其他账户'' AFTER `name`'
);
CALL add_column_if_not_exists(
  'erp_account',
  'bank_name',
  '`bank_name` VARCHAR(128) NULL COMMENT ''开户行（银行账户时有值）'' AFTER `account_type`'
);
CALL add_column_if_not_exists(
  'erp_account',
  'bank_account',
  '`bank_account` VARCHAR(64) NULL COMMENT ''银行账号（银行账户时有值）'' AFTER `bank_name`'
);

DROP PROCEDURE IF EXISTS add_column_if_not_exists;

CREATE TABLE IF NOT EXISTS `erp_finance_transfer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `no` VARCHAR(64) NOT NULL COMMENT '转账单号',
  `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态：10-未审核 20-已审核',
  `transfer_time` DATETIME NOT NULL COMMENT '转账时间',
  `out_account_id` BIGINT NOT NULL COMMENT '转出账户ID',
  `in_account_id` BIGINT NOT NULL COMMENT '转入账户ID',
  `transfer_price` DECIMAL(24, 6) NOT NULL COMMENT '转账金额',
  `exchange_rate` DECIMAL(18, 6) NOT NULL DEFAULT 1 COMMENT '汇率',
  `fee_price` DECIMAL(24, 6) NOT NULL DEFAULT 0 COMMENT '手续费',
  `fee_expense_category` VARCHAR(64) NULL COMMENT '费用项目',
  `finance_user_id` BIGINT NULL COMMENT '财务人员用户ID',
  `remark` VARCHAR(512) NULL COMMENT '备注',
  `file_url` VARCHAR(512) NULL COMMENT '附件URL',
  `creator` VARCHAR(64) NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` VARCHAR(64) NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` BIT(1) NOT NULL DEFAULT b'0',
  `tenant_id` BIGINT NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`),
  KEY `idx_out_account_id` (`out_account_id`),
  KEY `idx_in_account_id` (`in_account_id`),
  KEY `idx_transfer_time` (`transfer_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 银行转账单';

INSERT INTO `system_dict_type`
(`id`, `name`, `type`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `deleted_time`)
VALUES
(3230, 'ERP 账户类型', 'erp_account_type', 0, 'ERP 账户类型', '1', NOW(), '1', NOW(), b'0', '1970-01-01 00:00:00')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `type` = VALUES(`type`),
 `status` = VALUES(`status`),
 `remark` = VALUES(`remark`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0',
 `deleted_time` = '1970-01-01 00:00:00';

INSERT INTO `system_dict_data`
(`id`, `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(3450, 1, '银行账户', '1', 'erp_account_type', 0, 'primary', '', '', '1', NOW(), '1', NOW(), b'0'),
(3451, 2, '现金账户', '2', 'erp_account_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'),
(3452, 3, '其他账户', '3', 'erp_account_type', 0, 'default', '', '', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `sort` = VALUES(`sort`),
 `label` = VALUES(`label`),
 `value` = VALUES(`value`),
 `dict_type` = VALUES(`dict_type`),
 `status` = VALUES(`status`),
 `color_type` = VALUES(`color_type`),
 `css_class` = VALUES(`css_class`),
 `remark` = VALUES(`remark`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

DELETE FROM `system_role_menu`
WHERE `menu_id` IN (
  SELECT `id` FROM (
    SELECT `id`
    FROM `system_menu`
    WHERE `id` NOT IN (31129, 31130, 31131, 31132, 31133, 31134, 31135, 31136)
      AND (
        `permission` LIKE 'erp:finance-transfer:%'
        OR `component` IN ('erp/finance/transfer/index', 'erp/finance/cash-bank/transfer/index')
        OR (`name` = '银行转账' AND `parent_id` IN (2645, 31129))
      )
  ) temp
);

DELETE FROM `system_menu`
WHERE `id` IN (
  SELECT `id` FROM (
    SELECT `id`
    FROM `system_menu`
    WHERE `id` NOT IN (31129, 31130, 31131, 31132, 31133, 31134, 31135, 31136)
      AND (
        `permission` LIKE 'erp:finance-transfer:%'
        OR `component` IN ('erp/finance/transfer/index', 'erp/finance/cash-bank/transfer/index')
        OR (`name` = '银行转账' AND `parent_id` IN (2645, 31129))
      )
  ) temp
);

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31129, '现金银行', '', 2, 4, 2645, 'cash-bank', 'ep:credit-card', '', 'ErpFinanceCashBank',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31130, '银行转账', '', 2, 2, 31129, 'transfer', 'ep:switch-button', 'erp/finance/cash-bank/transfer/index', 'ErpFinanceTransfer',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31131, '银行转账查询', 'erp:finance-transfer:query', 3, 1, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31132, '银行转账创建', 'erp:finance-transfer:create', 3, 2, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31133, '银行转账更新', 'erp:finance-transfer:update', 3, 3, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31134, '银行转账删除', 'erp:finance-transfer:delete', 3, 4, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31135, '银行转账导出', 'erp:finance-transfer:export', 3, 5, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(31136, '银行转账审批', 'erp:finance-transfer:update-status', 3, 6, 31130, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

UPDATE `system_menu`
SET `name` = '账户管理',
    `sort` = 1,
    `parent_id` = 31129,
    `path` = 'account',
    `icon` = 'fa:universal-access',
    `component` = 'erp/finance/cash-bank/account/index',
    `component_name` = 'ErpAccount',
    `status` = 0,
    `visible` = b'1',
    `keep_alive` = b'1',
    `always_show` = b'1',
    `updater` = '1',
    `update_time` = NOW(),
    `deleted` = b'0'
WHERE `id` = 2646;

UPDATE `system_menu` SET `name` = '账户管理查询', `updater` = '1', `update_time` = NOW(), `deleted` = b'0' WHERE `id` = 2647;
UPDATE `system_menu` SET `name` = '账户管理创建', `updater` = '1', `update_time` = NOW(), `deleted` = b'0' WHERE `id` = 2648;
UPDATE `system_menu` SET `name` = '账户管理更新', `updater` = '1', `update_time` = NOW(), `deleted` = b'0' WHERE `id` = 2649;
UPDATE `system_menu` SET `name` = '账户管理删除', `updater` = '1', `update_time` = NOW(), `deleted` = b'0' WHERE `id` = 2650;
UPDATE `system_menu` SET `name` = '账户管理导出', `updater` = '1', `update_time` = NOW(), `deleted` = b'0' WHERE `id` = 2651;

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31129, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31130, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31131, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31132, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31133, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31134, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31135, '1', NOW(), '1', NOW(), b'0', 1),
(1, 31136, '1', NOW(), '1', NOW(), b'0', 1);
