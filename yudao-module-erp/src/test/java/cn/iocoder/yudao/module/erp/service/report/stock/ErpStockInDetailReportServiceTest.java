package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpStockInDetailReportServiceTest {

    @Test
    void shouldMaskPriceGroupAndProtectedStockInProductFields() {
        ErpStockInDetailReportService service = service(
                Set.of("item_productPrice", "supplierId", "item_remark", "col_totalCount"),
                Arrays.asList("brand", "categoryId", "shelf"));

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.contains("productPrice"));
        assertTrue(hidden.contains("totalAmount"));
        assertTrue(hidden.contains("relatedPartyName"));
        assertTrue(hidden.contains("itemRemark"));
        assertTrue(hidden.contains("count"));
        assertTrue(hidden.contains("brand"));
        assertTrue(hidden.contains("categoryName"));
        assertTrue(hidden.contains("shelf"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
    }

    @Test
    void shouldKeepPriceAndUnrelatedProductFieldsVisible() {
        ErpStockInDetailReportService service = service(Set.of("item_productId"), Collections.emptyList());

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.contains("productName"));
        assertFalse(hidden.contains("brand"));
        assertFalse(hidden.contains("vehicleModel"));
        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
    }

    private ErpStockInDetailReportService service(Set<String> stockInHidden, java.util.List<String> productHidden) {
        ErpStockInDetailReportService service = new ErpStockInDetailReportService();
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        PermissionApi permissionApi = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        when(masker.getHiddenFieldSet("erp_stock_in")).thenReturn(stockInHidden);
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(productHidden);
        return service;
    }
}
