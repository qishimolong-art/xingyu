package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ErpPurchaseFieldPermissionMaskerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @Mock
    private PermissionApi permissionApi;

    @Test
    void getHiddenFieldSet_mapsProductPricePermissionsToPurchaseItemFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_purchase_order"))
                .thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Arrays.asList("purchasePrice", "salePrice", "col_wholesalePrice"));

        Set<String> hiddenFields = fieldPermissionMasker.getHiddenFieldSet("erp_purchase_order");

        assertTrue(hiddenFields.contains("item_productPurchasePrice"));
        assertTrue(hiddenFields.contains("item_productPrice"));
        assertTrue(hiddenFields.contains("item_salePrice"));
        assertTrue(hiddenFields.contains("item_wholesalePrice"));
    }

    @Test
    void clearHiddenItemFields_masksMappedProductPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_purchase_order"))
                .thenReturn(Collections.emptyList());
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Arrays.asList("salePrice", "backupPrice1"));
        ErpPurchaseOrderRespVO.Item item = new ErpPurchaseOrderRespVO.Item();
        item.setSalePrice(new BigDecimal("12.00"));
        item.setBackupPrice1(new BigDecimal("10.00"));

        fieldPermissionMasker.clearHiddenItemFields("erp_purchase_order", Collections.singletonList(item));

        assertNull(item.getSalePrice());
        assertNull(item.getBackupPrice1());
    }

}
