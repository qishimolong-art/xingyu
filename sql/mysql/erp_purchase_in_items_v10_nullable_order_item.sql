-- ==============================================================
-- ERP purchase inbound items v10 patch: allow nullable order_item_id
-- Deploy:
--   mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_in_items_v10_nullable_order_item.sql
--
-- Manual purchase inbound creation does not link to a purchase order item.
-- Keep order_item_id nullable so inserts do not fail with:
--   Field 'order_item_id' doesn't have a default value
-- ==============================================================

ALTER TABLE `erp_purchase_in_items`
    MODIFY COLUMN `order_item_id` BIGINT NULL DEFAULT NULL COMMENT '采购订单项编号（手工新增入库时可为空）';

-- ==============================================================
-- End
-- ==============================================================
