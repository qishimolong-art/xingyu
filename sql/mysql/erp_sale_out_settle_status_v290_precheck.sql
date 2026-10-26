-- 销售单结算状态历史数据修复前检查（租户 1）
-- 口径：未收金额 = 原销售单结算金额 - 已审核且有效的收款核销金额。

SET @target_tenant_id := 1;

SELECT
    t.id,
    t.no,
    t.total_price AS stored_total_price,
    t.stored_receipt_price,
    t.stored_settle_status,
    t.receivable_amount,
    t.received_amount,
    t.receivable_amount - t.received_amount AS unreceived_amount,
    t.expected_settle_status
FROM (
    SELECT
        so.id,
        so.no,
        so.total_price,
        so.receipt_price AS stored_receipt_price,
        so.settle_status AS stored_settle_status,
        ROUND(ABS(COALESCE(
            CASE WHEN COALESCE(item_amount.has_original_price, 0) > 0 THEN
                COALESCE(item_amount.original_product_amount, 0)
                - ROUND(COALESCE(item_amount.original_product_amount, 0)
                    * COALESCE(so.discount_percent, 0) / 100, 2)
                + COALESCE(so.fee_amount, so.other_price, so.extra_fee, 0)
            ELSE so.total_price END, 0)), 2) AS receivable_amount,
        ROUND(ABS(COALESCE(receipt_amount.received_amount, 0)), 2) AS received_amount,
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
) t
WHERE COALESCE(t.stored_receipt_price, 0) <> t.received_amount
   OR COALESCE(t.stored_settle_status, -1) <> t.expected_settle_status
ORDER BY t.id;

