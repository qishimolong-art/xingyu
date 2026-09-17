-- 00G 无单销售退货；独立于原销售分摊与采购成本确认，不回填历史，不启用核算。
-- 仅确认/真实审核在已锁源单内建立定位行；查询不创建。避免空证据范围锁与库存锁反向。
CREATE TABLE IF NOT EXISTS erp_sale_return_current_cost_state (
 tenant_id BIGINT NOT NULL,
 return_id BIGINT NOT NULL,
 latest_confirmation_id BIGINT NULL,
 posted BIT NOT NULL DEFAULT b'0',
 PRIMARY KEY(tenant_id,return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无单销售退货同事务证据定位状态';

CREATE TABLE IF NOT EXISTS erp_sale_return_current_cost_confirmation (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 return_id BIGINT NOT NULL,
 revision INT NOT NULL,
 request_key VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
 request_hash CHAR(64) CHARACTER SET ascii NOT NULL,
 source_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 stock_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 evidence VARCHAR(500) NOT NULL,
 confirmed_by BIGINT NOT NULL,
 confirmed_at DATETIME(6) NOT NULL,
 consumed_at DATETIME(6) NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_sale_current_request(tenant_id,request_key),
 UNIQUE KEY uk_sale_current_revision(tenant_id,return_id,revision)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无单销售退货人工补齐或修改成本确认版本';

CREATE TABLE IF NOT EXISTS erp_sale_return_current_cost_line (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 confirmation_id BIGINT NOT NULL,
 return_item_id BIGINT NOT NULL,
 financial_amount DECIMAL(24,6) NOT NULL,
 settlement_amount DECIMAL(24,6) NOT NULL,
 evidence VARCHAR(500) NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_sale_current_line(tenant_id,confirmation_id,return_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无单销售退货确认逐行两套成本总额';

CREATE TABLE IF NOT EXISTS erp_sale_return_current_cost_posting_link (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 return_id BIGINT NOT NULL,
 return_item_id BIGINT NOT NULL,
 posting_id BIGINT NOT NULL,
 confirmation_id BIGINT NULL,
 confirmation_revision INT NULL,
 basis_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 source_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 stock_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 cost_source VARCHAR(32) NOT NULL,
 quantity DECIMAL(24,6) NOT NULL,
 financial_amount DECIMAL(24,6) NOT NULL,
 settlement_amount DECIMAL(24,6) NOT NULL,
 stock_basis LONGTEXT NOT NULL,
 posted_at DATETIME(6) NOT NULL,
 approved_by BIGINT NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_sale_current_posting(tenant_id,posting_id),
 UNIQUE KEY uk_sale_current_return_item(tenant_id,return_id,return_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='无单销售退货真实过账及审核时成本基准证据';

-- 不自动回填旧三表定位。若曾部署过未带定位行的试验版本，须先人工核对迁移方案。
-- 使用会话临时检查表明确失败；不删除、覆盖或补零任何业务证据。
CREATE TEMPORARY TABLE IF NOT EXISTS erp_sale_current_locator_migration_guard (
 missing_evidence BIGINT NOT NULL,
 CONSTRAINT ck_sale_current_locator_requires_reconciliation CHECK (missing_evidence=0)
);
INSERT INTO erp_sale_current_locator_migration_guard(missing_evidence)
SELECT
 (SELECT COUNT(*) FROM erp_sale_return_current_cost_confirmation c
  LEFT JOIN erp_sale_return_current_cost_state s ON s.tenant_id=c.tenant_id AND s.return_id=c.return_id
  LEFT JOIN erp_sale_return_current_cost_confirmation latest ON latest.tenant_id=s.tenant_id
    AND latest.return_id=s.return_id AND latest.id=s.latest_confirmation_id
  WHERE s.return_id IS NULL OR latest.id IS NULL OR latest.revision<c.revision)
 + (SELECT COUNT(*) FROM erp_sale_return_current_cost_line l
    LEFT JOIN erp_sale_return_current_cost_confirmation c ON c.tenant_id=l.tenant_id AND c.id=l.confirmation_id
    WHERE c.id IS NULL)
 + (SELECT COUNT(*) FROM erp_sale_return_current_cost_posting_link l
    LEFT JOIN erp_sale_return_current_cost_state s ON s.tenant_id=l.tenant_id AND s.return_id=l.return_id
    WHERE s.return_id IS NULL OR s.posted=b'0')
 + (SELECT COUNT(*) FROM erp_sale_return_current_cost_state s
    LEFT JOIN erp_sale_return_current_cost_confirmation c ON c.tenant_id=s.tenant_id
      AND c.return_id=s.return_id AND c.id=s.latest_confirmation_id
    WHERE s.latest_confirmation_id IS NOT NULL AND c.id IS NULL)
 + (SELECT COUNT(*) FROM erp_sale_return_current_cost_state s
    WHERE s.posted=b'1' AND NOT EXISTS
      (SELECT 1 FROM erp_sale_return_current_cost_posting_link l WHERE l.tenant_id=s.tenant_id AND l.return_id=s.return_id));
DROP TEMPORARY TABLE erp_sale_current_locator_migration_guard;
