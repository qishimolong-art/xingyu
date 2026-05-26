package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_IN_EXCEED_INABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_UPDATE_FAIL_APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchaseOrderServiceImpl} 的单元测试类
 */
public class ErpPurchaseOrderServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseOrderServiceImpl purchaseOrderService;

    @Mock
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAccountService accountService;

    @BeforeEach
    public void setUp() {
        // 替换 Redis 序号生成器（不连接真实 Redis）
        ReflectionTestUtils.setField(purchaseOrderService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    private ErpPurchaseOrderSaveReqVO.Item buildItem(Long productId, BigDecimal count, BigDecimal price) {
        ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
        item.setProductId(productId);
        item.setCount(count);
        item.setProductPrice(price);
        return item;
    }

    private ErpPurchaseOrderSaveReqVO buildBaseReqVO(Long supplierId, ErpPurchaseOrderSaveReqVO.Item... items) {
        ErpPurchaseOrderSaveReqVO vo = new ErpPurchaseOrderSaveReqVO();
        vo.setSupplierId(supplierId);
        vo.setOrderTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        vo.setItems(Arrays.asList(items));
        return vo;
    }

    // ========== createPurchaseOrder ==========

    @Test
    public void testCreatePurchaseOrder_success() {
        Long supplierId = 100L;
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(supplierId, item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setName("螺丝")));
        when(purchaseOrderMapper.selectByNo(any())).thenReturn(null);

        purchaseOrderService.createPurchaseOrder(reqVO);

        verify(supplierService).validateSupplier(eq(supplierId));
        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).insert(orderCaptor.capture());
        ErpPurchaseOrderDO inserted = orderCaptor.getValue();
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals("CGDD20260520000001", inserted.getNo());
        assertEquals(new BigDecimal("10"), inserted.getTotalCount());
        // totalPrice = 50 - 50*0% = 50
        assertEquals(0, inserted.getTotalPrice().compareTo(new BigDecimal("50.00")));
        verify(purchaseOrderItemMapper).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseOrder_giftItemForcesZeroPrice() {
        ErpPurchaseOrderSaveReqVO.Item gift = buildItem(200L, new BigDecimal("3"), new BigDecimal("99"));
        gift.setGift(true);
        ErpPurchaseOrderSaveReqVO.Item normal = buildItem(201L, new BigDecimal("2"), new BigDecimal("100"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, gift, normal);

        when(productService.validProductList(any())).thenReturn(Arrays.asList(
                new ErpProductDO().setId(200L).setUnitId(1L),
                new ErpProductDO().setId(201L).setUnitId(1L)));

        purchaseOrderService.createPurchaseOrder(reqVO);

        ArgumentCaptor<List<ErpPurchaseOrderItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseOrderItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseOrderItemDO> items = itemsCaptor.getValue();
        // 赠品行：productPrice/totalPrice/taxPrice 全部强制为 0
        ErpPurchaseOrderItemDO giftItem = items.stream()
                .filter(it -> it.getProductId().equals(200L)).findFirst().orElseThrow(AssertionError::new);
        assertEquals(0, giftItem.getProductPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, giftItem.getTotalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, giftItem.getTaxPrice().compareTo(BigDecimal.ZERO));

        // 主表合计：totalCount 包含赠品行数量（3+2=5），totalProductPrice 排除赠品（仅 200）
        ArgumentCaptor<ErpPurchaseOrderDO> orderCaptor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).insert(orderCaptor.capture());
        ErpPurchaseOrderDO order = orderCaptor.getValue();
        assertEquals(0, order.getTotalCount().compareTo(new BigDecimal("5")));
        assertEquals(0, order.getTotalProductPrice().compareTo(new BigDecimal("200.00")));
    }

    @Test
    public void testCreatePurchaseOrder_countZero_throwException() {
        ErpPurchaseOrderSaveReqVO.Item invalid = buildItem(200L, BigDecimal.ZERO, new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, invalid);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_COUNT_POSITIVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testCreatePurchaseOrder_priceZero_throwException() {
        ErpPurchaseOrderSaveReqVO.Item invalid = buildItem(200L, new BigDecimal("5"), BigDecimal.ZERO);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, invalid);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.createPurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_PRICE_POSITIVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).insert(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testCreatePurchaseOrder_withAccountId_validatesAccount() {
        ErpPurchaseOrderSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, item);
        reqVO.setAccountId(50L);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));

        purchaseOrderService.createPurchaseOrder(reqVO);

        verify(accountService).validateAccount(eq(50L));
    }

    // ========== updatePurchaseOrder ==========

    @Test
    public void testUpdatePurchaseOrder_alreadyApproved_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setNo("CGDD001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L,
                buildItem(200L, new BigDecimal("1"), new BigDecimal("5")));
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_UPDATE_FAIL_APPROVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).updateById(any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testUpdatePurchaseOrder_notExists_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L);
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrder_giftModifyWhenHasIn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        // 旧项已入库 + gift=false；新项试图改为 gift=true
        ErpPurchaseOrderItemDO oldItem = new ErpPurchaseOrderItemDO()
                .setId(99L).setProductId(200L).setCount(new BigDecimal("5"))
                .setInCount(new BigDecimal("2")).setGift(false);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(oldItem));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));

        ErpPurchaseOrderSaveReqVO.Item changedGift = buildItem(200L, new BigDecimal("5"), new BigDecimal("10"));
        changedGift.setId(99L);
        changedGift.setGift(true);
        ErpPurchaseOrderSaveReqVO reqVO = buildBaseReqVO(100L, changedGift);
        reqVO.setId(10L);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrder(reqVO));
        assertEquals(PURCHASE_ORDER_ITEM_GIFT_MODIFY_FAIL_HAS_IN.getCode(), ex.getCode());
    }

    // ========== updatePurchaseOrderStatus ==========

    @Test
    public void testUpdatePurchaseOrderStatus_approveSuccess() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseOrderMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseOrderDO.class))).thenReturn(1);

        purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(purchaseOrderMapper).updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseOrderDO.class));
    }

    @Test
    public void testUpdatePurchaseOrderStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(PURCHASE_ORDER_APPROVE_FAIL.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenAlreadyProcess_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenHasIn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(new BigDecimal("3")).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_processWhenHasReturn_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(new BigDecimal("1"));
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.PROCESS.getStatus()));
        assertEquals(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderStatus_approveLostByOptimisticLock_throwException() {
        ErpPurchaseOrderDO existing = new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseOrderMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderStatus(10L, ErpAuditStatus.APPROVE.getStatus()));
        assertEquals(PURCHASE_ORDER_APPROVE_FAIL.getCode(), ex.getCode());
    }

    // ========== updatePurchaseOrderInCount ==========

    @Test
    public void testUpdatePurchaseOrderInCount_success() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("3"));

        purchaseOrderService.updatePurchaseOrderInCount(10L, inMap);

        verify(purchaseOrderItemMapper).updateById(any(ErpPurchaseOrderItemDO.class));
        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getInCount().compareTo(new BigDecimal("3")));
    }

    @Test
    public void testUpdatePurchaseOrderInCount_excess_throwException() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("11")); // 超过 count

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderInCount(10L, inMap));
        assertEquals(PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderInCount_unchanged_skipUpdate() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("3"));
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> inMap = new HashMap<>();
        inMap.put(1L, new BigDecimal("3")); // 与现有相同

        purchaseOrderService.updatePurchaseOrderInCount(10L, inMap);

        verify(purchaseOrderItemMapper, never()).updateById(any(ErpPurchaseOrderItemDO.class));
        // 父订单更新仍发生
        verify(purchaseOrderMapper).updateById(any(ErpPurchaseOrderDO.class));
    }

    // ========== updatePurchaseOrderReturnCount ==========

    @Test
    public void testUpdatePurchaseOrderReturnCount_excess_throwException() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("5"))
                .setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO().setId(200L).setName("螺丝"));
        Map<Long, BigDecimal> returnMap = new HashMap<>();
        returnMap.put(1L, new BigDecimal("6")); // 超过 inCount

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.updatePurchaseOrderReturnCount(10L, returnMap));
        assertEquals(PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED.getCode(), ex.getCode());
    }

    @Test
    public void testUpdatePurchaseOrderReturnCount_success() {
        ErpPurchaseOrderItemDO item = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("5"))
                .setReturnCount(BigDecimal.ZERO);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> returnMap = new HashMap<>();
        returnMap.put(1L, new BigDecimal("2"));

        purchaseOrderService.updatePurchaseOrderReturnCount(10L, returnMap);

        verify(purchaseOrderItemMapper).updateById(any(ErpPurchaseOrderItemDO.class));
        ArgumentCaptor<ErpPurchaseOrderDO> captor = ArgumentCaptor.forClass(ErpPurchaseOrderDO.class);
        verify(purchaseOrderMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getReturnCount().compareTo(new BigDecimal("2")));
    }

    // ========== deletePurchaseOrder ==========

    @Test
    public void testDeletePurchaseOrder_success() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.singletonList(order));

        purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L));

        verify(purchaseOrderMapper).deleteById(eq(10L));
        verify(purchaseOrderItemMapper).deleteByOrderId(eq(10L));
    }

    @Test
    public void testDeletePurchaseOrder_alreadyApproved_throwException() {
        ErpPurchaseOrderDO approved = new ErpPurchaseOrderDO().setId(10L).setNo("CGDD001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.singletonList(approved));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L)));
        assertEquals(PURCHASE_ORDER_DELETE_FAIL_APPROVE.getCode(), ex.getCode());
        verify(purchaseOrderMapper, never()).deleteById(any(Long.class));
    }

    @Test
    public void testDeletePurchaseOrder_emptyList_noOp() {
        when(purchaseOrderMapper.selectByIds(any())).thenReturn(Collections.emptyList());

        purchaseOrderService.deletePurchaseOrder(Collections.singletonList(10L));

        verify(purchaseOrderMapper, never()).deleteById(any(Long.class));
        verify(purchaseOrderItemMapper, never()).deleteByOrderId(any(Long.class));
    }

    // ========== validatePurchaseOrder ==========

    @Test
    public void testValidatePurchaseOrder_notApproved_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(
                new ErpPurchaseOrderDO().setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.validatePurchaseOrder(10L));
        assertEquals(PURCHASE_ORDER_NOT_APPROVE.getCode(), ex.getCode());
    }

    @Test
    public void testValidatePurchaseOrder_success() {
        ErpPurchaseOrderDO approved = new ErpPurchaseOrderDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(approved);

        ErpPurchaseOrderDO result = purchaseOrderService.validatePurchaseOrder(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
    }

    @Test
    public void testValidatePurchaseOrder_notExists_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.validatePurchaseOrder(10L));
        assertEquals(PURCHASE_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ========== getPurchaseOrderItemListByOrderIds ==========

    @Test
    public void testGetPurchaseOrderItemListByOrderIds_emptyInput_returnsEmpty() {
        List<ErpPurchaseOrderItemDO> result = purchaseOrderService
                .getPurchaseOrderItemListByOrderIds(Collections.emptyList());
        assertTrue(result.isEmpty());
        verify(purchaseOrderItemMapper, never()).selectListByOrderIds(any());
    }

    @Test
    public void testGetPurchaseOrderItemListByOrderIds_delegatesToMapper() {
        List<ErpPurchaseOrderItemDO> mocked = Collections.singletonList(new ErpPurchaseOrderItemDO().setId(1L));
        when(purchaseOrderItemMapper.selectListByOrderIds(any())).thenReturn(mocked);

        List<ErpPurchaseOrderItemDO> result = purchaseOrderService
                .getPurchaseOrderItemListByOrderIds(Collections.singletonList(10L));

        assertEquals(1, result.size());
    }

    // ========== getInableItemsByOrderId ==========

    @Test
    public void testGetInableItemsByOrderId_success() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus()));
        ErpPurchaseOrderItemDO item1 = new ErpPurchaseOrderItemDO()
                .setId(1L).setProductId(200L).setProductPrice(new BigDecimal("10"))
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("3"))
                .setWarehouseId(7L);
        ErpPurchaseOrderItemDO itemAllIn = new ErpPurchaseOrderItemDO()
                .setId(2L).setProductId(201L).setProductPrice(new BigDecimal("20"))
                .setCount(new BigDecimal("5")).setInCount(new BigDecimal("5"))
                .setWarehouseId(7L);
        when(purchaseOrderItemMapper.selectListByOrderId(eq(10L)))
                .thenReturn(Arrays.asList(item1, itemAllIn));
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        ErpProductRespVO p1 = new ErpProductRespVO();
        p1.setId(200L);
        p1.setName("螺丝");
        p1.setCode("P001");
        productMap.put(200L, p1);
        when(productService.getProductVOMap(any())).thenReturn(productMap);

        List<ErpPurchaseOrderInableItemRespVO> result = purchaseOrderService.getInableItemsByOrderId(10L);

        // item1 应有结果（inable = 10-3 = 7），itemAllIn 已全部入库被 convertList 过滤掉
        assertEquals(1, result.size());
        ErpPurchaseOrderInableItemRespVO vo = result.get(0);
        assertNotNull(vo);
        assertEquals(Long.valueOf(1L), vo.getOrderItemId());
        assertEquals(0, vo.getInableCount().compareTo(new BigDecimal("7")));
        assertEquals("螺丝", vo.getProductName());
    }

    @Test
    public void testGetInableItemsByOrderId_orderNotApproved_throwException() {
        when(purchaseOrderMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseOrderDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> purchaseOrderService.getInableItemsByOrderId(10L));
        assertEquals(PURCHASE_ORDER_NOT_APPROVE.getCode(), ex.getCode());
    }

    // ========== 涵盖文档要求 PURCHASE_ORDER_IN_EXCEED_INABLE 的常量引用 ==========

    @Test
    public void testInExceedInableErrorCodeValue() {
        // 该错误码用于采购入库 Service 的"分批入库"分支引用；本测试用例确保枚举值未被破坏
        assertEquals(1_030_101_012, PURCHASE_ORDER_IN_EXCEED_INABLE.getCode());
    }

}
