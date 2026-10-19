package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpPurchaseDocumentItemSortTest {

    @Test
    void supportsPurchaseDetailFieldsAndRejectsSqlFragments() {
        assertSupported(ErpPurchaseOrderItemMapper::isSupportedOrderField, List.of(
                "inStatus", "gift", "productCode", "warehouseId", "deptId", "productId", "stockCount",
                "count", "productPrice", "lastPurchasePrice", "totalProductPrice", "vehicleModel", "standard",
                "featureCode", "productUnitName", "weight", "packageQty", "pieceCount", "arrivalCount",
                "warehousePosition", "drawingNo", "batchNo", "factoryCode", "brand", "remark"));
        assertSupported(ErpPurchaseInItemMapper::isSupportedOrderField, List.of(
                "warehouseId", "deptId", "gift", "productCode", "productId", "count", "productPrice",
                "lastPurchasePrice", "totalProductPrice", "productCostPrice", "productCostAmount", "adjustStatus",
                "invoiceStatus", "returnStatus", "returnCount", "stockCount", "productBarCode", "productUnitName",
                "packageQty", "pieceCount", "wholeQty", "warehousePosition", "barCode", "batchNo", "drawingNo",
                "brand", "vehicleModel", "originPlace", "remark", "taxPercent", "taxPrice", "totalPrice"));
        assertSupported(ErpPurchaseReturnItemMapper::isSupportedOrderField, List.of(
                "warehouseId", "deptId", "productCode", "productName", "count", "productPrice",
                "lastPurchasePrice", "totalProductPrice", "batchNo", "sourceInNo", "inCount", "returnableCount",
                "partCode", "partName", "vehicleModel", "originPlace", "stockCount", "productBarCode",
                "productUnitName", "weight", "packageQty", "pieceCount", "remark", "taxPercent", "taxPrice",
                "totalPrice"));
        assertSupported(ErpPurchaseInvoiceItemMapper::isSupportedOrderField, List.of(
                "sourceTypeName", "sourceDocNo", "productCode", "productId", "stockCount", "count",
                "sourceRemainingCount", "packageQty", "pieceCount", "productPrice", "taxExclusivePrice",
                "taxPercent", "taxPrice", "totalPrice", "remark"));
        assertSupported(ErpPurchasePriceAdjustItemMapper::isSupportedOrderField, List.of(
                "inNo", "productCode", "productName", "batchNo", "oldPrice", "adjustRatio", "newPrice",
                "adjustPrice", "vehicleModel", "standard", "featureCode", "originPlace", "brand", "drawingNo",
                "warehouseId", "deptId", "warehousePosition", "productUnitName", "weight", "packageQty",
                "pieceCount", "stockCount", "count"));

        String injection = "id desc; delete from erp_purchase_in_items";
        assertFalse(ErpPurchaseOrderItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpPurchaseInItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpPurchaseReturnItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpPurchaseInvoiceItemMapper.isSupportedOrderField(injection));
        assertFalse(ErpPurchasePriceAdjustItemMapper.isSupportedOrderField(injection));
    }

    @Test
    void usesDisplayAndCalculatedExpressionsWithNullLastAndStableTieBreak() {
        String orderSql = ErpPurchaseOrderItemMapper.buildSortedQuery(1L, null, "productName", "desc")
                .getCustomSqlSegment();
        assertTrue(orderSql.contains("erp_product"));
        assertTrue(orderSql.contains("CASE WHEN"));
        assertTrue(orderSql.contains("DESC"));
        assertTrue(orderSql.contains("erp_purchase_order_items.id ASC"));

        String inSql = ErpPurchaseInItemMapper.buildSortedQuery(1L, null, "returnStatus", "asc")
                .getCustomSqlSegment();
        assertTrue(inSql.contains("erp_purchase_return_items"));
        assertTrue(inSql.contains("erp_purchase_in_items.id ASC"));

        String returnSql = ErpPurchaseReturnItemMapper.buildSortedQuery(1L, null, "pieceCount", "desc")
                .getCustomSqlSegment();
        assertTrue(returnSql.contains("package_qty > 0"));
        assertTrue(returnSql.contains("erp_purchase_return_items.id ASC"));

        String invoiceSql = ErpPurchaseInvoiceItemMapper.buildSortedQuery(1L, null, "sourceDocNo", "asc")
                .getCustomSqlSegment();
        assertTrue(invoiceSql.contains("COALESCE"));
        assertTrue(invoiceSql.contains("erp_purchase_invoice_item.id ASC"));

        String adjustSql = ErpPurchasePriceAdjustItemMapper.buildSortedQuery(1L, null, "inNo", "desc")
                .getCustomSqlSegment();
        assertTrue(adjustSql.contains("in_no"));
        assertTrue(adjustSql.contains("erp_purchase_price_adjust_item.id ASC"));
    }

    @Test
    void validatesFieldDirectionPairAndDefaultsToOriginalOrder() {
        String defaultSql = ErpPurchaseInItemMapper.buildSortedQuery(1L, null, null, null)
                .getCustomSqlSegment();
        assertTrue(defaultSql.contains("erp_purchase_in_items.id ASC"));

        assertThrows(ServiceException.class,
                () -> ErpPurchaseInItemMapper.buildSortedQuery(1L, null, "productName", null));
        assertThrows(ServiceException.class,
                () -> ErpPurchaseInItemMapper.buildSortedQuery(1L, null, null, "asc"));
        assertThrows(ServiceException.class,
                () -> ErpPurchaseInItemMapper.buildSortedQuery(1L, null, "productName", "sideways"));
        assertThrows(ServiceException.class,
                () -> ErpPurchaseInItemMapper.buildSortedQuery(1L, null, "unknownField", "asc"));
    }

    private void assertSupported(Function<String, Boolean> checker, List<String> fields) {
        fields.forEach(field -> assertTrue(checker.apply(field), field));
    }
}
