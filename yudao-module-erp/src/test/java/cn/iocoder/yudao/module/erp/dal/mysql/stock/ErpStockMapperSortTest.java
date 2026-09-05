package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockMapperSortTest {

    @Test
    void shouldWhitelistSharedStockSelectorFields() {
        assertTrue(ErpStockMapper.getOrderExpression("warehouseName", null).contains("erp_warehouse"));
        assertTrue(ErpStockMapper.getOrderExpression("deptName", null).contains("system_dept"));
        assertNotNull(ErpStockMapper.getOrderExpression("pendingInCount", null));
        assertTrue(ErpStockMapper.getOrderExpression("availableCount", null).contains("erp_sale_cart_items"));
        assertTrue(ErpStockMapper.getOrderExpression("availableCount", null).contains("erp_stock.count"));
        assertNotNull(ErpStockMapper.getOrderExpression("lastPurchasePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("retailPrice", null));
        assertNull(ErpStockMapper.getOrderExpression("id desc; delete from erp_stock", null));
    }

    @Test
    void summaryValueReadersReturnZeroForNullRow() {
        assertEquals(0L, ErpStockMapper.getLong(null, "total_rows"));
        assertEquals(BigDecimal.ZERO, ErpStockMapper.getBigDecimal(null, "total_stock_count"));
    }
}
