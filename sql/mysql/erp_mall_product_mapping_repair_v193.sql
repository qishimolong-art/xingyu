-- ERP 同步商城商品历史映射修复（v193）
-- 背景：
--   ERP 配件同步旧逻辑只按 erp_product_id 查 erp_mall_product_mapping。
--   如果原商城商品未提前建立映射，修改 ERP 配件后会新建 SPU，导致同名商品重复。
--
-- 本脚本仅处理已确认的历史重复数据：
--   - 保留商城商品 SPU：15227
--   - 重复商城商品 SPU：15232
--
-- 安全说明：
--   - 不物理删除商品、不删除 SKU、不删除订单数据。
--   - 仅将相关映射改回 15227，并将 15232 移入商品回收站（status = -1）。
--   - 执行前请先查看脚本开头的 SELECT 结果是否符合预期。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @keep_spu_id := 15227;
SET @duplicate_spu_id := 15232;

-- 1. 执行前确认：保留商品、重复商品、相关 SKU、现有映射
SELECT id, name, code, introduction, description, category_id, brand_id, status, deleted, tenant_id
FROM product_spu
WHERE id IN (@keep_spu_id, @duplicate_spu_id);

SELECT id, spu_id, price, stock, deleted, tenant_id
FROM product_sku
WHERE spu_id IN (@keep_spu_id, @duplicate_spu_id)
ORDER BY spu_id, id;

SELECT id, erp_product_id, mall_spu_id, mall_sku_id, sync_status, fail_reason, deleted, tenant_id
FROM erp_mall_product_mapping
WHERE mall_spu_id IN (@keep_spu_id, @duplicate_spu_id)
ORDER BY id;

START TRANSACTION;

SELECT @keep_sku_id := id
FROM product_sku
WHERE spu_id = @keep_spu_id
  AND deleted = b'0'
ORDER BY id
LIMIT 1;

-- 2. 保留商品参数为空时，从重复商品补齐，避免回收重复项后丢失已同步的配件字段。
UPDATE product_spu keep_spu
JOIN product_spu duplicate_spu
  ON duplicate_spu.id = @duplicate_spu_id
 AND duplicate_spu.deleted = b'0'
SET keep_spu.code = CASE
        WHEN keep_spu.code IS NULL OR keep_spu.code = '' THEN duplicate_spu.code
        ELSE keep_spu.code
    END,
    keep_spu.standard = CASE
        WHEN keep_spu.standard IS NULL OR keep_spu.standard = '' THEN duplicate_spu.standard
        ELSE keep_spu.standard
    END,
    keep_spu.feature_code = CASE
        WHEN keep_spu.feature_code IS NULL OR keep_spu.feature_code = '' THEN duplicate_spu.feature_code
        ELSE keep_spu.feature_code
    END,
    keep_spu.vehicle_model = CASE
        WHEN keep_spu.vehicle_model IS NULL OR keep_spu.vehicle_model = '' THEN duplicate_spu.vehicle_model
        ELSE keep_spu.vehicle_model
    END,
    keep_spu.update_time = NOW()
WHERE keep_spu.id = @keep_spu_id
  AND keep_spu.deleted = b'0';

-- 3. 将原来指向重复商品的 ERP 映射改回保留商品。
UPDATE erp_mall_product_mapping
SET mall_spu_id = @keep_spu_id,
    mall_sku_id = COALESCE(@keep_sku_id, mall_sku_id),
    sync_status = 0,
    fail_reason = '历史重复商品已合并到保留 SPU 15227',
    last_sync_time = NOW(),
    update_time = NOW()
WHERE mall_spu_id = @duplicate_spu_id
  AND deleted = b'0';

-- 4. 将重复商品移入回收站，避免继续在商品列表和小程序展示。
UPDATE product_spu
SET status = -1,
    update_time = NOW()
WHERE id = @duplicate_spu_id
  AND deleted = b'0';

COMMIT;

-- 5. 执行后确认：映射应指向 15227，15232 应进入回收站。
SELECT id, name, code, standard, feature_code, vehicle_model, status, deleted
FROM product_spu
WHERE id IN (@keep_spu_id, @duplicate_spu_id);

SELECT id, erp_product_id, mall_spu_id, mall_sku_id, sync_status, fail_reason, last_sync_time, deleted
FROM erp_mall_product_mapping
WHERE mall_spu_id IN (@keep_spu_id, @duplicate_spu_id)
   OR fail_reason = '历史重复商品已合并到保留 SPU 15227'
ORDER BY update_time DESC, id DESC;
