package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock.ErpStockTransferInDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpStockTransferInDetailReportServiceTest {

    @Test
    void shouldHideBothPriceAndAmountGroupsWhenUnitPriceIsHidden() {
        ErpStockTransferInDetailReportService service = serviceWithHidden(
                "erp_stock_transfer_in", "item_productPrice");

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "amountVisible"));
        assertTrue(hidden.contains("productPrice"));
        assertTrue(hidden.contains("costUnitPrice"));
        assertTrue(hidden.contains("totalAmount"));
        assertTrue(hidden.contains("costAmount"));
    }

    @Test
    void shouldKeepUnitPricesWhenOnlyAmountIsHidden() {
        ErpStockTransferInDetailReportService service = serviceWithHidden(
                "erp_stock_transfer_in", "item_totalPrice");

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "amountVisible"));
        assertFalse(hidden.contains("productPrice"));
        assertFalse(hidden.contains("costUnitPrice"));
        assertTrue(hidden.contains("totalAmount"));
        assertTrue(hidden.contains("costAmount"));
    }

    @Test
    void shouldRejectPriceAndHiddenProductFilters() {
        ErpStockTransferInDetailReportService priceService = serviceWithHidden(
                "erp_stock_transfer_in", "item_productPrice");
        Filter priceFilter = filter();
        priceFilter.setPriceMin(BigDecimal.ONE);
        Object priceVisibility = ReflectionTestUtils.invokeMethod(priceService, "visibility");
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(priceService, "validate", priceFilter, priceVisibility));

        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        for (String field : new String[]{"code", "name", "vehicleModel", "originPlace", "standard", "drawingNo"}) {
            when(masker.isFieldHidden("erp_product", field)).thenReturn(true);
        }
        ErpStockTransferInDetailReportService productService = new ErpStockTransferInDetailReportService();
        ReflectionTestUtils.setField(productService, "fieldPermissionMasker", masker);
        Filter productFilter = filter();
        productFilter.setProductKeyword("制动片");
        Object productVisibility = ReflectionTestUtils.invokeMethod(productService, "visibility");
        assertThrows(ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(productService, "validate", productFilter, productVisibility));
    }

    private ErpStockTransferInDetailReportService serviceWithHidden(String module, String field) {
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        when(masker.isFieldHidden(module, field)).thenReturn(true);
        ErpStockTransferInDetailReportService service = new ErpStockTransferInDetailReportService();
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        return service;
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        return filter;
    }
}
