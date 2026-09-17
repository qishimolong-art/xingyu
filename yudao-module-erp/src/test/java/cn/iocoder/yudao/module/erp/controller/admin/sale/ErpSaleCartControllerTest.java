package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Mock
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFieldConfigService fieldConfigService;

    @BeforeEach
    public void setUp() {
        lenient().when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());
    }

    // ==================== createSaleCart ====================

    @Test
    public void testCreateSaleCart_paramPassThrough() {
        ErpSaleCartDraftCreateReqVO reqVO = new ErpSaleCartDraftCreateReqVO();
        reqVO.setCustomerId(21L);
        reqVO.setItems(Collections.emptyList());
        when(saleCartService.createSaleCart(any())).thenReturn(101L);

        CommonResult<Long> result = controller.createSaleCart(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(101L, result.getData());
        verify(saleCartService).createSaleCart(argThat(request ->
                Long.valueOf(21L).equals(request.getCustomerId())
                        && request.getItems().isEmpty()));
    }

    @Test
    public void testCreateSaleCart_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod(
                "createSaleCart", ErpSaleCartDraftCreateReqVO.class);
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

    @Test
    public void testUpdateSaleCartDraft_paramPassThrough() {
        ErpSaleCartDraftUpdateReqVO reqVO = new ErpSaleCartDraftUpdateReqVO();
        reqVO.setId(20L);
        reqVO.setItems(Collections.emptyList());

        CommonResult<Boolean> result = controller.updateSaleCartDraft(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).updateSaleCartDraft(argThat(request ->
                Long.valueOf(20L).equals(request.getId()) && request.getItems().isEmpty()));
    }

    @Test
    public void testUpdateSaleCartDraft_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod(
                "updateSaleCartDraft", ErpSaleCartDraftUpdateReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:update"));
    }

    // ==================== submitSaleCart ====================

    @Test
    public void testSubmitSaleCart_paramPassThrough() {
        ErpSaleCartSubmitRespVO respVO = new ErpSaleCartSubmitRespVO();
        respVO.setId(30L);
        respVO.setStatus(20);
        when(saleCartService.submitSaleCart(eq(30L))).thenReturn(respVO);

        CommonResult<ErpSaleCartSubmitRespVO> result = controller.submitSaleCart(30L);

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
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

    @Test
    public void testGetFirstApproveConfig_paramPassThrough() {
        ErpSaleCartFirstApproveConfigRespVO respVO = new ErpSaleCartFirstApproveConfigRespVO();
        respVO.setEnabled(true);
        when(saleCartService.getFirstApproveConfig()).thenReturn(respVO);

        CommonResult<ErpSaleCartFirstApproveConfigRespVO> result = controller.getFirstApproveConfig();

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
        verify(saleCartService).getFirstApproveConfig();
    }

    @Test
    public void testGetFirstApproveConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("getFirstApproveConfig");
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:query"));
        assertTrue(anno.value().contains("erp:sale-cart:first-approve-config"));
    }

    @Test
    public void testUpdateFirstApproveConfig_paramPassThrough() {
        ErpSaleCartFirstApproveConfigSaveReqVO reqVO = new ErpSaleCartFirstApproveConfigSaveReqVO();
        reqVO.setEnabled(true);
        reqVO.setDeptAuthEnabled(false);
        reqVO.setIncludeChildDept(true);

        CommonResult<Boolean> result = controller.updateFirstApproveConfig(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleCartService).updateFirstApproveConfig(eq(reqVO));
    }

    @Test
    public void testUpdateFirstApproveConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleCartController.class.getMethod("updateFirstApproveConfig",
                ErpSaleCartFirstApproveConfigSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:first-approve-config"));
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
        cart.setDeptId(82L);
        when(saleCartService.getSaleCart(eq(80L))).thenReturn(cart);
        when(saleCartService.getSaleCartItemListByCartId(eq(80L))).thenReturn(Collections.emptyList());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());
        when(saleCartService.isFirstApproveRequiredForDept(eq(82L))).thenReturn(false);

        CommonResult<ErpSaleCartRespVO> result = controller.getSaleCart(80L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(80L, result.getData().getId());
        assertEquals(Boolean.FALSE, result.getData().getFirstApproveRequired());
    }

    @Test
    public void testGetSaleCart_itemWarehouseNameFilledByWarehouseId() {
        ErpSaleCartDO cart = new ErpSaleCartDO();
        cart.setId(80L);
        ErpSaleCartItemDO item = new ErpSaleCartItemDO();
        item.setId(1L);
        item.setCartId(80L);
        item.setProductId(10L);
        item.setWarehouseId(20L);
        item.setCount(BigDecimal.ONE);
        when(saleCartService.getSaleCart(eq(80L))).thenReturn(cart);
        when(saleCartService.getSaleCartItemListByCartId(eq(80L))).thenReturn(Collections.singletonList(item));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(20L,
                ErpWarehouseDO.builder().id(20L).name("分发仓").warehouseCode("WH001").build()));

        CommonResult<ErpSaleCartRespVO> result = controller.getSaleCart(80L);

        assertEquals(0, result.getCode());
        assertEquals("分发仓", result.getData().getItems().get(0).getWarehouseName());
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
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleCartRespVO>> result = controller.getSaleCartPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(90L, result.getData().getList().get(0).getId());
    }

    @Test
    public void testGetSaleCartPage_withoutItems_returnsEmptyItems() {
        ErpSaleCartPageReqVO pageReqVO = new ErpSaleCartPageReqVO();
        ErpSaleCartDO cart = new ErpSaleCartDO();
        cart.setId(91L);
        cart.setCustomerId(92L);
        PageResult<ErpSaleCartDO> pageResult = new PageResult<>(singletonList(cart), 1L);
        when(saleCartService.getSaleCartPage(eq(pageReqVO))).thenReturn(pageResult);
        when(saleCartService.getSaleCartItemListByCartIds(any())).thenReturn(Collections.emptyList());
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleCartRespVO>> result = controller.getSaleCartPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getList().get(0).getItems());
        assertTrue(result.getData().getList().get(0).getItems().isEmpty());
    }

    @Test
    public void testGetSaleCartPage_includeItemsFalse_usesLightList() {
        ErpSaleCartPageReqVO pageReqVO = new ErpSaleCartPageReqVO();
        pageReqVO.setIncludeItems(false);
        ErpSaleCartDO cart = new ErpSaleCartDO();
        cart.setId(92L);
        cart.setDeptId(12L);
        PageResult<ErpSaleCartDO> pageResult = new PageResult<>(singletonList(cart), 1L);
        when(saleCartService.getSaleCartPage(eq(pageReqVO))).thenReturn(pageResult);
        when(saleCartItemMapper.selectItemCountMapByCartIds(any()))
                .thenReturn(Collections.singletonMap(92L, 3));
        when(saleCartService.getFirstApproveRequiredMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleCartRespVO>> result = controller.getSaleCartPage(pageReqVO);

        ErpSaleCartRespVO respVO = result.getData().getList().get(0);
        assertEquals(3, respVO.getItemCount());
        assertNull(respVO.getItems());
        verify(saleCartService, never()).getSaleCartItemListByCartIds(any());
        verify(saleCartItemMapper).selectItemCountMapByCartIds(any());
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
                ErpSaleCartPageReqVO.class, String.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-cart:export"));
    }

}
