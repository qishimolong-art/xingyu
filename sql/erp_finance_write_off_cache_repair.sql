-- 修复历史业务单据上的已核销金额缓存。
-- 执行前请先备份数据库，并先执行各段 UPDATE 前对应的 SELECT 确认影响范围。
-- 核销金额只统计：未删除、已审核的付款/收款单，以及状态为“生效”的核销明细。

START TRANSACTION;

-- 采购入库：付款金额为正数
UPDATE erp_purchase_in t
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.payment_price) AS allocated_price
    FROM erp_finance_payment_item i
    INNER JOIN erp_finance_payment p
        ON p.id = i.payment_id AND p.tenant_id = i.tenant_id
        AND p.deleted = 0 AND p.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 11
    GROUP BY i.tenant_id, i.biz_id
) a ON a.tenant_id = t.tenant_id AND a.biz_id = t.id
SET t.payment_price = COALESCE(a.allocated_price, 0)
WHERE t.deleted = 0
  AND COALESCE(t.payment_price, 0) <> COALESCE(a.allocated_price, 0);

-- 采购退货：核销明细为负数，业务单据退款缓存保存正数
UPDATE erp_purchase_return t
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, ABS(SUM(i.payment_price)) AS allocated_price
    FROM erp_finance_payment_item i
    INNER JOIN erp_finance_payment p
        ON p.id = i.payment_id AND p.tenant_id = i.tenant_id
        AND p.deleted = 0 AND p.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 12
    GROUP BY i.tenant_id, i.biz_id
) a ON a.tenant_id = t.tenant_id AND a.biz_id = t.id
SET t.refund_price = COALESCE(a.allocated_price, 0)
WHERE t.deleted = 0
  AND COALESCE(t.refund_price, 0) <> COALESCE(a.allocated_price, 0);

-- 销售出库：收款金额为正数
UPDATE erp_sale_out t
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, SUM(i.receipt_price) AS allocated_price
    FROM erp_finance_receipt_item i
    INNER JOIN erp_finance_receipt r
        ON r.id = i.receipt_id AND r.tenant_id = i.tenant_id
        AND r.deleted = 0 AND r.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 21
    GROUP BY i.tenant_id, i.biz_id
) a ON a.tenant_id = t.tenant_id AND a.biz_id = t.id
SET t.receipt_price = COALESCE(a.allocated_price, 0)
WHERE t.deleted = 0
  AND COALESCE(t.receipt_price, 0) <> COALESCE(a.allocated_price, 0);

-- 销售退货：核销明细为负数，业务单据退款缓存保存正数
UPDATE erp_sale_return t
LEFT JOIN (
    SELECT i.tenant_id, i.biz_id, ABS(SUM(i.receipt_price)) AS allocated_price
    FROM erp_finance_receipt_item i
    INNER JOIN erp_finance_receipt r
        ON r.id = i.receipt_id AND r.tenant_id = i.tenant_id
        AND r.deleted = 0 AND r.status = 20
    WHERE i.deleted = 0 AND i.write_off_status = 1 AND i.biz_type = 22
    GROUP BY i.tenant_id, i.biz_id
) a ON a.tenant_id = t.tenant_id AND a.biz_id = t.id
SET t.refund_price = COALESCE(a.allocated_price, 0)
WHERE t.deleted = 0
  AND COALESCE(t.refund_price, 0) <> COALESCE(a.allocated_price, 0);

COMMIT;
