package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockDocumentItemSortTest {

    @Test
    void supportsInventoryDetailFieldsAndRejectsSqlFragments() {
        List<String> common = List.of("warehouseId", "warehouseDeptName", "productId", "productName",
                "productCode", "productBarCode", "productUnitName", "stockCount", "batchNo", "count",
                "packageQty", "pieceCount", "weight", "totalWeight", "productPrice", "totalPrice", "remark");
        assertSupported(ErpStockInItemMapper::isSupportedOrderField, common);
        assertSupported(ErpStockOutItemMapper::isSupportedOrderField, common);
        assertSupported(ErpStockCheckItemMapper::isSupportedOrderField, List.of(
                "warehouseId", "warehouseDeptName", "productId", "productName", "productCode",
                "productBarCode", "productUnitName", "stockCount", "actualCount", "count", "batchNo",
                "packageQty", "pieceCount", "weight", "totalWeight", "productPrice", "totalPrice", "remark"));
        assertSupported(ErpStockMoveItemMapper::isSupportedOrderField, List.of(
                "fromWarehouseId", "fromWarehouseDeptName", "toWarehouseId", "toWarehouseDeptName",
                "productId", "productName", "productCode", "productBarCode", "productUnitName", "stockCount",
                "batchNo", "count", "packageQty", "pieceCount", "weight", "totalWeight", "productPrice",
                "totalPrice", "remark"));
        assertSupported(ErpWarehouseMoveItemMapper::isSupportedOrderField, List.of(
                "productCode", "productName", "vehicleModel", "originPlace", "standard", "featureCode",
                "brand", "drawingNo", "productUnitName", "batchNo", "count", "packageQty", "pieceCount",
                "fromWarehouseName", "fromShelf", "fromStockCount", "toWarehouseName", "toShelf",
                "toStockCount", "costPrice", "costAmount", "productPrice", "totalPrice", "weight",
                "totalWeight", "remark"));

        String injection = "id desc; delete from erp_stock_in_item";
        assertFalse(ErpStockInItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpStockOutItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpStockCheckItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpStockMoveItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpWarehouseMoveItemMapper.isSupportedOrderField(injection));
    }

    @Test
    void usesDisplayAndCalculatedExpressionsWithNullLastAndStableTieBreak() {
        String inSql = ErpStockInItemMapper.buildSortedQuery(1L, null, "productName", "desc")
                .getCustomSqlSegment();
        assertTrue(inSql.contains("erp_product"));
        assertTrue(inSql.contains("CASE WHEN"));
        assertTrue(inSql.contains("DESC"));
        assertTrue(inSql.contains("erp_stock_in_item.id ASC"));

        String checkSql = ErpStockCheckItemMapper.buildSortedQuery(1L, null, "warehouseDeptName", "asc")
                .getCustomSqlSegment();
        assertTrue(checkSql.contains("system_dept"));
        assertTrue(checkSql.contains("erp_stock_check_item.id ASC"));

        String transferSql = ErpStockMoveItemMapper.buildSortedQuery(1L, null, "pieceCount", "desc")
                .getCustomSqlSegment();
        assertTrue(transferSql.contains("package_qty > 0"));
        assertTrue(transferSql.contains("erp_stock_move_item.id ASC"));

        String moveSql = ErpWarehouseMoveItemMapper.buildSortedQuery(1L, null, "fromStockCount", "asc")
                .getCustomSqlSegment();
        assertTrue(moveSql.contains("erp_stock"));
        assertTrue(moveSql.contains("from_warehouse_id"));
        assertTrue(moveSql.contains("erp_warehouse_move_item.id ASC"));
    }

    @Test
    void validatesFieldDirectionPairAndDefaultsToOriginalOrder() {
        String defaultSql = ErpStockInItemMapper.buildSortedQuery(1L, null, null, null)
                .getCustomSqlSegment();
        assertTrue(defaultSql.contains("erp_stock_in_item.id ASC"));

        assertThrows(ServiceException.class,
                () -> ErpStockInItemMapper.buildSortedQuery(1L, null, "productName", null));
        assertThrows(ServiceException.class,
                () -> ErpStockInItemMapper.buildSortedQuery(1L, null, null, "asc"));
        assertThrows(ServiceException.class,
                () -> ErpStockInItemMapper.buildSortedQuery(1L, null, "productName", "sideways"));
        assertThrows(ServiceException.class,
                () -> ErpStockInItemMapper.buildSortedQuery(1L, null, "unknownField", "asc"));
    }

    private void assertSupported(Function<String, Boolean> checker, List<String> fields) {
        fields.forEach(field -> assertTrue(checker.apply(field), field));
    }
}
