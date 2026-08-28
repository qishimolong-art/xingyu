-- ERP 采购调价 v15: 主表 + 子表 + erp_purchase_in / erp_purchase_in_items 扩展字段
-- 兼容 MySQL 5.7 / 8.0.x 全版本（不使用 ADD COLUMN IF NOT EXISTS，改为基于 information_schema 判断的存储过程）
-- 幂等：重复执行安全
-- 执行前置：erp_purchase_in_v2.sql、erp_purchase_items_v2.sql 等已执行

-- ========== 1. 主表：erp_purchase_price_adjust ==========
CREATE TABLE IF NOT EXISTS `erp_purchase_price_adjust` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT        COMMENT '编号',
    `no`                  VARCHAR(64)                                  COMMENT '调价单号（CGTJ 前缀）',
    `status`              TINYINT       NOT NULL DEFAULT 10            COMMENT '状态：10=待审批 20=已通过 30=已拒绝',
    `adjust_time`         DATETIME                                     COMMENT '调价日期',
    `supplier_id`         BIGINT                                       COMMENT '供应商编号',
    `dept_id`             BIGINT                                       COMMENT '部门编号',
    `adjuster`            BIGINT                                       COMMENT '调价人（系统用户 ID）',
    `adjust_type`         TINYINT                                      COMMENT '调价类型：10=按入库单调价 20=添加明细',
    `remark`              VARCHAR(500)                                 COMMENT '备注',
    `total_adjust_price`  DECIMAL(18,2) DEFAULT 0                      COMMENT '调价总金额（可正可负）',
    `approve_time`        DATETIME                                     COMMENT '审批通过时间',
    `tenant_id`           BIGINT        NOT NULL DEFAULT 0             COMMENT '租户编号',
    `creator`             VARCHAR(64)   DEFAULT ''                     COMMENT '创建者',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`             VARCHAR(64)   DEFAULT ''                     COMMENT '更新者',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIT(1)        NOT NULL DEFAULT b'0'          COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_purchase_price_adjust_no` (`no`),
    KEY `idx_purchase_price_adjust_supplier_id` (`supplier_id`),
    KEY `idx_purchase_price_adjust_status` (`status`),
    KEY `idx_purchase_price_adjust_adjust_time` (`adjust_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购调价单';

-- ========== 2. 子表：erp_purchase_price_adjust_item ==========
CREATE TABLE IF NOT EXISTS `erp_purchase_price_adjust_item` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT        COMMENT '编号',
    `adjust_id`           BIGINT        NOT NULL                       COMMENT '调价单编号',
    `in_id`               BIGINT                                       COMMENT '采购入库单编号',
    `in_no`               VARCHAR(64)                                  COMMENT '采购入库单号（冗余）',
    `in_item_id`          BIGINT                                       COMMENT '采购入库项编号',
    `product_id`          BIGINT                                       COMMENT '产品编号',
    `warehouse_id`        BIGINT                                       COMMENT '仓库编号',
    `old_price`           DECIMAL(22,6)                                COMMENT '调价前单价',
    `new_price`           DECIMAL(22,6)                                COMMENT '调价后单价',
    `count`               DECIMAL(16,3)                                COMMENT '入库数量快照',
    `adjust_ratio`        DECIMAL(10,4)                                COMMENT '调价比率（按入库单方式填；添加明细方式为空）',
    `adjust_price`        DECIMAL(18,2)                                COMMENT '调价金额 = (new-old)*count',
    `product_code`        VARCHAR(64)                                  COMMENT '配件编码',
    `product_name`        VARCHAR(128)                                 COMMENT '配件名称',
    `product_unit_name`   VARCHAR(64)                                  COMMENT '单位名称',
    `weight`              DECIMAL(24,6)                                COMMENT '重量',
    `package_qty`         INT                                          COMMENT '包装数',
    `vehicle_model`       VARCHAR(128)                                 COMMENT '车型',
    `standard`            VARCHAR(128)                                 COMMENT '规格',
    `feature_code`        VARCHAR(64)                                  COMMENT '特征码',
    `origin_place`        VARCHAR(128)                                 COMMENT '产地',
    `brand`               VARCHAR(64)                                  COMMENT '品牌',
    `drawing_no`          VARCHAR(64)                                  COMMENT '图号',
    `warehouse_position`  VARCHAR(64)                                  COMMENT '货架位',
    `tenant_id`           BIGINT        NOT NULL DEFAULT 0             COMMENT '租户编号',
    `creator`             VARCHAR(64)   DEFAULT ''                     COMMENT '创建者',
    `create_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`             VARCHAR(64)   DEFAULT ''                     COMMENT '更新者',
    `update_time`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             BIT(1)        NOT NULL DEFAULT b'0'          COMMENT '是否删除',
    PRIMARY KEY (`id`),
    KEY `idx_purchase_price_adjust_item_adjust_id` (`adjust_id`),
    KEY `idx_purchase_price_adjust_item_in_id` (`in_id`),
    KEY `idx_purchase_price_adjust_item_in_item_id` (`in_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购调价单明细';

-- ========== 3. 扩展 erp_purchase_in：adjusted ==========
DROP PROCEDURE IF EXISTS erp_purchase_in_v15_apply;

DELIMITER $$
CREATE PROCEDURE erp_purchase_in_v15_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_purchase_in' AND COLUMN_NAME = 'adjusted') THEN
        ALTER TABLE erp_purchase_in ADD COLUMN adjusted BIT(1) DEFAULT b'0' COMMENT '是否被调价过（前端列表"（调）"标识依据）';
    END IF;
END$$
DELIMITER ;

CALL erp_purchase_in_v15_apply();
DROP PROCEDURE IF EXISTS erp_purchase_in_v15_apply;

-- ========== 4. 扩展 erp_purchase_in_items：original_product_price / adjusted / adjust_id ==========
DROP PROCEDURE IF EXISTS erp_purchase_in_items_v15_apply;

DELIMITER $$
CREATE PROCEDURE erp_purchase_in_items_v15_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_purchase_in_items' AND COLUMN_NAME = 'original_product_price') THEN
        ALTER TABLE erp_purchase_in_items ADD COLUMN original_product_price DECIMAL(22,6) COMMENT '原价快照（首次调价时写入，后续不变）';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_purchase_in_items' AND COLUMN_NAME = 'adjusted') THEN
        ALTER TABLE erp_purchase_in_items ADD COLUMN adjusted BIT(1) DEFAULT b'0' COMMENT '是否被调价过';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_purchase_in_items' AND COLUMN_NAME = 'adjust_id') THEN
        ALTER TABLE erp_purchase_in_items ADD COLUMN adjust_id BIGINT COMMENT '最后一次调价单 ID';
    END IF;
END$$
DELIMITER ;

CALL erp_purchase_in_items_v15_apply();
DROP PROCEDURE IF EXISTS erp_purchase_in_items_v15_apply;

-- ========== 5. 幂等索引：idx_purchase_in_items_adjust_id ==========
DROP PROCEDURE IF EXISTS erp_purchase_in_items_v15_index;

DELIMITER $$
CREATE PROCEDURE erp_purchase_in_items_v15_index()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_purchase_in_items' AND INDEX_NAME = 'idx_purchase_in_items_adjust_id') THEN
        CREATE INDEX idx_purchase_in_items_adjust_id ON erp_purchase_in_items(adjust_id);
    END IF;
END$$
DELIMITER ;

CALL erp_purchase_in_items_v15_index();
DROP PROCEDURE IF EXISTS erp_purchase_in_items_v15_index;
