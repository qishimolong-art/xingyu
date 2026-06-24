-- 用户-价格字段关联表 建表脚本
-- 日期：2026-06-17

CREATE TABLE IF NOT EXISTS system_user_price_field (
    id               BIGINT          AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id          BIGINT          NOT NULL                   COMMENT '用户ID',
    price_field_code VARCHAR(64)     NOT NULL                   COMMENT '价格字段编码',
    visible          TINYINT(1)      NOT NULL DEFAULT 1         COMMENT '是否可见：1=可见，0=不可见',
    creator          VARCHAR(64)     DEFAULT ''                 COMMENT '创建者',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater          VARCHAR(64)     DEFAULT ''                 COMMENT '更新者',
    update_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted          TINYINT(1)      NOT NULL DEFAULT 0         COMMENT '是否删除',
    tenant_id        BIGINT          NOT NULL DEFAULT 0         COMMENT '租户编号',
    UNIQUE KEY uk_user_price_field (user_id, price_field_code, tenant_id)
) COMMENT='用户-价格字段关联';

-- 预置价格字段说明（如需初始化，可参考以下 INSERT IGNORE 语句，按实际业务字段编码补充）
-- INSERT IGNORE INTO system_user_price_field (user_id, price_field_code, visible, tenant_id) VALUES
--   (1, 'purchase_price',   1, 1),  -- 采购价
--   (1, 'sale_price',       1, 1),  -- 销售价
--   (1, 'cost_price',       1, 1),  -- 成本价
--   (1, 'last_purchase',    1, 1),  -- 最近采购价
--   (1, 'price_system_1',   1, 1);  -- 价格体系1
