-- ----------------------------
-- ERP 库存成本核算 一期 数据库迁移脚本
-- 用途：为 erp_stock 与 erp_stock_record 增加成本核算相关字段，支持移动加权平均法
-- 日期：2026-05-08
-- 作者：Agent-DB
-- ----------------------------

-- 1. erp_stock 追加成本均价、成本金额字段
ALTER TABLE erp_stock
    ADD COLUMN cost_price  DECIMAL(16,6) DEFAULT 0.000000 COMMENT '成本均价（移动加权平均单价），单位：元',
    ADD COLUMN cost_amount DECIMAL(20,4) DEFAULT 0.0000   COMMENT '成本金额 = count × cost_price，单位：元';

-- 2. erp_stock_record 追加本次单价/金额、结存成本均价/金额、业务发生日期
ALTER TABLE erp_stock_record
    ADD COLUMN unit_price   DECIMAL(16,6) DEFAULT NULL     COMMENT '本次业务单价（入库进价 / 出库成本均价），单位：元',
    ADD COLUMN total_price  DECIMAL(20,4) DEFAULT NULL     COMMENT '本次业务金额 = count × unit_price，单位：元',
    ADD COLUMN cost_price   DECIMAL(16,6) DEFAULT 0.000000 COMMENT '结存成本均价（业务发生后的仓库内成本均价），单位：元',
    ADD COLUMN cost_amount  DECIMAL(20,4) DEFAULT 0.0000   COMMENT '结存成本金额 = total_count × cost_price，单位：元',
    ADD COLUMN biz_date     DATETIME      DEFAULT NULL     COMMENT '业务发生日期（取自单据的业务日期，如入库时间/出库时间）';
