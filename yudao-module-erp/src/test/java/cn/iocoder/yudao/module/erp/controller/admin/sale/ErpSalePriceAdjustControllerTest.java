package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSettlementSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustItemMapper;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSalePriceAdjustController} 的单元测试
 */
public class ErpSalePriceAdjustControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePriceAdjustController controller;

    @Mock
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpFinanceReceiptItemMapper financeReceiptItemMapper;
    @Mock
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpFieldConfigService fieldConfigService;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Mock
    private ErpSalePriceAdjustItemMapper salePriceAdjustItemMapper;

    // ==================== createSalePriceAdjust ====================

    @Test
    public void testCreateSalePriceAdjust_paramPassThrough() {
        ErpSalePriceAdjustSaveReqVO reqVO = new ErpSalePriceAdjustSaveReqVO();
        reqVO.setCustomerId(1L);
        when(salePriceAdjustService.createSalePriceAdjust(any())).thenReturn(200L);

        CommonResult<Long> result = controller.createSalePriceAdjust(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(200L, result.getData());
        verify(salePriceAdjustService).createSalePriceAdjust(eq(reqVO));
    }

    @Test
    public void testCreateSalePriceAdjust_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("createSalePriceAdjust", ErpSalePriceAdjustSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:create"));
    }

    // ==================== updateSalePriceAdjust ====================

    @Test
    public void testUpdateSalePriceAdjust_paramPassThrough() {
        ErpSalePriceAdjustSaveReqVO reqVO = new ErpSalePriceAdjustSaveReqVO();
        reqVO.setId(10L);

        CommonResult<Boolean> result = controller.updateSalePriceAdjust(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(salePriceAdjustService).updateSalePriceAdjust(eq(reqVO));
    }

    @Test
    public void testUpdateSalePriceAdjust_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("updateSalePriceAdjust", ErpSalePriceAdjustSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:update"));
    }

    // ==================== updateSalePriceAdjustStatus ====================

    @Test
    public void testUpdateSalePriceAdjustStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSalePriceAdjustStatus(15L, 20);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(salePriceAdjustService).updateSalePriceAdjustStatus(eq(15L), eq(20));
    }

    @Test
    public void testUpdateSalePriceAdjustStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("updateSalePriceAdjustStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:update-status"));
    }

    // ==================== deleteSalePriceAdjust ====================

    @Test
    public void testDeleteSalePriceAdjust_paramPassThrough() {
        List<Long> ids = Arrays.asList(5L, 6L);

        CommonResult<Boolean> result = controller.deleteSalePriceAdjust(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(salePriceAdjustService).deleteSalePriceAdjust(eq(ids));
    }

    @Test
    public void testDeleteSalePriceAdjust_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("deleteSalePriceAdjust", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:delete"));
    }

    // ==================== getSalePriceAdjust ====================

    @Test
    public void testGetSalePriceAdjust_adjustNull_returnsNullData() {
        when(salePriceAdjustService.getSalePriceAdjust(eq(100L))).thenReturn(null);

        CommonResult<ErpSalePriceAdjustRespVO> result = controller.getSalePriceAdjust(100L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(salePriceAdjustService).getSalePriceAdjust(eq(100L));
    }

    @Test
    public void testGetSalePriceAdjust_adjustExists_returnsRespVO() {
        ErpSalePriceAdjustDO adjust = new ErpSalePriceAdjustDO();
        adjust.setId(100L);
        adjust.setCustomerId(null);
        adjust.setDeptId(null);
        adjust.setAdjustUserId(null);
        adjust.setCreator(null);
        when(salePriceAdjustService.getSalePriceAdjust(eq(100L))).thenReturn(adjust);
        when(salePriceAdjustService.getSalePriceAdjustItemListByAdjustId(eq(100L))).thenReturn(Collections.emptyList());

        CommonResult<ErpSalePriceAdjustRespVO> result = controller.getSalePriceAdjust(100L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(100L, result.getData().getId());
    }

    @Test
    public void testGetSalePriceAdjust_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("getSalePriceAdjust", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:query"));
    }

    @Test
    public void testGetSalePriceAdjustSettlementSummaryReturnsUnsignedStatusByAbsComparison() {
        when(salePriceAdjustService.getSalePriceAdjust(eq(10L))).thenReturn(new ErpSalePriceAdjustDO()
                .setId(10L).setTotalAdjustPrice(new BigDecimal("100")));
        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType()))).thenReturn(BigDecimal.ZERO);

        ErpSalePriceAdjustSettlementSummaryRespVO none =
                controller.getSalePriceAdjustSettlementSummary(10L).getData();

        assertNotNull(none);
        assertEquals(0, new BigDecimal("100").compareTo(none.getSettlementAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(none.getWrittenOffAmount()));
        assertEquals(Integer.valueOf(0), none.getSettlementStatus());

        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("40"));

        ErpSalePriceAdjustSettlementSummaryRespVO partial =
                controller.getSalePriceAdjustSettlementSummary(10L).getData();

        assertNotNull(partial);
        assertEquals(0, new BigDecimal("60").compareTo(partial.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(1), partial.getSettlementStatus());

        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("100"));

        ErpSalePriceAdjustSettlementSummaryRespVO writtenOff =
                controller.getSalePriceAdjustSettlementSummary(10L).getData();

        assertNotNull(writtenOff);
        assertEquals(0, BigDecimal.ZERO.compareTo(writtenOff.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(2), writtenOff.getSettlementStatus());
    }

    @Test
    public void testGetSalePriceAdjustSettlementSummaryPreservesNegativeAmounts() {
        when(salePriceAdjustService.getSalePriceAdjust(eq(10L))).thenReturn(new ErpSalePriceAdjustDO()
                .setId(10L).setTotalAdjustPrice(new BigDecimal("-100")));
        when(financeReceiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(
                eq(10L), eq(ErpBizTypeEnum.SALE_PRICE_ADJUST.getType()))).thenReturn(new BigDecimal("-40"));

        ErpSalePriceAdjustSettlementSummaryRespVO data =
                controller.getSalePriceAdjustSettlementSummary(10L).getData();

        assertNotNull(data);
        assertEquals(0, new BigDecimal("-100").compareTo(data.getSettlementAmount()));
        assertEquals(0, new BigDecimal("-40").compareTo(data.getWrittenOffAmount()));
        assertEquals(0, new BigDecimal("-60").compareTo(data.getUnwrittenOffAmount()));
        assertEquals(Integer.valueOf(1), data.getSettlementStatus());
    }

    // ==================== getSalePriceAdjustPage ====================

    @Test
    public void testGetSalePriceAdjustPage_emptyResult() {
        ErpSalePriceAdjustPageReqVO pageReqVO = new ErpSalePriceAdjustPageReqVO();
        when(salePriceAdjustService.getSalePriceAdjustPage(eq(pageReqVO)))
                .thenReturn(PageResult.empty(0L));

        CommonResult<PageResult<ErpSalePriceAdjustRespVO>> result = controller.getSalePriceAdjustPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(0L, result.getData().getTotal());
        verify(salePriceAdjustService).getSalePriceAdjustPage(eq(pageReqVO));
    }

    @Test
    public void testGetSalePriceAdjustPage_withData() {
        ErpSalePriceAdjustPageReqVO pageReqVO = new ErpSalePriceAdjustPageReqVO();
        ErpSalePriceAdjustDO adjust = new ErpSalePriceAdjustDO();
        adjust.setId(300L);
        adjust.setCustomerId(301L);
        adjust.setDeptId(null);
        adjust.setAdjustUserId(null);
        adjust.setCreator(null);
        PageResult<ErpSalePriceAdjustDO> pageResult = new PageResult<>(singletonList(adjust), 1L);
        when(salePriceAdjustService.getSalePriceAdjustPage(eq(pageReqVO))).thenReturn(pageResult);
        when(salePriceAdjustService.getSalePriceAdjustItemListByAdjustIds(any())).thenReturn(Collections.emptyList());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSalePriceAdjustRespVO>> result = controller.getSalePriceAdjustPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(300L, result.getData().getList().get(0).getId());
    }

    @Test
    public void testGetSalePriceAdjustPage_includeItemsFalse_usesLightList() {
        ErpSalePriceAdjustPageReqVO pageReqVO = new ErpSalePriceAdjustPageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpSalePriceAdjustDO adjust = new ErpSalePriceAdjustDO();
        adjust.setId(301L);
        adjust.setCustomerId(302L);
        ErpSalePriceAdjustItemDO item = new ErpSalePriceAdjustItemDO()
                .setAdjustId(301L)
                .setSaleOutNo("XSCK001")
                .setOutCount(new BigDecimal("2"))
                .setOldPrice(new BigDecimal("10"))
                .setNewPrice(new BigDecimal("12"));
        when(salePriceAdjustService.getSalePriceAdjustPage(eq(pageReqVO)))
                .thenReturn(new PageResult<>(singletonList(adjust), 1L));
        when(salePriceAdjustItemMapper.selectSummaryListByAdjustIds(any()))
                .thenReturn(singletonList(item));
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSalePriceAdjustRespVO>> result = controller.getSalePriceAdjustPage(pageReqVO);

        ErpSalePriceAdjustRespVO respVO = result.getData().getList().get(0);
        assertEquals("XSCK001", respVO.getSourceNo());
        assertEquals(0, new BigDecimal("20").compareTo(respVO.getTotalOriginalPrice()));
        assertEquals(0, new BigDecimal("24").compareTo(respVO.getTotalAdjustedPrice()));
        assertNull(respVO.getItems());
        verify(salePriceAdjustService, never()).getSalePriceAdjustItemListByAdjustIds(any());
        verify(salePriceAdjustItemMapper).selectSummaryListByAdjustIds(any());
    }

    @Test
    public void testGetSalePriceAdjustPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("getSalePriceAdjustPage", ErpSalePriceAdjustPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:query"));
    }

    // ==================== getAdjustableItemsByCustomerId ====================

    @Test
    public void testGetAdjustableItemsByCustomerId_paramPassThrough() {
        List<ErpSaleOutItemForAdjustRespVO> items = Collections.emptyList();
        when(salePriceAdjustService.getAdjustableItemsByCustomerId(eq(500L), eq(600L), eq(Boolean.FALSE))).thenReturn(items);

        CommonResult<List<ErpSaleOutItemForAdjustRespVO>> result =
                controller.getAdjustableItemsByCustomerId(500L, 600L, Boolean.FALSE);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        verify(salePriceAdjustService).getAdjustableItemsByCustomerId(eq(500L), eq(600L), eq(Boolean.FALSE));
    }

    @Test
    public void testGetAdjustableItemsByCustomerId_saleOutIdNull() {
        List<ErpSaleOutItemForAdjustRespVO> items = Collections.emptyList();
        when(salePriceAdjustService.getAdjustableItemsByCustomerId(eq(500L), eq(null), eq(null))).thenReturn(items);

        CommonResult<List<ErpSaleOutItemForAdjustRespVO>> result =
                controller.getAdjustableItemsByCustomerId(500L, null, null);

        assertEquals(0, result.getCode());
        verify(salePriceAdjustService).getAdjustableItemsByCustomerId(eq(500L), eq(null), eq(null));
    }

    @Test
    public void testGetAdjustableItemsByCustomerId_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSalePriceAdjustController.class.getMethod("getAdjustableItemsByCustomerId",
                Long.class, Long.class, Boolean.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-price-adjust:query"));
    }

}
