-- 00D：仅为切换后同事务首次创建、无任何库存历史的维度建立真实零起点。
-- 不回填旧库存，不改变旧DO，不启用全局开关。
CREATE TABLE IF NOT EXISTS erp_stock_dual_cost_dimension_mutex (
 tenant_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 warehouse_id BIGINT NOT NULL,
 PRIMARY KEY(tenant_id,product_id,warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='新库存维度事务行锁，非核算凭据';
CREATE TABLE IF NOT EXISTS erp_stock_dual_cost_origin (
 tenant_id BIGINT NOT NULL,
 stock_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 warehouse_id BIGINT NOT NULL,
 origin_kind VARCHAR(30) NOT NULL,
 available_from DATETIME(6) NOT NULL,
 created_by BIGINT NOT NULL,
 action_key VARCHAR(80) CHARACTER SET ascii NOT NULL,
 history_check_version VARCHAR(40) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 PRIMARY KEY(tenant_id,stock_id),
 UNIQUE KEY uk_dimension(tenant_id,product_id,warehouse_id),
 UNIQUE KEY uk_action(tenant_id,action_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='不可变新库存出生证明；无记录的已核余额为人工期初';
