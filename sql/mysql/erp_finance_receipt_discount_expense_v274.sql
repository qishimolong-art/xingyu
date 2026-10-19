-- 收款单优惠自动生成非现金费用支付（v274）
-- MySQL 8，可重复执行。
-- 仅校验来源字段、补齐幂等索引与基础选项；不回填历史收款单、不修改角色权限和业务数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS migrate_receipt_discount_expense_v274;
DELIMITER $$
CREATE PROCEDURE migrate_receipt_discount_expense_v274()
BEGIN
    DECLARE duplicate_sources BIGINT DEFAULT 0;
    DECLARE required_columns INT DEFAULT 0;

    IF DATABASE() IS NULL OR DATABASE() = '' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v274: no target database selected';
    END IF;
    IF VERSION() < '8.0' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v274: MySQL 8 is required';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'erp_finance_receipt')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense_item')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'erp_base_data')
       OR NOT EXISTS (SELECT 1 FROM information_schema.tables
                      WHERE table_schema = DATABASE() AND table_name = 'system_tenant') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v274: required tables are missing';
    END IF;

    SELECT COUNT(*) INTO required_columns
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'erp_payable_expense'
      AND column_name IN ('tenant_id', 'source_type', 'source_id', 'source_no', 'deleted');
    IF required_columns <> 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v274: payable expense source columns are missing';
    END IF;

    SELECT COUNT(*) INTO duplicate_sources
    FROM (
        SELECT tenant_id, source_type, source_id, deleted
        FROM erp_payable_expense
        WHERE source_type IS NOT NULL AND source_id IS NOT NULL
        GROUP BY tenant_id, source_type, source_id, deleted
        HAVING COUNT(*) > 1
    ) duplicate_rows;
    IF duplicate_sources > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v274: duplicate payable expense sources must be resolved first';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM (
            SELECT index_name, non_unique,
                   GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_list
            FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense'
            GROUP BY index_name, non_unique
        ) indexes
        WHERE indexes.non_unique = 0
          AND indexes.columns_list = 'tenant_id,source_type,source_id,deleted'
    ) THEN
        ALTER TABLE erp_payable_expense
            ADD UNIQUE KEY uk_receipt_discount_expense_source
                (tenant_id, source_type, source_id, deleted);
    END IF;
END$$
DELIMITER ;

CALL migrate_receipt_discount_expense_v274();
DROP PROCEDURE IF EXISTS migrate_receipt_discount_expense_v274;

INSERT INTO erp_base_data
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`,
 `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0,
       '收款单优惠自动生成费用支付基础选项', '1', NOW(), '1', NOW(), b'0', tenant.id
FROM system_tenant tenant
CROSS JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name, 'credit' AS code, 20 AS sort
    UNION ALL SELECT 'payable_expense_biz_type', '一般费用', 'general_expense', 30
    UNION ALL SELECT 'payable_expense_type', '其他', 'other', 10
    UNION ALL SELECT 'payable_expense_doc_type', '正常单据', 'normal', 10
    UNION ALL SELECT 'payable_expense_item_project', '销售减收', 'sale_reduction', 10
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

SELECT index_name, non_unique,
       GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_list
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'erp_payable_expense'
GROUP BY index_name, non_unique
HAVING columns_list = 'tenant_id,source_type,source_id,deleted';

SELECT t.id AS tenant_id, seed.type, seed.name, COUNT(d.id) AS active_count
FROM system_tenant t
CROSS JOIN (
    SELECT 'settle_method' AS type, '挂账' AS name
    UNION ALL SELECT 'payable_expense_biz_type', '一般费用'
    UNION ALL SELECT 'payable_expense_type', '其他'
    UNION ALL SELECT 'payable_expense_doc_type', '正常单据'
    UNION ALL SELECT 'payable_expense_item_project', '销售减收'
) seed
LEFT JOIN erp_base_data d ON d.tenant_id = t.id AND d.type = seed.type
    AND d.name = seed.name AND d.deleted = b'0'
WHERE t.deleted = b'0' AND t.status = 0
GROUP BY t.id, seed.type, seed.name
ORDER BY t.id, seed.type, seed.name;
