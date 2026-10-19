package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSaleCartItemMapperSortTest {

    private static final List<String> DETAIL_SORT_FIELDS = List.of(
            "productId", "productCode", "warehouseId", "warehouseDeptName",
            "count", "productPrice", "lastSalePrice", "totalPrice", "giftFlag",
            "batchNo", "brand", "vehicleModel", "standard", "originPlace",
            "productUnitName", "weight", "packageQty", "pieceCount", "stockCount", "remark");

    @Test
    void supportsEverySortableSaleCartDetailField() {
        DETAIL_SORT_FIELDS.forEach(field -> {
            assertTrue(ErpSaleCartItemMapper.isSupportedOrderField(field), field);
            assertNotNull(ErpSaleCartItemMapper.getOrderSpec(field), field);
        });
        assertFalse(ErpSaleCartItemMapper.isSupportedOrderField("id desc; delete from erp_sale_cart_items"));
    }

    @Test
    void usesDisplayAndCalculatedExpressionsWithNullLastAndStableTieBreak() {
        String productSql = sql("productId", "asc");
        assertTrue(productSql.contains("erp_product"));
        assertTrue(productSql.contains("CASE WHEN"));
        assertTrue(productSql.contains("TRIM"));
        assertTrue(productSql.contains("erp_sale_cart_items.id ASC"));

        String lastSalePriceSql = sql("lastSalePrice", "desc");
        assertTrue(lastSalePriceSql.contains("erp_sale_out_items"));
        assertTrue(lastSalePriceSql.contains("so.status"));
        assertTrue(lastSalePriceSql.contains("DESC"));
        assertTrue(lastSalePriceSql.contains("erp_sale_cart_items.id ASC"));

        String pieceCountSql = sql("pieceCount", "asc");
        assertTrue(pieceCountSql.contains("package_qty > 0"));
        assertTrue(pieceCountSql.contains("count / erp_sale_cart_items.package_qty"));
    }

    @Test
    void defaultsToOriginalIdOrderAndKeepsEditExclusions() {
        String defaultSql = ErpSaleCartItemMapper.buildSortedQuery(9L, List.of(21L, 22L), null, null)
                .getCustomSqlSegment();

        assertTrue(defaultSql.contains("cart_id"));
        assertTrue(defaultSql.contains("NOT IN"));
        assertTrue(defaultSql.contains("erp_sale_cart_items.id ASC"));
    }

    private String sql(String field, String direction) {
        QueryWrapper<?> query = ErpSaleCartItemMapper.buildSortedQuery(9L, null, field, direction);
        return query.getCustomSqlSegment();
    }

}
