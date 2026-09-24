package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleItemPriceReferenceFiller;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpSaleItemPriceReferenceFiller itemPriceReferenceFiller;
    @Mock
    private ErpFieldConfigService fieldConfigService;
    @Mock
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Mock
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Mock
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;

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
        assertTrue(anno.value().contains("erp:sale-order:create"));
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
        assertTrue(anno.value().contains("erp:sale-order:update"));
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
        assertTrue(anno.value().contains("erp:sale-order:update-status"));
    }

    @Test
    public void testUpdateSaleOrderStatus_rejectsProcessStatus() {
        assertThrows(RuntimeException.class, () -> controller.updateSaleOrderStatus(11L, 10));
        verify(saleOrderService, never()).updateSaleOrderStatus(any(), any());
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
        assertTrue(anno.value().contains("erp:sale-order:delete"));
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
    public void testGetSaleOrder_itemWarehouseNameFilledByWarehouseId() {
        ErpSaleOrderDO order = new ErpSaleOrderDO();
        order.setId(1024L);
        ErpSaleOrderItemDO item = new ErpSaleOrderItemDO();
        item.setId(1L);
        item.setOrderId(1024L);
        item.setProductId(10L);
        item.setWarehouseId(20L);
        item.setCount(BigDecimal.ONE);
        when(saleOrderService.getSaleOrder(eq(1024L))).thenReturn(order);
        when(saleOrderService.getSaleOrderItemListByOrderId(eq(1024L))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(10L,
                new ErpProductRespVO().setId(10L).setName("P1")));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(20L,
                ErpWarehouseDO.builder().id(20L).name("分发仓").warehouseCode("WH001").build()));
        when(stockService.getStockCount(eq(10L))).thenReturn(BigDecimal.TEN);

        CommonResult<ErpSaleOrderRespVO> result = controller.getSaleOrder(1024L);

        assertEquals(0, result.getCode());
        assertEquals("分发仓", result.getData().getItems().get(0).getWarehouseName());
    }

    @Test
    public void testGetSaleOrder_customerNameFilledByCustomerId() {
        ErpSaleOrderDO order = new ErpSaleOrderDO();
        order.setId(1025L);
        order.setCustomerId(30L);
        when(saleOrderService.getSaleOrder(eq(1025L))).thenReturn(order);
        when(saleOrderService.getSaleOrderItemListByOrderId(eq(1025L))).thenReturn(Collections.emptyList());
        when(customerService.getCustomer(eq(30L)))
                .thenReturn(new ErpCustomerDO().setId(30L).setName("客户甲"));

        CommonResult<ErpSaleOrderRespVO> result = controller.getSaleOrder(1025L);

        assertEquals(0, result.getCode());
        assertEquals("客户甲", result.getData().getCustomerName());
        verify(customerService).getCustomer(eq(30L));
    }

    @Test
    public void testGetSaleOrder_costComesFromLinkedSaleOutStockRecordsAndIgnoresFee() {
        ErpSaleOrderDO order = new ErpSaleOrderDO();
        order.setId(1026L);
        order.setFeeAmount(new BigDecimal("30"));
        ErpSaleOrderItemDO item = new ErpSaleOrderItemDO();
        item.setId(101L);
        item.setOrderId(1026L);
        item.setProductId(10L);
        item.setWarehouseId(20L);
        item.setCount(new BigDecimal("3"));
        when(saleOrderService.getSaleOrder(eq(1026L))).thenReturn(order);
        when(saleOrderService.getSaleOrderItemListByOrderId(eq(1026L))).thenReturn(Collections.singletonList(item));
        when(productService.getProductVOMap(any())).thenReturn(Collections.singletonMap(10L,
                new ErpProductRespVO().setId(10L).setName("P1")));
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.singletonMap(20L,
                ErpWarehouseDO.builder().id(20L).name("分发仓").warehouseCode("WH001").build()));
        when(stockService.getStockCount(eq(10L))).thenReturn(BigDecimal.TEN);
        when(saleOutItemMapper.selectList(any())).thenReturn(Collections.singletonList(
                new ErpSaleOutItemDO().setId(201L).setOrderItemId(101L)));
        when(stockRecordMapper.selectList(any())).thenReturn(Arrays.asList(
                new ErpStockRecordDO().setBizItemId(201L)
                        .setCount(new BigDecimal("-1")).setTotalPrice(new BigDecimal("-40")),
                new ErpStockRecordDO().setBizItemId(201L)
                        .setCount(new BigDecimal("-2")).setTotalPrice(new BigDecimal("-80"))));

        ErpSaleOrderRespVO.Item result = controller.getSaleOrder(1026L).getData().getItems().get(0);

        assertEquals(0, new BigDecimal("120").compareTo(result.getSaleCostAmount()));
        assertEquals(0, new BigDecimal("40.00").compareTo(result.getSaleCostPrice()));
    }

    @Test
    public void testGetSaleOrder_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("getSaleOrder", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-order:query"));
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
    public void testGetSaleOrderPage_includeItemsFalse_usesLightList() {
        ErpSaleOrderPageReqVO reqVO = new ErpSaleOrderPageReqVO();
        reqVO.setIncludeItems(false);
        ErpSaleOrderDO order = new ErpSaleOrderDO();
        order.setId(201L);
        PageResult<ErpSaleOrderDO> pageResult = new PageResult<>(Collections.singletonList(order), 1L);
        when(saleOrderService.getSaleOrderPage(eq(reqVO))).thenReturn(pageResult);
        when(saleOrderItemMapper.selectProductNamesMapByOrderIds(any()))
                .thenReturn(Collections.singletonMap(201L, "机油滤芯"));
        when(customerService.getCustomerMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<PageResult<ErpSaleOrderRespVO>> result = controller.getSaleOrderPage(reqVO);

        ErpSaleOrderRespVO respVO = result.getData().getList().get(0);
        assertEquals("机油滤芯", respVO.getProductNames());
        assertNull(respVO.getItems());
        verify(saleOrderService, never()).getSaleOrderItemListByOrderIds(any());
        verify(saleOrderItemMapper).selectProductNamesMapByOrderIds(any());
    }

    @Test
    public void testGetSaleOrderPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("getSaleOrderPage", ErpSaleOrderPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-order:query"));
    }

    // ========== exportSaleOrderExcel ==========

    @Test
    public void testExportSaleOrderExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOrderController.class.getMethod("exportSaleOrderExcel",
                ErpSaleOrderPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-order:export"));
    }

}
