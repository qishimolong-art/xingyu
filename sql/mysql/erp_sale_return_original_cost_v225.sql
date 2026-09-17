-- 00E：原销售事件的退货成本累计和追加分摊，不回填旧退货、不启用新核算。
CREATE TABLE IF NOT EXISTS erp_sale_return_cost_progress (
 tenant_id BIGINT NOT NULL,
 source_posting_id BIGINT NOT NULL,
 original_quantity DECIMAL(24,6) NOT NULL,
 original_financial_amount DECIMAL(24,6) NOT NULL,
 original_settlement_amount DECIMAL(24,6) NOT NULL,
 returned_quantity DECIMAL(24,6) NOT NULL,
 returned_financial_amount DECIMAL(24,6) NOT NULL,
 returned_settlement_amount DECIMAL(24,6) NOT NULL,
 rule_version VARCHAR(64) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 PRIMARY KEY(tenant_id,source_posting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='原销售过账退货成本进度，须与分摊事件核对';
CREATE TABLE IF NOT EXISTS erp_sale_return_cost_allocation (
 id BIGINT NOT NULL AUTO_INCREMENT,
 tenant_id BIGINT NOT NULL,
 source_posting_id BIGINT NOT NULL,
 return_id BIGINT NOT NULL,
 return_item_id BIGINT NOT NULL,
 return_posting_id BIGINT NOT NULL,
 quantity DECIMAL(24,6) NOT NULL,
 financial_amount DECIMAL(24,6) NOT NULL,
 settlement_amount DECIMAL(24,6) NOT NULL,
 source_signature CHAR(64) CHARACTER SET ascii NOT NULL,
 posted_at DATETIME(6) NOT NULL,
 rule_version VARCHAR(64) NOT NULL,
 PRIMARY KEY(id),
 UNIQUE KEY uk_return_posting(tenant_id,return_posting_id),
 UNIQUE KEY uk_return_item_source(tenant_id,return_id,return_item_id,source_posting_id),
 KEY idx_source(tenant_id,source_posting_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='追加原销售成本反向分摊，禁止覆盖已消费成本';

-- 原事件当前读须只锁匹配来源，避免按业务ID查询扫描正常库存流水。
SET @erp_return_schema = DATABASE();
SET @erp_return_index_ddl = IF(EXISTS (
 SELECT 1 FROM information_schema.statistics
 WHERE table_schema=@erp_return_schema AND table_name='erp_stock_dual_cost_posting' AND seq_in_index<=4
 GROUP BY index_name
 HAVING COUNT(*)=4 AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,biz_type,biz_id,biz_item_id'
 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''posting business source index already available''',
 'ALTER TABLE erp_stock_dual_cost_posting ADD INDEX idx_posting_business_source_v225 (tenant_id,biz_type,biz_id,biz_item_id,id)');
PREPARE erp_return_index_stmt FROM @erp_return_index_ddl;
EXECUTE erp_return_index_stmt;
DEALLOCATE PREPARE erp_return_index_stmt;
