-- ============================================================================
-- ERP 财务核算 v26：凭证归属（跨月调整）
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 6：erp_voucher_attribution 凭证归属（跨月调整）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_voucher_attribution` (
    `id`                    BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `biz_type`              TINYINT         NOT NULL COMMENT '业务单据类型（11种凭证类型映射）',
    `biz_id`                BIGINT          NOT NULL COMMENT '业务单 ID',
    `biz_no`                VARCHAR(64)     DEFAULT NULL COMMENT '业务单号（冗余）',
    `biz_date`              DATETIME        NOT NULL COMMENT '业务发生日期',
    `biz_amount`            DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '原始金额',
    `discount_amount`       DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '折让金额',
    `received_amount`       DECIMAL(20, 2)  NOT NULL DEFAULT 0.00 COMMENT '实收金额',
    `voucher_make_date`     DATE            DEFAULT NULL COMMENT '制单日期（实际生成日期）',
    `attribution_year`      INT             DEFAULT NULL COMMENT '归属年',
    `attribution_month`     TINYINT         DEFAULT NULL COMMENT '归属月（1-12）',
    `attribution_status`    TINYINT         NOT NULL DEFAULT 10 COMMENT '归属状态：10未归属 20已归属 30已生成凭证',
    `voucher_id`            BIGINT          DEFAULT NULL COMMENT '生成的凭证 ID',
    `transaction_party`     VARCHAR(128)    DEFAULT NULL COMMENT '交易单位（客户/供应商/项目名）',
    `handler_user_id`       BIGINT          DEFAULT NULL COMMENT '业务员 ID',
    `auditor_user_id`       BIGINT          DEFAULT NULL COMMENT '审核人 ID',
    `audit_time`            DATETIME        DEFAULT NULL COMMENT '确认时间',
    `settle_method`         VARCHAR(32)     DEFAULT NULL COMMENT '结算方式',
    `dept_id`               BIGINT          DEFAULT NULL COMMENT '部门 ID',
    `shipping_method`       VARCHAR(32)     DEFAULT NULL COMMENT '运输方式',
    `receivable_confirmed`  BIT(1)          DEFAULT NULL COMMENT '应收确认',
    `invoice_issued`        BIT(1)          DEFAULT NULL COMMENT '是否开票',
    `ship_status`           TINYINT         DEFAULT NULL COMMENT '发货状态',
    `summary`               VARCHAR(500)    DEFAULT NULL COMMENT '摘要',
    `remark`                VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `creator`               VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`               VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`               BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`             BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 凭证归属（跨月调整）';

DROP PROCEDURE IF EXISTS erp_voucher_attribution_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_voucher_attribution_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher_attribution' AND INDEX_NAME = 'idx_attribution_ym') THEN
        CREATE INDEX idx_attribution_ym ON erp_voucher_attribution (attribution_year, attribution_month);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher_attribution' AND INDEX_NAME = 'idx_attribution_status') THEN
        CREATE INDEX idx_attribution_status ON erp_voucher_attribution (attribution_status);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_voucher_attribution' AND INDEX_NAME = 'idx_voucher_id') THEN
        CREATE INDEX idx_voucher_id ON erp_voucher_attribution (voucher_id);
    END IF;
END$$
DELIMITER ;
CALL erp_voucher_attribution_v26_idx();
DROP PROCEDURE IF EXISTS erp_voucher_attribution_v26_idx;
