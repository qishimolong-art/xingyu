package cn.iocoder.yudao.module.erp.service.report.stock;

import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ErpStockOutDetailReportServiceTest {

    @Test
    void shouldMaskLinkedCountPriceCostAndProductFields() {
        ErpStockOutDetailReportService service = service(
                Set.of("item_count", "item_productPrice", "customerId"),
                Set.of("col_unitPrice"), List.of("featureCode", "categoryId"));

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.containsAll(Set.of("count", "cancelCount", "afterCancelCount")));
        assertTrue(hidden.containsAll(Set.of("unitPrice", "totalAmount", "productPrice",
                "productAmount", "afterCancelAmount")));
        assertTrue(hidden.containsAll(Set.of("costUnitPrice", "costAmount")));
        assertTrue(hidden.containsAll(Set.of("relatedPartyName", "featureCode", "productCategoryName")));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "costVisible"));
    }

    @Test
    void shouldKeepIndependentGroupsVisible() {
        ErpStockOutDetailReportService service = service(
                Set.of("item_productId"), Collections.emptySet(), Collections.emptyList());

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.contains("productName"));
        assertFalse(hidden.contains("count"));
        assertFalse(hidden.contains("brand"));
        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "priceVisible"));
        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "costVisible"));
    }

    private ErpStockOutDetailReportService service(Set<String> stockOutHidden,
                                                     Set<String> stockRecordHidden,
                                                     List<String> productHidden) {
        ErpStockOutDetailReportService service = new ErpStockOutDetailReportService();
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        PermissionApi permissionApi = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        when(masker.getHiddenFieldSet("erp_stock_out")).thenReturn(stockOutHidden);
        when(masker.getHiddenFieldSet("erp_stock_record")).thenReturn(stockRecordHidden);
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(productHidden);
        return service;
    }
}
