-- ==============================================================
-- ERP 采购入库 v9 收尾补丁：放宽 order_no NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_in_v9_nullable_order_no.sql
--
-- 背景：手工新增采购入库时，系统未关联采购订单，此时 order_id/order_no 都可能为空。
-- 但数据库表里 order_no 仍是 NOT NULL 且无默认值，导致插入时报错：
--   "Field 'order_no' doesn't have a default value"。
--
-- 处理方式：将 order_no 调整为可空，兼容“手工新增入库”和“由采购订单生成入库”两种场景。
-- ==============================================================

ALTER TABLE `erp_purchase_in`
    MODIFY COLUMN `order_no` VARCHAR(64) NULL DEFAULT NULL COMMENT '采购订单号（手工新增入库时可为空）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
