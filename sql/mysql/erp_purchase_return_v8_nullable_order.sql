-- ==============================================================
-- ERP 采购退货 八期 收尾补丁：放宽 order_id NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v8_nullable_order.sql
--
-- 背景：八期"按库存退货"模式下，前端不传 orderId（按库存退货不强制关联原订单）。
--   DB 原 DDL 里 order_id 是 NOT NULL 无默认值，MyBatis-Plus 省略该列 INSERT 时
--   报错："Field 'order_id' doesn't have a default value"。
--
-- 本次放宽：保留业务必填语义（按单退货模式下 Service 层仍会从 orderId 带出 supplierId），
--   DB 侧放宽为允许 NULL 作为兜底，兼容按库存退货场景。
-- ==============================================================

ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `order_id` BIGINT NULL DEFAULT NULL COMMENT '采购订单编号（按库存退货时可为空）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
