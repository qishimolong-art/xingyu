package cn.iocoder.yudao.module.erp.controller.admin.stock;

import java.math.BigDecimal;
import java.util.*;
import lombok.Data;
import lombok.experimental.Accessors;

/** Explicit export dependencies for other stock documents; no business writes. */
final class ErpOtherStockExportSupport {
    private ErpOtherStockExportSupport() {}
    static Set<String> hidden(Class<?> head, Set<String> source) {
        Set<String> normalized = new HashSet<>(source == null ? Collections.emptySet() : source);
        for (String field : new HashSet<>(normalized)) if (field.startsWith("col_")) normalized.add(field.substring(4));
        Map<String,String> aliases = new HashMap<>();
        for (String field : Arrays.asList("ProductCode", "ProductName", "ProductBarCode", "ProductUnitName", "FactoryCode", "DrawingNo", "Brand", "VehicleModel", "OriginPlace", "Shelf")) aliases.put("item"+field,"item_productId");
        aliases.put("itemWarehouseName","item_warehouseId");
        aliases.put("itemWarehouseDeptName","item_warehouseId");
        aliases.put("warehouseNames","item_warehouseId");
        Set<String> hidden = ErpStockMovementExportSupport.hidden(head, normalized, aliases);
        if (hidden.contains("item_weight") || hidden.contains("item_count")) hidden.addAll(Arrays.asList("itemTotalWeight","totalWeight"));
        if (hidden.contains("item_totalPrice")) hidden.add("totalPrice");
        if (hidden.contains("supplierId")) hidden.add("supplierName");
        if (hidden.contains("customerId")) hidden.add("customerName");
        if (hidden.contains("deptId")) hidden.add("deptName");
        if (hidden.contains("pickStatus")) hidden.add("pickStatusName");
        return hidden;
    }
    @Data
    @Accessors(chain = true)
    public static class Item {
        private String productCode, productName, productBarCode, productUnitName;
        private String warehouseName, warehouseDeptName, batchNo, shelf, factoryCode;
        private String drawingNo, brand, vehicleModel, originPlace, remark;
        private Integer packageQty;
        private BigDecimal count, weight, totalWeight, productPrice, totalPrice;
        private Boolean priceVisible;
    }
}
