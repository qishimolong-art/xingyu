-- ERP stock available-count sorting performance indexes v214.
-- Safe to execute repeatedly: only creates missing indexes and never changes business data.

DROP PROCEDURE IF EXISTS add_erp_stock_available_sort_index_v214;

DELIMITER //
CREATE PROCEDURE add_erp_stock_available_sort_index_v214(
    IN tableName VARCHAR(64),
    IN indexName VARCHAR(64),
    IN indexSql TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tableName
          AND INDEX_NAME = indexName
    ) THEN
        SET @addIndexSql = CONCAT('CREATE INDEX `', indexName, '` ON `', tableName, '` ', indexSql);
        PREPARE stmt FROM @addIndexSql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- Main stock filtering used before sorting by availableCount.
CALL add_erp_stock_available_sort_index_v214(
    'erp_stock',
    'idx_erp_stock_tenant_deleted_wh_product',
    '(`tenant_id`, `deleted`, `warehouse_id`, `product_id`, `id`)'
);

-- Occupied-count sources used by ErpStockMapper.availableCountExpression().
CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_cart_items',
    'idx_erp_sale_cart_items_stock_cart',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `cart_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_cart',
    'idx_erp_sale_cart_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_out',
    'idx_erp_sale_out_tenant_source_deleted',
    '(`tenant_id`, `source_id`, `deleted`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_out_items',
    'idx_erp_sale_out_items_stock_out',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `out_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_return_items',
    'idx_erp_purchase_return_items_stock_return',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `return_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_out_item',
    'idx_erp_stock_out_item_stock_out',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `out_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_move_item',
    'idx_erp_stock_move_item_from_stock_move',
    '(`tenant_id`, `deleted`, `product_id`, `from_warehouse_id`, `move_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_warehouse_move_item',
    'idx_erp_wh_move_item_from_stock_move',
    '(`tenant_id`, `deleted`, `product_id`, `from_warehouse_id`, `move_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_check_item',
    'idx_erp_stock_check_item_stock_count',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `count`, `check_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_out_bill_item',
    'idx_erp_stock_out_bill_item_stock_bill',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `bill_id`)'
);

-- The same stock page also calculates pending-in and in-transit counts for the returned rows.
CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_in_items',
    'idx_erp_purchase_in_items_stock_in',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `in_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_return_items',
    'idx_erp_sale_return_items_stock_return',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `return_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_in_item',
    'idx_erp_stock_in_item_stock_in',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `in_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_move_item',
    'idx_erp_stock_move_item_to_stock_move',
    '(`tenant_id`, `deleted`, `product_id`, `to_warehouse_id`, `move_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_warehouse_move_item',
    'idx_erp_wh_move_item_to_stock_move',
    '(`tenant_id`, `deleted`, `product_id`, `to_warehouse_id`, `move_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_in_bill_item',
    'idx_erp_stock_in_bill_item_stock_bill',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `bill_id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_order_items',
    'idx_erp_purchase_order_items_stock_order',
    '(`tenant_id`, `deleted`, `product_id`, `warehouse_id`, `order_id`)'
);

-- Optional main-table status indexes let MySQL choose a main-first plan on smaller pending documents.
CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_return',
    'idx_erp_purchase_return_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_out',
    'idx_erp_stock_out_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_move',
    'idx_erp_stock_move_tenant_dir_status_deleted_id',
    '(`tenant_id`, `transfer_direction`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_warehouse_move',
    'idx_erp_wh_move_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_check',
    'idx_erp_stock_check_tenant_type_status_deleted_id',
    '(`tenant_id`, `check_type`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_out_bill',
    'idx_erp_stock_out_bill_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_in',
    'idx_erp_purchase_in_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_sale_return',
    'idx_erp_sale_return_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_in',
    'idx_erp_stock_in_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_stock_in_bill',
    'idx_erp_stock_in_bill_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

CALL add_erp_stock_available_sort_index_v214(
    'erp_purchase_order',
    'idx_erp_purchase_order_tenant_status_deleted_id',
    '(`tenant_id`, `status`, `deleted`, `id`)'
);

DROP PROCEDURE IF EXISTS add_erp_stock_available_sort_index_v214;
