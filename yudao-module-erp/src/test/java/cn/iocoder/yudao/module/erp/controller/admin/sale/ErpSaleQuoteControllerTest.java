package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSaleQuoteController} 的单元测试
 */
public class ErpSaleQuoteControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleQuoteController controller;

    @Mock
    private ErpSaleQuoteService saleQuoteService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private AdminUserApi adminUserApi;

    // ==================== createSaleQuote ====================

    @Test
    public void testCreateSaleQuote_paramPassThrough() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setCustomerId(1L);
        when(saleQuoteService.createSaleQuote(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createSaleQuote(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(saleQuoteService).createSaleQuote(eq(reqVO));
    }

    @Test
    public void testCreateSaleQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("createSaleQuote", ErpSaleQuoteSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:create"));
    }

    // ==================== updateSaleQuote ====================

    @Test
    public void testUpdateSaleQuote_paramPassThrough() {
        ErpSaleQuoteSaveReqVO reqVO = new ErpSaleQuoteSaveReqVO();
        reqVO.setId(10L);

        CommonResult<Boolean> result = controller.updateSaleQuote(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleQuoteService).updateSaleQuote(eq(reqVO));
    }

    @Test
    public void testUpdateSaleQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("updateSaleQuote", ErpSaleQuoteSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:update"));
    }

    // ==================== approveSaleQuote ====================

    @Test
    public void testApproveSaleQuote_paramPassThrough() {
        when(saleQuoteService.approveSaleQuote(eq(7L))).thenReturn(800L);

        CommonResult<Long> result = controller.approveSaleQuote(7L);

        assertEquals(0, result.getCode());
        assertEquals(800L, result.getData());
        verify(saleQuoteService).approveSaleQuote(eq(7L));
    }

    @Test
    public void testApproveSaleQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("approveSaleQuote", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:approve"));
    }

    // ==================== convertSaleQuoteToCart ====================

    @Test
    public void testConvertSaleQuoteToCart_paramPassThrough() {
        ErpSaleQuoteConvertCartReqVO reqVO = new ErpSaleQuoteConvertCartReqVO();
        reqVO.setQuoteId(33L);
        when(saleQuoteService.convertToCart(any())).thenReturn(900L);

        CommonResult<Long> result = controller.convertSaleQuoteToCart(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(900L, result.getData());
        verify(saleQuoteService).convertToCart(eq(reqVO));
    }

    @Test
    public void testConvertSaleQuoteToCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("convertSaleQuoteToCart", ErpSaleQuoteConvertCartReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:convert-cart"));
    }

    // ==================== deleteSaleQuote ====================

    @Test
    public void testDeleteSaleQuote_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        CommonResult<Boolean> result = controller.deleteSaleQuote(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleQuoteService).deleteSaleQuote(eq(ids));
    }

    @Test
    public void testDeleteSaleQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("deleteSaleQuote", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:delete"));
    }

    // ==================== getSaleQuote ====================

    @Test
    public void testGetSaleQuote_quoteNull_returnsNullData() {
        when(saleQuoteService.getSaleQuote(eq(50L))).thenReturn(null);

        CommonResult<ErpSaleQuoteRespVO> result = controller.getSaleQuote(50L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleQuoteService).getSaleQuote(eq(50L));
    }

    @Test
    public void testGetSaleQuote_quoteExists_returnsRespVO() {
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO();
        quote.setId(50L);
        quote.setCustomerId(60L);
        quote.setStatus(10);
        when(saleQuoteService.getSaleQuote(eq(50L))).thenReturn(quote);
        when(saleQuoteService.getSaleQuoteItemListByQuoteId(eq(50L))).thenReturn(Collections.emptyList());
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<ErpSaleQuoteRespVO> result = controller.getSaleQuote(50L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(50L, result.getData().getId());
    }

    @Test
    public void testGetSaleQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("getSaleQuote", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:query"));
    }

    // ==================== getSaleQuotePage ====================

    @Test
    public void testGetSaleQuotePage_emptyResult() {
        ErpSaleQuotePageReqVO pageReqVO = new ErpSaleQuotePageReqVO();
        when(saleQuoteService.getSaleQuotePage(eq(pageReqVO)))
                .thenReturn(PageResult.empty(0L));

        CommonResult<PageResult<ErpSaleQuoteRespVO>> result = controller.getSaleQuotePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(saleQuoteService).getSaleQuotePage(eq(pageReqVO));
    }

    @Test
    public void testGetSaleQuotePage_withData() {
        ErpSaleQuotePageReqVO pageReqVO = new ErpSaleQuotePageReqVO();
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO();
        quote.setId(70L);
        quote.setCustomerId(71L);
        quote.setStatus(10);
        PageResult<ErpSaleQuoteDO> pageResult = new PageResult<>(singletonList(quote), 1L);
        when(saleQuoteService.getSaleQuotePage(eq(pageReqVO))).thenReturn(pageResult);
        when(saleQuoteService.getSaleQuoteItemListByQuoteIds(any())).thenReturn(Collections.emptyList());
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleQuoteRespVO>> result = controller.getSaleQuotePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        assertEquals(70L, result.getData().getList().get(0).getId());
    }

    @Test
    public void testGetSaleQuotePage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("getSaleQuotePage", ErpSaleQuotePageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:query"));
    }

    // ==================== exportSaleQuoteExcel ====================

    @Test
    public void testExportSaleQuoteExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleQuoteController.class.getMethod("exportSaleQuoteExcel",
                ErpSaleQuotePageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:export"));
    }

}
