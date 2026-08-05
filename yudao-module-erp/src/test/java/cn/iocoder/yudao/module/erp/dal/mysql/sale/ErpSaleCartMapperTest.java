package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpSaleCartMapperTest {

    @Test
    void shouldWhitelistRestoredDeliveryAndVinSortFields() {
        assertNotNull(ErpSaleCartMapper.getOrderColumn("deliveryMethod"));
        assertNotNull(ErpSaleCartMapper.getOrderColumn("vin"));
    }

    @Test
    void shouldRejectUnknownOrUnsafeSortFields() {
        assertNull(ErpSaleCartMapper.getOrderColumn(null));
        assertNull(ErpSaleCartMapper.getOrderColumn("id desc; delete from erp_sale_cart"));
    }
}
