package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
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
 * {@link ErpSaleOrderController} 的单元测试
 */
public class ErpSaleOrderControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOrderController controller;

    @Mock
    private ErpSaleOrderService saleOrderService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;

    // ========== createSaleOrder ==========

    @Test
    public void testCreateSaleOrder_paramPassThrough() {
        ErpSaleOrderSaveReqVO reqVO = new ErpSaleOrderSaveReqVO();
        when(saleOrderService.createSaleOrder(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createSaleOrder(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(saleOrderService).createSaleOrder(eq(reqVO));
    }

    @Test
    public void testCreateSaleOrder_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("createSaleOrder", ErpSaleOrderSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:create"));
    }

    // ========== updateSaleOrder ==========

    @Test
    public void testUpdateSaleOrder_paramPassThrough() {
        ErpSaleOrderSaveReqVO reqVO = new ErpSaleOrderSaveReqVO();
        reqVO.setId(11L);

        CommonResult<Boolean> result = controller.updateSaleOrder(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOrderService).updateSaleOrder(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOrder_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("updateSaleOrder", ErpSaleOrderSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update"));
    }

    // ========== updateSaleOrderStatus ==========

    @Test
    public void testUpdateSaleOrderStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSaleOrderStatus(11L, 20);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOrderService).updateSaleOrderStatus(eq(11L), eq(20));
    }

    @Test
    public void testUpdateSaleOrderStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("updateSaleOrderStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update-status"));
    }

    // ========== deleteSaleOrder ==========

    @Test
    public void testDeleteSaleOrder_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        CommonResult<Boolean> result = controller.deleteSaleOrder(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOrderService).deleteSaleOrder(eq(ids));
    }

    @Test
    public void testDeleteSaleOrder_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("deleteSaleOrder", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:delete"));
    }

    // ========== getSaleOrder ==========

    @Test
    public void testGetSaleOrder_nullShortCircuit() {
        when(saleOrderService.getSaleOrder(eq(1024L))).thenReturn(null);

        CommonResult<ErpSaleOrderRespVO> result = controller.getSaleOrder(1024L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleOrderService).getSaleOrder(eq(1024L));
    }

    @Test
    public void testGetSaleOrder_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("getSaleOrder", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== getSaleOrderPage ==========

    @Test
    public void testGetSaleOrderPage_emptyShortCircuit() {
        ErpSaleOrderPageReqVO reqVO = new ErpSaleOrderPageReqVO();
        PageResult<ErpSaleOrderDO> emptyPage = new PageResult<>(new ArrayList<>(), 0L);
        when(saleOrderService.getSaleOrderPage(eq(reqVO))).thenReturn(emptyPage);

        CommonResult<PageResult<ErpSaleOrderRespVO>> result = controller.getSaleOrderPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(saleOrderService).getSaleOrderPage(eq(reqVO));
    }

    @Test
    public void testGetSaleOrderPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("getSaleOrderPage", ErpSaleOrderPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== exportSaleOrderExcel ==========

    @Test
    public void testExportSaleOrderExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("exportSaleOrderExcel",
                ErpSaleOrderPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:export"));
    }

}
