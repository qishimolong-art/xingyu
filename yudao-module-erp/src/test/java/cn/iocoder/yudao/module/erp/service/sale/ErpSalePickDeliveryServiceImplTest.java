package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutExpressFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryOrderRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickTaskRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitFileDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickQuantityAggregateDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliveryPickTaskMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitFileMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.ErpSalePickDeliverySubmitItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.notification.ErpSalePickTaskCreatedEvent;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CART_FULFILLMENT_TASK_STARTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_RETURN_PICK_ALREADY_STARTED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_PICK_ALREADY_STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpSalePickDeliveryServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSalePickDeliveryServiceImpl service;

    @Mock
    private AdminUserApi adminUserApi;

    @Mock
    private ErpSalePickDeliverySubmitMapper submitMapper;
    @Mock
    private ErpSalePickDeliveryItemMapper itemMapper;
    @Mock
    private ErpSalePickDeliveryOrderMapper orderMapper;
    @Mock
    private ErpSalePickDeliverySubmitItemMapper submitItemMapper;
    @Mock
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Mock
    private ErpSalePickDeliverySubmitFileMapper submitFileMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Mock
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void getPickPage_shouldExposeStructuredSourceNames() {
        ErpSalePickPageReqVO reqVO = new ErpSalePickPageReqVO();
        List<ErpSalePickDeliveryPickTaskDO> tasks = List.of(
                new ErpSalePickDeliveryPickTaskDO().setId(1L).setSaleOutNo("XS001")
                        .setTotalItemCount(1).setPickedItemCount(0),
                new ErpSalePickDeliveryPickTaskDO().setId(2L)
                        .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceNo("XSST001")
                        .setTotalItemCount(1).setPickedItemCount(0),
                new ErpSalePickDeliveryPickTaskDO().setId(3L)
                        .setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType()).setSourceNo("XSTH001")
                        .setTotalItemCount(1).setPickedItemCount(0),
                new ErpSalePickDeliveryPickTaskDO().setId(4L)
                        .setSourceType(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType()).setSourceNo("CKYH001")
                        .setTotalItemCount(1).setPickedItemCount(0),
                new ErpSalePickDeliveryPickTaskDO().setId(5L)
                        .setSourceType(ErpSaleBizSourceTypeEnum.STOCK_IN.getType()).setSourceNo("QTRK001")
                        .setTotalItemCount(1).setPickedItemCount(0),
                new ErpSalePickDeliveryPickTaskDO().setId(6L)
                        .setSourceType(ErpSaleBizSourceTypeEnum.STOCK_OUT.getType()).setSourceNo("QTCK001")
                        .setTotalItemCount(1).setPickedItemCount(0));
        when(pickTaskMapper.selectPage(reqVO)).thenReturn(new PageResult<>(tasks, 6L));

        PageResult<ErpSalePickTaskRespVO> result = service.getPickPage(reqVO, false);

        assertEquals(List.of("销售单", "销售手推车", "销售退货", "仓库移货", "其它入库", "其它出库"),
                result.getList().stream().map(ErpSalePickTaskRespVO::getSourceTypeName)
                        .collect(java.util.stream.Collectors.toList()));
        assertEquals(List.of("XS001", "XSST001", "XSTH001", "CKYH001", "QTRK001", "QTCK001"),
                result.getList().stream().map(ErpSalePickTaskRespVO::getDisplayNo)
                        .collect(java.util.stream.Collectors.toList()));
    }

    @Test
    void generateForWarehouseMove_shouldUseDestinationWarehouseAndShelf() {
        ErpWarehouseMoveDO warehouseMove = new ErpWarehouseMoveDO().setId(82L).setNo("CKYH001")
                .setDeptId(10L).setFromWarehouseId(1L).setToWarehouseId(2L)
                .setStatus(ErpWarehouseMoveStatusEnum.PICKING.getStatus());
        ErpWarehouseMoveItemDO moveItem = new ErpWarehouseMoveItemDO().setId(8201L).setMoveId(82L)
                .setFromWarehouseId(1L).setToWarehouseId(2L).setProductId(1001L)
                .setCount(new BigDecimal("3")).setFromShelf("A-01").setToShelf("B-02");
        when(warehouseMoveMapper.selectByIdForUpdate(82L)).thenReturn(warehouseMove);
        when(warehouseMoveItemMapper.selectListByMoveIdForUpdate(82L)).thenReturn(List.of(moveItem));
        when(warehouseService.getWarehouse(2L)).thenReturn(new ErpWarehouseDO().setId(2L).setName("调入仓"));
        when(productService.getProductVOMap(Set.of(1001L))).thenReturn(Map.of(1001L,
                new ErpProductRespVO().setId(1001L).setName("测试商品")));
        doAnswer(invocation -> {
            ((ErpSalePickDeliveryPickTaskDO) invocation.getArgument(0)).setId(9001L);
            return 1;
        }).when(pickTaskMapper).insert(any(ErpSalePickDeliveryPickTaskDO.class));
        TenantContextHolder.setTenantId(1L);
        try {
            service.generateForWarehouseMove(82L);
        } finally {
            TenantContextHolder.clear();
        }

        verify(warehouseService).getWarehouse(2L);
        verify(pickTaskMapper).insert((ErpSalePickDeliveryPickTaskDO) argThat((ErpSalePickDeliveryPickTaskDO task) ->
                Long.valueOf(2L).equals(task.getWarehouseId()) && "调入仓".equals(task.getWarehouseName())));
        verify(itemMapper).insert((ErpSalePickDeliveryItemDO) argThat((ErpSalePickDeliveryItemDO item) ->
                Long.valueOf(2L).equals(item.getWarehouseId())
                && "调入仓".equals(item.getWarehouseName()) && "B-02".equals(item.getWarehousePosition())
                && Long.valueOf(8201L).equals(item.getWarehouseMoveItemId())));
        verify(eventPublisher).publishEvent((Object) argThat(event -> event instanceof ErpSalePickTaskCreatedEvent
                && Long.valueOf(1L).equals(((ErpSalePickTaskCreatedEvent) event).getTenantId())
                && Long.valueOf(9001L).equals(((ErpSalePickTaskCreatedEvent) event).getTaskId())));
    }

    @Test
    void generateCompletedForWarehouseMove_shouldPersistCompletedPickAuditWithoutPendingNotification() {
        ErpWarehouseMoveDO warehouseMove = new ErpWarehouseMoveDO().setId(83L).setNo("CKYH002")
                .setDeptId(10L).setFromWarehouseId(1L).setToWarehouseId(2L)
                .setStatus(ErpWarehouseMoveStatusEnum.APPROVE.getStatus());
        ErpWarehouseMoveItemDO moveItem = new ErpWarehouseMoveItemDO().setId(8301L).setMoveId(83L)
                .setFromWarehouseId(1L).setToWarehouseId(2L).setProductId(1001L)
                .setCount(new BigDecimal("5")).setToShelf("B-03");
        when(warehouseMoveMapper.selectByIdForUpdate(83L)).thenReturn(warehouseMove);
        when(warehouseMoveItemMapper.selectListByMoveIdForUpdate(83L)).thenReturn(List.of(moveItem));
        when(warehouseService.getWarehouse(2L)).thenReturn(new ErpWarehouseDO().setId(2L).setName("调入仓"));
        when(productService.getProductVOMap(Set.of(1001L))).thenReturn(Map.of(1001L,
                new ErpProductRespVO().setId(1001L).setCode("P1001").setName("测试商品")));
        doAnswer(invocation -> {
            ((ErpSalePickDeliveryOrderDO) invocation.getArgument(0)).setId(9100L);
            return 1;
        }).when(orderMapper).insert(any(ErpSalePickDeliveryOrderDO.class));
        doAnswer(invocation -> {
            ((ErpSalePickDeliveryPickTaskDO) invocation.getArgument(0)).setId(9101L);
            return 1;
        }).when(pickTaskMapper).insert(any(ErpSalePickDeliveryPickTaskDO.class));
        doAnswer(invocation -> {
            ((ErpSalePickDeliveryItemDO) invocation.getArgument(0)).setId(9102L);
            return 1;
        }).when(itemMapper).insert(any(ErpSalePickDeliveryItemDO.class));
        doAnswer(invocation -> {
            ((ErpSalePickDeliverySubmitDO) invocation.getArgument(0)).setId(9103L);
            return 1;
        }).when(submitMapper).insert(any(ErpSalePickDeliverySubmitDO.class));
        TenantContextHolder.setTenantId(1L);
        try {
            service.generateCompletedForWarehouseMove(83L, 99L);
        } finally {
            TenantContextHolder.clear();
        }

        ArgumentCaptor<ErpSalePickDeliveryOrderDO> orderCaptor =
                ArgumentCaptor.forClass(ErpSalePickDeliveryOrderDO.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertEquals(ErpSalePickStatusEnum.DONE.getStatus(), orderCaptor.getValue().getPickStatus());
        assertEquals(1, orderCaptor.getValue().getPickedItemCount());
        ArgumentCaptor<ErpSalePickDeliveryPickTaskDO> taskCaptor =
                ArgumentCaptor.forClass(ErpSalePickDeliveryPickTaskDO.class);
        verify(pickTaskMapper).insert(taskCaptor.capture());
        assertEquals(ErpSalePickStatusEnum.DONE.getStatus(), taskCaptor.getValue().getStatus());
        assertEquals(1, taskCaptor.getValue().getPickedItemCount());
        verify(itemMapper).insert((ErpSalePickDeliveryItemDO) argThat((ErpSalePickDeliveryItemDO item) ->
                ErpSalePickStatusEnum.DONE.getStatus().equals(item.getPickStatus())
                        && new BigDecimal("5").compareTo(item.getPickedCount()) == 0
                        && Long.valueOf(99L).equals(item.getPickUserId())
                        && Long.valueOf(8301L).equals(item.getWarehouseMoveItemId())));
        verify(submitMapper).insert((ErpSalePickDeliverySubmitDO) argThat((ErpSalePickDeliverySubmitDO submit) ->
                Long.valueOf(9100L).equals(submit.getOrderId())
                        && Long.valueOf(9101L).equals(submit.getPickTaskId())
                        && Integer.valueOf(10).equals(submit.getType())
                        && Long.valueOf(99L).equals(submit.getSubmitUserId())
                        && Boolean.TRUE.equals(submit.getQuantityDetails())));
        verify(submitItemMapper).insert((ErpSalePickDeliverySubmitItemDO) argThat(
                (ErpSalePickDeliverySubmitItemDO item) -> Long.valueOf(9103L).equals(item.getSubmitId())
                        && Long.valueOf(9102L).equals(item.getItemId())
                        && new BigDecimal("5").compareTo(item.getQuantity()) == 0));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void rollbackUnstartedSaleCartFulfillment_noOrder_noop() {
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L))
                .thenReturn(null);

        service.rollbackUnstartedSaleCartFulfillment(72L);

        verify(orderMapper).selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L);
        verifyNoInteractions(submitMapper, pickTaskMapper, itemMapper);
    }

    @Test
    void rollbackUnstartedSaleCartFulfillment_unstarted_deletesChildrenBeforeOrder() {
        ErpSalePickDeliveryOrderDO order = unstartedOrder(88L, 72L);
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setId(89L).setOrderId(88L)
                .setStatus(ErpSalePickStatusEnum.WAITING.getStatus()).setPickedItemCount(0);
        ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                .setId(90L).setOrderId(88L).setPickTaskId(89L)
                .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(88L)).thenReturn(List.of(task));
        when(itemMapper.selectListByOrderId(88L)).thenReturn(List.of(item));

        service.rollbackUnstartedSaleCartFulfillment(72L);

        InOrder deletionOrder = inOrder(itemMapper, pickTaskMapper, orderMapper);
        deletionOrder.verify(itemMapper).deleteByOrderId(88L);
        deletionOrder.verify(pickTaskMapper).deleteByOrderId(88L);
        deletionOrder.verify(orderMapper).deleteById(88L);
    }

    @Test
    void rollbackUnstartedSaleCartFulfillment_submitExists_rejectedWithoutDelete() {
        ErpSalePickDeliveryOrderDO order = unstartedOrder(88L, 72L);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(88L))
                .thenReturn(List.of(new ErpSalePickDeliverySubmitDO().setId(91L).setOrderId(88L)));
        when(pickTaskMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(88L)).thenReturn(List.of());

        assertServiceException(() -> service.rollbackUnstartedSaleCartFulfillment(72L),
                SALE_CART_FULFILLMENT_TASK_STARTED);

        verify(itemMapper, never()).deleteByOrderId(anyLong());
        verify(pickTaskMapper, never()).deleteByOrderId(anyLong());
        verify(orderMapper, never()).deleteById(anyLong());
    }

    @Test
    void rollbackUnstartedSaleCartFulfillment_hiddenItemProgress_rejectedWithoutDelete() {
        ErpSalePickDeliveryOrderDO order = unstartedOrder(88L, 72L);
        ErpSalePickDeliveryItemDO progressedItem = new ErpSalePickDeliveryItemDO()
                .setId(90L).setOrderId(88L)
                .setPickedCount(BigDecimal.ONE).setDeliveredCount(BigDecimal.ZERO)
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(88L)).thenReturn(List.of(progressedItem));

        assertServiceException(() -> service.rollbackUnstartedSaleCartFulfillment(72L),
                SALE_CART_FULFILLMENT_TASK_STARTED);

        verify(itemMapper, never()).deleteByOrderId(anyLong());
        verify(pickTaskMapper, never()).deleteByOrderId(anyLong());
        verify(orderMapper, never()).deleteById(anyLong());
    }

    @Test
    void rollbackUnstartedSaleCartFulfillment_hiddenTaskProgress_rejectedWithoutDelete() {
        ErpSalePickDeliveryOrderDO order = unstartedOrder(88L, 72L);
        ErpSalePickDeliveryPickTaskDO progressedTask = new ErpSalePickDeliveryPickTaskDO()
                .setId(89L).setOrderId(88L)
                .setStatus(ErpSalePickStatusEnum.PARTIAL.getStatus()).setPickedItemCount(1);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(88L)).thenReturn(List.of(progressedTask));
        when(itemMapper.selectListByOrderId(88L)).thenReturn(List.of());

        assertServiceException(() -> service.rollbackUnstartedSaleCartFulfillment(72L),
                SALE_CART_FULFILLMENT_TASK_STARTED);

        verify(itemMapper, never()).deleteByOrderId(anyLong());
        verify(pickTaskMapper, never()).deleteByOrderId(anyLong());
        verify(orderMapper, never()).deleteById(anyLong());
    }

    @Test
    void rollbackUnstartedSaleReturnFulfillment_unstarted_deletesOnlyReturnWork() {
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(188L)
                .setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType()).setSourceId(72L)
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setPickedItemCount(0).setDeliveredItemCount(0);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(188L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(188L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(188L)).thenReturn(List.of());

        service.rollbackUnstartedSaleReturnFulfillment(72L);

        InOrder deletionOrder = inOrder(itemMapper, pickTaskMapper, orderMapper);
        deletionOrder.verify(itemMapper).deleteByOrderId(188L);
        deletionOrder.verify(pickTaskMapper).deleteByOrderId(188L);
        deletionOrder.verify(orderMapper).deleteById(188L);
    }

    @Test
    void rollbackUnstartedSaleReturnFulfillment_partialPick_rejected() {
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(188L)
                .setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType()).setSourceId(72L)
                .setPickStatus(ErpSalePickStatusEnum.PARTIAL.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setPickedItemCount(1).setDeliveredItemCount(0);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 72L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(188L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(188L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(188L)).thenReturn(List.of());

        assertServiceException(() -> service.rollbackUnstartedSaleReturnFulfillment(72L),
                SALE_RETURN_PICK_ALREADY_STARTED);
        verify(orderMapper, never()).deleteById(anyLong());
    }

    @Test
    void rollbackUnstartedWarehouseMoveFulfillment_unstarted_deletesOnlyWarehouseMoveWork() {
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(288L)
                .setSourceType(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType()).setSourceId(82L)
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setPickedItemCount(0).setDeliveredItemCount(0);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), 82L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(288L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(288L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(288L)).thenReturn(List.of());

        service.rollbackUnstartedWarehouseMoveFulfillment(82L);

        InOrder deletionOrder = inOrder(itemMapper, pickTaskMapper, orderMapper);
        deletionOrder.verify(itemMapper).deleteByOrderId(288L);
        deletionOrder.verify(pickTaskMapper).deleteByOrderId(288L);
        deletionOrder.verify(orderMapper).deleteById(288L);
    }

    @Test
    void rollbackUnstartedWarehouseMoveFulfillment_partialPick_rejected() {
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(288L)
                .setSourceType(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType()).setSourceId(82L)
                .setPickStatus(ErpSalePickStatusEnum.PARTIAL.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setPickedItemCount(1).setDeliveredItemCount(0);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), 82L))
                .thenReturn(order);
        when(submitMapper.selectListByOrderId(288L)).thenReturn(List.of());
        when(pickTaskMapper.selectListByOrderId(288L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(288L)).thenReturn(List.of());

        assertServiceException(() -> service.rollbackUnstartedWarehouseMoveFulfillment(82L),
                WAREHOUSE_MOVE_PICK_ALREADY_STARTED);
        verify(orderMapper, never()).deleteById(anyLong());
    }

    private ErpSalePickDeliveryOrderDO unstartedOrder(Long orderId, Long saleCartId) {
        return new ErpSalePickDeliveryOrderDO().setId(orderId)
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceId(saleCartId)
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setPickedItemCount(0).setDeliveredItemCount(0);
    }

    @Test
    void deliveryNames_shouldKeepEarlierBatchUsersAndAppendLegacyUsers() {
        when(submitMapper.selectDeliveryUsersBySaleOutId(100L)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setSubmitUserId(1L),
                new ErpSalePickDeliverySubmitDO().setSubmitUserId(2L)));
        // 明细只剩最后一次操作人，提交记录中的第一批人员仍应保留。
        when(itemMapper.selectDeliveryUsersBySaleOutId(100L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(2L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(3L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(4L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(5L),
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(6L)));
        when(adminUserApi.getUserMap(Set.of(1L, 2L, 3L, 4L, 5L, 6L))).thenReturn(Map.of(
                1L, deliveryUser(1L, "  张三 "), 2L, deliveryUser(2L, "李四"),
                3L, deliveryUser(3L, "王五"), 4L, deliveryUser(4L, "张三"),
                5L, deliveryUser(5L, "  ")));
        assertEquals("张三、李四、王五", service.getSaleOutDeliveryUserNames(100L));
        verify(submitMapper).selectDeliveryUsersBySaleOutId(100L);
        verify(itemMapper).selectDeliveryUsersBySaleOutId(100L);
        verifyNoMoreInteractions(submitMapper, itemMapper);
    }

    @Test
    void deliveryNames_shouldSupportLegacyOnlyAndSkipMissingUsers() {
        when(itemMapper.selectDeliveryUsersBySaleOutId(101L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setDeliveryUserId(3L)));
        when(adminUserApi.getUserMap(Set.of(3L))).thenReturn(Map.of(3L, deliveryUser(3L, "王五")));
        assertEquals("王五", service.getSaleOutDeliveryUserNames(101L));
        when(adminUserApi.getUserMap(Set.of(3L))).thenReturn(Map.of());
        assertEquals("", service.getSaleOutDeliveryUserNames(101L));
    }

    @Test
    void deliveryNames_shouldNotReadOtherOrdersOrResolveUsersWhenEmpty() {
        assertEquals("", service.getSaleOutDeliveryUserNames(null));
        verifyNoInteractions(submitMapper, itemMapper, adminUserApi);
        assertEquals("", service.getSaleOutDeliveryUserNames(102L));
        verify(submitMapper).selectDeliveryUsersBySaleOutId(102L);
        verify(itemMapper).selectDeliveryUsersBySaleOutId(102L);
        verifyNoInteractions(adminUserApi);
    }

    private AdminUserRespDTO deliveryUser(Long id, String name) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id);
        user.setNickname(name);
        return user;
    }

    @Test
    void buildItemRespList_shouldKeepPositionPackageAndCalculatePieceCount() {
        List<ErpSalePickDeliveryItemDO> items = Arrays.asList(
                new ErpSalePickDeliveryItemDO()
                        .setId(1L)
                        .setStockInItemId(101L)
                        .setWarehousePosition("A-01")
                        .setPackageQty(4)
                        .setCount(new BigDecimal("10"))
                        .setPickedCount(new BigDecimal("6"))
                        .setDeliveredCount(new BigDecimal("4")),
                new ErpSalePickDeliveryItemDO()
                        .setId(2L)
                        .setStockOutItemId(202L)
                        .setWarehousePosition("B-02")
                        .setPackageQty(0)
                        .setCount(new BigDecimal("5")));

        List<ErpSalePickDeliveryItemRespVO> respList = ReflectionTestUtils.invokeMethod(
                service, "buildItemRespList", items);

        assertEquals(101L, respList.get(0).getStockInItemId());
        assertEquals("A-01", respList.get(0).getWarehousePosition());
        assertEquals(4, respList.get(0).getPackageQty());
        assertEquals(0, new BigDecimal("2.5").compareTo(respList.get(0).getPieceCount()));
        assertEquals(0, new BigDecimal("2").compareTo(respList.get(0).getAvailableDeliveryCount()));
        assertEquals(0, new BigDecimal("6").compareTo(respList.get(0).getRemainingDeliveryCount()));
        assertEquals("B-02", respList.get(1).getWarehousePosition());
        assertEquals(202L, respList.get(1).getStockOutItemId());
        assertEquals(0, respList.get(1).getPackageQty());
        assertNull(respList.get(1).getPieceCount());
    }

    @Test
    void calculateTotalPieceCount_shouldSkipBlankPackageQty() {
        List<ErpSalePickDeliveryItemDO> items = Arrays.asList(
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("10")).setPackageQty(4),
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("3")).setPackageQty(2),
                new ErpSalePickDeliveryItemDO().setCount(new BigDecimal("9")).setPackageQty(null));

        BigDecimal totalPieceCount = ReflectionTestUtils.invokeMethod(service, "calculateTotalPieceCount", items);

        assertEquals(0, new BigDecimal("4").compareTo(totalPieceCount));
    }

    @Test
    void deliveryPage_shouldSplitSameOrderByWarehouseAndKeepTaskProgressIndependent() {
        ErpSaleDeliveryPageReqVO reqVO = new ErpSaleDeliveryPageReqVO();
        ErpSalePickDeliveryPickTaskDO firstTask = new ErpSalePickDeliveryPickTaskDO()
                .setId(21L).setOrderId(9L).setSaleOutNo("XS-001").setWarehouseId(101L)
                .setWarehouseName("一仓").setStatus(20).setTotalItemCount(2);
        ErpSalePickDeliveryPickTaskDO secondTask = new ErpSalePickDeliveryPickTaskDO()
                .setId(22L).setOrderId(9L).setSaleOutNo("XS-001").setWarehouseId(102L)
                .setWarehouseName("二仓").setStatus(10).setTotalItemCount(1);
        when(pickTaskMapper.selectDeliveryPage(reqVO)).thenReturn(
                new PageResult<>(List.of(firstTask, secondTask), 2L));
        when(orderMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliveryOrderDO().setId(9L).setDeptId(7L)));
        when(itemMapper.selectListByPickTaskIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setId(1L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(new BigDecimal("6")).setDeliveredCount(new BigDecimal("4"))
                        .setPickStatus(20).setDeliveryStatus(20),
                new ErpSalePickDeliveryItemDO().setId(2L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5),
                new ErpSalePickDeliveryItemDO().setId(3L).setPickTaskId(22L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5)));

        PageResult<ErpSaleDeliveryOrderRespVO> page = service.getDeliveryPage(reqVO, false);

        assertEquals(2L, page.getTotal());
        assertEquals(21L, page.getList().get(0).getId());
        assertEquals(9L, page.getList().get(0).getOrderId());
        assertEquals("一仓", page.getList().get(0).getWarehouseName());
        assertEquals("0/2", page.getList().get(0).getDeliveryProgress());
        assertEquals(20, page.getList().get(0).getDeliveryStatus());
        assertEquals(1, page.getList().get(0).getDeliverableItemCount());
        assertEquals(22L, page.getList().get(1).getId());
        assertEquals("二仓", page.getList().get(1).getWarehouseName());
        assertEquals(5, page.getList().get(1).getDeliveryStatus());
    }

    @Test
    void deliverySubmitPage_shouldReusePreciseLegacyBatchButCountOnlyCurrentWarehouseItems() {
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO().setId(21L).setOrderId(9L);
        ErpSalePickDeliverySubmitDO legacy = new ErpSalePickDeliverySubmitDO().setId(31L).setOrderId(9L)
                .setPickTaskId(null).setType(20).setSubmitUserId(1L).setItemCount(2).setQuantityDetails(true);
        when(pickTaskMapper.selectById(21L)).thenReturn(task);
        when(orderMapper.selectById(9L)).thenReturn(new ErpSalePickDeliveryOrderDO().setId(9L));
        when(submitMapper.selectDeliveryPageByTaskId(any(PageParam.class), eq(21L), eq(9L)))
                .thenReturn(new PageResult<>(List.of(legacy), 1L));
        when(submitFileMapper.selectListBySubmitIds(anyCollection())).thenReturn(List.of());
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));
        when(submitItemMapper.selectCountBySubmitIdAndPickTaskId(31L, 21L)).thenReturn(1L);

        PageResult<ErpSalePickDeliverySubmitRespVO> page = service.getDeliverySubmitPage(21L, new PageParam(), false);

        assertEquals(1L, page.getTotal());
        assertEquals(1, page.getList().get(0).getItemCount());
        assertEquals("甲", page.getList().get(0).getSubmitUserName());
    }

    @Test
    void deliveryDetail_shouldWarnWhenLegacyBatchCannotBeAssignedToWarehouse() {
        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setId(21L).setOrderId(9L).setWarehouseId(101L).setWarehouseName("一仓")
                .setStatus(10).setTotalItemCount(1);
        when(pickTaskMapper.selectById(21L)).thenReturn(task);
        when(orderMapper.selectById(9L)).thenReturn(new ErpSalePickDeliveryOrderDO().setId(9L));
        when(itemMapper.selectListByPickTaskId(21L)).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setId(1L).setPickTaskId(21L).setCount(BigDecimal.TEN)
                        .setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setPickStatus(10).setDeliveryStatus(5)));
        when(submitMapper.selectUnassignedLegacyDeliveryCount(9L)).thenReturn(1L);

        ErpSaleDeliveryOrderRespVO detail = service.getDelivery(21L, false, false);

        assertTrue(detail.getLegacyHistoryWarning());
        assertEquals("一仓", detail.getWarehouseName());
        verify(submitMapper, never()).selectDeliveryPageByTaskId(any(), anyLong(), anyLong());
    }

    @Test
    void operationItemPage_shouldKeepEachBatchRowAndIgnoreLegacySubmits() {
        LocalDateTime firstTime = LocalDateTime.of(2026, 10, 9, 10, 0);
        LocalDateTime secondTime = LocalDateTime.of(2026, 10, 9, 11, 0);
        when(orderMapper.selectBySaleOutId(100L)).thenReturn(new ErpSalePickDeliveryOrderDO().setId(9L));
        when(submitMapper.selectListBySaleOutIdAndType(100L, 10)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(12L).setType(10).setSubmitUserId(1L)
                        .setSubmitTime(secondTime).setQuantityDetails(true),
                new ErpSalePickDeliverySubmitDO().setId(11L).setType(10).setSubmitUserId(1L)
                        .setSubmitTime(firstTime).setQuantityDetails(true),
                new ErpSalePickDeliverySubmitDO().setId(10L).setType(10).setSubmitUserId(2L)
                        .setQuantityDetails(false)));
        when(submitItemMapper.selectPageBySubmitIds(any(PageParam.class), argThat(ids ->
                ids.size() == 2 && ids.containsAll(Set.of(11L, 12L))))).thenReturn(new PageResult<>(List.of(
                new ErpSalePickDeliverySubmitItemDO().setId(102L).setSubmitId(12L).setItemId(2L)
                        .setProductCode("P-2").setProductName("商品二").setQuantity(new BigDecimal("4")),
                new ErpSalePickDeliverySubmitItemDO().setId(101L).setSubmitId(11L).setItemId(1L)
                        .setProductCode("P-1").setProductName("商品一").setQuantity(new BigDecimal("6"))), 2L));
        when(itemMapper.selectBatchIds(argThat(ids -> ids.size() == 2 && ids.containsAll(Set.of(1L, 2L)))))
                .thenReturn(List.of(
                        new ErpSalePickDeliveryItemDO().setId(1L).setStandard("规格一")
                                .setWarehouseId(21L).setWarehouseName("一仓"),
                        new ErpSalePickDeliveryItemDO().setId(2L).setStandard("规格二")
                                .setWarehouseId(22L).setWarehouseName("二仓")));
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));

        PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> page =
                service.getSaleOutPickDeliveryOperationItemPage(100L, 10, new PageParam());

        assertEquals(2L, page.getTotal());
        assertEquals(2, page.getList().size());
        assertEquals(12L, page.getList().get(0).getSubmitId());
        assertEquals("甲", page.getList().get(0).getSubmitUserName());
        assertEquals("规格二", page.getList().get(0).getStandard());
        assertEquals("二仓", page.getList().get(0).getWarehouseName());
        assertEquals(0, new BigDecimal("4").compareTo(page.getList().get(0).getQuantity()));
        assertEquals(11L, page.getList().get(1).getSubmitId());
        assertEquals(firstTime, page.getList().get(1).getSubmitTime());
    }

    @Test
    void operationItemPage_shouldRejectUnsupportedTypeWithoutQuerying() {
        PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> page =
                service.getSaleOutPickDeliveryOperationItemPage(100L, 30, new PageParam());

        assertTrue(page.getList().isEmpty());
        assertEquals(0L, page.getTotal());
        verifyNoInteractions(orderMapper, submitItemMapper);
    }

    @Test
    void generateForSaleOut_shouldBindDeliveryFilesAndSetLatestPrimaryWhenBlank() {
        LocalDateTime olderTime = LocalDateTime.of(2026, 10, 20, 9, 0);
        LocalDateTime latestTime = LocalDateTime.of(2026, 10, 20, 10, 0);
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(100L)
                .setNo("XS-100").setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType()).setSourceId(72L);
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(88L);
        ErpSalePickDeliverySubmitDO older = new ErpSalePickDeliverySubmitDO().setId(11L)
                .setOrderId(88L).setType(20).setSubmitTime(olderTime);
        ErpSalePickDeliverySubmitDO latest = new ErpSalePickDeliverySubmitDO().setId(12L)
                .setOrderId(88L).setType(20).setSubmitTime(latestTime);
        when(saleOutMapper.selectByIdForUpdate(100L)).thenReturn(saleOut);
        when(orderMapper.selectBySourceForUpdate(ErpSaleBizSourceTypeEnum.CART.getType(), 72L)).thenReturn(order);
        when(pickTaskMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(itemMapper.selectListByOrderId(88L)).thenReturn(List.of());
        when(submitMapper.selectListByOrderId(88L)).thenReturn(List.of(older, latest));
        when(submitFileMapper.selectListBySubmitIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliverySubmitFileDO().setId(101L).setSubmitId(11L)
                        .setSort(1).setFileUrl("https://example.com/older.jpg"),
                new ErpSalePickDeliverySubmitFileDO().setId(103L).setSubmitId(12L)
                        .setSort(2).setFileUrl("https://example.com/latest-2.jpg"),
                new ErpSalePickDeliverySubmitFileDO().setId(102L).setSubmitId(12L)
                        .setSort(1).setFileUrl("https://example.com/latest-1.jpg")));

        service.generateForSaleOut(100L);

        verify(saleOutMapper).updateById(argThat((ErpSaleOutDO update) -> update.getId().equals(100L)
                && "https://example.com/latest-1.jpg".equals(update.getExpressFileUrl())));
        assertEquals("https://example.com/latest-1.jpg", saleOut.getExpressFileUrl());
        verify(submitMapper, times(2)).updateById(any(ErpSalePickDeliverySubmitDO.class));
    }

    @Test
    void primaryExpressSync_shouldNeverOverwriteExistingFile() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(100L)
                .setExpressFileUrl("https://example.com/existing.jpg");
        ErpSalePickDeliverySubmitDO delivery = new ErpSalePickDeliverySubmitDO()
                .setId(12L).setType(20).setSubmitTime(LocalDateTime.now());

        ReflectionTestUtils.invokeMethod(service, "syncPrimaryExpressFileIfBlank", saleOut, List.of(delivery));

        verify(saleOutMapper, never()).updateById(any(ErpSaleOutDO.class));
        verifyNoInteractions(submitFileMapper);
        assertEquals("https://example.com/existing.jpg", saleOut.getExpressFileUrl());
    }

    @Test
    void expressFilePage_shouldPutManualFirstDeduplicateAndKeepDeliveryOrder() {
        ErpSaleOutDO saleOut = new ErpSaleOutDO().setId(100L)
                .setExpressFileUrl("https://example.com/manual.jpg");
        when(saleOutMapper.selectById(100L)).thenReturn(saleOut);
        when(submitMapper.selectListBySaleOutIdAndType(100L, 20)).thenReturn(new ArrayList<>(List.of(
                new ErpSalePickDeliverySubmitDO().setId(11L).setType(20).setSubmitUserId(1L)
                        .setSubmitTime(LocalDateTime.of(2026, 10, 20, 9, 0)),
                new ErpSalePickDeliverySubmitDO().setId(12L).setType(20).setSubmitUserId(1L)
                        .setSubmitTime(LocalDateTime.of(2026, 10, 20, 10, 0)))));
        when(submitFileMapper.selectListBySubmitIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliverySubmitFileDO().setId(102L).setSubmitId(12L).setSort(1)
                        .setFileUrl("https://example.com/latest.jpg").setFileName("latest.jpg"),
                new ErpSalePickDeliverySubmitFileDO().setId(103L).setSubmitId(12L).setSort(2)
                        .setFileUrl("https://example.com/latest.jpg").setFileName("duplicate.jpg"),
                new ErpSalePickDeliverySubmitFileDO().setId(101L).setSubmitId(11L).setSort(1)
                        .setFileUrl("https://example.com/older.jpg").setFileName("older.jpg")));
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(10);

        PageResult<ErpSaleOutExpressFileRespVO> page = service.getSaleOutExpressFilePage(100L, pageParam);

        assertEquals(3L, page.getTotal());
        assertEquals("MANUAL", page.getList().get(0).getSourceType());
        assertEquals("https://example.com/manual.jpg", page.getList().get(0).getFileUrl());
        assertEquals("https://example.com/latest.jpg", page.getList().get(1).getFileUrl());
        assertEquals("https://example.com/older.jpg", page.getList().get(2).getFileUrl());
    }

    @Test
    void expressFilePage_shouldKeepAutoSyncedPrimaryAsDeliverySource() {
        when(saleOutMapper.selectById(100L)).thenReturn(new ErpSaleOutDO().setId(100L)
                .setExpressFileUrl("https://example.com/delivery.jpg"));
        when(submitMapper.selectListBySaleOutIdAndType(100L, 20)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(12L).setType(20).setSubmitUserId(1L)));
        when(submitFileMapper.selectListBySubmitIds(anyCollection())).thenReturn(List.of(
                new ErpSalePickDeliverySubmitFileDO().setId(102L).setSubmitId(12L).setSort(1)
                        .setFileUrl("https://example.com/delivery.jpg")));
        when(adminUserApi.getUserMap(Set.of(1L))).thenReturn(Map.of(1L, deliveryUser(1L, "甲")));

        PageResult<ErpSaleOutExpressFileRespVO> page = service.getSaleOutExpressFilePage(100L, new PageParam());

        assertEquals(1L, page.getTotal());
        assertEquals("DELIVERY", page.getList().get(0).getSourceType());
    }

    @Test
    void deliveryFileExists_shouldBatchResolveSaleOutIdsWithoutNPlusOne() {
        when(submitMapper.selectListBySaleOutIdsAndType(Set.of(100L, 101L), 20)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(11L).setSaleOutId(100L),
                new ErpSalePickDeliverySubmitDO().setId(12L).setSaleOutId(101L)));
        when(submitFileMapper.selectSubmitIdsWithFiles(Set.of(11L, 12L))).thenReturn(List.of(12L));

        Set<Long> result = service.getSaleOutIdsWithDeliveryFiles(Set.of(100L, 101L));

        assertEquals(Set.of(101L), result);
        verify(submitMapper).selectListBySaleOutIdsAndType(Set.of(100L, 101L), 20);
        verify(submitFileMapper).selectSubmitIdsWithFiles(Set.of(11L, 12L));
    }

    @Test
    void sourcePickDetail_shouldAggregateQuantitiesTasksAndParticipants() {
        LocalDateTime firstPickTime = LocalDateTime.of(2026, 10, 24, 9, 0);
        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO().setId(70L)
                .setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType()).setSourceId(7L)
                .setPickStatus(20).setTotalItemCount(3).setPickedItemCount(1);
        when(orderMapper.selectBySource(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 7L)).thenReturn(order);
        when(itemMapper.selectPickQuantityAggregatesByOrderId(70L)).thenReturn(List.of(
                new ErpSalePickQuantityAggregateDO().setPickTaskId(701L).setTotalItemCount(2)
                        .setTotalQuantity(new BigDecimal("5.5")).setPickedQuantity(new BigDecimal("2.0")),
                new ErpSalePickQuantityAggregateDO().setPickTaskId(702L).setTotalItemCount(1)
                        .setTotalQuantity(new BigDecimal("3")).setPickedQuantity(BigDecimal.ZERO)));
        when(pickTaskMapper.selectListByOrderId(70L)).thenReturn(List.of(
                new ErpSalePickDeliveryPickTaskDO().setId(701L).setWarehouseName("一仓")
                        .setTotalItemCount(2).setPickedItemCount(1),
                new ErpSalePickDeliveryPickTaskDO().setId(702L).setWarehouseName("二仓")
                        .setTotalItemCount(1).setPickedItemCount(0)));
        when(submitMapper.selectListByOrderIdAndType(70L, 10)).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(1L).setPickTaskId(701L).setSubmitUserId(10L)
                        .setSubmitTime(firstPickTime),
                new ErpSalePickDeliverySubmitDO().setId(2L).setPickTaskId(701L).setSubmitUserId(11L)
                        .setSubmitTime(firstPickTime.plusHours(1))));
        when(adminUserApi.getUserMap(Set.of(10L, 11L))).thenReturn(Map.of(
                10L, deliveryUser(10L, "甲"), 11L, deliveryUser(11L, "乙")));

        ErpSalePickSourceDetailRespVO result = service.getSourcePickDetail(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 7L);

        assertEquals(new BigDecimal("8.5"), result.getSummary().getTotalQuantity());
        assertEquals(new BigDecimal("2.0"), result.getSummary().getPickedQuantity());
        assertEquals(new BigDecimal("6.5"), result.getSummary().getRemainingPickQuantity());
        assertEquals(2, result.getPickTasks().size());
        assertEquals(List.of("甲", "乙"), result.getPickTasks().get(0).getPickerNames());
        assertEquals(firstPickTime, result.getPickTasks().get(0).getStartTime());
        assertEquals(new BigDecimal("3.5"), result.getPickTasks().get(0).getRemainingPickQuantity());
        assertTrue(result.getPickTasks().get(1).getPickerNames().isEmpty());
        verify(itemMapper).selectPickQuantityAggregatesByOrderId(70L);
        verify(pickTaskMapper).selectListByOrderId(70L);
    }

    @Test
    void sourcePickDetail_shouldReturnExplicitEmptyTasksWhenNoOrderExists() {
        when(orderMapper.selectBySource(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 8L)).thenReturn(null);

        ErpSalePickSourceDetailRespVO result = service.getSourcePickDetail(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 8L);

        assertNull(result.getSummary());
        assertTrue(result.getPickTasks().isEmpty());
        verifyNoInteractions(itemMapper, pickTaskMapper, adminUserApi);
    }

    @Test
    void sourcePickOperationPage_shouldReturnOnlyPagedPickQuantityRows() {
        PageParam pageParam = new PageParam();
        pageParam.setPageNo(1);
        pageParam.setPageSize(20);
        when(orderMapper.selectBySource(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 7L))
                .thenReturn(new ErpSalePickDeliveryOrderDO().setId(70L));
        ErpSalePickDeliverySubmitItemDO submitItem = new ErpSalePickDeliverySubmitItemDO()
                .setId(101L).setSubmitId(201L).setItemId(301L).setProductCode("P001")
                .setProductName("滤芯").setQuantity(new BigDecimal("1.25"));
        when(submitItemMapper.selectPageByOrderIdAndType(pageParam, 70L, 10))
                .thenReturn(new PageResult<>(List.of(submitItem), 1L));
        when(submitMapper.selectBatchIds(Set.of(201L))).thenReturn(List.of(
                new ErpSalePickDeliverySubmitDO().setId(201L).setType(10).setSubmitUserId(10L)
                        .setSubmitTime(LocalDateTime.of(2026, 10, 24, 10, 0))));
        when(itemMapper.selectBatchIds(Set.of(301L))).thenReturn(List.of(
                new ErpSalePickDeliveryItemDO().setId(301L).setWarehouseName("一仓").setStandard("A")));
        when(adminUserApi.getUserMap(Set.of(10L))).thenReturn(Map.of(10L, deliveryUser(10L, "甲")));

        PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> result =
                service.getSourcePickOperationItemPage(
                        ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), 7L, pageParam);

        assertEquals(1L, result.getTotal());
        assertEquals("甲", result.getList().get(0).getSubmitUserName());
        assertEquals(new BigDecimal("1.25"), result.getList().get(0).getQuantity());
        verify(submitItemMapper).selectPageByOrderIdAndType(pageParam, 70L, 10);
    }

    @Test
    void sourceContract_shouldCopySnapshotFieldsAndOrderByWarehousePosition() throws IOException {
        String serviceSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/service/sale/ErpSalePickDeliveryServiceImpl.java");
        String mapperSource = readProjectFile(
                "src/main/java/cn/iocoder/yudao/module/erp/dal/mysql/sale/pickdelivery/ErpSalePickDeliveryItemMapper.java");

        assertTrue(serviceSource.contains(".setWarehousePosition(saleOutItem.getWarehousePosition())"));
        assertTrue(serviceSource.contains(".setPackageQty(saleOutItem.getPackageQty())"));
        assertTrue(serviceSource.contains(".setWarehouseMoveItemId(moveItem.getId())"));
        assertTrue(serviceSource.contains("ErpSaleBizSourceTypeEnum.isPickOnly(order.getSourceType())"));
        assertTrue(serviceSource.contains("new ErpWarehouseMovePickCompletedEvent("));
        assertTrue(serviceSource.contains(".setStockInItemId(stockInItem.getId())"));
        assertTrue(serviceSource.contains(".setStockOutItemId(stockOutItem.getId())"));
        assertTrue(serviceSource.contains("new ErpStockInPickCompletedEvent("));
        assertTrue(serviceSource.contains("new ErpStockOutPickCompletedEvent("));
        assertTrue(serviceSource.contains("submitMapper.selectListByOrderId(order.getId())"));
        assertTrue(serviceSource.contains("syncPrimaryExpressFileIfBlank(saleOut, submits)"));
        assertTrue(mapperSource.contains("warehouse_position IS NULL OR warehouse_position = ''"));
        assertTrue(mapperSource.contains("orderByAsc(\"warehouse_position\")"));
        assertTrue(mapperSource.contains("orderByAsc(\"id\")"));
    }

    private String readProjectFile(String modulePath) throws IOException {
        Path path = Path.of(modulePath);
        if (Files.exists(path)) {
            return Files.readString(path);
        }
        return Files.readString(Path.of("code/yudao-module-erp").resolve(modulePath));
    }

}
