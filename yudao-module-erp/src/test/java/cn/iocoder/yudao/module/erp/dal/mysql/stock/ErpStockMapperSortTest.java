package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpStockMapperSortTest {

    @Test
    void shouldWhitelistSharedStockSelectorFields() {
        assertTrue(ErpStockMapper.getOrderExpression("warehouseName", null).contains("erp_warehouse"));
        assertTrue(ErpStockMapper.getOrderExpression("deptName", null).contains("system_dept"));
        assertNotNull(ErpStockMapper.getOrderExpression("pendingInCount", null));
        assertNotNull(ErpStockMapper.getOrderExpression("lastPurchasePrice", null));
        assertNotNull(ErpStockMapper.getOrderExpression("retailPrice", null));
        assertNull(ErpStockMapper.getOrderExpression("id desc; delete from erp_stock", null));
    }
}
