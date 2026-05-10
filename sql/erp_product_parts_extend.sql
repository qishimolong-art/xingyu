-- ============================================================
-- ERP 配件信息管理模块扩展（MySQL）
-- 功能：为 erp_product 增加配件管理所需字段，并新建通用件子表
-- 执行时间：一次性 DDL，幂等执行请自行判断（本脚本未带 IF NOT EXISTS 包裹）
-- ============================================================

-- 1. erp_product 表新增字段
ALTER TABLE `erp_product`
    ADD COLUMN `code`                 VARCHAR(32)   DEFAULT NULL COMMENT '配件编码（自动生成，P+6位流水）',
    ADD COLUMN `default_warehouse_id` BIGINT        DEFAULT NULL COMMENT '默认仓库编号，关联 erp_warehouse.id',
    ADD COLUMN `vehicle_model`        VARCHAR(128)  DEFAULT NULL COMMENT '适用车型',
    ADD COLUMN `factory_code`         VARCHAR(64)   DEFAULT NULL COMMENT '厂家编码',
    ADD COLUMN `reference_price`      DECIMAL(10,2) DEFAULT NULL COMMENT '参考价',
    ADD COLUMN `retail_price`         DECIMAL(10,2) DEFAULT NULL COMMENT '零售价',
    ADD COLUMN `last_purchase_price`  DECIMAL(10,2) DEFAULT NULL COMMENT '最后一次采购入库价格（采购入库回写）',
    ADD COLUMN `gross_profit_rate`    INT           DEFAULT NULL COMMENT '毛利率（%），可为负',
    ADD COLUMN `backup_price1`        DECIMAL(10,2) DEFAULT NULL COMMENT '备用价1',
    ADD COLUMN `wholesale_price`      DECIMAL(10,2) DEFAULT NULL COMMENT '批发价',
    ADD COLUMN `stock_max`            INT           DEFAULT NULL COMMENT '库存上限',
    ADD COLUMN `stock_min`            INT           DEFAULT NULL COMMENT '库存下限',
    ADD COLUMN `stock_standard`       INT           DEFAULT NULL COMMENT '标准库存',
    ADD COLUMN `package_qty`          INT           DEFAULT 1    COMMENT '包装数（默认 1）',
    ADD COLUMN `main_image`           VARCHAR(512)  DEFAULT NULL COMMENT '主图 URL（走 infra 文件服务）',
    ADD COLUMN `detail_content`       TEXT                       COMMENT '配件详情页 Markdown 内容',
    ADD COLUMN `merged_flag`          TINYINT(1)    DEFAULT 0    COMMENT '是否已合并：0-未合并，1-已合并',
    ADD COLUMN `merged_target_id`     BIGINT        DEFAULT NULL COMMENT '合并目标配件编号';

-- 2. 索引
ALTER TABLE `erp_product`
    ADD UNIQUE KEY `uk_erp_product_code` (`code`),
    ADD KEY `idx_erp_product_merged_flag` (`merged_flag`),
    ADD KEY `idx_erp_product_default_warehouse_id` (`default_warehouse_id`);

-- 3. 通用件子表
CREATE TABLE `erp_product_universal` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT   COMMENT '主键',
    `product_id`        BIGINT       NOT NULL                  COMMENT '主配件编号（关联 erp_product.id）',
    `universal_code`    VARCHAR(32)  NOT NULL                  COMMENT '通用件编码（对应 erp_product.code）',
    `universal_name`    VARCHAR(128)                           COMMENT '通用件名称（冗余，便于展示）',
    `universal_vehicle` VARCHAR(128)                           COMMENT '适用车型（单车型，一条一个）',
    `creator`           VARCHAR(64)  DEFAULT ''                COMMENT '创建者',
    `updater`           VARCHAR(64)  DEFAULT ''                COMMENT '更新者',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           BIT(1)       DEFAULT b'0'              COMMENT '是否删除',
    `tenant_id`         BIGINT       NOT NULL DEFAULT 0        COMMENT '租户编号',
    PRIMARY KEY (`id`),
    KEY `idx_erp_product_universal_product_id` (`product_id`),
    KEY `idx_erp_product_universal_code` (`universal_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 配件通用件（可替代关系）';
