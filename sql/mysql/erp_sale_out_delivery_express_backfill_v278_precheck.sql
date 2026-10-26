-- 销售单送货快递单兼容主图回填前检查（v278）。
-- 只读脚本：确认目标库、依赖表/字段和预计影响范围，不修改任何数据。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SELECT DATABASE() AS target_database;

SELECT required_object, object_exists
FROM (
    SELECT 'erp_sale_out.express_file_url' AS required_object,
           EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'erp_sale_out'
                    AND column_name = 'express_file_url') AS object_exists
    UNION ALL
    SELECT 'erp_sale_pick_delivery_submit.sale_out_id',
           EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'erp_sale_pick_delivery_submit'
                    AND column_name = 'sale_out_id')
    UNION ALL
    SELECT 'erp_sale_pick_delivery_submit_file.file_url',
           EXISTS(SELECT 1 FROM information_schema.columns
                  WHERE table_schema = DATABASE() AND table_name = 'erp_sale_pick_delivery_submit_file'
                    AND column_name = 'file_url')
) dependency_check;

SELECT COUNT(DISTINCT sale_out.id) AS pending_backfill_count
FROM erp_sale_out sale_out
INNER JOIN erp_sale_pick_delivery_submit submit
        ON submit.sale_out_id = sale_out.id
       AND submit.tenant_id = sale_out.tenant_id
       AND submit.deleted = b'0'
       AND submit.type = 20
INNER JOIN erp_sale_pick_delivery_submit_file submit_file
        ON submit_file.submit_id = submit.id
       AND submit_file.tenant_id = submit.tenant_id
       AND submit_file.deleted = b'0'
       AND submit_file.file_url IS NOT NULL
       AND TRIM(submit_file.file_url) <> ''
WHERE sale_out.deleted = b'0'
  AND (sale_out.express_file_url IS NULL OR TRIM(sale_out.express_file_url) = '');

SELECT COUNT(DISTINCT sale_out.id) AS protected_existing_express_count
FROM erp_sale_out sale_out
INNER JOIN erp_sale_pick_delivery_submit submit
        ON submit.sale_out_id = sale_out.id
       AND submit.tenant_id = sale_out.tenant_id
       AND submit.deleted = b'0'
       AND submit.type = 20
INNER JOIN erp_sale_pick_delivery_submit_file submit_file
        ON submit_file.submit_id = submit.id
       AND submit_file.tenant_id = submit.tenant_id
       AND submit_file.deleted = b'0'
WHERE sale_out.deleted = b'0'
  AND sale_out.express_file_url IS NOT NULL
  AND TRIM(sale_out.express_file_url) <> '';

-- 应为 0：销售单与送货提交存在跨租户关联。
SELECT COUNT(*) AS cross_tenant_submit_relation_count
FROM erp_sale_out sale_out
INNER JOIN erp_sale_pick_delivery_submit submit
        ON submit.sale_out_id = sale_out.id
       AND submit.tenant_id <> sale_out.tenant_id
WHERE sale_out.deleted = b'0'
  AND submit.deleted = b'0'
  AND submit.type = 20;

-- 应为 0：送货提交与附件存在跨租户关联。
SELECT COUNT(*) AS cross_tenant_file_relation_count
FROM erp_sale_pick_delivery_submit submit
INNER JOIN erp_sale_pick_delivery_submit_file submit_file
        ON submit_file.submit_id = submit.id
       AND submit_file.tenant_id <> submit.tenant_id
WHERE submit.deleted = b'0'
  AND submit_file.deleted = b'0'
  AND submit.type = 20;

-- 仅用于核对：同一销售单下相同 URL 的有效送货附件会在详情接口去重，回填仍只选一张。
SELECT submit.sale_out_id, submit_file.file_url, COUNT(*) AS duplicate_count
FROM erp_sale_pick_delivery_submit submit
INNER JOIN erp_sale_pick_delivery_submit_file submit_file
        ON submit_file.submit_id = submit.id
       AND submit_file.tenant_id = submit.tenant_id
       AND submit_file.deleted = b'0'
WHERE submit.deleted = b'0'
  AND submit.type = 20
  AND submit.sale_out_id IS NOT NULL
  AND submit_file.file_url IS NOT NULL
  AND TRIM(submit_file.file_url) <> ''
GROUP BY submit.sale_out_id, submit_file.file_url
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, submit.sale_out_id ASC;

