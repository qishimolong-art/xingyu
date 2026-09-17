-- MySQL 8：00D目标库存当前读和追加流水历史存在性查询的定点索引。
-- 不增加唯一约束、不更新数据、不改变权限。同前导列的有效BTREE索引已存在时跳过。
-- 若下列专用名称已被不同列占用，ALTER会明确失败，禁止覆盖原索引。
SET @erp_dimension_schema = DATABASE();
SET @erp_dimension_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_dimension_schema AND table_name='erp_stock' AND seq_in_index<=3
    GROUP BY index_name
    HAVING COUNT(*)=3
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,product_id,warehouse_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_stock dimension lookup index already available''',
   'ALTER TABLE erp_stock ADD INDEX idx_stock_dimension_lookup_v223 (tenant_id,product_id,warehouse_id)');
PREPARE erp_dimension_statement FROM @erp_dimension_ddl;
EXECUTE erp_dimension_statement;
DEALLOCATE PREPARE erp_dimension_statement;

SET @erp_dimension_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_dimension_schema AND table_name='erp_stock_dual_cost_posting' AND seq_in_index<=3
    GROUP BY index_name
    HAVING COUNT(*)=3
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,product_id,warehouse_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_stock_dual_cost_posting dimension lookup index already available''',
   'ALTER TABLE erp_stock_dual_cost_posting ADD INDEX idx_posting_dimension_lookup_v223 (tenant_id,product_id,warehouse_id)');
PREPARE erp_dimension_statement FROM @erp_dimension_ddl;
EXECUTE erp_dimension_statement;
DEALLOCATE PREPARE erp_dimension_statement;
