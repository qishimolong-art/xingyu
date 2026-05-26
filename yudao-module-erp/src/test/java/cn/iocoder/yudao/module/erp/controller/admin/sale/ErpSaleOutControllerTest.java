package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSaleOutController} 的单元测试
 */
public class ErpSaleOutControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOutController controller;

    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;

    // ========== createSaleOut ==========

    @Test
    public void testCreateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        when(saleOutService.createSaleOut(any())).thenReturn(101L);

        CommonResult<Long> result = controller.createSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(101L, result.getData());
        verify(saleOutService).createSaleOut(eq(reqVO));
    }

    @Test
    public void testCreateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("createSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:create"));
    }

    // ========== updateSaleOut ==========

    @Test
    public void testUpdateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setId(22L);

        CommonResult<Boolean> result = controller.updateSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOut(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update"));
    }

    // ========== updateSaleOutStatus ==========

    @Test
    public void testUpdateSaleOutStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSaleOutStatus(22L, 30);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOutStatus(eq(22L), eq(30));
    }

    @Test
    public void testUpdateSaleOutStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOutStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update-status"));
    }

    // ========== deleteSaleOut ==========

    @Test
    public void testDeleteSaleOut_paramPassThrough() {
        List<Long> ids = Arrays.asList(10L, 20L);

        CommonResult<Boolean> result = controller.deleteSaleOut(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).deleteSaleOut(eq(ids));
    }

    @Test
    public void testDeleteSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("deleteSaleOut", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:delete"));
    }

    // ========== getSaleOut ==========

    @Test
    public void testGetSaleOut_nullShortCircuit() {
        when(saleOutService.getSaleOut(eq(1024L))).thenReturn(null);

        CommonResult<ErpSaleOutRespVO> result = controller.getSaleOut(1024L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleOutService).getSaleOut(eq(1024L));
    }

    @Test
    public void testGetSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOut", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== getSaleOutPage ==========

    @Test
    public void testGetSaleOutPage_emptyShortCircuit() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        PageResult<ErpSaleOutDO> emptyPage = new PageResult<>(new ArrayList<>(), 0L);
        when(saleOutService.getSaleOutPage(eq(reqVO))).thenReturn(emptyPage);

        CommonResult<PageResult<ErpSaleOutRespVO>> result = controller.getSaleOutPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(saleOutService).getSaleOutPage(eq(reqVO));
    }

    @Test
    public void testGetSaleOutPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOutPage", ErpSaleOutPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== getReturnableItems ==========

    @Test
    public void testGetReturnableItems_paramPassThrough() {
        List<ErpSaleReturnableItemRespVO> mockItems = Collections.singletonList(new ErpSaleReturnableItemRespVO());
        when(saleOutService.getReturnableItemsByOutId(eq(17386L))).thenReturn(mockItems);

        CommonResult<List<ErpSaleReturnableItemRespVO>> result = controller.getReturnableItems(17386L);

        assertEquals(0, result.getCode());
        assertEquals(mockItems, result.getData());
        verify(saleOutService).getReturnableItemsByOutId(eq(17386L));
    }

    @Test
    public void testGetReturnableItems_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getReturnableItems", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:create"));
    }

    // ========== exportSaleOutExcel ==========

    @Test
    public void testExportSaleOutExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("exportSaleOutExcel",
                ErpSaleOutPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:export"));
    }

}
