-- 销售手推车历史小程序单据来源类型回填
-- 口径：仅处理未删除、来源类型为空、备注包含“小程序生成”的单据。
-- MALL_ORDER = 60。已有其他来源类型的记录不覆盖，需要单独核实。

SELECT COUNT(*) AS candidate_count
FROM erp_sale_cart
WHERE deleted = b'0'
  AND source_type IS NULL
  AND remark LIKE '%小程序生成%';

SELECT tenant_id, source_type, COUNT(*) AS conflict_count
FROM erp_sale_cart
WHERE deleted = b'0'
  AND source_type IS NOT NULL
  AND source_type <> 60
  AND remark LIKE '%小程序生成%'
GROUP BY tenant_id, source_type;

START TRANSACTION;

UPDATE erp_sale_cart
SET source_type = 60
WHERE deleted = b'0'
  AND source_type IS NULL
  AND remark LIKE '%小程序生成%';

SELECT ROW_COUNT() AS updated_count;

SELECT COUNT(*) AS remaining_count
FROM erp_sale_cart
WHERE deleted = b'0'
  AND source_type IS NULL
  AND remark LIKE '%小程序生成%';

COMMIT;
