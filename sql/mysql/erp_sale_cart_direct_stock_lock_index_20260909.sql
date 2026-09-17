-- 00H 销售直接审核：父单明细及调拨来源定位索引（MySQL 8）。
-- 仅增加非唯一索引；不改业务/菜单/角色数据，不覆盖已有索引。
-- 允许复用可见 BTREE 完整前导列的更长索引；同名不兼容时 ALTER 明确失败。
-- DDL 非原子；须在部署维护窗口执行，保留执行前 SHOW INDEX 及失败结果。
SET @erp_cart_lock_schema = DATABASE();

SET @erp_cart_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_cart_lock_schema AND table_name='erp_sale_cart_items' AND seq_in_index<=2
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,cart_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_sale_cart_items compatible index already available''',
   'ALTER TABLE erp_sale_cart_items ADD INDEX idx_cart_item_parent_lock_20260909 (tenant_id,cart_id)');
PREPARE erp_cart_lock_statement FROM @erp_cart_lock_ddl;
EXECUTE erp_cart_lock_statement;
DEALLOCATE PREPARE erp_cart_lock_statement;

SET @erp_cart_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_cart_lock_schema AND table_name='erp_stock_move_item' AND seq_in_index<=2
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,move_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_stock_move_item compatible index already available''',
   'ALTER TABLE erp_stock_move_item ADD INDEX idx_move_item_parent_lock_20260909 (tenant_id,move_id)');
PREPARE erp_cart_lock_statement FROM @erp_cart_lock_ddl;
EXECUTE erp_cart_lock_statement;
DEALLOCATE PREPARE erp_cart_lock_statement;

SET @erp_cart_lock_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.statistics
    WHERE table_schema=@erp_cart_lock_schema AND table_name='erp_stock_move' AND seq_in_index<=3
    GROUP BY index_name
    HAVING COUNT(*)=3
       AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,source_type,source_id'
       AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
), 'SELECT ''erp_stock_move compatible index already available''',
   'ALTER TABLE erp_stock_move ADD INDEX idx_move_source_lock_20260909 (tenant_id,source_type,source_id)');
PREPARE erp_cart_lock_statement FROM @erp_cart_lock_ddl;
EXECUTE erp_cart_lock_statement;
DEALLOCATE PREPARE erp_cart_lock_statement;
