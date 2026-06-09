package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseReturnModeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
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

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BIZ_PROCESS_FAIL_VOUCHER_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_BY_ORDER_SOURCE_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_EXCEED_RETURNABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_ITEM_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_MODE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_SOURCE_IN_ITEM_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_SUPPLIER_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_RETURN_UPDATE_FAIL_APPROVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchaseReturnServiceImpl} 的单元测试类
 */
public class ErpPurchaseReturnServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseReturnServiceImpl purchaseReturnService;

    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Mock
    private ErpVoucherService voucherService;
    @Mock
    private ErpBookOpenService bookOpenService;
    @Mock
    private ErpVoucherMapper voucherMapper;
    @Mock
    private ErpVoucherItemMapper voucherItemMapper;

    @BeforeEach
    public void setUp() {
        // 替换 Redis 序号生成器（不连接真实 Redis）
        ReflectionTestUtils.setField(purchaseReturnService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    // ==================== Helper ====================

    private ErpPurchaseReturnSaveReqVO.Item buildItem(Long productId, BigDecimal count, BigDecimal price) {
        ErpPurchaseReturnSaveReqVO.Item item = new ErpPurchaseReturnSaveReqVO.Item();
        item.setProductId(productId);
        item.setProductUnitId(1L);
        item.setWarehouseId(10L);
        item.setCount(count);
        item.setProductPrice(price);
        return item;
    }

    private ErpPurchaseReturnSaveReqVO.Item buildByOrderItem(Long productId, BigDecimal count, BigDecimal price,
                                                            Long sourceInItemId) {
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(productId, count, price);
        item.setSourceInId(900L);
        item.setSourceInItemId(sourceInItemId);
        item.setSourceInNo("RKD20260101001");
        return item;
    }

    private ErpPurchaseReturnSaveReqVO buildByStockReqVO(ErpPurchaseReturnSaveReqVO.Item... items) {
        ErpPurchaseReturnSaveReqVO vo = new ErpPurchaseReturnSaveReqVO();
        vo.setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        vo.setReturnMode(ErpPurchaseReturnModeEnum.BY_STOCK.getMode());
        vo.setSupplierId(100L);
        vo.setItems(Arrays.asList(items));
        return vo;
    }

    private ErpPurchaseReturnSaveReqVO buildByOrderReqVO(ErpPurchaseReturnSaveReqVO.Item... items) {
        ErpPurchaseReturnSaveReqVO vo = new ErpPurchaseReturnSaveReqVO();
        vo.setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        vo.setReturnMode(ErpPurchaseReturnModeEnum.BY_ORDER.getMode());
        vo.setSupplierId(100L);
        vo.setItems(Arrays.asList(items));
        return vo;
    }

    private ErpPurchaseInItemDO buildInItem(Long id, BigDecimal count) {
        return new ErpPurchaseInItemDO().setId(id).setCount(count);
    }

    // ==================== createPurchaseReturn ====================

    @Test
    public void testCreatePurchaseReturn_byStock_success() {
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("10"));
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setName("螺丝")));
        when(purchaseReturnMapper.selectByNo(any())).thenReturn(null);

        Long id = purchaseReturnService.createPurchaseReturn(reqVO);

        ArgumentCaptor<ErpPurchaseReturnDO> captor = ArgumentCaptor.forClass(ErpPurchaseReturnDO.class);
        verify(purchaseReturnMapper).insert(captor.capture());
        ErpPurchaseReturnDO inserted = captor.getValue();
        assertEquals("CGTH20260520000001", inserted.getNo());
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals(Long.valueOf(100L), inserted.getSupplierId());
        assertEquals(0, inserted.getTotalCount().compareTo(new BigDecimal("3")));
        verify(purchaseReturnItemMapper).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseReturn_byOrder_success() {
        ErpPurchaseReturnSaveReqVO.Item item = buildByOrderItem(200L, new BigDecimal("3"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(item);

        // 原入库项 999 已入 10
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                buildInItem(999L, new BigDecimal("10"))));
        // 其他退货单已退：0
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(any(), isNull()))
                .thenReturn(Collections.emptyMap());
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseReturnMapper.selectByNo(any())).thenReturn(null);

        purchaseReturnService.createPurchaseReturn(reqVO);

        verify(purchaseReturnMapper).insert(any(ErpPurchaseReturnDO.class));
        verify(purchaseReturnItemMapper).insertBatch(anyList());
    }

    @Test
    public void testCreatePurchaseReturn_returnModeNull_throwException() {
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("1"), new BigDecimal("5")));
        reqVO.setReturnMode(null);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_MODE_INVALID);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_returnModeInvalid_throwException() {
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("1"), new BigDecimal("5")));
        reqVO.setReturnMode(99); // 非法值

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_MODE_INVALID);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byOrder_sourceRequired_throwException() {
        // byOrder 模式但 sourceInItemId 为 null
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("10"));
        item.setSourceInItemId(null);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(item);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_BY_ORDER_SOURCE_REQUIRED);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byOrder_sourceInItemNotExists_throwException() {
        ErpPurchaseReturnSaveReqVO.Item item = buildByOrderItem(200L, new BigDecimal("3"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(item);

        // 入库项 999 在数据库中不存在
        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(any(), isNull()))
                .thenReturn(Collections.emptyMap());

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_SOURCE_IN_ITEM_NOT_EXISTS);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byOrder_exceedReturnable_throwException() {
        // 原入库 10，其他已退 6，本次退 5 → 超出 (10-6=4)
        ErpPurchaseReturnSaveReqVO.Item item = buildByOrderItem(200L, new BigDecimal("5"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(item);

        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                buildInItem(999L, new BigDecimal("10"))));
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(999L, new BigDecimal("6"));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(any(), isNull()))
                .thenReturn(returnedMap);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_EXCEED_RETURNABLE, 999L, new BigDecimal("4"), new BigDecimal("5"));
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byStock_countZero_throwException() {
        // byStock 模式下，count<=0 走到 validatePurchaseReturnItems 抛 ITEM_COUNT_POSITIVE
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, BigDecimal.ZERO, new BigDecimal("10"));
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_ITEM_COUNT_POSITIVE);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byStock_priceZero_throwException() {
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), BigDecimal.ZERO);
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_ITEM_PRICE_POSITIVE);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byStock_supplierRequired_throwException() {
        // byStock + supplierId=null（既无 orderId 带出，也未填写）
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("10"));
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);
        reqVO.setSupplierId(null);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseReturnMapper.selectByNo(any())).thenReturn(null);

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_SUPPLIER_REQUIRED);
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_noAlreadyExists_throwException() {
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("10"));
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        // 单号已存在
        when(purchaseReturnMapper.selectByNo(any())).thenReturn(new ErpPurchaseReturnDO().setId(99L));

        // PURCHASE_RETURN_NO_EXISTS 无参
        org.junit.jupiter.api.Assertions.assertThrows(
                cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> purchaseReturnService.createPurchaseReturn(reqVO));
        verify(purchaseReturnMapper, never()).insert(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testCreatePurchaseReturn_byOrder_aggregateCountExceedsReturnable_throwException() {
        // 多行命中同一 sourceInItemId=999，累加 3+4=7 > 入库 5，应抛错
        ErpPurchaseReturnSaveReqVO.Item itemA = buildByOrderItem(200L, new BigDecimal("3"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO.Item itemB = buildByOrderItem(200L, new BigDecimal("4"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(itemA, itemB);

        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                buildInItem(999L, new BigDecimal("5"))));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(any(), isNull()))
                .thenReturn(Collections.emptyMap());

        assertServiceException(() -> purchaseReturnService.createPurchaseReturn(reqVO),
                PURCHASE_RETURN_EXCEED_RETURNABLE, 999L, new BigDecimal("5"), new BigDecimal("7"));
    }

    @Test
    public void testCreatePurchaseReturn_withOrderId_validatesPurchaseOrder() {
        // 带 orderId 时，应调 purchaseOrderService.validatePurchaseOrder 校验
        ErpPurchaseReturnSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("10"));
        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(item);
        reqVO.setOrderId(555L);
        reqVO.setSupplierId(null); // 让 supplierId 从 order 带出

        when(purchaseOrderService.validatePurchaseOrder(eq(555L))).thenReturn(
                new ErpPurchaseOrderDO().setId(555L).setNo("CGDD001").setSupplierId(100L).setDeptId(88L));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseReturnMapper.selectByNo(any())).thenReturn(null);

        purchaseReturnService.createPurchaseReturn(reqVO);

        verify(purchaseOrderService).validatePurchaseOrder(eq(555L));
        ArgumentCaptor<ErpPurchaseReturnDO> captor = ArgumentCaptor.forClass(ErpPurchaseReturnDO.class);
        verify(purchaseReturnMapper).insert(captor.capture());
        // 从订单带出供应商
        assertEquals(Long.valueOf(100L), captor.getValue().getSupplierId());
        assertEquals("CGDD001", captor.getValue().getOrderNo());
        assertEquals(Long.valueOf(88L), captor.getValue().getDeptId());
    }

    // ==================== updatePurchaseReturn ====================

    @Test
    public void testUpdatePurchaseReturn_success() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L))).thenReturn(Collections.emptyList());

        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("3"), new BigDecimal("10")));
        reqVO.setId(10L);

        purchaseReturnService.updatePurchaseReturn(reqVO);

        verify(purchaseReturnMapper).updateById(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testUpdatePurchaseReturn_notExists_throwException() {
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(null);

        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("3"), new BigDecimal("10")));
        reqVO.setId(10L);

        assertServiceException(() -> purchaseReturnService.updatePurchaseReturn(reqVO),
                PURCHASE_RETURN_NOT_EXISTS);
        verify(purchaseReturnMapper, never()).updateById(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testUpdatePurchaseReturn_alreadyApproved_throwException() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("3"), new BigDecimal("10")));
        reqVO.setId(10L);

        assertServiceException(() -> purchaseReturnService.updatePurchaseReturn(reqVO),
                PURCHASE_RETURN_UPDATE_FAIL_APPROVE, "CGTH001");
        verify(purchaseReturnMapper, never()).updateById(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testUpdatePurchaseReturn_returnModeInvalid_throwException() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        ErpPurchaseReturnSaveReqVO reqVO = buildByStockReqVO(
                buildItem(200L, new BigDecimal("3"), new BigDecimal("10")));
        reqVO.setId(10L);
        reqVO.setReturnMode(null);

        assertServiceException(() -> purchaseReturnService.updatePurchaseReturn(reqVO),
                PURCHASE_RETURN_MODE_INVALID);
    }

    @Test
    public void testUpdatePurchaseReturn_byOrder_excludesCurrentReturnIdInReturnedCountQuery() {
        // 更新场景：调 selectReturnedCountMapBySourceInItemIdsExcludeReturn 时应传 excludeReturnId = 当前单 id (10L)
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        when(purchaseInItemMapper.selectBatchIds(any())).thenReturn(Collections.singletonList(
                buildInItem(999L, new BigDecimal("10"))));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIdsExcludeReturn(any(), eq(10L)))
                .thenReturn(Collections.emptyMap());
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L))).thenReturn(Collections.emptyList());

        ErpPurchaseReturnSaveReqVO.Item item = buildByOrderItem(200L, new BigDecimal("3"),
                new BigDecimal("10"), 999L);
        ErpPurchaseReturnSaveReqVO reqVO = buildByOrderReqVO(item);
        reqVO.setId(10L);

        purchaseReturnService.updatePurchaseReturn(reqVO);

        // 验证 excludeReturnId 是当前单 id
        ArgumentCaptor<Long> excludeIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(purchaseReturnItemMapper).selectReturnedCountMapBySourceInItemIdsExcludeReturn(
                any(), excludeIdCaptor.capture());
        assertEquals(Long.valueOf(10L), excludeIdCaptor.getValue());
    }

    // ==================== updatePurchaseReturnStatus ====================

    @Test
    public void testUpdatePurchaseReturnStatus_approveSuccess() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setSupplierId(100L)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setRefundPrice(BigDecimal.ZERO)
                .setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseReturnMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseReturnDO.class))).thenReturn(1);

        ErpPurchaseReturnItemDO item = new ErpPurchaseReturnItemDO()
                .setId(1L).setReturnId(10L).setProductId(200L).setWarehouseId(7L)
                .setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("10"));
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L)))
                .thenReturn(Collections.singletonList(item));

        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(true);
        when(supplierService.getSupplier(eq(100L))).thenReturn(
                new ErpSupplierDO().setId(100L).setName("芋道供应商"));
        when(autoVoucherBuilder.buildPurchaseReturnItems(any(), any())).thenReturn(Collections.emptyList());

        purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        // 审批通过 → 库存出库（count 取负）
        ArgumentCaptor<ErpStockRecordCreateReqBO> stockCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService).createStockRecord(stockCaptor.capture());
        assertEquals(0, stockCaptor.getValue().getCount().compareTo(new BigDecimal("-3")));
        // 生成凭证
        verify(voucherService).createVoucherFromBiz(
                eq(ErpVoucherSourceBizTypeEnum.PURCHASE_RETURN.getType()), eq(10L), eq("CGTH001"),
                any(), any(), any(), anyList());
    }

    @Test
    public void testUpdatePurchaseReturnStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setRefundPrice(BigDecimal.ZERO);
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(
                () -> purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_RETURN_APPROVE_FAIL);
    }

    @Test
    public void testUpdatePurchaseReturnStatus_processWhenRefundExists_throwException() {
        // 已审核 + refundPrice>0 → 反审报错
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setRefundPrice(new BigDecimal("50"));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(
                () -> purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.PROCESS.getStatus()),
                PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND);
    }

    @Test
    public void testUpdatePurchaseReturnStatus_processWhenVoucherApproved_throwException() {
        // 反审时凭证已审核
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setRefundPrice(BigDecimal.ZERO);
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        ErpVoucherDO approvedVoucher = new ErpVoucherDO().setId(1L).setVoucherNo("记-202605-000001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherTypeEnum.PURCHASE.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(approvedVoucher));

        assertServiceException(
                () -> purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "记-202605-000001");
        verify(voucherMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdatePurchaseReturnStatus_processSuccess_deletesUnapprovedVoucher() {
        // 反审时凭证未审核 → 自动删除 + stockRecord 走正向数量
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setSupplierId(100L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setRefundPrice(BigDecimal.ZERO)
                .setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        ErpVoucherDO unapprovedVoucher = new ErpVoucherDO().setId(99L)
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherTypeEnum.PURCHASE.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(unapprovedVoucher));
        when(purchaseReturnMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any(ErpPurchaseReturnDO.class))).thenReturn(1);

        ErpPurchaseReturnItemDO item = new ErpPurchaseReturnItemDO()
                .setId(1L).setReturnId(10L).setProductId(200L).setWarehouseId(7L)
                .setCount(new BigDecimal("3")).setProductPrice(new BigDecimal("10"));
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L)))
                .thenReturn(Collections.singletonList(item));

        purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(eq(99L));
        verify(voucherItemMapper).delete(any());
        // 反审 → 库存恢复（count 取正）
        ArgumentCaptor<ErpStockRecordCreateReqBO> stockCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService).createStockRecord(stockCaptor.capture());
        assertEquals(0, stockCaptor.getValue().getCount().compareTo(new BigDecimal("3")));
        // 反审不应生成凭证
        verify(voucherService, never()).createVoucherFromBiz(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseReturnStatus_approveSkipVoucherWhenBookDisabled() {
        // bookOpenService.isVoucherTypeEnabled 返回 false 时跳过凭证生成
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setSupplierId(100L)
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setRefundPrice(BigDecimal.ZERO)
                .setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseReturnMapper.updateByIdAndStatus(eq(10L), any(), any())).thenReturn(1);
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L))).thenReturn(Collections.emptyList());

        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(false);

        purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(any(), any(), any(), any(), any(), any(), any());
        verify(supplierService, never()).getSupplier(anyLong());
    }

    @Test
    public void testUpdatePurchaseReturnStatus_approveOptimisticLockFail_throwException() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setRefundPrice(BigDecimal.ZERO)
                .setReturnTime(LocalDateTime.of(2026, 5, 20, 10, 0));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);
        // 乐观锁失败：updateByIdAndStatus 返回 0
        when(purchaseReturnMapper.updateByIdAndStatus(eq(10L), any(), any())).thenReturn(0);

        assertServiceException(
                () -> purchaseReturnService.updatePurchaseReturnStatus(10L, ErpAuditStatus.APPROVE.getStatus()),
                PURCHASE_RETURN_APPROVE_FAIL);
        verify(stockRecordService, never()).createStockRecord(any());
    }

    // ==================== updatePurchaseReturnRefundPrice ====================

    @Test
    public void testUpdatePurchaseReturnRefundPrice_success() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setRefundPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100"));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        purchaseReturnService.updatePurchaseReturnRefundPrice(10L, new BigDecimal("50"));

        ArgumentCaptor<ErpPurchaseReturnDO> captor = ArgumentCaptor.forClass(ErpPurchaseReturnDO.class);
        verify(purchaseReturnMapper).updateById(captor.capture());
        assertEquals(Long.valueOf(10L), captor.getValue().getId());
        assertEquals(0, captor.getValue().getRefundPrice().compareTo(new BigDecimal("50")));
    }

    @Test
    public void testUpdatePurchaseReturnRefundPrice_noopWhenEqual() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setRefundPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100"));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        // 传入与现有完全相同 → 不调 updateById
        purchaseReturnService.updatePurchaseReturnRefundPrice(10L, BigDecimal.ZERO);

        verify(purchaseReturnMapper, never()).updateById(any(ErpPurchaseReturnDO.class));
    }

    @Test
    public void testUpdatePurchaseReturnRefundPrice_exceed_throwException() {
        ErpPurchaseReturnDO existing = new ErpPurchaseReturnDO()
                .setId(10L).setRefundPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100"));
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(
                () -> purchaseReturnService.updatePurchaseReturnRefundPrice(10L, new BigDecimal("150")),
                PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED, new BigDecimal("150"), new BigDecimal("100"));
        verify(purchaseReturnMapper, never()).updateById(any(ErpPurchaseReturnDO.class));
    }

    // ==================== deletePurchaseReturn ====================

    @Test
    public void testDeletePurchaseReturn_success() {
        ErpPurchaseReturnDO toDelete = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseReturnMapper.selectByIds(any())).thenReturn(Collections.singletonList(toDelete));

        purchaseReturnService.deletePurchaseReturn(Collections.singletonList(10L));

        verify(purchaseReturnMapper).deleteById(eq(10L));
        verify(purchaseReturnItemMapper).deleteByReturnId(eq(10L));
    }

    @Test
    public void testDeletePurchaseReturn_alreadyApproved_throwException() {
        ErpPurchaseReturnDO approved = new ErpPurchaseReturnDO()
                .setId(10L).setNo("CGTH001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseReturnMapper.selectByIds(any())).thenReturn(Collections.singletonList(approved));

        assertServiceException(
                () -> purchaseReturnService.deletePurchaseReturn(Collections.singletonList(10L)),
                PURCHASE_RETURN_DELETE_FAIL_APPROVE, "CGTH001");
        verify(purchaseReturnMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testDeletePurchaseReturn_emptyList_noOp() {
        when(purchaseReturnMapper.selectByIds(any())).thenReturn(Collections.emptyList());

        purchaseReturnService.deletePurchaseReturn(Collections.singletonList(10L));

        verify(purchaseReturnMapper, never()).deleteById(anyLong());
        verify(purchaseReturnItemMapper, never()).deleteByReturnId(anyLong());
    }

    // ==================== getPurchaseReturn / validatePurchaseReturn / getPurchaseReturnPage ====================

    @Test
    public void testGetPurchaseReturn_delegatesToMapper() {
        ErpPurchaseReturnDO mocked = new ErpPurchaseReturnDO().setId(10L).setNo("CGTH001");
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(mocked);

        ErpPurchaseReturnDO result = purchaseReturnService.getPurchaseReturn(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
        assertEquals("CGTH001", result.getNo());
    }

    @Test
    public void testValidatePurchaseReturn_notApprove_throwException() {
        ErpPurchaseReturnDO inProcess = new ErpPurchaseReturnDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(inProcess);

        assertServiceException(
                () -> purchaseReturnService.validatePurchaseReturn(10L),
                PURCHASE_RETURN_NOT_APPROVE);
    }

    @Test
    public void testValidatePurchaseReturn_success() {
        ErpPurchaseReturnDO approved = new ErpPurchaseReturnDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseReturnMapper.selectById(eq(10L))).thenReturn(approved);

        ErpPurchaseReturnDO result = purchaseReturnService.validatePurchaseReturn(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
        assertEquals(ErpAuditStatus.APPROVE.getStatus(), result.getStatus());
    }

    @Test
    public void testGetPurchaseReturnPage_delegatesToMapper() {
        PageResult<ErpPurchaseReturnDO> mocked = new PageResult<>(
                Collections.singletonList(new ErpPurchaseReturnDO().setId(10L)), 1L);
        when(purchaseReturnMapper.selectPage(any(ErpPurchaseReturnPageReqVO.class))).thenReturn(mocked);

        PageResult<ErpPurchaseReturnDO> result = purchaseReturnService
                .getPurchaseReturnPage(new ErpPurchaseReturnPageReqVO());

        assertSame(mocked, result);
        assertEquals(1, result.getList().size());
    }

    // ==================== 子表查询方法 ====================

    @Test
    public void testGetPurchaseReturnItemListByReturnId_delegatesToMapper() {
        List<ErpPurchaseReturnItemDO> mocked = Collections.singletonList(
                new ErpPurchaseReturnItemDO().setId(1L).setReturnId(10L));
        when(purchaseReturnItemMapper.selectListByReturnId(eq(10L))).thenReturn(mocked);

        List<ErpPurchaseReturnItemDO> result = purchaseReturnService.getPurchaseReturnItemListByReturnId(10L);

        assertEquals(1, result.size());
        assertEquals(Long.valueOf(1L), result.get(0).getId());
    }

    @Test
    public void testGetPurchaseReturnItemListByReturnIds_emptyInput_returnsEmpty() {
        List<ErpPurchaseReturnItemDO> result = purchaseReturnService
                .getPurchaseReturnItemListByReturnIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(purchaseReturnItemMapper, never()).selectListByReturnIds(any());
    }

    @Test
    public void testGetPurchaseReturnItemListByReturnIds_success() {
        List<ErpPurchaseReturnItemDO> mocked = Arrays.asList(
                new ErpPurchaseReturnItemDO().setId(1L),
                new ErpPurchaseReturnItemDO().setId(2L));
        when(purchaseReturnItemMapper.selectListByReturnIds(any())).thenReturn(mocked);

        List<ErpPurchaseReturnItemDO> result = purchaseReturnService
                .getPurchaseReturnItemListByReturnIds(Arrays.asList(10L, 11L));

        assertEquals(2, result.size());
        verify(purchaseReturnItemMapper, times(1)).selectListByReturnIds(any());
    }

}
