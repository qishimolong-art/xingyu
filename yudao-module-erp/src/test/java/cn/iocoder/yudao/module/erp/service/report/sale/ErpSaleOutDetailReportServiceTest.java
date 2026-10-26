package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.PriceEvent;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.Summary;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpSaleOutDetailReportServiceTest {

    @Test
    void findPreviousPriceOnlyUsesEventsBeforeCurrentApproval() {
        LocalDateTime current = LocalDateTime.of(2026, 10, 13, 10, 0);
        PriceEvent older = event(current.minusDays(2), "10.00", "SALE_OUT");
        PriceEvent latestAdjust = event(current.minusSeconds(1), "9.00", "SALE_ADJUST");
        PriceEvent sameTime = event(current, "8.00", "SALE_OUT");

        assertEquals(new BigDecimal("9.00"), ErpSaleOutDetailReportService.findPreviousPrice(
                Arrays.asList(older, latestAdjust, sameTime), current));
        assertNull(ErpSaleOutDetailReportService.findPreviousPrice(Arrays.asList(older), null));
        assertNull(ErpSaleOutDetailReportService.findPreviousPrice(null, current));
    }

    @Test
    void formatsCustomerRegionFromBuiltInAreaData() {
        assertEquals("北京市 北京市 朝阳区", ErpSaleReportAreaFormatter.format(110105L));
        assertNull(ErpSaleReportAreaFormatter.format(null));
        assertNull(ErpSaleReportAreaFormatter.format(Long.MAX_VALUE));
    }

    @Test
    void stockCostFieldPermissionControlsCostColumns() {
        ErpSaleFieldPermissionMasker masker = mock(ErpSaleFieldPermissionMasker.class);
        ErpSaleOutDetailReportService service = new ErpSaleOutDetailReportService();
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);

        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "isCostVisible"));
        when(masker.isFieldHidden("erp_stock", "costPrice")).thenReturn(true);
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(service, "isCostVisible"));
        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "isPriceVisible"));

        when(masker.isFieldHidden("erp_stock", "costPrice")).thenReturn(false);
        when(masker.isFieldHidden("erp_stock", "costAmount")).thenReturn(true);
        assertEquals(Boolean.FALSE, ReflectionTestUtils.invokeMethod(service, "isCostVisible"));
        assertEquals(Boolean.TRUE, ReflectionTestUtils.invokeMethod(service, "isPriceVisible"));
    }

    @Test
    void emptySummaryOnlyExposesCostFieldsWhenCostIsVisible() {
        ErpSaleOutDetailReportService service = new ErpSaleOutDetailReportService();

        Summary visible = ReflectionTestUtils.invokeMethod(service, "emptySummary", true, true);
        assertEquals(BigDecimal.ZERO, visible.getTotalSaleCostAmount());
        assertEquals(0L, visible.getMissingSaleCostCount());
        assertEquals(Boolean.TRUE, visible.getCostVisible());

        Summary hidden = ReflectionTestUtils.invokeMethod(service, "emptySummary", true, false);
        assertNull(hidden.getTotalSaleCostAmount());
        assertNull(hidden.getMissingSaleCostCount());
        assertEquals(BigDecimal.ZERO, hidden.getTotalAmount());
        assertEquals(Boolean.FALSE, hidden.getCostVisible());
    }

    @Test
    void hiddenCostPermissionMasksPageAndExportWithoutMaskingSalePrice() {
        ErpSaleFieldPermissionMasker masker = mock(ErpSaleFieldPermissionMasker.class);
        when(masker.isFieldHidden("erp_stock", "costPrice")).thenReturn(true);
        ErpSaleOutDetailReportService service = new ErpSaleOutDetailReportService();
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        Row row = new Row().setProductPrice(new BigDecimal("100.00"))
                .setSaleCostPrice(new BigDecimal("60.00")).setSaleCostAmount(new BigDecimal("120.00"));

        ReflectionTestUtils.invokeMethod(service, "applyPriceVisibility", List.of(row));

        assertEquals(new BigDecimal("100.00"), row.getProductPrice());
        assertNull(row.getSaleCostPrice());
        assertNull(row.getSaleCostAmount());
        assertEquals(Boolean.TRUE, row.getPriceVisible());
        assertEquals(Boolean.FALSE, row.getCostVisible());

        Set<String> fields = ReflectionTestUtils.invokeMethod(service, "exportFields", true, false);
        assertTrue(fields.contains("productPrice"));
        assertFalse(fields.contains("saleCostPrice"));
        assertFalse(fields.contains("saleCostAmount"));
    }

    private PriceEvent event(LocalDateTime time, String price, String documentType) {
        return new PriceEvent().setCustomerId(1L).setProductId(2L).setEventTime(time)
                .setDocumentTypeCode(documentType).setItemId(3L).setPrice(new BigDecimal(price));
    }
}
