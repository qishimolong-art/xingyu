package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ErpSaleFieldPermissionMaskerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleFieldPermissionMasker masker;

    @Mock
    private PermissionApi permissionApi;
    @Mock
    private ErpCustomerMapper customerMapper;

    @Test
    void getHiddenFieldSet_addsSalePriceFieldsWhenBackupPriceHidden() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_order", null, true, null))
                .thenReturn(List.of("item_productPrice", "item_salePrice", "item_totalProductPrice",
                        "item_totalPrice", "totalProductPrice", "totalPrice", "totalAdjustPrice"));

        Set<String> result = masker.getHiddenFieldSet("erp_sale_order");

        assertTrue(result.contains("item_productPrice"));
        assertTrue(result.contains("item_salePrice"));
        assertTrue(result.contains("item_totalProductPrice"));
        assertTrue(result.contains("item_totalPrice"));
        assertTrue(result.contains("totalProductPrice"));
        assertTrue(result.contains("totalPrice"));
        assertTrue(result.contains("totalAdjustPrice"));
    }

    @Test
    void maskFormWithItems_masksSalePricesWhenPurchasePriceHidden() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_price_adjust", null, true, 6))
                .thenReturn(List.of("totalPrice", "totalAdjustPrice", "item_productPrice",
                        "item_oldPrice", "item_newPrice", "item_adjustPrice"));
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 6;
        document.totalPrice = BigDecimal.TEN;
        document.totalAdjustPrice = BigDecimal.ONE;
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.TEN;
        item.oldPrice = BigDecimal.ONE;
        item.newPrice = BigDecimal.valueOf(2);
        item.adjustPrice = BigDecimal.valueOf(3);
        document.items = List.of(item);

        masker.maskFormWithItems("erp_sale_price_adjust", document);

        assertNull(document.totalPrice);
        assertNull(document.totalAdjustPrice);
        assertNull(item.productPrice);
        assertNull(item.oldPrice);
        assertNull(item.newPrice);
        assertNull(item.adjustPrice);
    }

    @Test
    void maskFormWithItems_keepsSalePricesWhenProductPricesVisible() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_order", null, true, 3))
                .thenReturn(Collections.emptyList());
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        document.totalPrice = BigDecimal.TEN;
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.ONE;
        document.items = List.of(item);

        masker.maskFormWithItems("erp_sale_order", document);

        assertEquals(BigDecimal.TEN, document.totalPrice);
        assertEquals(BigDecimal.ONE, item.productPrice);
    }

    @Test
    void maskFormWithItems_keepsRetailCustomerPricesWhenLastPurchaseHidden() {
        when(permissionApi.getCurrentUserHiddenFields(eq("erp_sale_order"), eq(null), eq(true), eq(3)))
                .thenReturn(Collections.emptyList());
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        document.totalPrice = BigDecimal.TEN;
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.ONE;
        document.items = List.of(item);

        masker.maskFormWithItems("erp_sale_order", document);

        assertEquals(BigDecimal.TEN, document.totalPrice);
        assertEquals(BigDecimal.ONE, item.productPrice);
    }

    @Test
    void maskSaleDetailFormWithItems_keepsPricesButMasksNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_order", null, true, 3))
                .thenReturn(List.of("totalPrice", "item_productPrice", "remark", "item_remark"));
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        document.totalPrice = BigDecimal.TEN;
        document.remark = "内部备注";
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.ONE;
        item.remark = "明细备注";
        document.items = List.of(item);

        masker.maskSaleDetailFormWithItems("erp_sale_order", document);

        assertEquals(BigDecimal.TEN, document.totalPrice);
        assertEquals(BigDecimal.ONE, item.productPrice);
        assertNull(document.remark);
        assertNull(item.remark);
    }

    @Test
    void maskSaleDetailFormsWithItems_keepsPricesButMasksNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_cart", null, true, 3))
                .thenReturn(List.of("totalPrice", "item_productPrice", "remark", "item_remark"));
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        document.totalPrice = BigDecimal.TEN;
        document.remark = "列表备注";
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.ONE;
        item.remark = "列表明细备注";
        document.items = List.of(item);

        masker.maskSaleDetailFormsWithItems("erp_sale_cart", List.of(document));

        assertEquals(BigDecimal.TEN, document.totalPrice);
        assertEquals(BigDecimal.ONE, item.productPrice);
        assertNull(document.remark);
        assertNull(item.remark);
    }

    @Test
    void maskSaleDetailExportRows_keepsPricesButMasksNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_out", null, true, 3))
                .thenReturn(List.of("item_productPrice", "item_remark"));
        TestSaleExportRow row = new TestSaleExportRow();
        row.priceLevel = 3;
        row.itemProductPrice = BigDecimal.TEN;
        row.itemRemark = "导出明细备注";

        masker.maskSaleDetailExportRows("erp_sale_out", List.of(row));

        assertEquals(BigDecimal.TEN, row.itemProductPrice);
        assertNull(row.itemRemark);
    }

    @Test
    void maskSaleDetailSelectRows_keepsPricesButMasksNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_price_adjust", null, true, 3))
                .thenReturn(List.of("select_col_productPrice", "select_col_remark"));
        TestSaleSelectRow row = new TestSaleSelectRow();
        row.priceLevel = 3;
        row.productPrice = BigDecimal.TEN;
        row.remark = "选择弹窗备注";

        masker.maskSaleDetailSelectRows("erp_sale_price_adjust", List.of(row));

        assertEquals(BigDecimal.TEN, row.productPrice);
        assertNull(row.remark);
    }

    @Test
    void clearSaleDetailHiddenItemFields_keepsPricesButMasksNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_quote", null, true, 3))
                .thenReturn(List.of("item_productPrice", "item_remark"));
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        TestSaleItem item = new TestSaleItem();
        item.productPrice = BigDecimal.valueOf(20);
        item.remark = "明细备注";

        masker.clearSaleDetailHiddenItemFields("erp_sale_quote", document, List.of(item));

        assertEquals(BigDecimal.valueOf(20), item.productPrice);
        assertNull(item.remark);
    }

    @Test
    void preserveSaleDetailHiddenItemFields_keepsIncomingPricesButPreservesNonPriceFields() {
        when(permissionApi.getCurrentUserHiddenFields("erp_sale_quote", null, true, 3))
                .thenReturn(List.of("item_productPrice", "item_remark"));
        TestSaleDocument document = new TestSaleDocument();
        document.priceLevel = 3;
        TestSaleItem targetItem = new TestSaleItem();
        targetItem.id = 1L;
        targetItem.productPrice = BigDecimal.valueOf(30);
        targetItem.remark = "新备注";
        TestSaleItem sourceItem = new TestSaleItem();
        sourceItem.id = 1L;
        sourceItem.productPrice = BigDecimal.ZERO;
        sourceItem.remark = "旧备注";

        masker.preserveSaleDetailHiddenItemFields("erp_sale_quote", document,
                List.of(targetItem), List.of(sourceItem));

        assertEquals(BigDecimal.valueOf(30), targetItem.productPrice);
        assertEquals("旧备注", targetItem.remark);
    }

    static class TestSaleDocument {
        Integer priceLevel;
        BigDecimal totalPrice;
        BigDecimal totalAdjustPrice;
        String remark;
        List<TestSaleItem> items;
    }

    static class TestSaleItem {
        Long id;
        BigDecimal productPrice;
        BigDecimal oldPrice;
        BigDecimal newPrice;
        BigDecimal adjustPrice;
        String remark;
    }

    static class TestSaleSelectRow {
        Integer priceLevel;
        BigDecimal productPrice;
        String remark;
    }

    static class TestSaleExportRow {
        Integer priceLevel;
        BigDecimal itemProductPrice;
        String itemRemark;
    }

}
