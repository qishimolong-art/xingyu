package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpSaleOutMapperTest {

    @Test
    void shouldWhitelistCompactListSortFields() {
        assertNotNull(ErpSaleOutMapper.getOrderColumn("printCount"));
        assertNotNull(ErpSaleOutMapper.getOrderColumn("no"));
        assertNotNull(ErpSaleOutMapper.getOrderColumn("deliveryMethod"));
        assertNotNull(ErpSaleOutMapper.getOrderColumn("totalCount"));
        assertNotNull(ErpSaleOutMapper.getOrderColumn("totalProductPrice"));
        assertNotNull(ErpSaleOutMapper.getOrderColumn("sourceCreateTime"));
    }

    @Test
    void shouldRejectUnknownOrUnsafeSortFields() {
        assertNull(ErpSaleOutMapper.getOrderColumn(null));
        assertNull(ErpSaleOutMapper.getOrderColumn("shipmentStatus"));
        assertNull(ErpSaleOutMapper.getOrderColumn("id desc; delete from erp_sale_out"));
    }
}
