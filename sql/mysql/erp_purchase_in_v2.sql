-- ERP 采购入库主表 二期 字段扩展脚本
-- 用途：为 erp_purchase_in 增加汽配行业扩展字段（26 个）
-- 日期：2026-05-08

ALTER TABLE `erp_purchase_in`
    -- ========== 系统信息 ==========
    ADD COLUMN `purchaser`          VARCHAR(64)    DEFAULT NULL                  COMMENT '采购员（用户名或显示名）',
    ADD COLUMN `invoice_type`       VARCHAR(64)    DEFAULT NULL                  COMMENT '开票类型（收据/不开票/专票/普票）',
    ADD COLUMN `transport_method`   VARCHAR(64)    DEFAULT NULL                  COMMENT '运输方式（快递/物流/自提）',
    ADD COLUMN `settle_method`      VARCHAR(64)    DEFAULT NULL                  COMMENT '结算方式（挂账/现结/月结）',
    ADD COLUMN `purchase_area`      VARCHAR(64)    DEFAULT NULL                  COMMENT '进货区',
    ADD COLUMN `accountant`         VARCHAR(64)    DEFAULT NULL                  COMMENT '记账员',
    ADD COLUMN `float_rate`         DECIMAL(8,4)   DEFAULT 1.0000                COMMENT '浮动率',
    ADD COLUMN `package_count`      INT            DEFAULT 0                     COMMENT '件数',
    ADD COLUMN `factory_order_no`   VARCHAR(128)   DEFAULT NULL                  COMMENT '厂家单号',
    ADD COLUMN `order_method`       VARCHAR(64)    DEFAULT NULL                  COMMENT '开单方式（正常单/样品单/赠品单）',
    -- ========== 运费信息 ==========
    ADD COLUMN `freight_type1`      VARCHAR(64)    DEFAULT NULL                  COMMENT '运费类型 1',
    ADD COLUMN `freight_type2`      VARCHAR(64)    DEFAULT NULL                  COMMENT '运费类型 2',
    ADD COLUMN `freight_object1`    VARCHAR(64)    DEFAULT NULL                  COMMENT '运费对象 1',
    ADD COLUMN `freight_object2`    VARCHAR(64)    DEFAULT NULL                  COMMENT '运费对象 2',
    ADD COLUMN `logistics_company`  VARCHAR(128)   DEFAULT NULL                  COMMENT '物流公司',
    -- ========== 供应商信息 ==========
    ADD COLUMN `handler`            VARCHAR(64)    DEFAULT NULL                  COMMENT '经办人（默认当前用户）',
    ADD COLUMN `tax_rate`           DECIMAL(8,4)   DEFAULT NULL                  COMMENT '税率（百分比）',
    ADD COLUMN `dept_id`            BIGINT         DEFAULT NULL                  COMMENT '部门 ID',
    ADD COLUMN `purchase_discount`  DECIMAL(20,4)  DEFAULT 0.0000                COMMENT '采购折让金额',
    ADD COLUMN `priority`           VARCHAR(64)    DEFAULT NULL                  COMMENT '优先级（正常件/紧急件）',
    ADD COLUMN `unloader`           VARCHAR(64)    DEFAULT NULL                  COMMENT '卸货员',
    ADD COLUMN `float_record`       VARCHAR(128)   DEFAULT NULL                  COMMENT '浮动记录',
    ADD COLUMN `receive_unit`       VARCHAR(128)   DEFAULT NULL                  COMMENT '收货单位',
    ADD COLUMN `total_freight1`     DECIMAL(20,4)  DEFAULT 0.0000                COMMENT '总运费 1',
    ADD COLUMN `total_freight2`     DECIMAL(20,4)  DEFAULT 0.0000                COMMENT '总运费 2',
    ADD COLUMN `payment_date`       DATETIME       DEFAULT NULL                  COMMENT '付款日期',
    ADD COLUMN `has_invoice`        TINYINT(1)     DEFAULT 0                     COMMENT '是否发票（复选）',
    -- ========== 其他信息 ==========
    ADD COLUMN `business_entity`    VARCHAR(128)   DEFAULT NULL                  COMMENT '所属经营';
