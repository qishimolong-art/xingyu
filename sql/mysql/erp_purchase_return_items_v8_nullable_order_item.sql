-- ==============================================================
-- ERP 采购退货子表 八期 收尾补丁：放宽 order_item_id NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_items_v8_nullable_order_item.sql
--
-- 背景：三期引入"按入库单退货"和"按库存退货"模式后，子表不再强制关联采购订单项。
--   - 按入库单退货：使用 source_in_id / source_in_item_id，order_item_id 为空
--   - 按库存退货：都不关联，order_item_id 为空
--   - 按订单退货（旧模式）：仍使用 order_item_id
--
--   DB 原 DDL 里 order_item_id 是 NOT NULL 无默认值，MyBatis-Plus 省略该列 INSERT 时
--   报错："Field 'order_item_id' doesn't have a default value"。
--
-- 本次放宽：DB 侧放宽为允许 NULL 作为兜底，兼容新退货模式。
-- ==============================================================

ALTER TABLE `erp_purchase_return_items`
    MODIFY COLUMN `order_item_id` BIGINT NULL DEFAULT NULL COMMENT '采购订单项编号（新退货模式下可为空）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
