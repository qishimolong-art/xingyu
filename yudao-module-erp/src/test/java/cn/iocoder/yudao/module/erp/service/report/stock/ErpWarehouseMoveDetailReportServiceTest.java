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

class ErpWarehouseMoveDetailReportServiceTest {

    @Test
    void shouldMaskCostAndMovePriceGroupsWithoutLeakingProductFields() {
        ErpWarehouseMoveDetailReportService service = new ErpWarehouseMoveDetailReportService();
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        PermissionApi permissionApi = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        when(masker.getHiddenFieldSet("erp_warehouse_move")).thenReturn(
                Set.of("item_costPrice", "item_totalPrice", "item_remark"));
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Arrays.asList("brand", "featureCode"));

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.contains("costPrice"));
        assertTrue(hidden.contains("costAmount"));
        assertTrue(hidden.contains("movePrice"));
        assertTrue(hidden.contains("moveAmount"));
        assertTrue(hidden.contains("brand"));
        assertTrue(hidden.contains("featureCode"));
        assertTrue(hidden.contains("itemRemark"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "costVisible"));
        assertFalse((Boolean) ReflectionTestUtils.getField(visibility, "movePriceVisible"));
    }

    @Test
    void shouldKeepBothPriceGroupsWhenPermissionsAreVisible() {
        ErpWarehouseMoveDetailReportService service = new ErpWarehouseMoveDetailReportService();
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        PermissionApi permissionApi = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        when(masker.getHiddenFieldSet("erp_warehouse_move")).thenReturn(Collections.emptySet());
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "costVisible"));
        assertTrue((Boolean) ReflectionTestUtils.getField(visibility, "movePriceVisible"));
    }

    @Test
    void shouldMapExistingListAndDetailFieldPermissionsPrecisely() {
        ErpWarehouseMoveDetailReportService service = new ErpWarehouseMoveDetailReportService();
        ErpStockFieldPermissionMasker masker = mock(ErpStockFieldPermissionMasker.class);
        PermissionApi permissionApi = mock(PermissionApi.class);
        ReflectionTestUtils.setField(service, "fieldPermissionMasker", masker);
        ReflectionTestUtils.setField(service, "permissionApi", permissionApi);
        when(masker.getHiddenFieldSet("erp_warehouse_move")).thenReturn(Set.of(
                "col_fromWarehouseName", "col_productCodes", "col_totalCount", "item_productId"));
        when(permissionApi.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.emptyList());

        Object visibility = ReflectionTestUtils.invokeMethod(service, "visibility");
        @SuppressWarnings("unchecked")
        Set<String> hidden = (Set<String>) ReflectionTestUtils.getField(visibility, "hiddenFields");

        assertTrue(hidden.contains("fromWarehouseName"));
        assertTrue(hidden.contains("productCode"));
        assertTrue(hidden.contains("productName"));
        assertTrue(hidden.contains("count"));
        assertFalse(hidden.contains("brand"));
        assertFalse(hidden.contains("vehicleModel"));
    }
}
