-- ============================================================================
-- ERP 财务核算 v26：凭证主表 + 凭证分录明细
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 4：erp_voucher 凭证主表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_voucher` (
    `id`                     BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voucher_word`           VARCHAR(8)      NOT NULL DEFAULT '记' COMMENT '凭证字',
    `voucher_no`             VARCHAR(32)     NOT NULL COMMENT '凭证编号（如 记-202605-000001）',
    `voucher_date`           DATE            NOT NULL COMMENT '凭证日期',
    `period_year`            INT             DEFAULT NULL COMMENT '归属年',
    `period_month`           TINYINT         DEFAULT NULL COMMENT '归属月（1-12）',
    `attachment_count`       INT             NOT NULL DEFAULT 0 COMMENT '附件张数',
    `summary`                VARCHAR(500)    DEFAULT NULL COMMENT '摘要（取第一行分录摘要）',
    `total_debit`            DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '借方合计',
    `total_credit`           DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '贷方合计',
    `audit_status`           TINYINT         NOT NULL DEFAULT 10 COMMENT '审核状态：10未审核 20已审核 30已反审',
    `source_type`            TINYINT         NOT NULL DEFAULT 1 COMMENT '来源类型：1手动 2系统自动 3跨期归属',
    `source_biz_type`        TINYINT         DEFAULT NULL COMMENT '业务来源类型（11种凭证类型映射）',
    `source_biz_id`          BIGINT          DEFAULT NULL COMMENT '业务单据 ID',
    `source_biz_no`          VARCHAR(64)     DEFAULT NULL COMMENT '业务单号冗余',
    `generate_business_doc`  BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否产生业务单据',
    `project_id`             BIGINT          DEFAULT NULL COMMENT '辅助：项目 ID',
    `dept_id`                BIGINT          DEFAULT NULL COMMENT '辅助：部门 ID',
    `customer_id`            BIGINT          DEFAULT NULL COMMENT '辅助：客户 ID',
    `person_user_id`         BIGINT          DEFAULT NULL COMMENT '辅助：个人（用户 ID）',
    `maker_user_id`          BIGINT          DEFAULT NULL COMMENT '制单人 ID',
    `maker_user_name`        VARCHAR(32)     DEFAULT NULL COMMENT '制单人姓名',
    `bookkeeper`             VARCHAR(32)     DEFAULT NULL COMMENT '记账人',
    `bookkeeper_time`        DATETIME        DEFAULT NULL COMMENT '登账日期',
    `cashier`                VARCHAR(32)     DEFAULT NULL COMMENT '出纳',
    `supervisor`             VARCHAR(32)     DEFAULT NULL COMMENT '主管',
    `auditor_user_id`        BIGINT          DEFAULT NULL COMMENT '审核人 ID',
    `auditor_user_name`      VARCHAR(32)     DEFAULT NULL COMMENT '审核人姓名',
    `audit_time`             DATETIME        DEFAULT NULL COMMENT '审核日期',
    `remark`                 VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `creator`                VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`                VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`              BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voucher_no` (`voucher_no`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 凭证主表';

DROP PROCEDURE IF EXISTS erp_voucher_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_voucher_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher' AND INDEX_NAME = 'idx_voucher_date') THEN
        CREATE INDEX idx_voucher_date ON erp_voucher (voucher_date);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher' AND INDEX_NAME = 'idx_period_ym') THEN
        CREATE INDEX idx_period_ym ON erp_voucher (period_year, period_month);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher' AND INDEX_NAME = 'idx_source_biz') THEN
        CREATE INDEX idx_source_biz ON erp_voucher (source_biz_type, source_biz_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher' AND INDEX_NAME = 'idx_audit_status') THEN
        CREATE INDEX idx_audit_status ON erp_voucher (audit_status);
    END IF;
END$$
DELIMITER ;
CALL erp_voucher_v26_idx();
DROP PROCEDURE IF EXISTS erp_voucher_v26_idx;

-- ----------------------------------------------------------------------------
-- 表 5：erp_voucher_item 凭证分录明细
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_voucher_item` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `voucher_id`      BIGINT          NOT NULL COMMENT '关联 erp_voucher.id',
    `line_no`         INT             NOT NULL COMMENT '行号',
    `summary`         VARCHAR(500)    DEFAULT NULL COMMENT '该行摘要',
    `subject_id`      BIGINT          NOT NULL COMMENT '关联 erp_accounting_subject.id',
    `subject_code`    VARCHAR(32)     DEFAULT NULL COMMENT '科目编码（冗余）',
    `subject_name`    VARCHAR(64)     DEFAULT NULL COMMENT '科目名称（冗余）',
    `auxiliary_type`  VARCHAR(32)     DEFAULT NULL COMMENT '辅助核算类型（supplier/customer/project/dept/person）',
    `auxiliary_id`    BIGINT          DEFAULT NULL COMMENT '辅助核算 ID',
    `auxiliary_name`  VARCHAR(128)    DEFAULT NULL COMMENT '辅助核算名称（冗余）',
    `debit_amount`    DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '借方金额',
    `credit_amount`   DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '贷方金额',
    `creator`         VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`         VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`       BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 凭证分录明细';

DROP PROCEDURE IF EXISTS erp_voucher_item_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_voucher_item_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher_item' AND INDEX_NAME = 'idx_voucher_line') THEN
        CREATE INDEX idx_voucher_line ON erp_voucher_item (voucher_id, line_no);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher_item' AND INDEX_NAME = 'idx_subject_id') THEN
        CREATE INDEX idx_subject_id ON erp_voucher_item (subject_id);
    END IF;
END$$
DELIMITER ;
CALL erp_voucher_item_v26_idx();
DROP PROCEDURE IF EXISTS erp_voucher_item_v26_idx;
