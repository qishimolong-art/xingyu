package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void masksAndPreservesPurchasePriceAdjustItemRemark() {
        when(permissionApi.getCurrentUserHiddenFields("erp_purchase_price_adjust"))
                .thenReturn(Collections.singletonList("item_remark"));
        when(permissionApi.getCurrentUserHiddenFields("erp_product"))
                .thenReturn(Collections.emptyList());
        ErpPurchasePriceAdjustRespVO.Item responseItem = new ErpPurchasePriceAdjustRespVO.Item();
        responseItem.setRemark("隐藏备注");

        fieldPermissionMasker.clearHiddenItemFields("erp_purchase_price_adjust",
                Collections.singletonList(responseItem));

        assertNull(responseItem.getRemark());

        ErpPurchasePriceAdjustSaveReqVO.Item targetItem = new ErpPurchasePriceAdjustSaveReqVO.Item();
        targetItem.setId(10L);
        ErpPurchasePriceAdjustItemDO sourceItem = new ErpPurchasePriceAdjustItemDO();
        sourceItem.setId(10L);
        sourceItem.setRemark("数据库原备注");

        fieldPermissionMasker.preserveHiddenItemFields("erp_purchase_price_adjust",
                Collections.singletonList(targetItem), Collections.singletonList(sourceItem));

        assertEquals("数据库原备注", targetItem.getRemark());
    }

}
