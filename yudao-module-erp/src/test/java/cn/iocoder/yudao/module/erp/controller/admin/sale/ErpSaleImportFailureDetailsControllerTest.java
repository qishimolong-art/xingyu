package cn.iocoder.yudao.module.erp.controller.admin.sale;

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

class ErpSaleImportFailureDetailsControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOrderController saleOrderController;
    @InjectMocks
    private ErpSaleQuoteController saleQuoteController;
    @InjectMocks
    private ErpSaleCartController saleCartController;
    @InjectMocks
    private ErpSaleReturnController saleReturnController;
    @InjectMocks
    private ErpSalePriceAdjustController salePriceAdjustController;

    @Mock
    private ErpImportExportRecordService importExportRecordService;

    @Test
    void downloadImportFailureDetails_usesSaleCreatePermissions() throws Exception {
        assertDownloadMapping(ErpSaleOrderController.class, "erp:sale-order:create");
        assertDownloadMapping(ErpSaleQuoteController.class, "erp:sale-quote:create");
        assertDownloadMapping(ErpSaleCartController.class, "erp:sale-cart:create");
        assertDownloadMapping(ErpSaleReturnController.class, "erp:sale-return:create");
        assertDownloadMapping(ErpSalePriceAdjustController.class, "erp:sale-price-adjust:create");
    }

    @Test
    void downloadImportFailureDetails_delegatesSaleOrderModule() throws Exception {
        Long recordId = 400L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        saleOrderController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_sale_order", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesSaleQuoteModule() throws Exception {
        Long recordId = 401L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        saleQuoteController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_sale_quote", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesSaleCartModule() throws Exception {
        Long recordId = 402L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        saleCartController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_sale_cart", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesSaleReturnModule() throws Exception {
        Long recordId = 403L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        saleReturnController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId, "erp_sale_return", response);
    }

    @Test
    void downloadImportFailureDetails_delegatesSalePriceAdjustModule() throws Exception {
        Long recordId = 404L;
        HttpServletResponse response = mock(HttpServletResponse.class);

        salePriceAdjustController.downloadImportFailureDetails(recordId, response);

        verify(importExportRecordService).downloadOwnImportFailureDetails(recordId,
                "erp_sale_price_adjust", response);
    }

    private void assertDownloadMapping(Class<?> controllerClass, String permission) throws Exception {
        Method method = controllerClass.getMethod("downloadImportFailureDetails",
                Long.class, HttpServletResponse.class);

        assertArrayEquals(new String[]{"/import-failure-details/download"}, method.getAnnotation(GetMapping.class).value());
        assertEquals(String.format("@ss.hasPermission('%s')", permission),
                method.getAnnotation(PreAuthorize.class).value());
    }

}
