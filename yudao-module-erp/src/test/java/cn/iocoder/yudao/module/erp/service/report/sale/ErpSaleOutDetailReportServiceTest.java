package cn.iocoder.yudao.module.erp.service.report.sale;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale.ErpSaleOutDetailReportModels.PriceEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    private PriceEvent event(LocalDateTime time, String price, String documentType) {
        return new PriceEvent().setCustomerId(1L).setProductId(2L).setEventTime(time)
                .setDocumentTypeCode(documentType).setItemId(3L).setPrice(new BigDecimal(price));
    }
}
