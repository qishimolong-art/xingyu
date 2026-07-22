package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpSaleOrderMapperTest {

    @Test
    void shouldWhitelistSupportedOrderFields() {
        assertNotNull(ErpSaleOrderMapper.getOrderColumn("no"));
        assertNotNull(ErpSaleOrderMapper.getOrderColumn("customerId"));
        assertNotNull(ErpSaleOrderMapper.getOrderColumn("totalPrice"));
        assertNotNull(ErpSaleOrderMapper.getOrderColumn("status"));
    }

    @Test
    void shouldRejectUnknownOrUnsafeOrderFields() {
        assertNull(ErpSaleOrderMapper.getOrderColumn(null));
        assertNull(ErpSaleOrderMapper.getOrderColumn("id desc; delete from erp_sale_order"));
        assertNull(ErpSaleOrderMapper.getOrderColumn("productNames"));
    }

    @Test
    void shouldAcceptOnlySupportedDirections() {
        assertEquals("ASC", ErpSaleOrderMapper.normalizeOrderDirection(" asc "));
        assertEquals("DESC", ErpSaleOrderMapper.normalizeOrderDirection("DESC"));
        assertNull(ErpSaleOrderMapper.normalizeOrderDirection(null));
        assertNull(ErpSaleOrderMapper.normalizeOrderDirection("ascending"));
        assertNull(ErpSaleOrderMapper.normalizeOrderDirection("desc, id"));
    }
}
