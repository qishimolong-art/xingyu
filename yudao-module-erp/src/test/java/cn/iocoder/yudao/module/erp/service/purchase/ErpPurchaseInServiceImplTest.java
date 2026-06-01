package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAutoVoucherBuilder;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpBookOpenService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpVoucherService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.BIZ_PROCESS_FAIL_VOUCHER_APPROVED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_DELETE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_COUNT_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_ITEM_PRICE_POSITIVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NO_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_IN_UPDATE_FAIL_APPROVE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_IN_EXCEED_INABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PURCHASE_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpPurchaseInServiceImpl} 的单元测试类
 */
public class ErpPurchaseInServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInServiceImpl purchaseInService;

    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Mock
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpPurchaseOrderService purchaseOrderService;
    @Mock
    private ErpStockRecordService stockRecordService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private AdminUserApi adminUserApi;
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
        ReflectionTestUtils.setField(purchaseInService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return prefix + "20260520000001";
            }
        });
    }

    private ErpPurchaseInSaveReqVO.Item buildItem(Long productId, BigDecimal count, BigDecimal price) {
        ErpPurchaseInSaveReqVO.Item item = new ErpPurchaseInSaveReqVO.Item();
        item.setProductId(productId);
        item.setProductUnitId(1L);
        item.setWarehouseId(10L);
        item.setCount(count);
        item.setProductPrice(price);
        return item;
    }

    private ErpPurchaseInSaveReqVO buildBaseReqVO(ErpPurchaseInSaveReqVO.Item... items) {
        ErpPurchaseInSaveReqVO vo = new ErpPurchaseInSaveReqVO();
        vo.setSupplierId(99L);
        vo.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        vo.setItems(Arrays.asList(items));
        return vo;
    }

    // ========== createPurchaseIn ==========

    @Test
    public void testCreatePurchaseIn_success() {
        // 仅含 supplierId（虽然 SaveReqVO 没 supplierId 字段，但若无 orderId，supplierId 由 BeanUtils 拷贝过去）
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L).setName("螺丝")));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(captor.capture());
        ErpPurchaseInDO inserted = captor.getValue();
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
        assertEquals("CGRK20260520000001", inserted.getNo());
        assertEquals(Long.valueOf(99L), inserted.getSupplierId());
        assertEquals("", inserted.getOrderNo());
        assertEquals(0, inserted.getTotalCount().compareTo(new BigDecimal("10")));
        // totalProductPrice = 50；taxPrice = 0；totalPrice = 50 - 0 + 0 = 50
        assertEquals(0, inserted.getTotalPrice().compareTo(new BigDecimal("50.00")));
        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        assertNull(itemsCaptor.getValue().get(0).getOrderItemId());
        // orderId 为空，不应调用订单更新
        assertEquals(null, inserted.getOrderId());
        verify(supplierService).validateSupplier(eq(99L));
        verify(purchaseOrderService, never()).updatePurchaseOrderInCount(anyLong(), any());
    }

    @Test
    public void testCreatePurchaseIn_withOrderId_carriesSupplierAndOrderNo() {
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);
        reqVO.setOrderId(50L);

        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L))).thenReturn(Collections.emptyList());
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(Collections.emptyMap());

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).insert(captor.capture());
        ErpPurchaseInDO inserted = captor.getValue();
        assertEquals("CGDD001", inserted.getOrderNo());
        assertEquals(Long.valueOf(999L), inserted.getSupplierId());
        // 有 orderId，触发更新订单入库数量
        verify(purchaseOrderService).updatePurchaseOrderInCount(eq(50L), any());
    }

    @Test
    public void testCreatePurchaseIn_countZero_throwException() {
        ErpPurchaseInSaveReqVO.Item invalid = buildItem(200L, BigDecimal.ZERO, new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(invalid);

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_ITEM_COUNT_POSITIVE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_priceNegative_throwException() {
        ErpPurchaseInSaveReqVO.Item invalid = buildItem(200L, new BigDecimal("5"), new BigDecimal("-1"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(invalid);

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_ITEM_PRICE_POSITIVE);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_noExists_throwException() {
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("10"), new BigDecimal("5"));
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        // 已存在同号入库单
        when(purchaseInMapper.selectByNo(any())).thenReturn(new ErpPurchaseInDO().setId(99L));

        assertServiceException(() -> purchaseInService.createPurchaseIn(reqVO),
                PURCHASE_IN_NO_EXISTS);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseIn_wholeQtyOverridesCount() {
        // wholeQty=10, packageQty=2 → count 应被覆盖为 20
        ErpPurchaseInSaveReqVO.Item item = buildItem(200L, new BigDecimal("3"), new BigDecimal("5"));
        item.setWholeQty(10);
        item.setPackageQty(2);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(item);

        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);

        purchaseInService.createPurchaseIn(reqVO);

        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseInItemDO> items = itemsCaptor.getValue();
        assertEquals(1, items.size());
        // wholeQty(10) × packageQty(2) = 20，而非 VO 传的 3
        assertEquals(0, items.get(0).getCount().compareTo(new BigDecimal("20")));
        assertEquals(Integer.valueOf(2), items.get(0).getPackageQty());
        assertEquals(Integer.valueOf(10), items.get(0).getWholeQty());
    }

    // ========== updatePurchaseIn ==========

    @Test
    public void testUpdatePurchaseIn_success() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO(
                buildItem(200L, new BigDecimal("5"), new BigDecimal("10")));
        reqVO.setId(10L);

        purchaseInService.updatePurchaseIn(reqVO);

        verify(purchaseInMapper).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseIn_notExists_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_NOT_EXISTS);
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseIn_alreadyApproved_throwException() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(approved);
        ErpPurchaseInSaveReqVO reqVO = buildBaseReqVO();
        reqVO.setId(10L);

        assertServiceException(() -> purchaseInService.updatePurchaseIn(reqVO),
                PURCHASE_IN_UPDATE_FAIL_APPROVE, "CGRK001");
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    // ========== updatePurchaseInStatus ==========

    @Test
    public void testUpdatePurchaseInStatus_approveSuccess_triggersStockAndLastPriceAndVoucher() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(10L)
                .setCount(new BigDecimal("5")).setProductPrice(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));

        // 开账启用，触发凭证生成
        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(true);
        when(supplierService.validateSupplier(eq(99L)))
                .thenReturn(new ErpSupplierDO().setId(99L).setName("芋道供应商"));
        when(autoVoucherBuilder.buildPurchaseInItems(any(), eq("芋道供应商")))
                .thenReturn(Collections.singletonList(new ErpVoucherItemDO()));

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        // 库存流水：bizType=PURCHASE_IN（正向）
        ArgumentCaptor<ErpStockRecordCreateReqBO> recordCaptor =
                ArgumentCaptor.forClass(ErpStockRecordCreateReqBO.class);
        verify(stockRecordService).createStockRecord(recordCaptor.capture());
        assertEquals(ErpStockRecordBizTypeEnum.PURCHASE_IN.getType(), recordCaptor.getValue().getBizType());
        assertEquals(0, recordCaptor.getValue().getCount().compareTo(new BigDecimal("5")));
        assertEquals(0, recordCaptor.getValue().getUnitPrice().compareTo(new BigDecimal("10")));
        assertEquals(LocalDateTime.of(2026, 5, 20, 10, 0, 0), recordCaptor.getValue().getBizDate());
        // 回写 lastPurchasePrice
        verify(productService).updateProductLastPurchasePrice(eq(200L), eq(new BigDecimal("10")));
        // 凭证生成
        verify(voucherService).createVoucherFromBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()),
                eq(10L), eq("CGRK001"), any(), any(), any(String.class), anyList());
    }

    @Test
    public void testUpdatePurchaseInStatus_approveWhenAlreadyApproved_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.APPROVE.getStatus()), PURCHASE_IN_APPROVE_FAIL);
        verify(purchaseInMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInStatus_processFailWhenPaymentExists_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(new BigDecimal("100"));
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.PROCESS.getStatus()), PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT);
        verify(purchaseInMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    public void testUpdatePurchaseInStatus_processFailWhenVoucherApproved_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        // 关联凭证已审核
        ErpVoucherDO approvedVoucher = new ErpVoucherDO()
                .setId(500L).setVoucherNo("记-202605-000001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(approvedVoucher));

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                        ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "记-202605-000001");
        verify(voucherMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testUpdatePurchaseInStatus_processSuccess_deletesUnauditedVoucher() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        // 关联凭证未审核 → 应被自动删除
        ErpVoucherDO unauditedVoucher = new ErpVoucherDO()
                .setId(500L).setVoucherNo("记-202605-000001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(eq(ErpVoucherSourceBizTypeEnum.PURCHASE_IN.getType()), eq(10L)))
                .thenReturn(Collections.singletonList(unauditedVoucher));
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.APPROVE.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(eq(500L));
        verify(voucherItemMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testUpdatePurchaseInStatus_approveSkipsVoucherWhenBookDisabled() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0))
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(eq(10L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPurchaseInDO.class))).thenReturn(1);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());
        // 未开账 → 不生成凭证
        when(bookOpenService.isVoucherTypeEnabled(any(), eq(ErpVoucherTypeEnum.PURCHASE.getType())))
                .thenReturn(false);

        purchaseInService.updatePurchaseInStatus(10L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(any(), any(), any(), any(), any(), any(), any());
        verify(autoVoucherBuilder, never()).buildPurchaseInItems(any(), any());
        verify(supplierService, never()).validateSupplier(any());
    }

    @Test
    public void testUpdatePurchaseInStatus_approveOptimisticLockFail_throwException() {
        ErpPurchaseInDO existing = new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setPaymentPrice(BigDecimal.ZERO);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(existing);
        when(purchaseInMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> purchaseInService.updatePurchaseInStatus(10L,
                ErpAuditStatus.APPROVE.getStatus()), PURCHASE_IN_APPROVE_FAIL);
        verify(stockRecordService, never()).createStockRecord(any());
    }

    // ========== updatePurchaseInPaymentPrice ==========

    @Test
    public void testUpdatePurchaseInPaymentPrice_success() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100")));

        purchaseInService.updatePurchaseInPaymentPrice(10L, new BigDecimal("50"));

        ArgumentCaptor<ErpPurchaseInDO> captor = ArgumentCaptor.forClass(ErpPurchaseInDO.class);
        verify(purchaseInMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getPaymentPrice().compareTo(new BigDecimal("50")));
    }

    @Test
    public void testUpdatePurchaseInPaymentPrice_unchanged_skipUpdate() {
        BigDecimal payment = new BigDecimal("50");
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(payment).setTotalPrice(new BigDecimal("100")));

        purchaseInService.updatePurchaseInPaymentPrice(10L, payment);

        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testUpdatePurchaseInPaymentPrice_exceed_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setPaymentPrice(BigDecimal.ZERO).setTotalPrice(new BigDecimal("100")));

        assertServiceException(() -> purchaseInService.updatePurchaseInPaymentPrice(10L,
                        new BigDecimal("200")),
                PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED, new BigDecimal("200"), new BigDecimal("100"));
        verify(purchaseInMapper, never()).updateById(any(ErpPurchaseInDO.class));
    }

    // ========== deletePurchaseIn ==========

    @Test
    public void testDeletePurchaseIn_success() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(purchaseInMapper.selectByIds(any())).thenReturn(Collections.singletonList(in));

        purchaseInService.deletePurchaseIn(Collections.singletonList(10L));

        verify(purchaseInMapper).deleteById(eq(10L));
        verify(purchaseInItemMapper).deleteByInId(eq(10L));
    }

    @Test
    public void testDeletePurchaseIn_alreadyApproved_throwException() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectByIds(any())).thenReturn(Collections.singletonList(approved));

        assertServiceException(() -> purchaseInService.deletePurchaseIn(Collections.singletonList(10L)),
                PURCHASE_IN_DELETE_FAIL_APPROVE, "CGRK001");
        verify(purchaseInMapper, never()).deleteById(anyLong());
    }

    @Test
    public void testDeletePurchaseIn_emptyResult_noOp() {
        when(purchaseInMapper.selectByIds(any())).thenReturn(Collections.emptyList());

        purchaseInService.deletePurchaseIn(Collections.singletonList(10L));

        verify(purchaseInMapper, never()).deleteById(anyLong());
        verify(purchaseInItemMapper, never()).deleteByInId(anyLong());
    }

    // ========== getPurchaseIn / validatePurchaseIn ==========

    @Test
    public void testGetPurchaseIn_returnsExisting() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L);
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(in);

        assertSame(in, purchaseInService.getPurchaseIn(10L));
    }

    @Test
    public void testValidatePurchaseIn_notExists_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(null);

        assertServiceException(() -> purchaseInService.validatePurchaseIn(10L),
                PURCHASE_IN_NOT_EXISTS);
    }

    @Test
    public void testValidatePurchaseIn_notApproved_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInService.validatePurchaseIn(10L),
                PURCHASE_IN_NOT_APPROVE);
    }

    @Test
    public void testValidatePurchaseIn_success() {
        ErpPurchaseInDO approved = new ErpPurchaseInDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(approved);

        ErpPurchaseInDO result = purchaseInService.validatePurchaseIn(10L);

        assertNotNull(result);
        assertEquals(Long.valueOf(10L), result.getId());
    }

    // ========== getPurchaseInPage ==========

    @Test
    public void testGetPurchaseInPage_delegatesToMapper() {
        ErpPurchaseInPageReqVO reqVO = new ErpPurchaseInPageReqVO();
        PageResult<ErpPurchaseInDO> page = new PageResult<>(
                Collections.singletonList(new ErpPurchaseInDO().setId(1L)), 1L);
        when(purchaseInMapper.selectPage(eq(reqVO))).thenReturn(page);

        assertSame(page, purchaseInService.getPurchaseInPage(reqVO));
    }

    // ========== 子表查询方法 ==========

    @Test
    public void testGetPurchaseInItemListByInId_delegatesToMapper() {
        List<ErpPurchaseInItemDO> mocked = Collections.singletonList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(mocked);

        assertSame(mocked, purchaseInService.getPurchaseInItemListByInId(10L));
    }

    @Test
    public void testGetPurchaseInItemListByInIds_emptyInput_returnsEmpty() {
        List<ErpPurchaseInItemDO> result = purchaseInService.getPurchaseInItemListByInIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(purchaseInItemMapper, never()).selectListByInIds(any());
    }

    // ========== getReturnableItemsByInId ==========

    @Test
    public void testGetReturnableItemsByInId_success() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        // 入库项：count=10
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("12.34")).setCount(new BigDecimal("10"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        // 已退 3
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(100L, new BigDecimal("3"));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any())).thenReturn(returnedMap);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setName("螺丝").setCode("P001")));

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertEquals(1, result.size());
        ErpPurchaseReturnableItemRespVO vo = result.get(0);
        assertEquals(0, vo.getInCount().compareTo(new BigDecimal("10")));
        assertEquals(0, vo.getReturnedCount().compareTo(new BigDecimal("3")));
        // 可退 = 10 - 3 = 7
        assertEquals(0, vo.getReturnableCount().compareTo(new BigDecimal("7")));
        assertEquals("螺丝", vo.getProductName());
        assertEquals("P001", vo.getProductCode());
        assertEquals("CGRK001", vo.getSourceInNo());
    }

    @Test
    public void testGetReturnableItemsByInId_returnableNegativeClampedToZero() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L).setNo("CGRK001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        // 入库 5，已退 8（异常超过）→ returnable 不能为负，应钳制为 0
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(100L).setInId(10L).setProductId(200L)
                .setCount(new BigDecimal("5"));
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.singletonList(item));
        Map<Long, BigDecimal> returnedMap = new HashMap<>();
        returnedMap.put(100L, new BigDecimal("8"));
        when(purchaseReturnItemMapper.selectReturnedCountMapBySourceInItemIds(any())).thenReturn(returnedMap);
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L)));

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getReturnableCount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testGetReturnableItemsByInId_inNotApproved_throwException() {
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(new ErpPurchaseInDO()
                .setId(10L).setStatus(ErpAuditStatus.PROCESS.getStatus()));

        assertServiceException(() -> purchaseInService.getReturnableItemsByInId(10L),
                PURCHASE_IN_NOT_APPROVE);
    }

    @Test
    public void testGetReturnableItemsByInId_emptyItems_returnsEmpty() {
        ErpPurchaseInDO purchaseIn = new ErpPurchaseInDO().setId(10L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(purchaseInMapper.selectById(eq(10L))).thenReturn(purchaseIn);
        when(purchaseInItemMapper.selectListByInId(eq(10L))).thenReturn(Collections.emptyList());

        List<ErpPurchaseReturnableItemRespVO> result = purchaseInService.getReturnableItemsByInId(10L);

        assertTrue(result.isEmpty());
        verify(purchaseReturnItemMapper, never()).selectReturnedCountMapBySourceInItemIds(any());
    }

    // ========== createPurchaseInFromOrder ==========

    @Test
    public void testCreatePurchaseInFromOrder_success() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L);
        when(purchaseOrderService.validatePurchaseOrder(eq(50L))).thenReturn(order);
        ErpPurchaseOrderItemDO orderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L).setProductUnitId(1L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("10"))
                .setInCount(new BigDecimal("3")).setTaxPercent(new BigDecimal("13"))
                .setGift(false);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(orderItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L))).thenReturn(Collections.emptyList());
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(Collections.emptyMap());

        // 自动审批后会再次 selectById（updatePurchaseInStatus 内部调用）
        // 让插入后的 selectById 也能拿到记录
        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        reqVO.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5")); // 可入库 = 10-3 = 7，5 ≤ 7 OK
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        purchaseInService.createPurchaseInFromOrder(reqVO);

        // 入库单插入
        verify(purchaseInMapper).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_orderItemNotFound_throwException() {
        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L));
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.emptyList());

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(999L); // 不存在
        reqItem.setCount(new BigDecimal("1"));
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> purchaseInService.createPurchaseInFromOrder(reqVO),
                PURCHASE_ORDER_NOT_EXISTS);
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_countExceedsInable_throwException() {
        when(purchaseOrderService.validatePurchaseOrder(eq(50L)))
                .thenReturn(new ErpPurchaseOrderDO().setId(50L));
        ErpPurchaseOrderItemDO orderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L)
                .setCount(new BigDecimal("10")).setInCount(new BigDecimal("8")); // 可入库=2
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(orderItem));
        when(productService.getProduct(eq(200L))).thenReturn(new ErpProductDO()
                .setId(200L).setName("螺丝"));

        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5")); // > 可入库 2
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        assertServiceException(() -> purchaseInService.createPurchaseInFromOrder(reqVO),
                PURCHASE_ORDER_IN_EXCEED_INABLE, "螺丝", new BigDecimal("2"), new BigDecimal("5"));
        verify(purchaseInMapper, never()).insert(any(ErpPurchaseInDO.class));
    }

    @Test
    public void testCreatePurchaseInFromOrder_giftForcesZeroPrice() {
        ErpPurchaseOrderDO order = new ErpPurchaseOrderDO().setId(50L).setNo("CGDD001").setSupplierId(999L);
        when(purchaseOrderService.validatePurchaseOrder(eq(50L))).thenReturn(order);
        // 赠品行
        ErpPurchaseOrderItemDO giftOrderItem = new ErpPurchaseOrderItemDO()
                .setId(101L).setProductId(200L).setProductUnitId(1L)
                .setProductPrice(new BigDecimal("99")).setCount(new BigDecimal("10"))
                .setInCount(BigDecimal.ZERO).setGift(true);
        when(purchaseOrderService.getPurchaseOrderItemListByOrderId(eq(50L)))
                .thenReturn(Collections.singletonList(giftOrderItem));
        when(productService.validProductList(any())).thenReturn(Collections.singletonList(
                new ErpProductDO().setId(200L).setUnitId(1L)));
        when(purchaseInMapper.selectByNo(any())).thenReturn(null);
        when(purchaseInMapper.selectListByOrderId(eq(50L))).thenReturn(Collections.emptyList());
        when(purchaseInItemMapper.selectOrderItemCountSumMapByInIds(any())).thenReturn(Collections.emptyMap());
        // 后续 updatePurchaseInStatus 链路
        ErpPurchaseInFromOrderReqVO reqVO = new ErpPurchaseInFromOrderReqVO();
        reqVO.setOrderId(50L);
        reqVO.setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        ErpPurchaseInFromOrderReqVO.Item reqItem = new ErpPurchaseInFromOrderReqVO.Item();
        reqItem.setOrderItemId(101L);
        reqItem.setCount(new BigDecimal("5"));
        reqItem.setWarehouseId(10L);
        reqVO.setItems(Collections.singletonList(reqItem));

        purchaseInService.createPurchaseInFromOrder(reqVO);

        // 验证插入的子表项：赠品行 productPrice 应被强制设为 0
        ArgumentCaptor<List<ErpPurchaseInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(purchaseInItemMapper, times(1)).insertBatch(itemsCaptor.capture());
        List<ErpPurchaseInItemDO> insertedItems = itemsCaptor.getValue();
        assertEquals(1, insertedItems.size());
        assertEquals(0, insertedItems.get(0).getProductPrice().compareTo(BigDecimal.ZERO));
    }

    // ========== getApprovedPurchaseInsBySupplier ==========

    @Test
    public void testGetApprovedPurchaseInsBySupplier_nullSupplier_returnsEmpty() {
        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(null);

        assertTrue(result.isEmpty());
        verify(purchaseInMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testGetApprovedPurchaseInsBySupplier_noInList_returnsEmpty() {
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(99L);

        assertTrue(result.isEmpty());
        verify(supplierService, never()).getSupplierMap(any());
    }

    @Test
    public void testGetApprovedPurchaseInsBySupplier_success() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        in.setUpdater("123");
        in.setUpdateTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        // 入库项 2 行（用于统计 itemCount）
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Arrays.asList(
                new ErpPurchaseInItemDO().setId(1L).setInId(10L),
                new ErpPurchaseInItemDO().setId(2L).setInId(10L)));
        Map<Long, ErpSupplierDO> supplierMap = new HashMap<>();
        supplierMap.put(99L, new ErpSupplierDO().setId(99L).setName("芋道供应商"));
        when(supplierService.getSupplierMap(any())).thenReturn(supplierMap);
        Map<Long, cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO> userMap = new HashMap<>();
        cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO user =
                new cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO();
        user.setId(123L);
        user.setNickname("张三");
        userMap.put(123L, user);
        when(adminUserApi.getUserMap(any())).thenReturn(userMap);

        List<ErpPurchaseInForAdjustRespVO> result = purchaseInService.getApprovedPurchaseInsBySupplier(99L);

        assertEquals(1, result.size());
        ErpPurchaseInForAdjustRespVO vo = result.get(0);
        assertEquals(Integer.valueOf(2), vo.getItemCount());
        assertEquals("芋道供应商", vo.getSupplierName());
        assertEquals(Long.valueOf(123L), vo.getAuditorId());
        assertEquals("张三", vo.getAuditorName());
        assertNotNull(vo.getApproveTime());
    }

    // ========== getApprovedPurchaseInItemsBySupplier ==========

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_nullSupplier_returnsEmpty() {
        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(null, false);

        assertTrue(result.isEmpty());
        verify(purchaseInMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_excludeAdjustedFiltersAdjustedItems() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        // 两项：一项已调价、一项未调价
        ErpPurchaseInItemDO adjustedItem = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))
                .setAdjusted(true);
        ErpPurchaseInItemDO unadjustedItem = new ErpPurchaseInItemDO()
                .setId(2L).setInId(10L).setProductId(201L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("20")).setCount(new BigDecimal("3"))
                .setAdjusted(false);
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Arrays.asList(adjustedItem, unadjustedItem));
        when(productService.getProductVOMap(any())).thenReturn(Collections.emptyMap());
        when(warehouseService.getWarehouseMap(any())).thenReturn(Collections.emptyMap());

        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(99L, true);

        // 已调价行被过滤掉，仅返回未调价行
        assertEquals(1, result.size());
        assertEquals(Long.valueOf(2L), result.get(0).getId());
    }

    @Test
    public void testGetApprovedPurchaseInItemsBySupplier_successWithFullAggregation() {
        ErpPurchaseInDO in = new ErpPurchaseInDO().setId(10L).setNo("CGRK001").setSupplierId(99L)
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setInTime(LocalDateTime.of(2026, 5, 20, 10, 0, 0));
        when(purchaseInMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.singletonList(in));
        ErpPurchaseInItemDO item = new ErpPurchaseInItemDO()
                .setId(1L).setInId(10L).setProductId(200L).setWarehouseId(5L)
                .setProductPrice(new BigDecimal("10")).setCount(new BigDecimal("5"))
                .setVehicleModel("大众-朗逸").setBrand("博世").setOriginPlace("德国");
        when(purchaseInItemMapper.selectListByInIds(any())).thenReturn(Collections.singletonList(item));
        // 产品资料
        Map<Long, ErpProductRespVO> productMap = new HashMap<>();
        ErpProductRespVO product = new ErpProductRespVO();
        product.setId(200L);
        product.setCode("P001");
        product.setName("螺丝");
        product.setUnitName("件");
        productMap.put(200L, product);
        when(productService.getProductVOMap(any())).thenReturn(productMap);
        // 仓库
        Map<Long, ErpWarehouseDO> warehouseMap = new HashMap<>();
        warehouseMap.put(5L, new ErpWarehouseDO().setId(5L).setName("主仓库"));
        when(warehouseService.getWarehouseMap(any())).thenReturn(warehouseMap);

        List<ErpPurchaseInItemForAdjustRespVO> result =
                purchaseInService.getApprovedPurchaseInItemsBySupplier(99L, false);

        assertEquals(1, result.size());
        ErpPurchaseInItemForAdjustRespVO vo = result.get(0);
        assertEquals("CGRK001", vo.getInNo());
        assertEquals("P001", vo.getProductCode());
        assertEquals("螺丝", vo.getProductName());
        assertEquals("件", vo.getProductUnitName());
        assertEquals("主仓库", vo.getWarehouseName());
        // item 自有的车型/品牌/产地优先于产品资料
        assertEquals("大众-朗逸", vo.getVehicleModel());
        assertEquals("博世", vo.getBrand());
        assertEquals("德国", vo.getOriginPlace());
    }

}
