package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSaleDocumentItemSortTest {

    @Test
    void supportsAllDetailTableFieldsAndRejectsSqlFragments() {
        assertSupported(ErpSaleOrderItemMapper::isSupportedOrderField, List.of(
                "productId", "warehouseId", "deptId", "batchNo", "stockCount", "productBarCode",
                "productUnitName", "weight", "packageQty", "pieceCount", "giftFlag", "remark", "count",
                "productPrice", "salePrice", "lastSalePrice", "totalProductPrice", "saleCostPrice",
                "saleCostAmount", "taxPercent", "taxPrice", "totalPrice"));
        assertSupported(ErpSaleOutItemMapper::isSupportedOrderField, List.of(
                "productCode", "productName", "vehicleModel", "standard", "featureCode", "brand", "giftFlag",
                "productUnitName", "count", "productPrice", "totalProductPrice", "saleCostPrice",
                "saleCostAmount", "actualSaleAmount", "drawingNo", "batchNo", "sourceWarehouseName",
                "warehouseName", "deptName", "stockCount", "warehousePosition", "unitWeight", "packageQty",
                "pieceCount", "totalWeight", "afterReductionPrice", "afterReductionAmount", "originPlace",
                "remark", "originalProductPrice"));
        assertSupported(ErpSaleReturnItemMapper::isSupportedOrderField, List.of(
                "warehouseId", "deptId", "batchNo", "productId", "productCode", "count", "productPrice",
                "totalPrice", "stockCount", "productBarCode", "productUnitName", "weight", "packageQty",
                "pieceCount", "returnReason", "warehousePosition", "remark", "outCount", "returnCount",
                "totalProductPrice", "taxPercent", "taxPrice"));
        assertSupported(ErpSaleQuoteItemMapper::isSupportedOrderField, List.of(
                "productId", "productCode", "warehouseId", "deptId", "count", "productPrice",
                "lastSalePrice", "totalPrice", "giftFlag", "batchNo", "stockCount", "productBarCode",
                "productUnitName", "weight", "packageQty", "pieceCount", "brand", "vehicleModel",
                "originPlace", "standard", "convertedCount", "remark"));
        assertSupported(ErpSalePriceAdjustItemMapper::isSupportedOrderField, List.of(
                "saleOutNo", "partCode", "partName", "oldPrice", "newPrice", "outCount", "stockCount",
                "warehouseName", "batchNo", "vehicleModel", "originPlace", "brand", "unit", "weight",
                "packageQty", "pieceCount", "adjustAmount", "adjustPrice", "adjustReason", "itemRemark"));

        String injection = "id desc; delete from erp_sale_out_items";
        assertFalse(ErpSaleOrderItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpSaleOutItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpSaleReturnItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpSaleQuoteItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpSalePriceAdjustItemMapper.isSupportedOrderField(injection));
    }

    @Test
    void usesDisplayAndCalculatedExpressionsWithNullLastAndIdTieBreak() {
        String orderSql = ErpSaleOrderItemMapper.buildSortedQuery(1L, null, "productId", "desc")
                .getCustomSqlSegment();
        assertTrue(orderSql.contains("erp_product"));
        assertTrue(orderSql.contains("CASE WHEN"));
        assertTrue(orderSql.contains("DESC"));
        assertTrue(orderSql.contains("erp_sale_order_items.id ASC"));

        String outSql = ErpSaleOutItemMapper.buildSortedQuery(1L, "saleCostAmount", "asc")
                .getCustomSqlSegment();
        assertTrue(outSql.contains("erp_stock_record"));
        assertTrue(outSql.contains("erp_sale_out_items.id ASC"));

        String returnSql = ErpSaleReturnItemMapper.buildSortedQuery(1L, null, "pieceCount", "desc")
                .getCustomSqlSegment();
        assertTrue(returnSql.contains("package_qty > 0"));
        assertTrue(returnSql.contains("erp_sale_return_items.id ASC"));

        String quoteSql = ErpSaleQuoteItemMapper.buildSortedQuery(1L, null, "lastSalePrice", "asc")
                .getCustomSqlSegment();
        assertTrue(quoteSql.contains("so.status = 20"));
        assertTrue(quoteSql.contains("erp_sale_quote_items.id ASC"));

        String adjustSql = ErpSalePriceAdjustItemMapper.buildSortedQuery(1L, null, "warehouseName", "desc")
                .getCustomSqlSegment();
        assertTrue(adjustSql.contains("erp_warehouse"));
        assertTrue(adjustSql.contains("erp_sale_price_adjust_item.id ASC"));
    }

    @Test
    void validatesFieldDirectionPairAndDefaultsToOriginalOrder() {
        String defaultSql = ErpSaleOutItemMapper.buildSortedQuery(1L, null, null).getCustomSqlSegment();
        assertTrue(defaultSql.contains("erp_sale_out_items.id ASC"));

        assertThrows(ServiceException.class,
                () -> ErpSaleOutItemMapper.buildSortedQuery(1L, "productName", null));
        assertThrows(ServiceException.class,
                () -> ErpSaleOutItemMapper.buildSortedQuery(1L, null, "asc"));
        assertThrows(ServiceException.class,
                () -> ErpSaleOutItemMapper.buildSortedQuery(1L, "productName", "sideways"));
        assertThrows(ServiceException.class,
                () -> ErpSaleOutItemMapper.buildSortedQuery(1L, "unknownField", "asc"));
    }

    private void assertSupported(Function<String, Boolean> checker, List<String> fields) {
        fields.forEach(field -> assertTrue(checker.apply(field), field));
    }

}
