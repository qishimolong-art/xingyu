-- v253 采购票据多来源迁移：生产执行前只读检查。
--
-- 使用方式：
--   1. 连接已复核的目标数据库；
--   2. 在同一会话先设置：SET @purchase_invoice_multi_source_schema = '目标数据库名';
--   3. 执行本文件并人工核对所有结果；本文件不执行 DDL/DML；
--   4. 只有 target_database_matches=1、required_table_count=5、结构状态符合预期，
--      且业务异常数量已确认后，才执行 erp_purchase_invoice_multi_source_v253.sql。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS connected_database,
       @purchase_invoice_multi_source_schema AS reviewed_database,
       CAST(DATABASE() AS BINARY) = CAST(@purchase_invoice_multi_source_schema AS BINARY)
         AS target_database_matches;

-- 必须为 0；正式脚本不会自动删除同名过程，避免误伤已有数据库对象。
SELECT COUNT(*) AS migration_routine_name_conflicts
FROM information_schema.routines
WHERE routine_schema=DATABASE() AND routine_type='PROCEDURE'
  AND routine_name='migrate_purchase_invoice_multi_source_v253';

SELECT COUNT(*) AS required_table_count
FROM information_schema.tables
WHERE table_schema=DATABASE() AND table_type='BASE TABLE'
  AND table_name IN ('erp_purchase_invoice','erp_purchase_invoice_item','erp_purchase_in_items',
                     'erp_purchase_price_adjust_item','erp_purchase_return_items');

-- 首次迁移前应为 0/0/0；完整迁移后应为 5/1/1。其它组合表示中断或手工残留，必须停止。
SELECT
  (SELECT COUNT(*) FROM information_schema.columns
   WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
     AND column_name IN ('source_type','source_doc_id','source_doc_no','source_item_id','amount_direction'))
    AS generic_source_column_count,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
   WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
     AND index_name='idx_invoice_generic_source') AS generic_source_index_count,
  (SELECT COUNT(*) FROM information_schema.tables
   WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_source_allocation'
     AND table_type='BASE TABLE') AS allocation_table_count;

SELECT
  (SELECT COUNT(*) FROM erp_purchase_invoice WHERE deleted=b'0') AS invoice_rows,
  (SELECT COUNT(*) FROM erp_purchase_invoice_item WHERE deleted=b'0') AS invoice_item_rows,
  (SELECT COUNT(*) FROM erp_purchase_invoice_item
   WHERE deleted=b'0' AND source_in_item_id IS NOT NULL) AS legacy_source_item_rows,
  (SELECT COUNT(*) FROM erp_purchase_invoice_item pii
   INNER JOIN erp_purchase_invoice pinv
     ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
    AND pinv.deleted=b'0' AND pinv.status=20
   WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL) AS approved_legacy_source_rows;

-- 必须为 0：旧票据来源明细已经不存在时，无法安全生成历史关闭台账。
SELECT COUNT(*) AS legacy_source_orphan_rows
FROM erp_purchase_invoice_item pii
LEFT JOIN erp_purchase_in_items source_item
  ON source_item.id=pii.source_in_item_id AND source_item.tenant_id=pii.tenant_id
 AND source_item.deleted=b'0'
WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL AND source_item.id IS NULL;

-- 必须为 0：历史来源缺少入库单 ID 或单号时，通用来源身份无法完整回填。
SELECT COUNT(*) AS legacy_source_missing_identity_rows
FROM erp_purchase_invoice_item
WHERE deleted=b'0' AND source_in_item_id IS NOT NULL
  AND (source_in_id IS NULL OR source_in_no IS NULL OR TRIM(source_in_no)='');

-- 允许大于 0，但必须人工确认；迁移会按“同租户、同来源明细只关闭一次”归并。
SELECT COUNT(*) AS legacy_source_multi_invoice_groups
FROM (
  SELECT pii.tenant_id,pii.source_in_item_id
  FROM erp_purchase_invoice_item pii
  INNER JOIN erp_purchase_invoice pinv
    ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
   AND pinv.deleted=b'0' AND pinv.status=20
  WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL
  GROUP BY pii.tenant_id,pii.source_in_item_id
  HAVING COUNT(DISTINCT pii.invoice_id)>1
) duplicate_legacy_source;

-- 历史已开票入库对应的已审批调价会被整项关闭；先核对预计数量及正负金额。
SELECT COUNT(*) AS historical_adjust_close_rows,
       COALESCE(SUM(CASE WHEN historical_adjust.adjust_price>0
                         THEN historical_adjust.adjust_price ELSE 0 END),0)
         AS positive_adjust_amount_sample,
       COALESCE(SUM(CASE WHEN historical_adjust.adjust_price<0
                         THEN historical_adjust.adjust_price ELSE 0 END),0)
         AS negative_adjust_amount_sample
FROM (
  SELECT DISTINCT pai.tenant_id,pai.id,pai.adjust_price
  FROM erp_purchase_price_adjust_item pai
  INNER JOIN erp_purchase_price_adjust pa
    ON pa.id=pai.adjust_id AND pa.tenant_id=pai.tenant_id
   AND pa.deleted=b'0' AND pa.status=20
  INNER JOIN erp_purchase_invoice_item pii
    ON pii.tenant_id=pai.tenant_id AND pii.deleted=b'0' AND pii.source_in_item_id=pai.in_item_id
  INNER JOIN erp_purchase_invoice pinv
    ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
   AND pinv.deleted=b'0' AND pinv.status=20
  WHERE pai.deleted=b'0' AND COALESCE(pai.adjust_price,0)<>0
) historical_adjust;

-- 来源数量异常会影响部分开票余额，非 0 时停止迁移并先核对业务数据。
SELECT
  (SELECT COUNT(*) FROM erp_purchase_in_items
   WHERE deleted=b'0' AND (`count` IS NULL OR `count`<=0)) AS invalid_inbound_count_rows,
  (SELECT COUNT(*) FROM erp_purchase_price_adjust_item
   WHERE deleted=b'0' AND (`count` IS NULL OR `count`<=0)) AS invalid_adjust_count_rows,
  (SELECT COUNT(*) FROM erp_purchase_return_items
   WHERE deleted=b'0' AND (`count` IS NULL OR `count`<=0)) AS invalid_return_count_rows;
