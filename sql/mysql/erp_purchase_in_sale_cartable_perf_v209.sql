-- 采购入库转销售手推车大明细分页/统计性能索引（v209）
-- 安全约束：只新增缺失索引，不修改业务数据；可重复执行。

DROP PROCEDURE IF EXISTS add_erp_purchase_in_sale_cartable_index_v209;

DELIMITER $$
CREATE PROCEDURE add_erp_purchase_in_sale_cartable_index_v209(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64),
    IN ddl_param VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.statistics
         WHERE table_schema = DATABASE()
           AND table_name = table_name_param
           AND index_name = index_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name_param, '` ADD INDEX `', index_name_param, '` ', ddl_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL add_erp_purchase_in_sale_cartable_index_v209(
    'erp_purchase_in_items',
    'idx_erp_purchase_in_items_tenant_in_deleted_id',
    '(`tenant_id`, `in_id`, `deleted`, `id`)'
);

CALL add_erp_purchase_in_sale_cartable_index_v209(
    'erp_sale_convert_record',
    'idx_erp_sale_convert_record_purchase_in_item',
    '(`tenant_id`, `convert_type`, `source_type`, `target_type`, `source_item_id`, `deleted`)'
);

DROP PROCEDURE IF EXISTS add_erp_purchase_in_sale_cartable_index_v209;
