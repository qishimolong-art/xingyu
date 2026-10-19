package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Row;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockAgeReportModels.Summary;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ErpStockAgeReportServiceTest {

    private final ErpStockAgeReportService service = new ErpStockAgeReportService();

    @Test
    void shouldReturnNullInsteadOfZeroWhenAmountPermissionIsHidden() {
        Row row = new Row().setCostUnitPrice(BigDecimal.ONE).setCostAmount(BigDecimal.ONE)
                .setWithin1MonthAmount(BigDecimal.ONE).setMonths2To3Amount(BigDecimal.ONE)
                .setMonths4To6Amount(BigDecimal.ONE).setMonths7To12Amount(BigDecimal.ONE)
                .setYears1To2Amount(BigDecimal.ONE).setYears2To3Amount(BigDecimal.ONE)
                .setOver3YearsAmount(BigDecimal.ONE);
        ReflectionTestUtils.invokeMethod(service, "applyAmountVisibility", Collections.singletonList(row), false);

        assertFalse(row.getAmountVisible());
        assertNull(row.getCostUnitPrice());
        assertNull(row.getCostAmount());
        assertNull(row.getWithin1MonthAmount());
        assertNull(row.getMonths2To3Amount());
        assertNull(row.getMonths4To6Amount());
        assertNull(row.getMonths7To12Amount());
        assertNull(row.getYears1To2Amount());
        assertNull(row.getYears2To3Amount());
        assertNull(row.getOver3YearsAmount());
    }

    @Test
    void shouldHideSummaryAmountsAndAmountExportColumnsTogether() {
        Summary summary = new Summary().setTotalAmount(BigDecimal.ONE)
                .setWithin1MonthAmount(BigDecimal.ONE).setMonths2To3Amount(BigDecimal.ONE)
                .setMonths4To6Amount(BigDecimal.ONE).setMonths7To12Amount(BigDecimal.ONE)
                .setYears1To2Amount(BigDecimal.ONE).setYears2To3Amount(BigDecimal.ONE)
                .setOver3YearsAmount(BigDecimal.ONE).setUnclassifiedAmount(BigDecimal.ONE);
        ReflectionTestUtils.invokeMethod(service, "applyAmountVisibility", summary, false);
        @SuppressWarnings("unchecked")
        Set<String> fields = ReflectionTestUtils.invokeMethod(service, "exportFields", false);

        assertFalse(summary.getAmountVisible());
        assertNull(summary.getTotalAmount());
        assertNull(summary.getUnclassifiedAmount());
        assertNotNull(fields);
        assertFalse(fields.contains("costUnitPrice"));
        assertFalse(fields.contains("costAmount"));
        assertFalse(fields.contains("within1MonthAmount"));
        assertTrue(fields.contains("stockQuantity"));
        assertTrue(fields.contains("within1MonthQuantity"));
    }
}
