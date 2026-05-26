-- ================================================================
-- ERP 供应商 八期字段扩展脚本
--
-- 背景：
--   八期任务：供应商表单补齐"淘汰日期"字段（与已有 obsolete 淘汰布尔开关配合使用）。
--   前端表单允许用户选择淘汰日期，后端不设默认值。
--
-- 作用：
--   erp_supplier 新增 obsolete_date（淘汰日期）
--
-- 部署命令：
--   mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_supplier_v8.sql
-- ================================================================

ALTER TABLE `erp_supplier`
    ADD COLUMN `obsolete_date` DATETIME NULL DEFAULT NULL COMMENT '淘汰日期';
