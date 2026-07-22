-- 销售手推车审批与调拨出库联动：上线前后只读核对脚本
-- 适用 MySQL 5.7/8.0。本脚本仅包含 SELECT，不自动修复任何业务数据。
-- 约定：销售手推车来源类型为 30；库存预占状态 1/2/3 分别为锁定中/已释放/已扣减。

-- 1. 库存锁定数基础异常：负数，或锁定数超过实存数。
SELECT tenant_id, id AS stock_id, product_id, warehouse_id, count, lock_count
FROM erp_stock
WHERE deleted = b'0'
  AND (COALESCE(lock_count, 0) < 0 OR COALESCE(lock_count, 0) > COALESCE(count, 0));

-- 2. 库存表锁定数与有效预占汇总不一致。
SELECT s.tenant_id,
       s.id AS stock_id,
       s.product_id,
       s.warehouse_id,
       COALESCE(s.lock_count, 0) AS stock_lock_count,
       COALESCE(l.active_lock_count, 0) AS detail_lock_count,
       COALESCE(s.lock_count, 0) - COALESCE(l.active_lock_count, 0) AS difference
FROM erp_stock s
LEFT JOIN (
    SELECT tenant_id, product_id, warehouse_id, SUM(lock_count) AS active_lock_count
    FROM erp_stock_lock
    WHERE deleted = b'0' AND status = 1
    GROUP BY tenant_id, product_id, warehouse_id
) l ON l.tenant_id = s.tenant_id
   AND l.product_id = s.product_id
   AND l.warehouse_id = s.warehouse_id
WHERE s.deleted = b'0'
  AND COALESCE(s.lock_count, 0) <> COALESCE(l.active_lock_count, 0);

-- 3. 已结束销售手推车仍残留有效预占（40 终审、50 已生成销售单、60 已转报价、90 已取消）。
SELECT c.tenant_id, c.id AS cart_id, c.no AS cart_no, c.status AS cart_status,
       COUNT(l.id) AS active_lock_rows, SUM(l.lock_count) AS active_lock_count
FROM erp_sale_cart c
JOIN erp_stock_lock l
  ON l.tenant_id = c.tenant_id
 AND l.biz_type = 30
 AND l.biz_id = c.id
 AND l.status = 1
 AND l.deleted = b'0'
WHERE c.deleted = b'0'
  AND c.status IN (40, 50, 60, 90)
GROUP BY c.tenant_id, c.id, c.no, c.status;

-- 4. 同一销售明细存在多条有效预占，提示历史重复锁库或并发竞争。
SELECT tenant_id, biz_id AS cart_id, biz_item_id AS cart_item_id,
       COUNT(*) AS active_lock_rows, SUM(lock_count) AS active_lock_count
FROM erp_stock_lock
WHERE deleted = b'0'
  AND biz_type = 30
  AND status = 1
GROUP BY tenant_id, biz_id, biz_item_id
HAVING COUNT(*) > 1;

-- 5. 所有关联调拨均已审批，但销售手推车仍长期停留待终审。
-- 阈值可按上线环境调整，默认检查超过 30 分钟的数据。
SELECT c.tenant_id, c.id AS cart_id, c.no AS cart_no, c.update_time,
       COUNT(m.id) AS move_count,
       SUM(CASE WHEN m.status = 20 THEN 1 ELSE 0 END) AS approved_move_count
FROM erp_sale_cart c
JOIN erp_stock_move m
  ON m.tenant_id = c.tenant_id
 AND m.source_type = 30
 AND m.source_id = c.id
 AND m.transfer_direction = 10
 AND m.deleted = b'0'
WHERE c.deleted = b'0'
  AND c.status = 30
  AND c.update_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)
GROUP BY c.tenant_id, c.id, c.no, c.update_time
HAVING COUNT(m.id) > 0
   AND COUNT(m.id) = SUM(CASE WHEN m.status = 20 THEN 1 ELSE 0 END);

-- 6. 调拨来源与销售手推车无法对应的孤立数据。
SELECT m.tenant_id, m.id AS move_id, m.no AS move_no, m.source_id AS cart_id, m.status
FROM erp_stock_move m
LEFT JOIN erp_sale_cart c
  ON c.tenant_id = m.tenant_id
 AND c.id = m.source_id
 AND c.deleted = b'0'
WHERE m.deleted = b'0'
  AND m.source_type = 30
  AND m.transfer_direction = 10
  AND c.id IS NULL;

-- 7. 销售来源单据方向异常：新流程只允许方向 10（调拨出库）。
SELECT tenant_id, id AS move_id, no AS move_no, source_id AS cart_id,
       transfer_direction, status, create_time
FROM erp_stock_move
WHERE deleted = b'0'
  AND source_type = 30
  AND (transfer_direction IS NULL OR transfer_direction <> 10);

-- 8. 同一销售来源存在多张有效调拨出库单，提示重复生成或并发竞争。
SELECT tenant_id, source_id AS cart_id, COUNT(*) AS transfer_out_count,
       GROUP_CONCAT(no ORDER BY id SEPARATOR ',') AS transfer_out_nos
FROM erp_stock_move
WHERE deleted = b'0'
  AND source_type = 30
  AND transfer_direction = 10
GROUP BY tenant_id, source_id
HAVING COUNT(*) > 1;

-- 9. 销售调拨出库审批产生了 MOVE_IN 流水；新流程中该结果不应继续新增。
SELECT m.tenant_id, m.id AS transfer_out_id, m.no AS transfer_out_no,
       m.source_id AS cart_id, COUNT(r.id) AS move_in_record_count
FROM erp_stock_move m
JOIN erp_stock_record r
  ON r.tenant_id = m.tenant_id
 AND r.biz_id = m.id
 AND r.biz_type = 30
 AND r.deleted = b'0'
WHERE m.deleted = b'0'
  AND m.source_type = 30
  AND m.transfer_direction = 10
GROUP BY m.tenant_id, m.id, m.no, m.source_id;

-- 10. 销售调拨出库自动生成了调拨入库单；新流程中该结果不应继续新增。
SELECT out_move.tenant_id, out_move.id AS transfer_out_id, out_move.no AS transfer_out_no,
       out_move.source_id AS cart_id, in_move.id AS transfer_in_id, in_move.no AS transfer_in_no,
       in_move.status AS transfer_in_status
FROM erp_stock_move out_move
JOIN erp_stock_move in_move
  ON in_move.tenant_id = out_move.tenant_id
 AND in_move.related_move_id = out_move.id
 AND in_move.transfer_direction = 20
 AND in_move.deleted = b'0'
WHERE out_move.deleted = b'0'
  AND out_move.source_type = 30
  AND out_move.transfer_direction = 10;
