-- ==============================================================
-- ERP 采购退货 七期 收尾：放宽已删字段的 NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v7_nullable.sql
--
-- 背景：七期从采购退货录入表单删除了 accountId / discountPercent /
--   discountPrice / otherPrice / totalPrice 等字段。DB 原 DDL 这些列
--   是 NOT NULL 无默认值，导致 MyBatis-Plus 省略写入时报错：
--   "Field 'xxx' doesn't have a default value"
--
-- 处理方式：统一放宽为允许 NULL，保留历史数据不动。
-- ==============================================================

ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `account_id`       BIGINT          NULL     DEFAULT NULL     COMMENT '结算账户编号（字段已下线，保留列以兼容历史数据）',
    MODIFY COLUMN `discount_percent` DECIMAL(5,2)    NULL     DEFAULT 0.00     COMMENT '优惠率（字段已下线）',
    MODIFY COLUMN `discount_price`   DECIMAL(20,4)   NULL     DEFAULT 0.0000   COMMENT '优惠金额（字段已下线）',
    MODIFY COLUMN `other_price`      DECIMAL(20,4)   NULL     DEFAULT 0.0000   COMMENT '其他费用（字段已下线）',
    MODIFY COLUMN `total_price`      DECIMAL(20,4)   NULL     DEFAULT 0.0000   COMMENT '应退金额（后端仍自动计算）';

-- ==============================================================
-- 脚本结束
-- ==============================================================
