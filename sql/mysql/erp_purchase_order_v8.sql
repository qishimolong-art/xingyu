-- ================================================================
-- ERP 采购订单 八期字段扩展脚本
--
-- 背景：
--   八期任务：采购订单表单补齐"单据类型"、"最近订货日期（只读）"两个字段。
--   documentType 为前端硬编码下拉，后端仅存字符串；latestOrderDate 留给后续业务回写。
--
-- 作用：
--   erp_purchase_order 新增 document_type（单据类型）、latest_order_date（最近订货日期，只读）
--
-- 部署命令：
--   mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_order_v8.sql
-- ================================================================

ALTER TABLE `erp_purchase_order`
    ADD COLUMN `document_type` VARCHAR(32) NULL DEFAULT NULL COMMENT '单据类型',
    ADD COLUMN `latest_order_date` DATETIME NULL DEFAULT NULL COMMENT '最近订货日期（只读）';
