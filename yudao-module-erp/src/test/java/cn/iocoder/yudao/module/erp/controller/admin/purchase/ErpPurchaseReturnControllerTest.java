package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.idev.excel.annotation.format.NumberFormat;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnRefundSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseReturnControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseReturnController controller;

    @Mock
    private ErpPurchaseReturnService purchaseReturnService;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpPurchaseFieldPermissionMasker fieldPermissionMasker;

    @BeforeEach
    void setUp() {
        lenient().when(purchaseReturnService.getPurchaseReturn(eq(10L))).thenReturn(new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH-001").setTotalPrice(new BigDecimal("100")));
        lenient().when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsUnrefundedStatus() {
        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getRefundedAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(0), data.getRefundStatus());
        verify(financePaymentItemMapper).selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()));
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsPartialStatusWithAbsoluteRefundAmount() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(new BigDecimal("-40"));

        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getRefundedAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(1), data.getRefundStatus());
    }

    @Test
    void getPurchaseReturnPaymentSummaryReturnsRefundedStatus() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_RETURN.getType()))).thenReturn(new BigDecimal("-100"));

        ErpPurchaseReturnRefundSummaryRespVO data = controller.getPurchaseReturnPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundableAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getRefundedAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getUnrefundedAmount()));
        assertEquals(Integer.valueOf(2), data.getRefundStatus());
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPurchaseReturnService() {
        List<DeptSimpleRespVO> expected = Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L));
        when(purchaseReturnService.getSupplierAvailableDeptSimpleList(100L)).thenReturn(expected);

        List<DeptSimpleRespVO> result = controller.getSupplierAvailableDeptSimpleList(100L).getData();

        assertEquals(expected, result);
        verify(purchaseReturnService).getSupplierAvailableDeptSimpleList(100L);
    }

    @Test
    void getSupplierAvailableDeptSimplePageDelegatesToPurchaseReturnService() {
        PageParam pageParam = new PageParam();
        PageResult<DeptSimpleRespVO> expected = new PageResult<>(Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L)), 1L);
        when(purchaseReturnService.getSupplierAvailableDeptSimplePage(100L, pageParam)).thenReturn(expected);

        PageResult<DeptSimpleRespVO> result = controller
                .getSupplierAvailableDeptSimplePage(100L, pageParam).getData();

        assertEquals(expected, result);
        verify(purchaseReturnService).getSupplierAvailableDeptSimplePage(100L, pageParam);
    }

    @Test
    void getPurchaseReturnExportFieldsIncludesMainAndDetailBusinessFieldsAndHonorsPermissions() {
        when(fieldPermissionMasker.getHiddenFieldSet("erp_purchase_return"))
                .thenReturn(Collections.emptySet());

        List<ErpExportFieldRespVO> fields = controller.getPurchaseReturnExportFields().getData();
        Set<String> names = fields.stream().map(ErpExportFieldRespVO::getField).collect(Collectors.toSet());

        assertTrue(names.containsAll(Arrays.asList(
                "returnMode", "purchaserName", "handlerName", "totalProductPrice", "totalTaxAmount", "mainTaxPercent",
                "refundPrice", "itemDeptName", "stockCount", "inCount", "returnableCount",
                "lastPurchasePrice", "itemTaxPercent", "itemTaxPrice", "factoryCode", "drawingNo")));

        when(fieldPermissionMasker.getHiddenFieldSet("erp_purchase_return"))
                .thenReturn(Collections.singleton("item_warehouseId"));
        Set<String> permittedNames = controller.getPurchaseReturnExportFields().getData().stream()
                .map(ErpExportFieldRespVO::getField).collect(Collectors.toSet());
        assertFalse(permittedNames.contains("warehouseName"));
        assertTrue(permittedNames.contains("itemDeptName"));
    }

    @Test
    void purchaseReturnExportUsesMoneyFormatsWithoutFormattingNaturalNumbers() throws Exception {
        Field amountField = ErpPurchaseReturnExportRespVO.class.getDeclaredField("productPrice");
        Field countField = ErpPurchaseReturnExportRespVO.class.getDeclaredField("itemCount");
        Field stockField = ErpPurchaseReturnExportRespVO.class.getDeclaredField("stockCount");

        assertEquals("0.00####", amountField.getAnnotation(NumberFormat.class).value());
        assertEquals("0.######", countField.getAnnotation(NumberFormat.class).value());
        assertEquals(BigDecimal.class, stockField.getType());
    }

}
