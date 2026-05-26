-- ============================================================================
-- ERP 财务核算 v26：三大报表项目模板（取数公式留空，后期与财务沟通后填）
-- ============================================================================

CREATE TABLE IF NOT EXISTS `erp_report_item_template` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
    `report_type`   TINYINT         NOT NULL COMMENT '报表类型：1资产负债表 2利润表 3现金流量表',
    `side`          TINYINT         DEFAULT NULL COMMENT '区域：1左侧 2右侧（资产负债/现金流量适用）',
    `item_name`     VARCHAR(128)    NOT NULL COMMENT '项目名称',
    `row_no`        INT             DEFAULT NULL COMMENT '行次',
    `formula`       VARCHAR(1000)   DEFAULT NULL COMMENT '取数公式（先空，后期填）',
    `parent_id`     BIGINT          DEFAULT NULL COMMENT '父项 ID（树形）',
    `sort`          INT             NOT NULL DEFAULT 0 COMMENT '排序',
    `enable`        BIT(1)          NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `remark`        VARCHAR(500)    DEFAULT NULL COMMENT '备注',
    `creator`       VARCHAR(64)     DEFAULT '' COMMENT '创建者',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`       VARCHAR(64)     DEFAULT '' COMMENT '更新者',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`     BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'ERP 三大报表项目模板';

DROP PROCEDURE IF EXISTS erp_report_item_template_v26_idx;
DELIMITER $$
CREATE PROCEDURE erp_report_item_template_v26_idx()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_report_item_template' AND INDEX_NAME = 'idx_report_side_sort') THEN
        CREATE INDEX idx_report_side_sort ON erp_report_item_template (report_type, side, sort);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_report_item_template' AND INDEX_NAME = 'idx_parent_id') THEN
        CREATE INDEX idx_parent_id ON erp_report_item_template (parent_id);
    END IF;
END$$
DELIMITER ;
CALL erp_report_item_template_v26_idx();
DROP PROCEDURE IF EXISTS erp_report_item_template_v26_idx;
