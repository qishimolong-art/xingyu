-- ==============================================================
-- ERP 采购退货 七期 收尾补丁：放宽 supplier_id NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v7_nullable_supplier.sql
--
-- 背景：采购退货页客户反馈点击保存时后端报
--   "Field 'supplier_id' doesn't have a default value"。
--   DB 原 DDL 里 supplier_id 是 NOT NULL 无默认值，当前端某条路径未正确回填
--   supplierId（例如按单退货模式下用户未选原入库单、或 formApi.setValues 第二参
--   为 false 导致合并失败），MyBatis-Plus 省略该列写入 → MySQL 拒绝。
--
-- 本次放宽：保留业务必填语义（后端 Service 层已新增校验抛业务异常），
--   DB 侧放宽为允许 NULL 作为兜底，避免用户拿到"字段无默认值"这种裸 SQL 错误。
-- ==============================================================

ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `supplier_id` BIGINT NULL DEFAULT NULL COMMENT '供应商编号（业务层必填，DB 放宽避免 MP 省略写入报错）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
