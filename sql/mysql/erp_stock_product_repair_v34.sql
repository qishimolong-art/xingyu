-- ERP 产品库存修复脚本 v34
-- 目的：
-- 1. 为历史上已存在、但未初始化库存的产品补齐 0 库存记录
-- 2. 清理已被删除/不存在产品对应的历史孤儿库存数据
-- 3. 使用逻辑删除，避免直接物理删数据
--
-- 适用场景：
-- - “配件信息”里已经有产品，但“产品库存”里看不到
-- - “产品库存”里存在一些已经不在“配件信息”中的产品
--
-- 执行建议：
-- 1. 先在测试环境验证
-- 2. 先执行本脚本中的“预览 SQL”
-- 3. 确认结果无误后，再执行“修复 SQL”

-- =========================================================
-- 一、预览：哪些产品缺少库存初始化
-- 说明：
-- - 按产品默认仓库补 1 条 0 库存
-- - 仅处理未删除产品、且默认仓库不为空的数据
-- =========================================================

SELECT
    p.id AS product_id,
    p.name AS product_name,
    p.code AS product_code,
    p.default_warehouse_id
FROM erp_product p
LEFT JOIN erp_stock s
       ON s.product_id = p.id
      AND s.warehouse_id = p.default_warehouse_id
      AND s.deleted = b'0'
WHERE p.deleted = b'0'
  AND p.default_warehouse_id IS NOT NULL
  AND s.id IS NULL
ORDER BY p.id DESC;

-- =========================================================
-- 二、预览：哪些库存是孤儿库存
-- 说明：
-- - 库存表里有记录，但产品表中已不存在或已逻辑删除
-- =========================================================

SELECT
    s.id,
    s.product_id,
    s.warehouse_id,
    s.count,
    s.lock_count,
    s.cost_price,
    s.cost_amount
FROM erp_stock s
LEFT JOIN erp_product p
       ON p.id = s.product_id
      AND p.deleted = b'0'
WHERE s.deleted = b'0'
  AND p.id IS NULL
ORDER BY s.id DESC;

-- =========================================================
-- 三、预览：哪些库存流水是孤儿数据
-- =========================================================

SELECT
    r.id,
    r.product_id,
    r.warehouse_id,
    r.count,
    r.biz_type,
    r.biz_no,
    r.biz_date
FROM erp_stock_record r
LEFT JOIN erp_product p
       ON p.id = r.product_id
      AND p.deleted = b'0'
WHERE r.deleted = b'0'
  AND p.id IS NULL
ORDER BY r.id DESC;

-- =========================================================
-- 四、预览：哪些库存锁定是孤儿数据
-- =========================================================

SELECT
    l.id,
    l.product_id,
    l.warehouse_id,
    l.lock_count,
    l.biz_type,
    l.biz_no,
    l.status
FROM erp_stock_lock l
LEFT JOIN erp_product p
       ON p.id = l.product_id
      AND p.deleted = b'0'
WHERE l.deleted = b'0'
  AND p.id IS NULL
ORDER BY l.id DESC;

-- =========================================================
-- 五、修复：为历史产品补齐默认仓库 0 库存
-- 说明：
-- - 只补不存在的库存行
-- - count / lock_count / cost_price / cost_amount 初始化为 0
-- =========================================================

INSERT INTO erp_stock (
    product_id,
    warehouse_id,
    count,
    lock_count,
    cost_price,
    cost_amount,
    creator,
    create_time,
    updater,
    update_time,
    deleted
)
SELECT
    p.id,
    p.default_warehouse_id,
    0,
    0,
    0,
    0,
    '1',
    NOW(),
    '1',
    NOW(),
    b'0'
FROM erp_product p
LEFT JOIN erp_stock s
       ON s.product_id = p.id
      AND s.warehouse_id = p.default_warehouse_id
      AND s.deleted = b'0'
WHERE p.deleted = b'0'
  AND p.default_warehouse_id IS NOT NULL
  AND s.id IS NULL;

-- =========================================================
-- 六、修复：逻辑删除孤儿库存
-- =========================================================

UPDATE erp_stock s
LEFT JOIN erp_product p
       ON p.id = s.product_id
      AND p.deleted = b'0'
SET s.deleted = b'1',
    s.updater = '1',
    s.update_time = NOW()
WHERE s.deleted = b'0'
  AND p.id IS NULL;

-- =========================================================
-- 七、修复：逻辑删除孤儿库存流水
-- =========================================================

UPDATE erp_stock_record r
LEFT JOIN erp_product p
       ON p.id = r.product_id
      AND p.deleted = b'0'
SET r.deleted = b'1',
    r.updater = '1',
    r.update_time = NOW()
WHERE r.deleted = b'0'
  AND p.id IS NULL;

-- =========================================================
-- 八、修复：逻辑删除孤儿库存锁定
-- =========================================================

UPDATE erp_stock_lock l
LEFT JOIN erp_product p
       ON p.id = l.product_id
      AND p.deleted = b'0'
SET l.deleted = b'1',
    l.updater = '1',
    l.update_time = NOW()
WHERE l.deleted = b'0'
  AND p.id IS NULL;

-- =========================================================
-- 九、修复后复核：当前还缺少库存初始化的产品数
-- =========================================================

SELECT COUNT(1) AS missing_stock_row_count
FROM erp_product p
LEFT JOIN erp_stock s
       ON s.product_id = p.id
      AND s.warehouse_id = p.default_warehouse_id
      AND s.deleted = b'0'
WHERE p.deleted = b'0'
  AND p.default_warehouse_id IS NOT NULL
  AND s.id IS NULL;

-- =========================================================
-- 十、修复后复核：当前仍存在的孤儿库存数
-- =========================================================

SELECT COUNT(1) AS orphan_stock_count
FROM erp_stock s
LEFT JOIN erp_product p
       ON p.id = s.product_id
      AND p.deleted = b'0'
WHERE s.deleted = b'0'
  AND p.id IS NULL;

SELECT COUNT(1) AS orphan_stock_record_count
FROM erp_stock_record r
LEFT JOIN erp_product p
       ON p.id = r.product_id
      AND p.deleted = b'0'
WHERE r.deleted = b'0'
  AND p.id IS NULL;

SELECT COUNT(1) AS orphan_stock_lock_count
FROM erp_stock_lock l
LEFT JOIN erp_product p
       ON p.id = l.product_id
      AND p.deleted = b'0'
WHERE l.deleted = b'0'
  AND p.id IS NULL;
