package cn.iocoder.yudao.module.erp.dal.mysql.product;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpProductMapperSortTest {

    @Test
    void shouldWhitelistSharedProductSelectorFields() {
        assertNotNull(ErpProductMapper.getOrderColumn("code"));
        assertNotNull(ErpProductMapper.getOrderColumn("lastPurchasePrice"));
        assertNotNull(ErpProductMapper.getOrderColumn("salePrice"));
        assertNotNull(ErpProductMapper.getOrderColumn("brand"));
        assertNotNull(ErpProductMapper.getOrderColumn("drawingNo"));
        assertNull(ErpProductMapper.getOrderColumn("id desc; delete from erp_product"));
    }
}
