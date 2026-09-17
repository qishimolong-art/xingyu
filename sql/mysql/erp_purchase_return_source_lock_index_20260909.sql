-- MySQL8：00F采购退货主从/历史来源数量当前读索引；不修改数据，不覆盖原索引。
SET @erp_return_lock_schema=DATABASE();

SET @erp_return_lock_ddl=IF(EXISTS (
 SELECT 1 FROM information_schema.statistics
 WHERE table_schema=@erp_return_lock_schema AND table_name='erp_purchase_return_items' AND seq_in_index<=2
 GROUP BY index_name HAVING COUNT(*)=2
 AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,return_id'
 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT 1', 'ALTER TABLE erp_purchase_return_items ADD INDEX idx_purchase_return_parent_20260909 (tenant_id,return_id)');
PREPARE erp_return_lock_stmt FROM @erp_return_lock_ddl;
EXECUTE erp_return_lock_stmt;
DEALLOCATE PREPARE erp_return_lock_stmt;

SET @erp_return_lock_ddl=IF(EXISTS (
 SELECT 1 FROM information_schema.statistics
 WHERE table_schema=@erp_return_lock_schema AND table_name='erp_purchase_return_items' AND seq_in_index<=2
 GROUP BY index_name HAVING COUNT(*)=2
 AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,source_in_item_id'
 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT 1', 'ALTER TABLE erp_purchase_return_items ADD INDEX idx_purchase_return_in_source_20260909 (tenant_id,source_in_item_id)');
PREPARE erp_return_lock_stmt FROM @erp_return_lock_ddl;
EXECUTE erp_return_lock_stmt;
DEALLOCATE PREPARE erp_return_lock_stmt;

SET @erp_return_lock_ddl=IF(EXISTS (
 SELECT 1 FROM information_schema.statistics
 WHERE table_schema=@erp_return_lock_schema AND table_name='erp_purchase_return_items' AND seq_in_index<=2
 GROUP BY index_name HAVING COUNT(*)=2
 AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,source_sale_return_item_id'
 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT 1', 'ALTER TABLE erp_purchase_return_items ADD INDEX idx_purchase_return_sale_trace_20260909 (tenant_id,source_sale_return_item_id)');
PREPARE erp_return_lock_stmt FROM @erp_return_lock_ddl;
EXECUTE erp_return_lock_stmt;
DEALLOCATE PREPARE erp_return_lock_stmt;
