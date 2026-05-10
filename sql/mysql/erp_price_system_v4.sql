-- ERP 价格体系 四期 建表脚本
-- 日期：2026-05-08

CREATE TABLE IF NOT EXISTS erp_price_system (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '价格体系编号',
    code        VARCHAR(64)  NOT NULL COMMENT '编码',
    name        VARCHAR(128) NOT NULL COMMENT '名称',
    status      TINYINT      DEFAULT 0     COMMENT '状态（0=启用 1=停用）',
    sort        INT          DEFAULT 0     COMMENT '排序',
    remark      VARCHAR(512) DEFAULT NULL  COMMENT '备注',
    creator     VARCHAR(64)  DEFAULT ''    COMMENT '创建者',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater     VARCHAR(64)  DEFAULT ''    COMMENT '更新者',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     BIT(1)       DEFAULT b'0'  COMMENT '是否删除',
    tenant_id   BIGINT       DEFAULT 0     COMMENT '租户编号',
    UNIQUE KEY uk_code (code, tenant_id, deleted)
) COMMENT='ERP 价格体系字典';

CREATE TABLE IF NOT EXISTS erp_product_price_system (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '编号',
    product_id       BIGINT         NOT NULL COMMENT '产品编号',
    price_system_id  BIGINT         NOT NULL COMMENT '价格体系编号',
    price            DECIMAL(20,4)  DEFAULT NULL COMMENT '单价',
    creator          VARCHAR(64)    DEFAULT '' COMMENT '创建者',
    create_time      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater          VARCHAR(64)    DEFAULT '' COMMENT '更新者',
    update_time      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          BIT(1)         DEFAULT b'0' COMMENT '是否删除',
    tenant_id        BIGINT         DEFAULT 0 COMMENT '租户编号',
    KEY idx_product (product_id),
    KEY idx_price_system (price_system_id)
) COMMENT='ERP 产品-价格体系关联';
