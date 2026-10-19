-- 付款单优惠自动生成其他收入：日期口径修复前置检查（只读）
-- 目标：主单日期、明细发生日期统一为其他收入单自身创建时间。

SELECT DATABASE() AS current_database, VERSION() AS mysql_version;

SELECT COUNT(*) AS affected_header_count
FROM erp_receivable_other_income oi
WHERE oi.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (oi.biz_time <=> oi.create_time);

SELECT COUNT(*) AS affected_item_count
FROM erp_receivable_other_income_item item
JOIN erp_receivable_other_income oi
  ON oi.id = item.income_id
 AND oi.deleted = 0
WHERE item.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (item.biz_date <=> oi.create_time);

SELECT oi.id,
       oi.no,
       oi.source_id,
       oi.source_no,
       oi.biz_time AS old_biz_time,
       oi.create_time AS target_biz_time,
       item.id AS item_id,
       item.biz_date AS old_item_biz_date
FROM erp_receivable_other_income oi
LEFT JOIN erp_receivable_other_income_item item
  ON item.income_id = oi.id
 AND item.deleted = 0
WHERE oi.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND (NOT (oi.biz_time <=> oi.create_time)
       OR NOT (item.biz_date <=> oi.create_time))
ORDER BY oi.id, item.id;
