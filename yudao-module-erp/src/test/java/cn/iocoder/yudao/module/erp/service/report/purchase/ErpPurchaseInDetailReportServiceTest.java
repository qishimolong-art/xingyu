package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInDetailReportModels.PriceEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErpPurchaseInDetailReportServiceTest {

    @Test
    void findPreviousPriceOnlyUsesEventsBeforeCurrentApproval() {
        LocalDateTime current = LocalDateTime.of(2026, 10, 10, 10, 0);
        PriceEvent older = event(current.minusDays(2), "10.00");
        PriceEvent latest = event(current.minusSeconds(1), "9.00");
        PriceEvent sameTime = event(current, "8.00");

        assertEquals(new BigDecimal("9.00"), ErpPurchaseInDetailReportService.findPreviousPrice(
                Arrays.asList(older, latest, sameTime), current));
        assertNull(ErpPurchaseInDetailReportService.findPreviousPrice(Arrays.asList(older), null));
        assertNull(ErpPurchaseInDetailReportService.findPreviousPrice(null, current));
    }

    private PriceEvent event(LocalDateTime time, String price) {
        return new PriceEvent().setProductId(1L).setEventTime(time).setDocumentTypeCode("PURCHASE_IN")
                .setItemId(1L).setPrice(new BigDecimal(price));
    }
}
