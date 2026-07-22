package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFieldConfigService fieldConfigService;

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
    public void testGetSaleQuote_itemDeptNameFilledByItemDeptId() {
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO();
        quote.setId(50L);
        quote.setDeptId(10L);
        quote.setStatus(10);
        ErpSaleQuoteItemDO item = new ErpSaleQuoteItemDO();
        item.setId(501L);
        item.setQuoteId(50L);
        item.setProductId(100L);
        item.setWarehouseId(200L);
        item.setDeptId(30L);
        item.setCount(BigDecimal.ONE);

        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(100L);
        product.setName("P1");
        product.setCode("P001");
        DeptRespDTO mainDept = new DeptRespDTO();
        mainDept.setId(10L);
        mainDept.setName("Sales Dept");
        DeptRespDTO itemDept = new DeptRespDTO();
        itemDept.setId(30L);
        itemDept.setName("Qionglai Dept");

        when(saleQuoteService.getSaleQuote(eq(50L))).thenReturn(quote);
        when(saleQuoteService.getSaleQuoteItemListByQuoteId(eq(50L))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(100L, product));
        Map<Long, DeptRespDTO> deptMap = new HashMap<>();
        deptMap.put(10L, mainDept);
        deptMap.put(30L, itemDept);
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);

        CommonResult<ErpSaleQuoteRespVO> result = controller.getSaleQuote(50L);

        assertEquals(0, result.getCode());
        assertEquals("Sales Dept", result.getData().getDeptName());
        assertEquals(1, result.getData().getItems().size());
        assertEquals(30L, result.getData().getItems().get(0).getDeptId());
        assertEquals("Qionglai Dept", result.getData().getItems().get(0).getDeptName());
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
                ErpSaleQuotePageReqVO.class, String.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-quote:export"));
    }

    @Test
    public void testBuildSaleQuoteExportList_masterDetailStyle() throws Exception {
        ErpSaleQuoteRespVO quote = new ErpSaleQuoteRespVO();
        quote.setNo("Q-001");
        quote.setCustomerName("客户A");
        quote.setStatus(10);
        quote.setRemark("备注");

        ErpSaleQuoteRespVO.Item item1 = new ErpSaleQuoteRespVO.Item();
        item1.setProductCode("P1");
        item1.setProductName("商品1");
        item1.setCount(java.math.BigDecimal.ONE);
        item1.setProductPrice(new java.math.BigDecimal("10.00"));

        ErpSaleQuoteRespVO.Item item2 = new ErpSaleQuoteRespVO.Item();
        item2.setProductCode("P2");
        item2.setProductName("商品2");
        item2.setCount(new java.math.BigDecimal("2"));
        item2.setProductPrice(new java.math.BigDecimal("20.00"));

        quote.setItems(Arrays.asList(item1, item2));

        Method method = ErpSaleQuoteController.class.getDeclaredMethod("buildSaleQuoteExportList", List.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ErpSaleQuoteExportRespVO> rows =
                (List<ErpSaleQuoteExportRespVO>) method.invoke(controller, Collections.singletonList(quote));

        assertEquals(2, rows.size());
        assertEquals("Q-001", rows.get(0).getNo());
        assertEquals("P1", rows.get(0).getProductCode());
        assertNull(rows.get(1).getNo());
        assertEquals("P2", rows.get(1).getProductCode());
    }

}
