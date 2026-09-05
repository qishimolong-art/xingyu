package cn.iocoder.yudao.module.erp.controller.admin.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableExpenseController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.ErpPayableOtherController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableOtherController;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.ErpReceivableOtherIncomeController;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ErpFinanceImportFailureDetailsControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceReceiptController financeReceiptController;
    @InjectMocks
    private ErpFinancePaymentController financePaymentController;
    @InjectMocks
    private ErpFinanceTransferController financeTransferController;
    @InjectMocks
    private ErpReceivableOtherController receivableOtherController;
    @InjectMocks
    private ErpReceivableOtherIncomeController receivableOtherIncomeController;
    @InjectMocks
    private ErpPayableOtherController payableOtherController;
    @InjectMocks
    private ErpPayableExpenseController payableExpenseController;

    @Mock
    private ErpImportExportRecordService importExportRecordService;

    @Test
    void downloadImportFailureDetails_usesFinanceImportPermissions() throws Exception {
        assertDownloadMapping(ErpFinanceReceiptController.class,
                "@ss.hasPermission('erp:finance-receipt:import') and "
                        + "@ss.hasPermission('erp:finance-receipt:update-status')");
        assertDownloadMapping(ErpFinancePaymentController.class,
                "@ss.hasPermission('erp:finance-payment:import')");
        assertDownloadMapping(ErpFinanceTransferController.class,
                "@ss.hasPermission('erp:finance-transfer:import')");
        assertDownloadMapping(ErpReceivableOtherController.class,
                "@ss.hasPermission('erp:receivable-other:import')");
        assertDownloadMapping(ErpReceivableOtherIncomeController.class,
                "@ss.hasPermission('erp:receivable-other-income:import')");
        assertDownloadMapping(ErpPayableOtherController.class,
                "@ss.hasPermission('erp:payable-other:import')");
        assertDownloadMapping(ErpPayableExpenseController.class,
                "@ss.hasPermission('erp:payable-expense:import')");
    }

    @Test
    void downloadImportFailureDetails_delegatesFinanceReceiptModule() throws Exception {
        Long recordId = 600L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        financeReceiptController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_finance_receipt", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesFinancePaymentModule() throws Exception {
        Long recordId = 601L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        financePaymentController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_finance_payment", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesFinanceTransferModule() throws Exception {
        Long recordId = 602L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        financeTransferController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_finance_transfer", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesReceivableOtherModule() throws Exception {
        Long recordId = 603L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        receivableOtherController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_receivable_other", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesReceivableOtherIncomeModule() throws Exception {
        Long recordId = 604L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        receivableOtherIncomeController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId,
                "erp_receivable_other_income", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesPayableOtherModule() throws Exception {
        Long recordId = 605L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        payableOtherController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_payable_other", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesPayableExpenseModule() throws Exception {
        Long recordId = 606L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        payableExpenseController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_payable_expense", response);
    }

    private void assertDownloadMapping(Class<?> controllerClass, String permissionExpression) throws Exception {
        Method method = controllerClass.getMethod("downloadImportFailureDetails",
                Long.class, HttpServletResponse.class);

        assertArrayEquals(new String[]{"/import-failure-details/download"},
                method.getAnnotation(GetMapping.class).value());
        assertEquals(permissionExpression, method.getAnnotation(PreAuthorize.class).value());
    }

}
