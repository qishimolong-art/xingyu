-- ERP 销售拣货/送货明细补齐仓位与包装数字段
-- 说明：字段从销售出库明细生成快照；历史数据按 sale_out_item_id 回填。

ALTER TABLE `erp_sale_pick_delivery_item`
    ADD COLUMN `warehouse_position` varchar(255) DEFAULT NULL COMMENT '仓位' AFTER `count`,
    ADD COLUMN `package_qty` int DEFAULT NULL COMMENT '包装数' AFTER `warehouse_position`;

UPDATE `erp_sale_pick_delivery_item` pdi
INNER JOIN `erp_sale_out_items` soi
    ON soi.`id` = pdi.`sale_out_item_id`
   AND soi.`tenant_id` = pdi.`tenant_id`
   AND soi.`deleted` = b'0'
SET pdi.`warehouse_position` = soi.`warehouse_position`,
    pdi.`package_qty` = soi.`package_qty`
WHERE pdi.`deleted` = b'0'
  AND (pdi.`warehouse_position` IS NULL OR pdi.`package_qty` IS NULL);
