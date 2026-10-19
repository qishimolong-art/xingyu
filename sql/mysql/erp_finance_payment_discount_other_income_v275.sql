-- 付款单优惠自动生成非现金其他收入（v275）
-- MySQL 8，可重复执行。
-- 仅增加来源字段、幂等索引和缺失的基础选项；不回填历史付款单，不修改角色权限。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_payment_discount_other_income_v275;
DELIMITER $$
CREATE PROCEDURE migrate_payment_discount_other_income_v275()
BEGIN
    DECLARE duplicate_sources BIGINT DEFAULT 0;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v275: no target database selected';
    END IF;
    IF VERSION() < '8.0' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v275: MySQL 8 is required';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'erp_finance_payment')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income_item')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_base_data')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'system_tenant') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v275: required tables are missing';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
                     AND column_name = 'source_type') THEN
        ALTER TABLE erp_receivable_other_income
            ADD COLUMN source_type VARCHAR(64) NULL COMMENT '来源类型' AFTER related_biz;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
                     AND column_name = 'source_id') THEN
        ALTER TABLE erp_receivable_other_income
            ADD COLUMN source_id BIGINT NULL COMMENT '来源单据ID' AFTER source_type;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
                     AND column_name = 'source_no') THEN
        ALTER TABLE erp_receivable_other_income
            ADD COLUMN source_no VARCHAR(64) NULL COMMENT '来源单号' AFTER source_id;
    END IF;

    SELECT COUNT(*) INTO duplicate_sources
    FROM (
        SELECT tenant_id, source_type, source_id, deleted
        FROM erp_receivable_other_income
        WHERE source_type IS NOT NULL AND source_id IS NOT NULL
        GROUP BY tenant_id, source_type, source_id, deleted
        HAVING COUNT(*) > 1
    ) duplicate_rows;
    IF duplicate_sources > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v275: duplicate other income sources must be resolved first';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM (
            SELECT index_name, non_unique,
                   GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_list
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
            GROUP BY index_name, non_unique
        ) indexes
        WHERE indexes.non_unique = 0
          AND indexes.columns_list = 'tenant_id,source_type,source_id,deleted'
    ) THEN
        ALTER TABLE erp_receivable_other_income
            ADD UNIQUE KEY uk_payment_discount_other_income_source
                (tenant_id, source_type, source_id, deleted);
    END IF;
END$$
DELIMITER ;

CALL migrate_payment_discount_other_income_v275();
DROP PROCEDURE IF EXISTS migrate_payment_discount_other_income_v275;

INSERT INTO erp_base_data
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0,
       '付款单优惠自动生成其他收入基础选项', '1', NOW(), '1', NOW(), b'0', tenant.id
FROM system_tenant tenant
CROSS JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name, 'credit' AS code, 20 AS sort
    UNION ALL SELECT 'receivable_other_income_type', '其他', 'other', 30
    UNION ALL SELECT 'receivable_other_income_doc_type', '正常单据', 'normal', 10
    UNION ALL SELECT 'receivable_other_income_item_project', '采购减付', 'purchase_payment_reduction', 60
) seed
WHERE tenant.deleted = b'0' AND tenant.status = 0
  AND NOT EXISTS (
      SELECT 1
      FROM erp_base_data existing
      WHERE existing.tenant_id = tenant.id
        AND existing.type = seed.type
        AND existing.name = seed.name
        AND existing.deleted = b'0'
  );

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
  AND column_name IN ('source_type', 'source_id', 'source_no')
ORDER BY ordinal_position;

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_list
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_receivable_other_income'
GROUP BY index_name, non_unique
HAVING columns_list = 'tenant_id,source_type,source_id,deleted';

SELECT t.id AS tenant_id, seed.type, seed.name, COUNT(d.id) AS active_count
FROM system_tenant t
CROSS JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name
    UNION ALL SELECT 'receivable_other_income_type', '其他'
    UNION ALL SELECT 'receivable_other_income_doc_type', '正常单据'
    UNION ALL SELECT 'receivable_other_income_item_project', '采购减付'
) seed
LEFT JOIN erp_base_data d ON d.tenant_id = t.id AND d.type = seed.type
    AND d.name = seed.name AND d.deleted = b'0'
WHERE t.deleted = b'0' AND t.status = 0
GROUP BY t.id, seed.type, seed.name
ORDER BY t.id, seed.type, seed.name;

SELECT COUNT(*) AS generated_payment_discount_other_income_count
FROM erp_receivable_other_income
WHERE deleted = b'0' AND source_type = '付款单折让';
