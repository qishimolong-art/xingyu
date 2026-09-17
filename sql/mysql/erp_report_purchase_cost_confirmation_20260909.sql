-- 2026-09-09：采购成本逐行明确金额确认。仅增表，不启用新核算、不补记历史待领货。
CREATE TABLE IF NOT EXISTS erp_purchase_cost_confirmation (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 purchase_in_id BIGINT NOT NULL,
 revision INT NOT NULL,
 request_key VARCHAR(80) CHARACTER SET ascii NOT NULL,
 request_hash CHAR(64) CHARACTER SET ascii NOT NULL,
 source_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 source_snapshot LONGTEXT NOT NULL,
 rule_version VARCHAR(50) NOT NULL,
 price_basis VARCHAR(40) NOT NULL,
 tax_status VARCHAR(20) NOT NULL,
 evidence VARCHAR(1000) NOT NULL,
 fee_treatment VARCHAR(1000) NOT NULL,
 confirmed_by BIGINT NOT NULL,
 confirmed_at DATETIME(6) NOT NULL,
 consumed_at DATETIME(6) NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_revision(tenant_id,purchase_in_id,revision),
 UNIQUE KEY uk_request(tenant_id,request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购核算成本确认不可变版本';
CREATE TABLE IF NOT EXISTS erp_purchase_cost_confirmation_line (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 confirmation_id BIGINT NOT NULL,
 source_item_id BIGINT NOT NULL,
 product_id BIGINT NOT NULL,
 warehouse_id BIGINT NOT NULL,
 quantity DECIMAL(24,6) NOT NULL,
 confirmed_net_total_amount DECIMAL(24,6) NOT NULL,
 raw_unit_price DECIMAL(24,6) NULL,
 raw_line_amount DECIMAL(24,6) NULL,
 evidence VARCHAR(1000) NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_source(tenant_id,confirmation_id,source_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='明确确认未税总成本，普通外采财务结算同额';
CREATE TABLE IF NOT EXISTS erp_purchase_cost_posting_link (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 confirmation_id BIGINT NOT NULL,
 revision INT NOT NULL,
 source_item_id BIGINT NOT NULL,
 action_key VARCHAR(160) CHARACTER SET ascii NOT NULL,
 financial_amount DECIMAL(24,6) NOT NULL,
 settlement_amount DECIMAL(24,6) NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_posting(tenant_id,action_key),
 UNIQUE KEY uk_consumption(tenant_id,confirmation_id,source_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实际库存过账与确认版本金额的不可变关系';
