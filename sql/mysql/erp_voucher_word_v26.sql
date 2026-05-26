-- ============================================================================
-- ERP 财务核算 v26：凭证字字典 + 1 条预置（"记"=记账凭证）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `erp_voucher_word` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `code`          VARCHAR(8)   NOT NULL COMMENT '凭证字代码（如"记"）',
    `name`          VARCHAR(32)  NOT NULL COMMENT '名称（如"记账凭证"）',
    `enable`        BIT(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `sort`          INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `creator`       VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_voucher_word_code` (`code`, `deleted`, `tenant_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 凭证字字典';

-- 预置 1 条
INSERT IGNORE INTO `erp_voucher_word`
(`code`, `name`, `enable`, `sort`, `remark`, `creator`, `updater`, `tenant_id`)
VALUES
('记', '记账凭证', b'1', 1, '系统默认凭证字', '1', '1', 1);
