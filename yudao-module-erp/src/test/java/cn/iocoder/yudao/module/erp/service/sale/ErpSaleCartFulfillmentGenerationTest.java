package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryPickTaskMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpSaleCartFulfillmentGenerationTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePickDeliveryServiceImpl service;
    @Mock private ErpSaleCartMapper saleCartMapper;
    @Mock private ErpSaleCartItemMapper saleCartItemMapper;
    @Mock private ErpSalePickDeliveryOrderMapper orderMapper;
    @Mock private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Mock private ErpSalePickDeliveryItemMapper itemMapper;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private ErpStockMoveService stockMoveService;
    @Mock private ErpProductService productService;
    @Mock private ErpCustomerService customerService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUpTenantAndIds() {
        TenantContextHolder.setTenantId(1L);
        lenient().when(productService.getProductVOMap(anyCollection())).thenReturn(Collections.emptyMap());
        lenient().doAnswer(invocation -> {
            invocation.<ErpSalePickDeliveryOrderDO>getArgument(0).setId(100L);
            return 1;
        }).when(orderMapper).insert(any(ErpSalePickDeliveryOrderDO.class));
        AtomicLong taskId = new AtomicLong(200L);
        lenient().doAnswer(invocation -> {
            invocation.<ErpSalePickDeliveryPickTaskDO>getArgument(0).setId(taskId.getAndIncrement());
            return 1;
        }).when(pickTaskMapper).insert(any(ErpSalePickDeliveryPickTaskDO.class));
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void generateForSaleCart_sameDeptMultiWarehouse_createsOneOrderAndTaskPerWarehouse() {
        Long cartId = 1L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("C1").setDeptId(10L);
        ErpSaleCartItemDO first = item(cartId, 11L, 101L, 3);
        ErpSaleCartItemDO second = item(cartId, 12L, 102L, 4);
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartIdForUpdate(cartId)).thenReturn(Arrays.asList(first, second));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Map.of(
                101L, warehouse(101L, 10L), 102L, warehouse(102L, 10L)));
        when(stockMoveService.getTransferOutListBySource(anyInt(), eq(cartId))).thenReturn(Collections.emptyList());

        service.generateForSaleCart(cartId);

        ArgumentCaptor<ErpSalePickDeliveryOrderDO> order = ArgumentCaptor.forClass(ErpSalePickDeliveryOrderDO.class);
        verify(orderMapper).insert(order.capture());
        assertEquals(2, order.getValue().getTotalItemCount());
        assertEquals(cartId, order.getValue().getSourceId());
        verify(pickTaskMapper, times(2)).insert(any(ErpSalePickDeliveryPickTaskDO.class));
        verify(itemMapper, times(2)).insert(argThat((ErpSalePickDeliveryItemDO value) ->
                value.getTransferOutId() == null));
        verify(stockMoveService, never()).getStockMoveItemListByMoveIds(anyCollection());
    }

    @Test
    void generateForSaleCart_mixedWarehouses_combinesDirectAndTransferItems() {
        Long cartId = 2L;
        ErpSaleCartDO cart = new ErpSaleCartDO().setId(cartId).setNo("C2").setDeptId(10L);
        ErpSaleCartItemDO direct = item(cartId, 21L, 201L, 2);
        ErpSaleCartItemDO cross = item(cartId, 22L, 202L, 5);
        ErpStockMoveDO transfer = new ErpStockMoveDO().setId(300L);
        ErpStockMoveItemDO transferItem = new ErpStockMoveItemDO().setId(301L).setMoveId(300L)
                .setProductId(22L).setFromWarehouseId(202L).setCount(new BigDecimal("5"));
        when(saleCartMapper.selectById(cartId)).thenReturn(cart);
        when(saleCartItemMapper.selectListByCartIdForUpdate(cartId)).thenReturn(Arrays.asList(direct, cross));
        when(warehouseService.getWarehouseMap(anyCollection())).thenReturn(Map.of(
                201L, warehouse(201L, 10L), 202L, warehouse(202L, 20L)));
        when(stockMoveService.getTransferOutListBySource(anyInt(), eq(cartId)))
                .thenReturn(Collections.singletonList(transfer));
        when(stockMoveService.getStockMoveItemListByMoveIds(Collections.singleton(300L)))
                .thenReturn(Collections.singletonList(transferItem));

        service.generateForSaleCart(cartId);

        ArgumentCaptor<ErpSalePickDeliveryItemDO> items = ArgumentCaptor.forClass(ErpSalePickDeliveryItemDO.class);
        verify(itemMapper, times(2)).insert(items.capture());
        assertTrue(items.getAllValues().stream().anyMatch(value -> value.getTransferOutId() == null
                && value.getProductId().equals(21L)));
        assertTrue(items.getAllValues().stream().anyMatch(value -> Long.valueOf(300L).equals(value.getTransferOutId())
                && Long.valueOf(301L).equals(value.getTransferOutItemId())));
    }

    @Test
    void generateForSaleCart_existingSourceOrder_isIdempotent() {
        when(orderMapper.selectBySource(anyInt(), eq(3L))).thenReturn(new ErpSalePickDeliveryOrderDO().setId(99L));
        service.generateForSaleCart(3L);
        verifyNoInteractions(saleCartMapper, saleCartItemMapper, pickTaskMapper, itemMapper);
    }

    @Test
    void completeDirectDelivery_publishesCartCompletionEvent() {
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(9L).setSourceType(30).setSourceId(8L);
        when(itemMapper.selectListByOrderId(9L)).thenReturn(Collections.singletonList(
                new ErpSalePickDeliveryItemDO().setOrderId(9L)));

        ReflectionTestUtils.invokeMethod(service, "approveLinkedCartTransferOutsAfterDelivery", order, 88L);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertInstanceOf(ErpSaleCartDeliveryCompletedEvent.class, event.getValue());
        ErpSaleCartDeliveryCompletedEvent completed = (ErpSaleCartDeliveryCompletedEvent) event.getValue();
        assertEquals(8L, completed.getSaleCartId());
        assertEquals(88L, completed.getDeliveryUserId());
        verify(stockMoveService, never()).approveSaleCartTransferOutAfterDelivery(anyLong(), anyLong());
    }

    private ErpSaleCartItemDO item(Long cartId, Long productId, Long warehouseId, int count) {
        return new ErpSaleCartItemDO().setCartId(cartId).setProductId(productId).setWarehouseId(warehouseId)
                .setCount(BigDecimal.valueOf(count)).setPackageQty(1);
    }

    private ErpWarehouseDO warehouse(Long id, Long deptId) {
        return new ErpWarehouseDO().setId(id).setDeptId(deptId).setName("W" + id);
    }

}
