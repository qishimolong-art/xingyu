-- MySQL 8，00E 来源明细当前读的父键定位索引。
-- 本期审批/编辑使用 FOR UPDATE，必须限制到同租户同父单明细，避免扫锁无关单据。
-- 不新增唯一约束、不修改业务数据、不删除或替换已有索引。
-- 复用可见 BTREE 完整前导列索引（允许更长索引）；同名异列冲突由 ALTER 明确拒绝。
SET @erp_source_lock_schema = DATABASE();

SET @erp_source_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_source_lock_schema AND table_name='erp_sale_return_items' AND seq_in_index<=2
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,return_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_sale_return_items source parent index already available''',
   'ALTER TABLE erp_sale_return_items ADD INDEX idx_sale_return_parent_lock_20260909 (tenant_id,return_id)');
PREPARE erp_source_lock_statement FROM @erp_source_lock_ddl;
EXECUTE erp_source_lock_statement;
DEALLOCATE PREPARE erp_source_lock_statement;

SET @erp_source_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_source_lock_schema AND table_name='erp_sale_out_items' AND seq_in_index<=2
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,out_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_sale_out_items source parent index already available''',
   'ALTER TABLE erp_sale_out_items ADD INDEX idx_sale_out_parent_lock_20260909 (tenant_id,out_id)');
PREPARE erp_source_lock_statement FROM @erp_source_lock_ddl;
EXECUTE erp_source_lock_statement;
DEALLOCATE PREPARE erp_source_lock_statement;

SET @erp_source_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_source_lock_schema AND table_name='erp_purchase_in_items' AND seq_in_index<=2
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,in_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_purchase_in_items source parent index already available''',
   'ALTER TABLE erp_purchase_in_items ADD INDEX idx_purchase_in_parent_lock_20260909 (tenant_id,in_id)');
PREPARE erp_source_lock_statement FROM @erp_source_lock_ddl;
EXECUTE erp_source_lock_statement;
DEALLOCATE PREPARE erp_source_lock_statement;
