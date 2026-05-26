package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
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
 * {@link ErpSaleCartController} 的单元测试
 */
public class ErpSaleCartControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleCartController controller;

    @Mock
    private ErpSaleCartService saleCartService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpProductService productService;

    // ==================== createSaleCart ====================

    @Test
    public void testCreateSaleCart_paramPassThrough() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        when(saleCartService.createSaleCart(any())).thenReturn(101L);

        CommonResult<Long> result = controller.createSaleCart(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(101L, result.getData());
        verify(saleCartService).createSaleCart(eq(reqVO));
    }

    @Test
    public void testCreateSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("createSaleCart", ErpSaleCartSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:create"));
    }

    // ==================== updateSaleCart ====================

    @Test
    public void testUpdateSaleCart_paramPassThrough() {
        ErpSaleCartSaveReqVO reqVO = new ErpSaleCartSaveReqVO();
        reqVO.setId(20L);

        CommonResult<Boolean> result = controller.updateSaleCart(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).updateSaleCart(eq(reqVO));
    }

    @Test
    public void testUpdateSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("updateSaleCart", ErpSaleCartSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:update"));
    }

    // ==================== submitSaleCart ====================

    @Test
    public void testSubmitSaleCart_paramPassThrough() {
        CommonResult<Boolean> result = controller.submitSaleCart(30L);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).submitSaleCart(eq(30L));
    }

    @Test
    public void testSubmitSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("submitSaleCart", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:submit"));
    }

    // ==================== firstApproveSaleCart ====================

    @Test
    public void testFirstApproveSaleCart_paramPassThrough() {
        CommonResult<Boolean> result = controller.firstApproveSaleCart(40L);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).firstApproveSaleCart(eq(40L));
    }

    @Test
    public void testFirstApproveSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("firstApproveSaleCart", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:first-approve"));
    }

    // ==================== finalApproveSaleCart ====================

    @Test
    public void testFinalApproveSaleCart_paramPassThrough() {
        when(saleCartService.finalApproveSaleCart(eq(50L))).thenReturn(Arrays.asList(777L, 778L));

        CommonResult<List<Long>> result = controller.finalApproveSaleCart(50L);

        assertEquals(0, result.getCode());
        assertEquals(Arrays.asList(777L, 778L), result.getData());
        verify(saleCartService).finalApproveSaleCart(eq(50L));
    }

    @Test
    public void testFinalApproveSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("finalApproveSaleCart", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:final-approve"));
    }

    // ==================== rejectSaleCart ====================

    @Test
    public void testRejectSaleCart_paramPassThrough() {
        CommonResult<Boolean> result = controller.rejectSaleCart(60L);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).rejectSaleCart(eq(60L));
    }

    @Test
    public void testRejectSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("rejectSaleCart", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:reject"));
    }

    // ==================== convertSaleCartToQuote ====================

    @Test
    public void testConvertSaleCartToQuote_paramPassThrough() {
        ErpSaleCartConvertQuoteReqVO reqVO = new ErpSaleCartConvertQuoteReqVO();
        reqVO.setCartId(70L);
        when(saleCartService.convertToQuote(any())).thenReturn(888L);

        CommonResult<Long> result = controller.convertSaleCartToQuote(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(888L, result.getData());
        verify(saleCartService).convertToQuote(eq(reqVO));
    }

    @Test
    public void testConvertSaleCartToQuote_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("convertSaleCartToQuote", ErpSaleCartConvertQuoteReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:convert-quote"));
    }

    // ==================== deleteSaleCart ====================

    @Test
    public void testDeleteSaleCart_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L);

        CommonResult<Boolean> result = controller.deleteSaleCart(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).deleteSaleCart(eq(ids));
    }

    @Test
    public void testDeleteSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("deleteSaleCart", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:delete"));
    }

    // ==================== getSaleCart ====================

    @Test
    public void testGetSaleCart_cartNull_returnsNullData() {
        when(saleCartService.getSaleCart(eq(80L))).thenReturn(null);

        CommonResult<ErpSaleCartRespVO> result = controller.getSaleCart(80L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleCartService).getSaleCart(eq(80L));
    }

    @Test
    public void testGetSaleCart_cartExists_returnsRespVO() {
        ErpSaleCartDO cart = new ErpSaleCartDO();
        cart.setId(80L);
        cart.setCustomerId(81L);
        when(saleCartService.getSaleCart(eq(80L))).thenReturn(cart);
        when(saleCartService.getSaleCartItemListByCartId(eq(80L))).thenReturn(Collections.emptyList());
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<ErpSaleCartRespVO> result = controller.getSaleCart(80L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(80L, result.getData().getId());
    }

    @Test
    public void testGetSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("getSaleCart", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:query"));
    }

    // ==================== getSaleCartPage ====================

    @Test
    public void testGetSaleCartPage_emptyResult() {
        ErpSaleCartPageReqVO pageReqVO = new ErpSaleCartPageReqVO();
        when(saleCartService.getSaleCartPage(eq(pageReqVO)))
                .thenReturn(PageResult.empty(0L));

        CommonResult<PageResult<ErpSaleCartRespVO>> result = controller.getSaleCartPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(0L, result.getData().getTotal());
        verify(saleCartService).getSaleCartPage(eq(pageReqVO));
    }

    @Test
    public void testGetSaleCartPage_withData() {
        ErpSaleCartPageReqVO pageReqVO = new ErpSaleCartPageReqVO();
        ErpSaleCartDO cart = new ErpSaleCartDO();
        cart.setId(90L);
        cart.setCustomerId(91L);
        PageResult<ErpSaleCartDO> pageResult = new PageResult<>(singletonList(cart), 1L);
        when(saleCartService.getSaleCartPage(eq(pageReqVO))).thenReturn(pageResult);
        when(saleCartService.getSaleCartItemListByCartIds(any())).thenReturn(Collections.emptyList());
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleCartRespVO>> result = controller.getSaleCartPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(90L, result.getData().getList().get(0).getId());
    }

    @Test
    public void testGetSaleCartPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("getSaleCartPage", ErpSaleCartPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:query"));
    }

    // ==================== exportSaleCartExcel ====================

    @Test
    public void testExportSaleCartExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("exportSaleCartExcel",
                ErpSaleCartPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:export"));
    }

}
