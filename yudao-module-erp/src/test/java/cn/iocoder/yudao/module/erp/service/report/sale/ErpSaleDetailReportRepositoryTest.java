package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSalePickDeliveryDetailReportModels;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleQuoteDetailReportModels;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpSaleDetailReportRepositoryTest {

    private static final ErpFinanceVisibleScope ALL = new ErpFinanceVisibleScope(true, List.of(), null);

    @Test
    void quoteReportLimitsStatusesAndUsesQuoteToCartProgress() {
        ErpSaleQuoteDetailReportRepository repository = new ErpSaleQuoteDetailReportRepository();
        ErpSaleQuoteDetailReportRepository.Scope scope =
                new ErpSaleQuoteDetailReportRepository.Scope(1L, ALL, ALL);
        Object sql = ReflectionTestUtils.invokeMethod(repository, "base",
                new ErpSaleQuoteDetailReportModels.Filter(), scope);
        String text = (String) ReflectionTestUtils.getField(sql, "text");
        String selectFields = ReflectionTestUtils.invokeMethod(repository, "selectFields");

        assertNotNull(text);
        assertNotNull(selectFields);
        assertTrue(text.contains("sq.status IN (20,30,40,50)"));
        assertTrue(text.contains("sqi.deleted=b'0'"));
        assertTrue(text.contains("sqi.tenant_id=:tenant"));
        assertTrue(selectFields.contains("c.area_id AS customer_area_id"));
        assertTrue(selectFields.contains("sq.source_id AS source_business_id"));
        assertTrue(selectFields.contains("'SALE_CART'"));
        assertTrue(selectFields.contains("'MALL_ORDER'"));
        assertFalse(text.contains("system_area"));
    }

    @Test
    void outReportPreservesOriginalOutAndSignsReturnAndAdjustRows() {
        ErpSaleOutDetailReportRepository repository = new ErpSaleOutDetailReportRepository();
        ErpSaleOutDetailReportRepository.Scope scope =
                new ErpSaleOutDetailReportRepository.Scope(1L, ALL, ALL, ALL, ALL);
        Object sql = ReflectionTestUtils.invokeMethod(repository, "reportSql",
                new ErpSaleOutDetailReportModels.Filter(), scope);
        String cte = (String) ReflectionTestUtils.getField(sql, "cte");

        assertNotNull(cte);
        assertTrue(cte.contains("COALESCE(ofi.original_product_price,ofi.product_price)"));
        assertTrue(cte.contains("-COALESCE(sri.count,0)"));
        assertTrue(cte.contains("ROUND(-COALESCE(sri.count,0)*COALESCE(sri.product_price,0),2)"));
        assertTrue(cte.contains("ROUND(COALESCE(sai.adjust_price,0),2)"));
        assertTrue(cte.contains("oa.freight-(SUM(oa.provisional_freight)"));
        assertTrue(cte.contains("sa.approve_time IS NOT NULL") || cte.contains("sa.status=20"));
        assertTrue(cte.contains("c.area_id AS customer_area_id"));
        assertTrue(cte.contains("COALESCE(so.order_id,so.source_id) AS source_business_id"));
        assertTrue(cte.contains("WHEN so.source_type=20 THEN 'SALE_QUOTE'"));
        assertTrue(cte.contains("WHEN so.source_type=30 THEN 'SALE_CART'"));
        assertTrue(cte.contains("WHEN so.source_type=50 THEN 'PURCHASE_IN'"));
        assertTrue(cte.contains("WHEN sr.return_mode=10 THEN sr.source_out_id END"));
        assertTrue(cte.contains("sai.id,sa.id,source_out.id,'SALE_OUT'"));
        assertFalse(cte.contains("system_area"));
    }

    @Test
    void pickDeliveryReportUsesCurrentItemTotalsAndKeepsMissingSources() {
        ErpSalePickDeliveryDetailReportRepository repository =
                new ErpSalePickDeliveryDetailReportRepository();
        ErpSalePickDeliveryDetailReportRepository.Scope scope =
                new ErpSalePickDeliveryDetailReportRepository.Scope(1L, ALL, ALL);

        Object pickSql = ReflectionTestUtils.invokeMethod(repository, "base",
                new ErpSalePickDeliveryDetailReportModels.Filter(), scope, false);
        Object deliverySql = ReflectionTestUtils.invokeMethod(repository, "base",
                new ErpSalePickDeliveryDetailReportModels.Filter(), scope, true);
        String pickText = (String) ReflectionTestUtils.getField(pickSql, "text");
        String deliveryText = (String) ReflectionTestUtils.getField(deliverySql, "text");

        assertNotNull(pickText);
        assertNotNull(deliveryText);
        assertTrue(pickText.contains("FROM erp_sale_pick_delivery_item i"));
        assertTrue(pickText.contains("i.picked_count IS NULL AND i.pick_status=30"));
        assertTrue(deliveryText.contains("i.delivered_count IS NULL AND i.delivery_status=30"));
        assertTrue(deliveryText.contains("GREATEST(("));
        assertTrue(deliveryText.contains("COALESCE(NULLIF(i.package_qty,0),p.package_qty) AS package_qty"));
        assertTrue(deliveryText.contains("COALESCE(NULLIF(i.warehouse_position,''),s.shelf)"));
        assertTrue(pickText.contains("LEFT JOIN erp_sale_cart sc"));
        assertTrue(pickText.contains("LEFT JOIN erp_sale_out so"));
        assertTrue(pickText.contains("i.tenant_id=:tenant AND i.deleted=b'0'"));
        assertTrue(pickText.contains("t.tenant_id=i.tenant_id"));
        assertTrue(pickText.contains("o.tenant_id=i.tenant_id"));
        assertFalse(pickText.contains("INNER JOIN erp_sale_cart"));
        assertFalse(pickText.contains("INNER JOIN erp_sale_out"));
    }

    @Test
    void pickDeliveryReportStripsFiltersForHiddenFields() {
        ErpSaleFieldPermissionMasker masker = mock(ErpSaleFieldPermissionMasker.class);
        when(masker.isFieldHidden("erp_sale_cart", "no")).thenReturn(true);
        when(masker.isFieldHidden("erp_sale_cart", "item_count")).thenReturn(true);
        ErpSalePickDeliveryDetailReportService service = new ErpSalePickDeliveryDetailReportService();
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ErpSalePickDeliveryDetailReportModels.Filter filter =
                new ErpSalePickDeliveryDetailReportModels.Filter();
        filter.setSourceCartNo("XSST-1");
        filter.setSaleOutNo("XSCK-1");
        filter.setCountMin(BigDecimal.ONE);
        filter.setRemainingCountMax(BigDecimal.TEN);
        filter.setOperationUserId(9L);

        ReflectionTestUtils.invokeMethod(service, "stripHiddenFilters", filter);

        assertNull(filter.getSourceCartNo());
        assertEquals("XSCK-1", filter.getSaleOutNo());
        assertNull(filter.getCountMin());
        assertNull(filter.getRemainingCountMax());
        assertEquals(9L, filter.getOperationUserId());
    }
}
