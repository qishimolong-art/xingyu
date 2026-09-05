package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
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

class ErpStockImportFailureDetailsControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpStockInController stockInController;
    @InjectMocks
    private ErpStockOutController stockOutController;
    @InjectMocks
    private ErpStockCheckController stockCheckController;
    @InjectMocks
    private ErpStockTransferOutController stockTransferOutController;
    @InjectMocks
    private ErpWarehouseMoveController warehouseMoveController;
    @InjectMocks
    private ErpWarehouseController warehouseController;

    @Mock
    private ErpImportExportRecordService importExportRecordService;

    @Test
    void downloadImportFailureDetails_usesStockImportPermissions() throws Exception {
        assertDownloadMapping(ErpStockInController.class, "erp:stock-in:import");
        assertDownloadMapping(ErpStockOutController.class, "erp:stock-out:import");
        assertDownloadMapping(ErpStockCheckController.class, "erp:stock-check:import");
        assertDownloadMapping(ErpStockTransferOutController.class, "erp:stock-transfer-out:import");
        assertDownloadMapping(ErpWarehouseMoveController.class, "erp:warehouse-move:create");
        assertDownloadMapping(ErpWarehouseController.class, "erp:warehouse:import");
    }

    @Test
    void downloadImportFailureDetails_delegatesStockInModule() throws Exception {
        Long recordId = 500L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        stockInController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_stock_in", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesStockOutModule() throws Exception {
        Long recordId = 501L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        stockOutController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_stock_out", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesStockCheckModule() throws Exception {
        Long recordId = 502L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        stockCheckController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_stock_check", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesStockTransferOutModule() throws Exception {
        Long recordId = 503L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        stockTransferOutController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId,
                "erp_stock_transfer_out", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesWarehouseMoveModule() throws Exception {
        Long recordId = 504L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        warehouseMoveController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_warehouse_move", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesWarehouseModule() throws Exception {
        Long recordId = 505L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        warehouseController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_warehouse", response);
    }

    private void assertDownloadMapping(Class<?> controllerClass, String permission) throws Exception {
        Method method = controllerClass.getMethod("downloadImportFailureDetails",
                Long.class, HttpServletResponse.class);

        assertArrayEquals(new String[]{"/import-failure-details/download"},
                method.getAnnotation(GetMapping.class).value());
        assertEquals(String.format("@ss.hasPermission('%s')", permission),
                method.getAnnotation(PreAuthorize.class).value());
    }

}
