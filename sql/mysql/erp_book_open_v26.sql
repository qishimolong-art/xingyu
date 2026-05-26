-- ============================================================================
-- ERP 财务核算 v26：系统开账主表 + 开账凭证类型勾选
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 表 2：erp_book_open 系统开账主表
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_book_open` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `no`               VARCHAR(32)  NOT NULL COMMENT '开账编号（前缀 KZ）',
    `chain_name`       VARCHAR(128) DEFAULT NULL COMMENT '连锁名称（仅文本）',
    `fiscal_year`      INT          NOT NULL COMMENT '会计年度',
    `period`           TINYINT      NOT NULL COMMENT '开账期间（1-12 月）',
    `start_date`       DATE         NOT NULL COMMENT '期间开始时间',
    `opened`           BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否开账：0未开 1已开',
    `operate_time`     DATETIME     DEFAULT NULL COMMENT '操作时间',
    `operator`         VARCHAR(32)  DEFAULT NULL COMMENT '操作人姓名（冗余）',
    `operator_user_id` BIGINT       DEFAULT NULL COMMENT '操作人 ID',
    `creator`          VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`          VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`        BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_book_open_no` (`no`, `deleted`, `tenant_id`),
    UNIQUE KEY `uk_chain_period` (`chain_name`, `fiscal_year`, `period`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 系统开账主表';

DROP PROCEDURE IF EXISTS erp_book_open_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_book_open_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_book_open' AND INDEX_NAME = 'idx_opened') THEN
        CREATE INDEX idx_opened ON erp_book_open (opened);
    END IF;
END$$
DELIMITER ;
CALL erp_book_open_v26_idx();
DROP PROCEDURE IF EXISTS erp_book_open_v26_idx;

-- ----------------------------------------------------------------------------
-- 表 3：erp_book_open_voucher_config 开账凭证类型勾选（11 种凭证类型）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_book_open_voucher_config` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `book_open_id`  BIGINT      NOT NULL COMMENT '关联 erp_book_open.id',
    `voucher_type`  TINYINT     NOT NULL COMMENT '11种凭证类型：1销售 2其他应收 3预收款 4预付款 5预收账款 6调拨出库 7采购 8其他应付 9其他出库 10其他入库 11银行转账',
    `enabled`       BIT(1)      NOT NULL DEFAULT b'1' COMMENT '是否启用生成',
    `sort`          INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `creator`       VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)      NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     BIGINT      NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 开账凭证类型勾选';

DROP PROCEDURE IF EXISTS erp_book_open_voucher_config_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_book_open_voucher_config_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_book_open_voucher_config' AND INDEX_NAME = 'idx_book_open_id') THEN
        CREATE INDEX idx_book_open_id ON erp_book_open_voucher_config (book_open_id);
    END IF;
END$$
DELIMITER ;
CALL erp_book_open_voucher_config_v26_idx();
DROP PROCEDURE IF EXISTS erp_book_open_voucher_config_v26_idx;
