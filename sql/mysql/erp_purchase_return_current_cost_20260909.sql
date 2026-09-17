-- 00F：采购退货实际库存成本及来源数量核对追加证据，不修改历史退货或启用开关。
CREATE TABLE IF NOT EXISTS erp_purchase_return_posting_link (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 return_id BIGINT NOT NULL,
 return_item_id BIGINT NOT NULL,
 posting_id BIGINT NOT NULL,
 source_in_id BIGINT NULL,
 source_in_item_id BIGINT NULL,
 trace_sale_return_id BIGINT NULL,
 trace_sale_return_item_id BIGINT NULL,
 quantity DECIMAL(24,6) NOT NULL,
 financial_cost_amount DECIMAL(24,6) NOT NULL,
 settlement_cost_amount DECIMAL(24,6) NOT NULL,
 source_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 quantity_evidence LONGTEXT NOT NULL,
 posted_at DATETIME(6) NOT NULL,
 rule_version VARCHAR(64) NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_purchase_return_posting(tenant_id,posting_id),
 UNIQUE KEY uk_purchase_return_item(tenant_id,return_id,return_item_id),
 KEY idx_purchase_return_source_in(tenant_id,source_in_item_id,id),
 KEY idx_purchase_return_trace_sale(tenant_id,trace_sale_return_item_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购退货均价扣库及真实来源数量核对证据';
