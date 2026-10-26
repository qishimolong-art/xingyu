package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void shouldFilterReceiptStatusByCalculatedUnreceivedAmount() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        reqVO.setReceiptStatus(ErpSaleOutPageReqVO.RECEIPT_STATUS_ALL);

        String sql = ErpSaleOutMapper.buildQuery(reqVO).getSqlSegment();

        assertTrue(sql.contains("CASE WHEN ROUND(ABS"));
        assertTrue(sql.contains("erp_finance_receipt_item"));
        assertTrue(sql.contains("original_product_price"));
    }

    @Test
    void shouldSortSettlementStatusByCalculatedUnreceivedAmount() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        reqVO.setOrderField("settleStatus");
        reqVO.setOrderDirection("asc");
        MPJLambdaWrapperX<ErpSaleOutDO> wrapper = new MPJLambdaWrapperX<>();

        ErpSaleOutMapper.orderByIfPresent(wrapper, reqVO);

        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("ORDER BY"));
        assertTrue(sql.contains("CASE WHEN ROUND(ABS"));
        assertTrue(sql.contains("ASC"));
        assertTrue(sql.contains("t.id ASC"));
    }
}
