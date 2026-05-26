-- ============================================================================
-- ERP 财务核算 v26：会计科目 + 期初余额（合并）+ 辅助核算关联表 + 15 条预置一级科目
-- 兼容 MySQL 5.7 / 8.0.x 全版本，无 8.0.23+ 专有语法
-- 部署：mysql --default-character-set=utf8mb4 -uroot -p ruoyi-vue-pro < erp_accounting_subject_v26.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 1：erp_accounting_subject 会计科目（合并期初余额）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_accounting_subject` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_code`      VARCHAR(32)     NOT NULL COMMENT '科目编码（1001、100201）',
    `subject_name`      VARCHAR(64)     NOT NULL COMMENT '科目名称',
    `short_name`        VARCHAR(32)     DEFAULT NULL COMMENT '简称',
    `subject_category`  TINYINT         NOT NULL COMMENT '大类：1资产 2负债 3共同 4权益 5成本 6损益',
    `parent_code`       VARCHAR(32)     DEFAULT NULL COMMENT '父科目编码，根节点 NULL',
    `subject_level`     TINYINT         NOT NULL DEFAULT 1 COMMENT '层级（1=一级 2=二级...）',
    `is_leaf`           BIT(1)          NOT NULL DEFAULT b'1' COMMENT '是否末级（凭证只能选末级）',
    `balance_direction` TINYINT         NOT NULL COMMENT '余额方向：1借 2贷',
    `opening_balance`   DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '期初余额',
    `enable`            BIT(1)          NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `sort`              INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `remark`            VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `creator`           VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`           VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`         BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_code` (`subject_code`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 会计科目（合并期初余额）';

-- 索引（幂等）
DROP PROCEDURE IF EXISTS erp_accounting_subject_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_accounting_subject_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_accounting_subject' AND INDEX_NAME = 'idx_parent_code') THEN
        CREATE INDEX idx_parent_code ON erp_accounting_subject (parent_code);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_accounting_subject' AND INDEX_NAME = 'idx_subject_category') THEN
        CREATE INDEX idx_subject_category ON erp_accounting_subject (subject_category);
    END IF;
END$$
DELIMITER ;
CALL erp_accounting_subject_v26_idx();
DROP PROCEDURE IF EXISTS erp_accounting_subject_v26_idx;

-- ----------------------------------------------------------------------------
-- 表 9：erp_subject_auxiliary 会计科目辅助核算关联表（科目 N:M 辅助核算类型）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_subject_auxiliary` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subject_id`      BIGINT       NOT NULL COMMENT '关联 erp_accounting_subject.id',
    `subject_code`    VARCHAR(32)  DEFAULT NULL COMMENT '冗余科目编码',
    `auxiliary_type`  VARCHAR(32)  NOT NULL COMMENT '辅助核算类型：supplier/customer/project/dept/person',
    `sort`            INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`         VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_subject_aux` (`subject_id`, `auxiliary_type`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 会计科目辅助核算关联表';

DROP PROCEDURE IF EXISTS erp_subject_auxiliary_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_subject_auxiliary_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_subject_auxiliary' AND INDEX_NAME = 'idx_subject_code') THEN
        CREATE INDEX idx_subject_code ON erp_subject_auxiliary (subject_code);
    END IF;
END$$
DELIMITER ;
CALL erp_subject_auxiliary_v26_idx();
DROP PROCEDURE IF EXISTS erp_subject_auxiliary_v26_idx;

-- ----------------------------------------------------------------------------
-- 预置 15 条最常用一级科目（INSERT IGNORE 支持重复执行）
-- 列：subject_code, subject_name, subject_category, parent_code, subject_level,
--     is_leaf, balance_direction, opening_balance, enable, sort, tenant_id
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `erp_accounting_subject`
(`subject_code`, `subject_name`, `short_name`, `subject_category`, `parent_code`,
 `subject_level`, `is_leaf`, `balance_direction`, `opening_balance`, `enable`,
 `sort`, `creator`, `updater`, `tenant_id`)
VALUES
('1001',   '现金',           '现金',     1, NULL, 1, b'1', 1, 0.00, b'1', 1,  '1', '1', 1),
('1002',   '银行存款',       '银行存款', 1, NULL, 1, b'1', 1, 0.00, b'1', 2,  '1', '1', 1),
('1122',   '应收账款',       '应收',     1, NULL, 1, b'1', 1, 0.00, b'1', 3,  '1', '1', 1),
('1123',   '预付账款',       '预付',     1, NULL, 1, b'1', 1, 0.00, b'1', 4,  '1', '1', 1),
('1405',   '库存商品',       '库存',     1, NULL, 1, b'1', 1, 0.00, b'1', 5,  '1', '1', 1),
('1601',   '固定资产',       '固资',     1, NULL, 1, b'1', 1, 0.00, b'1', 6,  '1', '1', 1),
('1602',   '累计折旧',       '折旧',     1, NULL, 1, b'1', 2, 0.00, b'1', 7,  '1', '1', 1),
('2202',   '应付账款',       '应付',     2, NULL, 1, b'1', 2, 0.00, b'1', 8,  '1', '1', 1),
('2203',   '预收账款',       '预收',     2, NULL, 1, b'1', 2, 0.00, b'1', 9,  '1', '1', 1),
('2221',   '应交税费',       '税费',     2, NULL, 1, b'1', 2, 0.00, b'1', 10, '1', '1', 1),
('4001',   '实收资本',       '资本',     4, NULL, 1, b'1', 2, 0.00, b'1', 11, '1', '1', 1),
('4104',   '利润分配',       '利润',     4, NULL, 1, b'1', 2, 0.00, b'1', 12, '1', '1', 1),
('6001',   '主营业务收入',   '主收入',   6, NULL, 1, b'1', 2, 0.00, b'1', 13, '1', '1', 1),
('6401',   '主营业务成本',   '主成本',   6, NULL, 1, b'1', 1, 0.00, b'1', 14, '1', '1', 1),
('6403',   '营业税金及附加', '税金附加', 6, NULL, 1, b'1', 1, 0.00, b'1', 15, '1', '1', 1);
