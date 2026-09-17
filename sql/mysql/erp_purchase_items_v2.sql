-- ERP 采购入库项 / 采购退货项 二期 字段扩展脚本
-- 用途：为 erp_purchase_in_items、erp_purchase_return_items 补齐汽配子表字段
-- 日期：2026-05-08

-- ========== 采购入库项：新增 10 个汽配字段 ==========
ALTER TABLE `erp_purchase_in_items`
    ADD COLUMN `package_qty`        INT          DEFAULT 1    COMMENT '包装数（默认 1，从商品资料带出）',
    ADD COLUMN `whole_qty`           INT          DEFAULT NULL COMMENT '整件数（用户填写，count = whole_qty × package_qty）',
    ADD COLUMN `warehouse_position`  VARCHAR(64)  DEFAULT NULL COMMENT '仓位 / 货架位（非仓库 ID，是仓库内的货架位）',
    ADD COLUMN `drawing_no`          VARCHAR(64)  DEFAULT NULL COMMENT '图号',
    ADD COLUMN `batch_no`            VARCHAR(64)  DEFAULT NULL COMMENT '批次',
    ADD COLUMN `bar_code`            VARCHAR(128) DEFAULT NULL COMMENT '条形码',
    ADD COLUMN `brand`               VARCHAR(64)  DEFAULT NULL COMMENT '品牌（从商品资料带出）',
    ADD COLUMN `vehicle_model`       VARCHAR(128) DEFAULT NULL COMMENT '适用车型（从商品资料带出）',
    ADD COLUMN `origin_place`        VARCHAR(64)  DEFAULT NULL COMMENT '产地（从商品资料带出）',
    ADD COLUMN `business_entity`     VARCHAR(64)  DEFAULT NULL COMMENT '所属经营（单据项级）';

-- ========== 采购退货项：新增 8 个汽配字段（vehicle_model / origin_place 一期已加，此处不重复） ==========
ALTER TABLE `erp_purchase_return_items`
    ADD COLUMN `package_qty`        INT          DEFAULT 1    COMMENT '包装数（从商品资料带出）',
    ADD COLUMN `whole_qty`           INT          DEFAULT NULL COMMENT '整件数（按单退货只读，按库存退货有用）',
    ADD COLUMN `warehouse_position`  VARCHAR(64)  DEFAULT NULL COMMENT '仓位 / 货架位（非仓库 ID，是仓库内的货架位）',
    ADD COLUMN `drawing_no`          VARCHAR(64)  DEFAULT NULL COMMENT '图号',
    ADD COLUMN `batch_no`            VARCHAR(64)  DEFAULT NULL COMMENT '批次',
    ADD COLUMN `bar_code`            VARCHAR(128) DEFAULT NULL COMMENT '条形码',
    ADD COLUMN `brand`               VARCHAR(64)  DEFAULT NULL COMMENT '品牌',
    ADD COLUMN `business_entity`     VARCHAR(64)  DEFAULT NULL COMMENT '所属经营（单据项级）';
