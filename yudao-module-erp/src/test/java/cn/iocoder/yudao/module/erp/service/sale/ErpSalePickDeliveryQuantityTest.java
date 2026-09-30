package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.service.stock.*;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class ErpSalePickDeliveryQuantityTest extends BaseMockitoUnitTest {
    @InjectMocks private ErpSalePickDeliveryServiceImpl service;
    @Mock private ErpSalePickDeliveryOrderMapper orderMapper;
    @Mock private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Mock private ErpSalePickDeliveryItemMapper itemMapper;
    @Mock private ErpSalePickDeliverySubmitMapper submitMapper;
    @Mock private ErpSalePickDeliverySubmitFileMapper submitFileMapper;
    @Mock private ErpSalePickDeliverySubmitItemMapper submitItemMapper;
    @Mock private ErpWarehouseService warehouseService;
    @Mock private ErpStockMoveService stockMoveService;
    private MockedStatic<SecurityFrameworkUtils> security;
    private ErpSalePickDeliveryOrderDO order;
    private ErpSalePickDeliveryPickTaskDO task;
    private ErpSalePickDeliveryItemDO item;
    private final Map<String, ErpSalePickDeliverySubmitDO> requests = new HashMap<>();

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        security = mockStatic(SecurityFrameworkUtils.class);
        security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(88L);
        order = new ErpSalePickDeliveryOrderDO().setId(1L).setPickStatus(10).setDeliveryStatus(5)
                .setTotalItemCount(1).setSourceType(30).setSaleOutId(3L);
        task = new ErpSalePickDeliveryPickTaskDO().setId(2L).setOrderId(1L).setWarehouseId(4L)
                .setStatus(10).setTotalItemCount(1).setSaleOutId(3L);
        item = new ErpSalePickDeliveryItemDO().setId(5L).setOrderId(1L).setPickTaskId(2L)
                .setCount(new BigDecimal("10")).setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                .setPickStatus(10).setDeliveryStatus(5).setTransferOutId(6L).setProductName("测试配件");
        when(pickTaskMapper.selectById(2L)).thenReturn(task);
        when(pickTaskMapper.selectByIdForUpdate(2L)).thenReturn(task);
        when(orderMapper.selectByIdForUpdate(1L)).thenReturn(order);
        when(warehouseService.getUserPickWarehouseIds(88L)).thenReturn(Collections.singletonList(4L));
        when(itemMapper.selectListByIdsForUpdate(anyCollection())).thenReturn(Collections.singletonList(item));
        when(itemMapper.selectListByOrderId(1L)).thenReturn(Collections.singletonList(item));
        when(itemMapper.updateById(any(ErpSalePickDeliveryItemDO.class))).thenAnswer(inv -> {
            ErpSalePickDeliveryItemDO update = inv.getArgument(0);
            if (update.getPickedCount() != null) item.setPickedCount(update.getPickedCount());
            if (update.getDeliveredCount() != null) item.setDeliveredCount(update.getDeliveredCount());
            if (update.getPickStatus() != null) item.setPickStatus(update.getPickStatus());
            if (update.getDeliveryStatus() != null) item.setDeliveryStatus(update.getDeliveryStatus());
            return 1;
        });
        when(itemMapper.selectCountByPickTaskIdAndPickStatus(2L, 30)).thenAnswer(inv -> item.getPickStatus() == 30 ? 1L : 0L);
        when(itemMapper.selectCountByOrderIdAndPickStatus(1L, 30)).thenAnswer(inv -> item.getPickStatus() == 30 ? 1L : 0L);
        when(itemMapper.selectCountByOrderIdAndDeliveryStatus(1L, 30)).thenAnswer(inv -> item.getDeliveryStatus() == 30 ? 1L : 0L);
        when(itemMapper.hasPickProgress(any(), any())).thenAnswer(inv -> item.getPickedCount().signum() > 0);
        when(itemMapper.hasDeliveryProgress(1L)).thenAnswer(inv -> item.getDeliveredCount().signum() > 0);
        when(pickTaskMapper.updateById(any(ErpSalePickDeliveryPickTaskDO.class))).thenAnswer(inv -> {
            task.setStatus(((ErpSalePickDeliveryPickTaskDO) inv.getArgument(0)).getStatus()); return 1;
        });
        when(orderMapper.updateById(any(ErpSalePickDeliveryOrderDO.class))).thenAnswer(inv -> {
            ErpSalePickDeliveryOrderDO update = inv.getArgument(0);
            if (update.getPickStatus() != null) order.setPickStatus(update.getPickStatus());
            if (update.getDeliveryStatus() != null) order.setDeliveryStatus(update.getDeliveryStatus());
            return 1;
        });
        when(submitMapper.insert(any(ErpSalePickDeliverySubmitDO.class))).thenAnswer(inv -> {
            ErpSalePickDeliverySubmitDO submit = inv.getArgument(0);
            submit.setId((long) requests.size() + 1);
            if (submit.getRequestId() != null) requests.put(submit.getType() + submit.getRequestId(), submit);
            return 1;
        });
        when(submitMapper.selectByRequestId(eq(1L), anyInt(), anyString())).thenAnswer(inv ->
                requests.get(inv.getArgument(1).toString() + inv.getArgument(2)));
    }

    @AfterEach void cleanup() { security.close(); TenantContextHolder.clear(); }

    private ErpSalePickSubmitReqVO.File file() {
        ErpSalePickSubmitReqVO.File file = new ErpSalePickSubmitReqVO.File(); file.setFileUrl("https://example.test/voucher.png"); return file;
    }
    private ErpSalePickDeliverySubmitItemReqVO quantity(String value) {
        ErpSalePickDeliverySubmitItemReqVO row = new ErpSalePickDeliverySubmitItemReqVO();
        row.setItemId(5L); row.setQuantity(value == null ? null : new BigDecimal(value)); return row;
    }
    private ErpSalePickSubmitReqVO pick(String value) {
        ErpSalePickSubmitReqVO req = new ErpSalePickSubmitReqVO(); req.setTaskId(2L);
        req.setRequestId(UUID.randomUUID().toString()); req.setItems(Collections.singletonList(quantity(value)));
        req.setFiles(Collections.singletonList(file())); return req;
    }
    private ErpSaleDeliverySubmitReqVO delivery(String value) {
        ErpSaleDeliverySubmitReqVO req = new ErpSaleDeliverySubmitReqVO(); req.setOrderId(1L);
        req.setRequestId(UUID.randomUUID().toString()); req.setItems(Collections.singletonList(quantity(value)));
        req.setFiles(Collections.singletonList(file())); return req;
    }

    @Test void sixPlusFour_shouldCompleteOnlyAfterFinalBatchAndKeepOriginalCount() {
        service.submitPick(pick("6"));
        assertEquals(new BigDecimal("6"), item.getPickedCount());
        assertEquals(20, item.getPickStatus()); assertEquals(20, task.getStatus()); assertEquals(20, order.getPickStatus());
        assertServiceException(() -> service.submitDelivery(delivery("6")), SALE_DELIVERY_NOT_READY);
        service.submitPick(pick("4"));
        assertEquals(30, task.getStatus()); assertEquals(30, order.getPickStatus()); assertEquals(10, order.getDeliveryStatus());
        assertEquals(new BigDecimal("10"), item.getCount());
        service.submitDelivery(delivery("6"));
        assertEquals(20, item.getDeliveryStatus()); assertEquals(20, order.getDeliveryStatus());
        verifyNoInteractions(stockMoveService);
        ErpSaleDeliverySubmitReqVO last = delivery("4"); service.submitDelivery(last); service.submitDelivery(last);
        assertEquals(30, order.getDeliveryStatus());
        verify(stockMoveService, times(1)).approveSaleCartTransferOutAfterDelivery(6L, 88L);
        verify(submitItemMapper, times(4)).insert(any(ErpSalePickDeliverySubmitItemDO.class));
    }
    @Test void retryPartialAndCompleted_shouldNotAccumulateTwice() {
        ErpSalePickSubmitReqVO req = pick("6"); service.submitPick(req); service.submitPick(req);
        assertEquals(new BigDecimal("6"), item.getPickedCount());
        ErpSalePickSubmitReqVO last = pick("4"); service.submitPick(last); service.submitPick(last);
        assertEquals(new BigDecimal("10"), item.getPickedCount());
        verify(submitMapper, times(2)).insert(any(ErpSalePickDeliverySubmitDO.class));
    }
    @Test void reusedRequestForChangedQuantity_shouldReject() {
        ErpSalePickSubmitReqVO req = pick("6"); service.submitPick(req); req.setItems(Collections.singletonList(quantity("3")));
        assertServiceException(() -> service.submitPick(req), SALE_PICK_DELIVERY_REQUEST_CONFLICT);
    }
    @Test void latestRemaining_shouldRejectStaleOverAllocation() {
        service.submitPick(pick("6"));
        assertServiceException(() -> service.submitPick(pick("6")), SALE_PICK_DELIVERY_QUANTITY_INVALID);
        assertEquals(new BigDecimal("6"), item.getPickedCount());
        verify(submitMapper, times(1)).insert(any(ErpSalePickDeliverySubmitDO.class));
    }
    @Test void invalidQuantities_shouldFailBeforeWriting() {
        for (String value : Arrays.asList(null, "0", "-1", "0.5", "0.000001", "10.000001", "0.1234567", "1000000000000000000")) {
            assertServiceException(() -> service.submitPick(pick(value)), SALE_PICK_DELIVERY_QUANTITY_INVALID);
        }
        verify(submitMapper, never()).insert(any(ErpSalePickDeliverySubmitDO.class));
    }
    @Test void fractionalDelivery_shouldFailBeforeWriting() {
        service.submitPick(pick("10"));
        assertServiceException(() -> service.submitDelivery(delivery("0.5")), SALE_PICK_DELIVERY_QUANTITY_INVALID);
        assertEquals(0, item.getDeliveredCount().signum());
        verifyNoInteractions(stockMoveService);
    }
    @Test void requestValidation_shouldRejectDecimals() {
        javax.validation.Validator validator = javax.validation.Validation.buildDefaultValidatorFactory().getValidator();
        assertTrue(validator.validate(quantity("6")).isEmpty());
        assertFalse(validator.validate(quantity("0.5")).isEmpty());
        assertFalse(validator.validate(quantity("1.0")).isEmpty());
    }
    @Test void legacyIds_shouldSubmitRemainingAfterPartial() {
        item.setCount(new BigDecimal("10.000000"));
        service.submitPick(pick("6")); ErpSalePickSubmitReqVO legacy = pick("1");
        legacy.setItems(null); legacy.setRequestId(null); legacy.setItemIds(Collections.singletonList(5L));
        service.submitPick(legacy); assertEquals(30, item.getPickStatus());
        ArgumentCaptor<ErpSalePickDeliverySubmitItemDO> captor = ArgumentCaptor.forClass(ErpSalePickDeliverySubmitItemDO.class);
        verify(submitItemMapper, times(2)).insert(captor.capture());
        assertEquals(0, new BigDecimal("4").compareTo(captor.getAllValues().get(1).getQuantity()));
    }
    @Test void legacyIds_shouldRejectFractionalRemainingWithoutRounding() {
        item.setCount(new BigDecimal("1.5"));
        ErpSalePickSubmitReqVO legacy = pick("1");
        legacy.setItems(null); legacy.setRequestId(null); legacy.setItemIds(Collections.singletonList(5L));
        assertServiceException(() -> service.submitPick(legacy), SALE_PICK_DELIVERY_QUANTITY_INVALID);
        verify(submitMapper, never()).insert(any(ErpSalePickDeliverySubmitDO.class));
        verify(itemMapper, never()).updateById(any(ErpSalePickDeliveryItemDO.class));
    }
    @Test void malformedPayload_shouldRejectDuplicateAndMixedFormatsAndMissingRequest() {
        ErpSalePickSubmitReqVO duplicate = pick("1"); duplicate.setItems(Arrays.asList(quantity("1"), quantity("2")));
        assertServiceException(() -> service.submitPick(duplicate), SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        ErpSalePickSubmitReqVO mixed = pick("1"); mixed.setItemIds(Collections.singletonList(5L));
        assertServiceException(() -> service.submitPick(mixed), SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        ErpSalePickSubmitReqVO missing = pick("1"); missing.setRequestId(null);
        assertServiceException(() -> service.submitPick(missing), SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        verifyNoInteractions(submitItemMapper);
    }
    @Test void wrongTask_shouldRejectBeforeWriting() {
        item.setPickTaskId(99L);
        assertServiceException(() -> service.submitPick(pick("1")), SALE_PICK_ITEM_INVALID);
        verify(submitMapper, never()).insert(any(ErpSalePickDeliverySubmitDO.class));
    }
    @Test void warehousePermissionAndVoucher_shouldRemainRequired() {
        when(warehouseService.getUserPickWarehouseIds(88L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> service.submitPick(pick("1")), SALE_PICK_TASK_WAREHOUSE_PERMISSION_DENIED);
        ErpSalePickSubmitReqVO noFile = pick("1"); noFile.setFiles(Collections.emptyList());
        assertServiceException(() -> service.submitPick(noFile), SALE_PICK_DELIVERY_FILE_REQUIRED);
        verify(submitMapper, never()).insert(any(ErpSalePickDeliverySubmitDO.class));
    }
    @Test void allItemsValidatedBeforeAnyWrite() {
        ErpSalePickDeliveryItemDO other = new ErpSalePickDeliveryItemDO().setId(7L).setOrderId(1L).setPickTaskId(2L)
                .setCount(BigDecimal.ONE).setPickedCount(BigDecimal.ZERO).setPickStatus(10);
        when(itemMapper.selectListByIdsForUpdate(anyCollection())).thenReturn(Arrays.asList(item, other));
        ErpSalePickSubmitReqVO req = pick("6"); ErpSalePickDeliverySubmitItemReqVO bad = quantity("2"); bad.setItemId(7L);
        req.setItems(Arrays.asList(quantity("6"), bad));
        assertServiceException(() -> service.submitPick(req), SALE_PICK_DELIVERY_QUANTITY_INVALID);
        verify(submitMapper, never()).insert(any(ErpSalePickDeliverySubmitDO.class));
        verify(itemMapper, never()).updateById(any(ErpSalePickDeliveryItemDO.class));
    }
    @Test void multipleWarehouses_shouldKeepWholeOrderBlockedUntilEveryItemIsPicked() {
        order.setTotalItemCount(2);
        service.submitPick(pick("6"));
        assertEquals(20, order.getPickStatus()); assertEquals(5, order.getDeliveryStatus());
        service.submitPick(pick("4"));
        assertEquals(30, task.getStatus()); assertEquals(20, order.getPickStatus());
        assertServiceException(() -> service.submitDelivery(delivery("1")), SALE_DELIVERY_NOT_READY);
    }

    @Test void submitDetail_shouldRejectUnrelatedBatch() {
        when(submitMapper.selectById(100L)).thenReturn(new ErpSalePickDeliverySubmitDO()
                .setId(100L).setPickTaskId(99L).setOrderId(1L).setType(10));
        assertServiceException(() -> service.getSubmitItemPage(2L, 100L, true,
                new cn.iocoder.yudao.framework.common.pojo.PageParam()), SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        verifyNoInteractions(submitItemMapper);
    }

}
