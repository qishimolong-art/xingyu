package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.Row;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableDetailReportModels.TYPE_SALE_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpReceivableDetailReportServiceTest {

    @Test
    void shouldClearMainNumberWhenOriginalDocumentNumberIsHidden() throws Exception {
        Row row = row();

        applySourceVisibility(row, Collections.singleton("no"), Collections.emptySet());

        assertNull(row.getDocumentNo());
        assertEquals("DD-1", row.getSourceNo());
    }

    @Test
    void shouldClearSourceTargetWhenSourceNumberIsHidden() throws Exception {
        Row row = row();

        applySourceVisibility(row, Collections.singleton("orderNo"), Collections.emptySet());

        assertNull(row.getSourceNo());
        assertNull(row.getSourceBusinessId());
        assertNull(row.getSourceDocumentTypeCode());
        assertEquals("CK-1", row.getDocumentNo());
    }

    @Test
    void shouldRejectRemovedManualWriteoffDocumentType() throws Exception {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(20);
        filter.setDocumentTypes(Arrays.asList(TYPE_SALE_OUT, "MANUAL_WRITEOFF"));
        ErpReceivableDetailReportService service = new ErpReceivableDetailReportService();
        Method method = ErpReceivableDetailReportService.class.getDeclaredMethod("validate", Filter.class);
        method.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, filter));

        assertTrue(exception.getCause() instanceof ServiceException);
        assertEquals("单据类型不正确", exception.getCause().getMessage());
    }

    private static Row row() {
        return new Row().setDocumentTypeCode(TYPE_SALE_OUT).setDocumentNo("CK-1").setSourceNo("DD-1")
                .setSourceBusinessId(7L).setSourceDocumentTypeCode("SALE_ORDER");
    }

    private static void applySourceVisibility(Row row, Set<String> sourceHidden, Set<String> accountHidden)
            throws Exception {
        ErpReceivableDetailReportService service = new ErpReceivableDetailReportService();
        Method method = ErpReceivableDetailReportService.class.getDeclaredMethod(
                "applySourceVisibility", Row.class, Set.class, Set.class);
        method.setAccessible(true);
        method.invoke(service, row, sourceHidden, accountHidden);
    }
}
