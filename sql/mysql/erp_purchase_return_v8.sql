-- ================================================================
-- ERP 采购退货 八期字段扩展脚本
--
-- 背景：
--   八期任务：采购退货主表补齐三个核心字段（税率、部门ID、经办人），用于和
--   供应商/采购订单的税率、部门、经办人字段对齐。
--   其余字段（仓库类型 warehouse_type / 运输方式 transport_method / 运费类型 freight_type /
--   物流公司 logistics_company / 单据来源 doc_source / 优先级 priority /
--   开单方式 order_method / 开票类型 invoice_type / 结算方式 settle_method / 件数 package_count /
--   发货区 shipping_area 等）已在历史脚本中存在，不重复添加。
--
-- 作用：
--   erp_purchase_return 新增：
--     - tax_rate（税率）
--     - dept_id（部门ID，Long）—— 注意与已有的 dept 字符串字段并存
--     - handler（经办人/制单人 用户ID，Long）—— 注意与已有的 maker 字符串字段并存
--
-- 部署命令：
--   mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v8.sql
--
-- 注意：
--   每个字段都是 NULL DEFAULT NULL，避免部分环境"Field doesn't have a default value"报错。
-- ================================================================

ALTER TABLE `erp_purchase_return`
    ADD COLUMN `tax_rate` DECIMAL(10,2) NULL DEFAULT NULL COMMENT '税率',
    ADD COLUMN `dept_id` BIGINT NULL DEFAULT NULL COMMENT '部门ID',
    ADD COLUMN `handler` BIGINT NULL DEFAULT NULL COMMENT '经办人/制单人（用户ID）';
