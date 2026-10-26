package cn.iocoder.yudao.module.erp.service.assembly;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpAssemblyWarehouseMoveCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_STOCK_INSUFFICIENT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_RECIPE_DISABLED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_FINISHED_BATCH_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_STATUS_CHANGED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_VOID_FINISHED_STOCK_INSUFFICIENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpAssemblyOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAssemblyOrderServiceImpl orderService;
    @Mock
    private ErpAssemblyOrderMapper orderMapper;
    @Mock
    private ErpAssemblyOrderItemMapper orderItemMapper;
    @Mock
    private ErpAssemblyRecipeMapper recipeMapper;
    @Mock
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpProductUnitService productUnitService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpWarehouseMoveService warehouseMoveService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpStockRecordMapper stockRecordMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void workflowTransitions_followConfiguredSequenceAndIncrementRevision() {
        mockReadinessContext(10, 0);
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(10), any())).thenReturn(1);

        orderService.confirm(100L);

        ArgumentCaptor<ErpAssemblyOrderDO> confirmCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(10), confirmCaptor.capture());
        assertEquals(15, confirmCaptor.getValue().getStatus());

        mockReadinessContext(15, 2);
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(15), any())).thenReturn(1);

        orderService.start(100L);

        ArgumentCaptor<ErpAssemblyOrderDO> startCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(15), startCaptor.capture());
        assertEquals(18, startCaptor.getValue().getStatus());
        assertEquals(3, startCaptor.getValue().getExecutionRevision());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void reverseConfirmAndCancelStart_onlyChangeStatus() {
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(
                new ErpAssemblyOrderDO().setId(100L).setNo("ZZD0001").setStatus(15));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(15), any())).thenReturn(1);

        orderService.reverseConfirm(100L);

        ArgumentCaptor<ErpAssemblyOrderDO> reverseCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(15), reverseCaptor.capture());
        assertEquals(10, reverseCaptor.getValue().getStatus());

        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(
                new ErpAssemblyOrderDO().setId(100L).setNo("ZZD0001").setStatus(18));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(18), any())).thenReturn(1);

        orderService.cancelStart(100L);

        ArgumentCaptor<ErpAssemblyOrderDO> cancelCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(18), cancelCaptor.capture());
        assertEquals(15, cancelCaptor.getValue().getStatus());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void complete_rejectsDraftToCompletedJump() {
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(
                new ErpAssemblyOrderDO().setId(100L).setNo("ZZD0001").setStatus(10));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.complete(100L));

        assertEquals(ASSEMBLY_ORDER_STATUS_CHANGED.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void complete_usesComponentMovingCostAndCreatesBalancedStockRecords() {
        mockCompletionContext(new BigDecimal("10"));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(18), any())).thenReturn(1);

        orderService.complete(100L);

        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2))
                .createStockRecord(recordCaptor.capture());
        List<ErpStockRecordCreateReqBO> records = recordCaptor.getAllValues();
        ErpStockRecordCreateReqBO componentOut = records.get(0);
        ErpStockRecordCreateReqBO finishedIn = records.get(1);
        assertEquals(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType(), componentOut.getBizType());
        assertEquals(new BigDecimal("-3.0"), componentOut.getCount());
        assertEquals(new BigDecimal("4.00"), componentOut.getUnitPrice());
        assertEquals(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType(), finishedIn.getBizType());
        assertEquals(new BigDecimal("2"), finishedIn.getCount());
        assertEquals(new BigDecimal("14.000000"), finishedIn.getUnitPrice());

        ArgumentCaptor<ErpAssemblyOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(18), orderCaptor.capture());
        assertEquals(new BigDecimal("12.000000"), orderCaptor.getValue().getMaterialCost());
        assertEquals(new BigDecimal("16.000000"), orderCaptor.getValue().getLaborCost());
        assertEquals(new BigDecimal("28.000000"), orderCaptor.getValue().getTotalCost());
        assertEquals(new BigDecimal("14.000000"), orderCaptor.getValue().getUnitCost());
    }

    @Test
    void createDraft_snapshotsModuleIdentityAndLaborCost() {
        ErpAssemblyOrderSaveReqVO reqVO = new ErpAssemblyOrderSaveReqVO();
        reqVO.setRecipeId(1L);
        reqVO.setWarehouseId(30L);
        reqVO.setSetCount(5);
        reqVO.setAssemblyTime(LocalDateTime.of(1970, 1, 1, 8, 0));
        when(recipeMapper.selectById(1L)).thenReturn(new ErpAssemblyRecipeDO()
                .setId(1L).setProductId(10L).setVersion(3).setStatus(1)
                .setModuleCode("MOD-001").setModuleName("测试模块")
                .setLaborCostPerSet(new BigDecimal("8")));
        when(warehouseService.getWarehouse(30L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO()
                        .setId(30L).setDeptId(40L));
        when(recipeItemMapper.selectListByRecipeId(1L)).thenReturn(Collections.singletonList(
                new ErpAssemblyRecipeItemDO().setId(301L).setRecipeId(1L).setProductId(20L)
                        .setQuantityPerSet(new BigDecimal("1.5")).setSort(1)));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setCode("P001").setName("组成件A")
                        .setBatchNoEnabled(false),
                new ErpProductDO().setId(20L).setCode("P002").setName("配件B")
                        .setBatchNoEnabled(false)));
        when(noRedisDAO.generate(any())).thenReturn("ZZD0001");

        orderService.createDraft(reqVO);

        ArgumentCaptor<ErpAssemblyOrderDO> captor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).insert(captor.capture());
        assertEquals("MOD-001", captor.getValue().getRecipeModuleCode());
        assertEquals("测试模块", captor.getValue().getRecipeModuleName());
        assertEquals(new BigDecimal("8.000000"), captor.getValue().getLaborCostPerSet());
        assertEquals(BigDecimal.ZERO, captor.getValue().getMaterialCost());
        assertEquals(BigDecimal.ZERO, captor.getValue().getLaborCost());
        assertEquals(null, captor.getValue().getOutWarehouseId());
        assertNotEquals(1970, captor.getValue().getAssemblyTime().getYear());
    }

    @Test
    void complete_crossWarehouseCreatesApprovedMoveFromSourceToInboundWarehouse() {
        ErpAssemblyOrderDO order = new ErpAssemblyOrderDO()
                .setId(100L).setNo("ZZD0001").setProductId(10L).setRecipeId(1L)
                .setWarehouseId(30L).setOutWarehouseId(31L).setDeptId(40L).setSetCount(2).setStatus(18)
                .setExecutionRevision(1)
                .setLaborCostPerSet(new BigDecimal("8"))
                .setAssemblyTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpAssemblyOrderItemDO item = new ErpAssemblyOrderItemDO()
                .setId(201L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("组件B").setBatchNoEnabled(false)
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("3.0"));
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(orderItemMapper.selectListByOrderId(100L)).thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setName("成品A").setUnitId(1L).setBatchNoEnabled(false),
                new ErpProductDO().setId(20L).setName("组件B").setBatchNoEnabled(false)));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(20L, 31L))
                .thenReturn(new ErpStockDO().setProductId(20L).setWarehouseId(31L)
                        .setCount(new BigDecimal("10")).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("4.00")));
        when(warehouseService.getWarehouse(31L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO()
                        .setId(31L).setName("出货仓"));
        when(stockService.getStock(10L, 31L)).thenReturn(
                new ErpStockDO().setProductId(10L).setWarehouseId(31L)
                        .setCount(new BigDecimal("5")).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("7.500000")).setShelf("A-01"));
        when(warehouseMoveService.createAndApproveAssemblyMove(any()))
                .thenReturn(new ErpWarehouseMoveDO().setId(900L).setNo("YHD0001").setStatus(20));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(18), any())).thenReturn(1);

        orderService.complete(100L);

        ArgumentCaptor<ErpStockRecordCreateReqBO> stockCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(stockCaptor.capture());
        assertEquals(31L, stockCaptor.getAllValues().get(1).getWarehouseId());
        ArgumentCaptor<ErpAssemblyWarehouseMoveCreateReqBO> moveCaptor =
                ArgumentCaptor.forClass(ErpAssemblyWarehouseMoveCreateReqBO.class);
        verify(warehouseMoveService).createAndApproveAssemblyMove(moveCaptor.capture());
        assertEquals(31L, moveCaptor.getValue().getFromWarehouseId());
        assertEquals(30L, moveCaptor.getValue().getToWarehouseId());
        assertEquals(new BigDecimal("7.500000"), moveCaptor.getValue().getCostPrice());
        assertNotEquals(order.getAssemblyTime(), moveCaptor.getValue().getMoveTime());
        ArgumentCaptor<ErpAssemblyOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(18), orderCaptor.capture());
        assertEquals(900L, orderCaptor.getValue().getWarehouseMoveId());
        assertEquals(orderCaptor.getValue().getCompleteTime(), moveCaptor.getValue().getMoveTime());
    }

    @Test
    void complete_failsBeforeWritingWhenComponentAvailableStockIsInsufficient() {
        mockCompletionContext(new BigDecimal("2"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.complete(100L));

        assertEquals(ASSEMBLY_ORDER_STOCK_INSUFFICIENT.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void complete_countsLockedStockAsUnavailable() {
        mockCompletionContext(new BigDecimal("10"));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(20L, 30L))
                .thenReturn(new ErpStockDO().setProductId(20L).setWarehouseId(30L)
                        .setCount(new BigDecimal("10")).setLockCount(new BigDecimal("8"))
                        .setCostPrice(new BigDecimal("4.00")));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.complete(100L));

        assertEquals(ASSEMBLY_ORDER_STOCK_INSUFFICIENT.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void complete_rejectsConcurrentStatusChange() {
        mockCompletionContext(new BigDecimal("10"));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(18), any())).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.complete(100L));

        assertEquals(ASSEMBLY_ORDER_STATUS_CHANGED.getCode(), exception.getCode());
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(any());
    }

    @Test
    void complete_supportsMultiBatchComponentAndFinishedBatch() {
        mockBatchCompletionContext("CP20261022");
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(18), any())).thenReturn(1);

        orderService.complete(100L);

        ArgumentCaptor<ErpStockRecordCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(3)).createStockRecord(captor.capture());
        List<ErpStockRecordCreateReqBO> records = captor.getAllValues();
        assertEquals("PC01", records.get(0).getBatchNo());
        assertEquals(new BigDecimal("-1.0"), records.get(0).getCount());
        assertEquals("PC02", records.get(1).getBatchNo());
        assertEquals(new BigDecimal("-2.0"), records.get(1).getCount());
        assertEquals("CP20261022", records.get(2).getBatchNo());
        assertEquals(new BigDecimal("2"), records.get(2).getCount());
        assertEquals(new BigDecimal("6.000000"), records.get(2).getUnitPrice());
    }

    @Test
    void complete_requiresFinishedBatchForBatchManagedProduct() {
        mockBatchCompletionValidationContext(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.complete(100L));

        assertEquals(ASSEMBLY_ORDER_FINISHED_BATCH_REQUIRED.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
        verify(stockMapper, never()).selectByProductIdAndWarehouseIdForUpdate(any(), any());
    }

    @Test
    void getOrderItemPage_rejectsMissingOrInaccessibleOrder() {
        ErpAssemblyOrderItemPageReqVO reqVO = new ErpAssemblyOrderItemPageReqVO();
        reqVO.setOrderId(99L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.getOrderItemPage(reqVO));

        assertEquals(ASSEMBLY_ORDER_NOT_EXISTS.getCode(), exception.getCode());
        verify(orderItemMapper, never()).selectPage(any());
    }

    @Test
    void complete_isIdempotentAfterOrderAlreadyCompleted() {
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(
                new ErpAssemblyOrderDO().setId(100L).setStatus(20));

        orderService.complete(100L);

        verify(orderItemMapper, never()).selectListByOrderId(any());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void createDraft_rejectsDisabledRecipe() {
        ErpAssemblyOrderSaveReqVO reqVO = new ErpAssemblyOrderSaveReqVO();
        reqVO.setRecipeId(1L);
        reqVO.setWarehouseId(30L);
        reqVO.setSetCount(1);
        when(recipeMapper.selectById(1L)).thenReturn(
                new ErpAssemblyRecipeDO().setId(1L).setProductId(10L).setStatus(0));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.createDraft(reqVO));

        assertEquals(ASSEMBLY_ORDER_RECIPE_DISABLED.getCode(), exception.getCode());
        verify(orderMapper, never()).insert(any(ErpAssemblyOrderDO.class));
    }

    @Test
    void reverseAssembly_reversesOriginalQuantityBatchAndCost() {
        mockVoidContext(new BigDecimal("-3.0"), new BigDecimal("4.00"));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(20), any())).thenReturn(1);

        orderService.reverseAssembly(100L);

        ArgumentCaptor<ErpStockRecordCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(captor.capture());
        List<ErpStockRecordCreateReqBO> records = captor.getAllValues();
        assertEquals(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN_CANCEL.getType(),
                records.get(0).getBizType());
        assertEquals(new BigDecimal("-2"), records.get(0).getCount());
        assertEquals(new BigDecimal("6.000000"), records.get(0).getUnitPrice());
        assertEquals(501L, records.get(0).getSourceRecordId());
        assertEquals(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT_CANCEL.getType(),
                records.get(1).getBizType());
        assertEquals(new BigDecimal("3.0"), records.get(1).getCount());
        assertEquals(new BigDecimal("4.00"), records.get(1).getUnitPrice());
        assertEquals(502L, records.get(1).getSourceRecordId());
        ArgumentCaptor<ErpAssemblyOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpAssemblyOrderDO.class);
        verify(orderMapper).updateByIdAndStatus(eq(100L), eq(20), orderCaptor.capture());
        assertEquals(15, orderCaptor.getValue().getStatus());
    }

    @Test
    void reverseAssembly_usesCurrentActiveExecutionRecords() {
        mockVoidContext(new BigDecimal("-3.0"), new BigDecimal("4.00"));
        ErpStockRecordDO previousFinished = new ErpStockRecordDO()
                .setId(401L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType())
                .setBizId(100L).setBizItemId(100L).setProductId(10L).setWarehouseId(30L)
                .setCount(new BigDecimal("2")).setUnitPrice(new BigDecimal("6.000000"));
        ErpStockRecordDO previousComponent = new ErpStockRecordDO()
                .setId(402L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType())
                .setBizId(100L).setBizItemId(201L).setProductId(20L).setWarehouseId(30L)
                .setCount(new BigDecimal("-3.0")).setUnitPrice(new BigDecimal("4.00"));
        ErpStockRecordDO previousFinishedCancel = new ErpStockRecordDO()
                .setId(403L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN_CANCEL.getType())
                .setSourceRecordId(401L);
        ErpStockRecordDO previousComponentCancel = new ErpStockRecordDO()
                .setId(404L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT_CANCEL.getType())
                .setSourceRecordId(402L);
        ErpStockRecordDO currentFinished = new ErpStockRecordDO()
                .setId(501L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType())
                .setBizId(100L).setBizItemId(100L).setProductId(10L).setWarehouseId(30L)
                .setCount(new BigDecimal("2")).setUnitPrice(new BigDecimal("6.000000"));
        ErpStockRecordDO currentComponent = new ErpStockRecordDO()
                .setId(502L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType())
                .setBizId(100L).setBizItemId(201L).setProductId(20L).setWarehouseId(30L)
                .setCount(new BigDecimal("-3.0")).setUnitPrice(new BigDecimal("4.00"));
        when(stockRecordMapper.selectListByBizTypesAndBizId(any(), eq(100L))).thenReturn(Arrays.asList(
                previousFinished, previousComponent, previousFinishedCancel, previousComponentCancel,
                currentFinished, currentComponent));
        when(orderMapper.updateByIdAndStatus(eq(100L), eq(20), any())).thenReturn(1);

        orderService.reverseAssembly(100L);

        ArgumentCaptor<ErpStockRecordCreateReqBO> captor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService, org.mockito.Mockito.times(2)).createStockRecord(captor.capture());
        assertEquals(501L, captor.getAllValues().get(0).getSourceRecordId());
        assertEquals(502L, captor.getAllValues().get(1).getSourceRecordId());
    }

    @Test
    void reverseAssembly_rejectsMismatchedOriginalQuantityBeforeWriting() {
        mockVoidContext(new BigDecimal("-2.0"), new BigDecimal("4.00"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.reverseAssembly(100L));

        assertEquals(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
        verify(orderMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void reverseAssembly_rejectsWhenFinishedAvailableStockIsInsufficient() {
        mockVoidContext(new BigDecimal("-3.0"), new BigDecimal("4.00"));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(10L, 30L))
                .thenReturn(new ErpStockDO().setProductId(10L).setWarehouseId(30L)
                        .setCount(new BigDecimal("1")).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("6.00")));
        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.reverseAssembly(100L));

        assertEquals(ASSEMBLY_ORDER_VOID_FINISHED_STOCK_INSUFFICIENT.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    @Test
    void reverseAssembly_rejectsLegacyVoidedOrder() {
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(
                new ErpAssemblyOrderDO().setId(100L).setNo("ZZD0001").setStatus(30));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> orderService.reverseAssembly(100L));

        assertEquals(ASSEMBLY_ORDER_STATUS_CHANGED.getCode(), exception.getCode());
        verify(stockRecordService, never()).createStockRecord(any());
    }

    private void mockCompletionContext(BigDecimal componentStockCount) {
        ErpAssemblyOrderDO order = new ErpAssemblyOrderDO()
                .setId(100L).setNo("ZZD0001").setProductId(10L).setRecipeId(1L)
                .setWarehouseId(30L).setDeptId(40L).setSetCount(2).setStatus(18)
                .setExecutionRevision(1)
                .setLaborCostPerSet(new BigDecimal("8"))
                .setAssemblyTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpAssemblyOrderItemDO item = new ErpAssemblyOrderItemDO()
                .setId(201L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("组件B").setBatchNoEnabled(false)
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("3.0"));
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(orderItemMapper.selectListByOrderId(100L)).thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setName("成品A").setBatchNoEnabled(false),
                new ErpProductDO().setId(20L).setName("组件B").setBatchNoEnabled(false)));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(20L, 30L))
                .thenReturn(new ErpStockDO().setProductId(20L).setWarehouseId(30L)
                        .setCount(componentStockCount).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("4.00")));
        when(warehouseService.getWarehouse(30L))
                .thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO()
                        .setId(30L).setName("总仓"));
    }

    private void mockReadinessContext(Integer status, Integer revision) {
        ErpAssemblyOrderDO order = new ErpAssemblyOrderDO()
                .setId(100L).setNo("ZZD0001").setProductId(10L).setRecipeId(1L)
                .setWarehouseId(30L).setDeptId(40L).setSetCount(2).setStatus(status)
                .setExecutionRevision(revision)
                .setAssemblyTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpAssemblyOrderItemDO item = new ErpAssemblyOrderItemDO()
                .setId(201L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("组件B").setBatchNoEnabled(false)
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("3.0"));
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(orderItemMapper.selectListByOrderId(100L)).thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setName("成品A").setBatchNoEnabled(false),
                new ErpProductDO().setId(20L).setName("组件B").setBatchNoEnabled(false)));
        Map<String, ErpStockDO> stockMap = new HashMap<>();
        stockMap.put("10_30", new ErpStockDO().setProductId(10L).setWarehouseId(30L)
                .setCount(BigDecimal.ZERO).setLockCount(BigDecimal.ZERO));
        stockMap.put("20_30", new ErpStockDO().setProductId(20L).setWarehouseId(30L)
                .setCount(new BigDecimal("10")).setLockCount(BigDecimal.ZERO));
        when(stockService.getStockMap(any(), any())).thenReturn(stockMap);
        when(warehouseService.getWarehouse(30L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO()
                        .setId(30L).setName("总仓"));
    }

    private void mockBatchCompletionContext(String finishedBatchNo) {
        mockBatchCompletionValidationContext(finishedBatchNo);
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(20L, 30L))
                .thenReturn(new ErpStockDO().setProductId(20L).setWarehouseId(30L)
                        .setCount(new BigDecimal("10")).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("4.00")));
        when(stockRecordMapper.selectBatchBalance(20L, 30L, "PC01"))
                .thenReturn(new BigDecimal("1.0"));
        when(stockRecordMapper.selectBatchBalance(20L, 30L, "PC02"))
                .thenReturn(new BigDecimal("2.0"));
        when(warehouseService.getWarehouse(30L))
                .thenReturn(new cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO()
                        .setId(30L).setName("总仓"));
    }

    private void mockBatchCompletionValidationContext(String finishedBatchNo) {
        ErpAssemblyOrderDO order = new ErpAssemblyOrderDO()
                .setId(100L).setNo("ZZD0001").setProductId(10L).setRecipeId(1L)
                .setWarehouseId(30L).setDeptId(40L).setSetCount(2).setStatus(18)
                .setExecutionRevision(1)
                .setFinishedBatchNo(finishedBatchNo)
                .setAssemblyTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpAssemblyOrderItemDO first = new ErpAssemblyOrderItemDO()
                .setId(201L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("批次组件B").setBatchNoEnabled(true).setBatchNo("PC01")
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("1.0"));
        ErpAssemblyOrderItemDO second = new ErpAssemblyOrderItemDO()
                .setId(202L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("批次组件B").setBatchNoEnabled(true).setBatchNo("PC02")
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("2.0"));
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(orderItemMapper.selectListByOrderId(100L)).thenReturn(Arrays.asList(first, second));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setName("批次成品A").setBatchNoEnabled(true),
                new ErpProductDO().setId(20L).setName("批次组件B").setBatchNoEnabled(true)));
    }

    private void mockVoidContext(BigDecimal componentRecordCount, BigDecimal componentRecordCost) {
        ErpAssemblyOrderDO order = new ErpAssemblyOrderDO()
                .setId(100L).setNo("ZZD0001").setProductId(10L).setRecipeId(1L)
                .setWarehouseId(30L).setDeptId(40L).setSetCount(2).setStatus(20)
                .setExecutionRevision(1)
                .setTotalCost(new BigDecimal("12.000000"))
                .setUnitCost(new BigDecimal("6.000000"))
                .setAssemblyTime(LocalDateTime.of(2026, 10, 22, 10, 0));
        ErpAssemblyOrderItemDO item = new ErpAssemblyOrderItemDO()
                .setId(201L).setOrderId(100L).setRecipeItemId(301L).setProductId(20L)
                .setProductName("组件B").setBatchNoEnabled(false)
                .setQuantityPerSet(new BigDecimal("1.5"))
                .setRequiredQuantity(new BigDecimal("3.0"))
                .setConsumeQuantity(new BigDecimal("3.0"))
                .setCostPrice(new BigDecimal("4.00"))
                .setCostAmount(new BigDecimal("12.000000"));
        when(orderMapper.selectByIdForUpdate(100L)).thenReturn(order);
        when(orderItemMapper.selectListByOrderId(100L)).thenReturn(Collections.singletonList(item));
        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(10L).setName("成品A").setBatchNoEnabled(false),
                new ErpProductDO().setId(20L).setName("组件B").setBatchNoEnabled(false)));
        when(stockMapper.selectByProductIdAndWarehouseIdForUpdate(10L, 30L))
                .thenReturn(new ErpStockDO().setProductId(10L).setWarehouseId(30L)
                        .setCount(new BigDecimal("2")).setLockCount(BigDecimal.ZERO)
                        .setCostPrice(new BigDecimal("6.00")));
        ErpStockRecordDO finished = new ErpStockRecordDO()
                .setId(501L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType())
                .setBizId(100L).setBizItemId(100L).setProductId(10L).setWarehouseId(30L)
                .setCount(new BigDecimal("2")).setUnitPrice(new BigDecimal("6.000000"));
        ErpStockRecordDO component = new ErpStockRecordDO()
                .setId(502L).setBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType())
                .setBizId(100L).setBizItemId(201L).setProductId(20L).setWarehouseId(30L)
                .setCount(componentRecordCount).setUnitPrice(componentRecordCost);
        org.mockito.Mockito.lenient().when(stockRecordMapper.selectListByBizTypesAndBizId(any(), eq(100L)))
                .thenReturn(Arrays.asList(finished, component));
    }
}
