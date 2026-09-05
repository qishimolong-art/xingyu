-- Customer price level options aligned with product price fields (v211).
-- Safe to execute repeatedly. Does not update erp_customer.price_level.

SET @tenant_id := 1;

INSERT INTO `erp_base_data`
(`type`, `name`, `code`, `sort`, `status`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT seed.type, seed.name, seed.code, seed.sort, 0, seed.remark, '1', NOW(), '1', NOW(), b'0', @tenant_id
FROM (
    SELECT 'price_level' AS type, '采购价' AS name, 'purchase_price' AS code, 10 AS sort, '客户价格级别对齐配件价格体系' AS remark
    UNION ALL SELECT 'price_level', '销售价', 'sale_price', 20, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '最低价', 'min_price', 30, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '参考价', 'reference_price', 40, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '零售价', 'retail_price', 50, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '最后采购入库价', 'last_purchase_in_price', 60, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '备用价1', 'spare_price', 70, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '批发价', 'wholesale_price', 80, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '股份价', 'share_price', 90, '客户价格级别对齐配件价格体系'
    UNION ALL SELECT 'price_level', '订货价', 'order_price', 100, '客户价格级别对齐配件价格体系'
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM `erp_base_data` existing
    WHERE existing.`type` = seed.type
      AND existing.`code` = seed.code
      AND existing.`tenant_id` = @tenant_id
      AND existing.`deleted` = b'0'
);

UPDATE `erp_base_data` bd
JOIN (
    SELECT 'purchase_price' AS code, '采购价' AS name, 10 AS sort
    UNION ALL SELECT 'sale_price', '销售价', 20
    UNION ALL SELECT 'min_price', '最低价', 30
    UNION ALL SELECT 'reference_price', '参考价', 40
    UNION ALL SELECT 'retail_price', '零售价', 50
    UNION ALL SELECT 'last_purchase_in_price', '最后采购入库价', 60
    UNION ALL SELECT 'spare_price', '备用价1', 70
    UNION ALL SELECT 'wholesale_price', '批发价', 80
    UNION ALL SELECT 'share_price', '股份价', 90
    UNION ALL SELECT 'order_price', '订货价', 100
) seed ON seed.code = bd.`code`
SET bd.`name` = seed.name,
    bd.`sort` = seed.sort,
    bd.`status` = 0,
    bd.`updater` = '1',
    bd.`update_time` = NOW()
WHERE bd.`type` = 'price_level'
  AND bd.`tenant_id` = @tenant_id
  AND bd.`deleted` = b'0';

UPDATE `erp_base_data`
SET `name` = '库存成本价（历史）',
    `status` = 1,
    `remark` = '历史价格级别，仅兼容已有客户，不再展示为可选项',
    `updater` = '1',
    `update_time` = NOW()
WHERE `type` = 'price_level'
  AND `code` = 'stock_cost_price'
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0';

SELECT `code`, `name`, `sort`, `status`
FROM `erp_base_data`
WHERE `type` = 'price_level'
  AND `tenant_id` = @tenant_id
  AND `deleted` = b'0'
ORDER BY `sort`, `id`;
