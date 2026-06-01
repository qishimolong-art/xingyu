-- ==============================================================
-- ERP 采购入库 v8 收尾补丁：放宽 order_id NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_in_v8_nullable_order.sql
--
-- 背景：普通采购入库允许不关联采购订单新增，此时 Service 层会传入 orderId = null。
--   但 DB 原 DDL 里 order_id 仍是 NOT NULL 且无默认值，导致插入时报错：
--   "Field 'order_id' doesn't have a default value"。
--
-- 处理方式：将 order_id 调整为可空，兼容“手工新增入库”和“由采购订单生成入库”两种场景。
-- ==============================================================

ALTER TABLE `erp_purchase_in`
    MODIFY COLUMN `order_id` BIGINT NULL DEFAULT NULL COMMENT '采购订单编号（手工新增入库时可为空）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
