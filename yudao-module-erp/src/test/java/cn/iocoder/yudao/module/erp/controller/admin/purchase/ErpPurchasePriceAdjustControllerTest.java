package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSettlementSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchasePriceAdjustControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchasePriceAdjustController controller;

    @Mock
    private ErpPurchasePriceAdjustService priceAdjustService;
    @Mock
    private ErpFinancePaymentItemMapper financePaymentItemMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private DeptApi deptApi;

    @BeforeEach
    void setUp() {
        lenient().when(priceAdjustService.getPurchasePriceAdjust(eq(10L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(10L).setNo("CGTJ-001").setTotalAdjustPrice(new BigDecimal("100")));
        lenient().when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryReturnsUnwrittenStatus() {
        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getSettlementAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getWrittenOffAmount()));
        assertEquals(0, new BigDecimal("100").compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(0), data.getSettlementStatus());
        verify(financePaymentItemMapper).selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()));
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryReturnsPartialStatusForPositiveAdjustment() {
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("40"));

        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("100").compareTo(data.getSettlementAmount()));
        assertEquals(0, new BigDecimal("40").compareTo(data.getWrittenOffAmount()));
        assertEquals(0, new BigDecimal("60").compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(1), data.getSettlementStatus());
    }

    @Test
    void getPurchasePriceAdjustPaymentSummaryKeepsNegativeAdjustmentDirection() {
        when(priceAdjustService.getPurchasePriceAdjust(eq(10L))).thenReturn(new ErpPurchasePriceAdjustDO()
                .setId(10L).setNo("CGTJ-001").setTotalAdjustPrice(new BigDecimal("-100")));
        when(financePaymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("-100"));

        ErpPurchasePriceAdjustSettlementSummaryRespVO data = controller.getPurchasePriceAdjustPaymentSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("-100").compareTo(data.getSettlementAmount()));
        assertEquals(0, new BigDecimal("-100").compareTo(data.getWrittenOffAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(2), data.getSettlementStatus());
    }

    @Test
    void getSupplierAvailableDeptSimpleListDelegatesToPriceAdjustService() {
        List<DeptSimpleRespVO> expected = Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L));
        when(priceAdjustService.getSupplierAvailableDeptSimpleList(100L)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimpleList(100L).getData());
        verify(priceAdjustService).getSupplierAvailableDeptSimpleList(100L);
    }

    @Test
    void getSupplierAvailableDeptSimplePageDelegatesToPriceAdjustService() {
        PageParam pageParam = new PageParam().setPageNo(1).setPageSize(20);
        PageResult<DeptSimpleRespVO> expected = new PageResult<>(Collections.singletonList(
                new DeptSimpleRespVO(7L, "采购部", 1L)), 1L);
        when(priceAdjustService.getSupplierAvailableDeptSimplePage(100L, pageParam)).thenReturn(expected);

        assertEquals(expected, controller.getSupplierAvailableDeptSimplePage(100L, pageParam).getData());
        verify(priceAdjustService).getSupplierAvailableDeptSimplePage(100L, pageParam);
    }

    @Test
    @SuppressWarnings("unchecked")
    void itemRemarkParticipatesInImportTemplateAndPermissionControlledExport() throws Exception {
        Map<String, String> detailAliases = readStaticField("DETAIL_IMPORT_FIELD_ALIAS_MAP", Map.class);
        Map<String, String> orderAliases = readStaticField("ORDER_IMPORT_FIELD_ALIAS_MAP", Map.class);
        Set<String> orderTemplateFields = readStaticField("ORDER_IMPORT_TEMPLATE_FIELDS", Set.class);
        Map<String, String> exportGroups = readStaticField("EXPORT_FIELD_GROUP_MAP", Map.class);
        Map<String, String> exportPermissions = readStaticField("EXPORT_FIELD_PERMISSION_MAP", Map.class);

        assertEquals("remark", detailAliases.get("item_remark"));
        assertEquals("remark", detailAliases.get("itemRemark"));
        assertEquals("itemRemark", orderAliases.get("item_remark"));
        assertEquals("itemRemark", orderAliases.get("itemRemark"));
        assertTrue(orderTemplateFields.contains("itemRemark"));
        assertEquals("detail", exportGroups.get("itemRemark"));
        assertEquals("item_remark", exportPermissions.get("itemRemark"));
        assertEquals("main", exportGroups.get("originalTotalPrice"));
        assertEquals("main", exportGroups.get("adjustedTotalPrice"));
        assertEquals("detail", exportGroups.get("stockCount"));
        assertEquals("detail", exportGroups.get("lastPurchasePrice"));
        assertEquals("item_oldPrice", exportPermissions.get("originalTotalPrice"));
        assertEquals("item_newPrice", exportPermissions.get("adjustedTotalPrice"));
        assertEquals("item_deptId", exportPermissions.get("itemDeptName"));
        assertEquals("item_stockCount", exportPermissions.get("stockCount"));
        assertEquals("item_lastPurchasePrice", exportPermissions.get("lastPurchasePrice"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void exportUsesOnlyReferencedWarehouseDeptAndStockBatches() throws Exception {
        ErpPurchasePriceAdjustRespVO.Item item = new ErpPurchasePriceAdjustRespVO.Item()
                .setProductId(11L).setWarehouseId(21L).setDeptId(31L)
                .setProductCode("P-11").setCount(new BigDecimal("2"));
        ErpPurchasePriceAdjustRespVO adjust = new ErpPurchasePriceAdjustRespVO()
                .setNo("CGTJ-001").setItems(Collections.singletonList(item));
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(21L).setName("采购仓");
        DeptRespDTO dept = new DeptRespDTO().setId(31L).setName("采购部");
        ErpStockDO stock = new ErpStockDO().setProductId(11L).setWarehouseId(21L)
                .setCount(new BigDecimal("8.5"));
        when(warehouseService.getWarehouseMap(Set.of(21L))).thenReturn(Map.of(21L, warehouse));
        when(deptApi.getDeptMap(Set.of(31L))).thenReturn(Map.of(31L, dept));
        when(stockService.getStockMap(Set.of(11L), Set.of(21L)))
                .thenReturn(Map.of("11_21", stock));

        Method method = ErpPurchasePriceAdjustController.class
                .getDeclaredMethod("buildExportList", List.class);
        method.setAccessible(true);
        List<ErpPurchasePriceAdjustExportRespVO> rows =
                (List<ErpPurchasePriceAdjustExportRespVO>) method.invoke(controller, List.of(adjust));

        assertEquals(1, rows.size());
        assertEquals("采购仓", rows.get(0).getWarehouseName());
        assertEquals("采购部", rows.get(0).getItemDeptName());
        assertEquals(0, new BigDecimal("8.5").compareTo(rows.get(0).getStockCount()));
        verify(warehouseService).getWarehouseMap(Set.of(21L));
        verify(deptApi).getDeptMap(Set.of(31L));
        verify(stockService).getStockMap(Set.of(11L), Set.of(21L));
        verify(warehouseService, never()).getWarehouseListByStatus(0);
    }

    @Test
    void exportMoneyFieldsUseTwoToSixDecimalExcelFormat() throws Exception {
        for (String fieldName : List.of("originalTotalPrice", "adjustedTotalPrice", "totalAdjustPrice",
                "paymentPrice", "oldPrice", "newPrice", "lastPurchasePrice", "adjustPrice")) {
            cn.idev.excel.annotation.format.NumberFormat style =
                    ErpPurchasePriceAdjustExportRespVO.class.getDeclaredField(fieldName)
                            .getAnnotation(cn.idev.excel.annotation.format.NumberFormat.class);
            assertNotNull(style, fieldName);
            assertEquals("0.00####", style.value(), fieldName);
        }
    }

    private static <T> T readStaticField(String fieldName, Class<T> fieldType) throws Exception {
        Field field = ErpPurchasePriceAdjustController.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(null));
    }

}
