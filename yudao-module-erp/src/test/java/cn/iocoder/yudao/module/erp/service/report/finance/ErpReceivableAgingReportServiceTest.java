package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.DetailFilter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.Filter;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpReceivableAgingReportModels.BUCKET_WITHIN_1_MONTH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErpReceivableAgingReportServiceTest {

    private final ErpReceivableAgingReportService service = new ErpReceivableAgingReportService();

    @Test
    void shouldRejectFutureAnalysisDate() {
        Filter filter = filter(LocalDate.now().plusDays(1));
        assertThrows(ServiceException.class, () -> service.validate(filter));
    }

    @Test
    void shouldRequireValidDetailDepartmentAndBucket() {
        DetailFilter filter = new DetailFilter();
        filter.setPageNo(1);
        filter.setPageSize(20);
        filter.setAnalysisDate(LocalDate.now());
        filter.setCustomerId(1L);
        filter.setBucket("UNKNOWN");
        assertThrows(ServiceException.class, () -> service.validateDetail(filter));

        filter.setDeptUnassigned(true);
        filter.setBucket(BUCKET_WITHIN_1_MONTH);
        assertDoesNotThrow(() -> service.validateDetail(filter));
    }

    @Test
    void shouldAcceptTodayWithBoundedPaging() {
        assertDoesNotThrow(() -> service.validate(filter(LocalDate.now())));
    }

    private Filter filter(LocalDate date) {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(20);
        filter.setAnalysisDate(date);
        return filter;
    }
}
