-- ERP 资金管理调整 v38
-- 1. 应收账款核销记录
-- 2. 收付款统一列表菜单调整
-- 3. 银行账户交易流水使用后端接口 /erp/account/transaction-page，无需新增表
-- 4. 银行账户补所属部门字段

CREATE TABLE IF NOT EXISTS `erp_receivable_writeoff` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint NOT NULL COMMENT '客户编号',
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
  KEY `idx_customer_time` (`customer_id`, `write_off_time`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 应收账款核销记录';

DROP PROCEDURE IF EXISTS add_erp_receivable_writeoff_biz_columns;
DELIMITER //
CREATE PROCEDURE add_erp_receivable_writeoff_biz_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_writeoff' AND COLUMN_NAME = 'biz_type'
    ) THEN
        ALTER TABLE `erp_receivable_writeoff` ADD COLUMN `biz_type` INT DEFAULT NULL COMMENT '业务类型' AFTER `customer_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_writeoff' AND COLUMN_NAME = 'biz_id'
    ) THEN
        ALTER TABLE `erp_receivable_writeoff` ADD COLUMN `biz_id` BIGINT DEFAULT NULL COMMENT '业务单据编号' AFTER `biz_type`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_writeoff' AND COLUMN_NAME = 'biz_no'
    ) THEN
        ALTER TABLE `erp_receivable_writeoff` ADD COLUMN `biz_no` VARCHAR(64) DEFAULT NULL COMMENT '业务单据号' AFTER `biz_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_receivable_writeoff' AND INDEX_NAME = 'idx_biz'
    ) THEN
        CREATE INDEX `idx_biz` ON `erp_receivable_writeoff` (`biz_type`, `biz_id`);
    END IF;
END //
DELIMITER ;

CALL add_erp_receivable_writeoff_biz_columns();
DROP PROCEDURE IF EXISTS add_erp_receivable_writeoff_biz_columns;

DROP PROCEDURE IF EXISTS add_erp_finance_fund_dept_column;
DELIMITER //
CREATE PROCEDURE add_erp_finance_fund_dept_column(IN tableName VARCHAR(64))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) THEN
        SET @addColumnSql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT ''所属部门''');
        PREPARE stmt FROM @addColumnSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND COLUMN_NAME = 'dept_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tableName AND INDEX_NAME = 'idx_dept_id'
    ) THEN
        SET @addIndexSql = CONCAT('CREATE INDEX `idx_dept_id` ON `', tableName, '` (`dept_id`)');
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

CALL add_erp_finance_fund_dept_column('erp_account');
DROP PROCEDURE IF EXISTS add_erp_finance_fund_dept_column;

INSERT IGNORE INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(31015, '应收账款核销', 'erp:receivable-account:writeoff', 3, 2, 3101, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 31015, '1', NOW(), '1', NOW(), b'0', 1);

-- 原“收款单”菜单作为收付款统一列表入口，旧“付款单”菜单隐藏，保留付款权限点。
UPDATE `system_menu`
SET `name` = '收付款单',
    `component` = 'erp/finance/receipt/index',
    `component_name` = 'ErpFinanceBill'
WHERE `id` = 2694;

UPDATE `system_menu`
SET `visible` = b'0'
WHERE `id` = 2687;
