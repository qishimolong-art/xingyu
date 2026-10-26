-- 销售单结算状态历史数据修复（租户 1，幂等）
-- 仅回写销售单冗余的 receipt_price、settle_status，不修改收款单及核销明细。

SET @target_tenant_id := 1;

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_out_settlement_v290;
CREATE TEMPORARY TABLE tmp_erp_sale_out_settlement_v290 AS
SELECT
    t.id,
    t.tenant_id,
    t.received_amount,
    t.expected_settle_status
FROM (
    SELECT
        so.id,
        so.tenant_id,
        COALESCE(receipt_amount.received_amount, 0) AS received_amount,
        CASE
            WHEN ROUND(ABS(COALESCE(
                CASE WHEN COALESCE(item_amount.has_original_price, 0) > 0 THEN
                    COALESCE(item_amount.original_product_amount, 0)
                    - ROUND(COALESCE(item_amount.original_product_amount, 0)
                        * COALESCE(so.discount_percent, 0) / 100, 2)
                    + COALESCE(so.fee_amount, so.other_price, so.extra_fee, 0)
                ELSE so.total_price END, 0)), 2)
                - ROUND(ABS(COALESCE(receipt_amount.received_amount, 0)), 2) <= 0 THEN 2
            WHEN ROUND(ABS(COALESCE(receipt_amount.received_amount, 0)), 2) > 0 THEN 1
            ELSE 0
        END AS expected_settle_status
    FROM erp_sale_out so
    LEFT JOIN (
        SELECT
            out_id,
            SUM(ROUND(COALESCE(original_product_price, product_price) * count, 2))
                AS original_product_amount,
            SUM(CASE WHEN original_product_price IS NOT NULL THEN 1 ELSE 0 END)
                AS has_original_price
        FROM erp_sale_out_items
        WHERE deleted = b'0' AND tenant_id = @target_tenant_id
        GROUP BY out_id
    ) item_amount ON item_amount.out_id = so.id
    LEFT JOIN (
        SELECT
            fri.biz_id,
            SUM(fri.receipt_price) AS received_amount
        FROM erp_finance_receipt_item fri
        INNER JOIN erp_finance_receipt fr
            ON fr.id = fri.receipt_id
            AND fr.tenant_id = fri.tenant_id
            AND fr.deleted = b'0'
            AND fr.status = 20
        WHERE fri.tenant_id = @target_tenant_id
          AND fri.deleted = b'0'
          AND fri.biz_type = 21
          AND fri.write_off_status = 1
        GROUP BY fri.biz_id
    ) receipt_amount ON receipt_amount.biz_id = so.id
    WHERE so.tenant_id = @target_tenant_id
      AND so.deleted = b'0'
) t;

SELECT COUNT(*) AS rows_to_update
FROM erp_sale_out so
INNER JOIN tmp_erp_sale_out_settlement_v290 expected
    ON expected.id = so.id AND expected.tenant_id = so.tenant_id
WHERE so.tenant_id = @target_tenant_id
  AND so.deleted = b'0'
  AND (COALESCE(so.receipt_price, 0) <> expected.received_amount
       OR COALESCE(so.settle_status, -1) <> expected.expected_settle_status);

UPDATE erp_sale_out so
INNER JOIN tmp_erp_sale_out_settlement_v290 expected
    ON expected.id = so.id AND expected.tenant_id = so.tenant_id
SET so.receipt_price = expected.received_amount,
    so.settle_status = expected.expected_settle_status,
    so.update_time = NOW()
WHERE so.tenant_id = @target_tenant_id
  AND so.deleted = b'0'
  AND (COALESCE(so.receipt_price, 0) <> expected.received_amount
       OR COALESCE(so.settle_status, -1) <> expected.expected_settle_status);

SELECT ROW_COUNT() AS updated_rows;

SELECT so.id, so.no, so.receipt_price, so.settle_status
FROM erp_sale_out so
WHERE so.tenant_id = @target_tenant_id
  AND so.deleted = b'0'
  AND so.no = 'XSCK20261022000001';

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_erp_sale_out_settlement_v290;

