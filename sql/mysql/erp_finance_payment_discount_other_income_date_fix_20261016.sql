-- 付款单优惠自动生成其他收入：日期口径修复
-- 幂等规则：只处理来源为“付款单折让”的自动单据，重复执行不再产生更新。
-- 执行前请先运行同名 _precheck.sql，确认影响范围。

START TRANSACTION;

UPDATE erp_receivable_other_income_item item
JOIN erp_receivable_other_income oi
  ON oi.id = item.income_id
 AND oi.deleted = 0
SET item.biz_date = oi.create_time
WHERE item.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (item.biz_date <=> oi.create_time);

SELECT ROW_COUNT() AS updated_item_count;

UPDATE erp_receivable_other_income oi
SET oi.biz_time = oi.create_time
WHERE oi.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (oi.biz_time <=> oi.create_time);

SELECT ROW_COUNT() AS updated_header_count;

SELECT COUNT(*) AS remaining_header_count
FROM erp_receivable_other_income oi
WHERE oi.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (oi.biz_time <=> oi.create_time);

SELECT COUNT(*) AS remaining_item_count
FROM erp_receivable_other_income_item item
JOIN erp_receivable_other_income oi
  ON oi.id = item.income_id
 AND oi.deleted = 0
WHERE item.deleted = 0
  AND oi.source_type = '付款单折让'
  AND oi.source_id IS NOT NULL
  AND oi.create_time IS NOT NULL
  AND NOT (item.biz_date <=> oi.create_time);

COMMIT;
