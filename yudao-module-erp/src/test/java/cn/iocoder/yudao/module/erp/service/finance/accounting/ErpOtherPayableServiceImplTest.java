package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable.ErpOtherPayableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherPayableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherPayableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpOtherPayableServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpOtherPayableServiceImpl otherPayableService;

    @Mock
    private ErpOtherPayableMapper otherPayableMapper;
    @Mock
    private ErpOtherPayableItemMapper otherPayableItemMapper;
    private final java.util.concurrent.atomic.AtomicReference<String> nextNoRef =
            new java.util.concurrent.atomic.AtomicReference<>("QTYF-DEFAULT");
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

    private static final Integer VT = ErpVoucherTypeEnum.OTHER_PAYABLE.getType();
    private static final Integer SBT = ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType();

    @BeforeEach
    void setupNoRedisDAO() {
        ReflectionTestUtils.setField(otherPayableService, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return nextNoRef.get();
            }
        });
    }

    // ==================== create ====================

    @Test
    void testCreateOtherPayable_success() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0));
        reqVO.setPartyType(2);
        reqVO.setPartyId(10L);
        reqVO.setPartyName("供应商A");
        reqVO.setDiscountAmount(new BigDecimal("5.00"));
        ErpOtherPayableSaveReqVO.Item item1 = new ErpOtherPayableSaveReqVO.Item();
        item1.setSummary("办公用品");
        item1.setAmount(new BigDecimal("100.00"));
        reqVO.setItems(singletonList(item1));

        // mock
        nextNoRef.set("QTYF20260510001");
        when(otherPayableMapper.selectByNo("QTYF20260510001")).thenReturn(null);

        // 调用
        Long id = otherPayableService.createOtherPayable(reqVO);

        // 断言主表
        ArgumentCaptor<ErpOtherPayableDO> captor = ArgumentCaptor.forClass(ErpOtherPayableDO.class);
        verify(otherPayableMapper).insert(captor.capture());
        ErpOtherPayableDO saved = captor.getValue();
        assertThat(saved.getNo()).isEqualTo("QTYF20260510001");
        assertThat(saved.getStatus()).isEqualTo(ErpAuditStatus.PROCESS.getStatus());
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(saved.getDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(saved.getActualAmount()).isEqualByComparingTo("95.00");
        assertThat(saved.getPartyName()).isEqualTo("供应商A");

        // 断言子表
        verify(otherPayableItemMapper).insertBatch(anyList());
    }

    @Test
    void testCreateOtherPayable_noExists_throwException() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setItems(singletonList(new ErpOtherPayableSaveReqVO.Item()));

        // mock: 单号已存在
        nextNoRef.set("QTYF001");
        when(otherPayableMapper.selectByNo("QTYF001")).thenReturn(new ErpOtherPayableDO());

        // 调用并断言异常
        assertServiceException(() -> otherPayableService.createOtherPayable(reqVO), OTHER_PAYABLE_NO_EXISTS);
    }

    @Test
    void testCreateOtherPayable_discountNull_defaultZero() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setDiscountAmount(null);
        ErpOtherPayableSaveReqVO.Item item = new ErpOtherPayableSaveReqVO.Item();
        item.setAmount(new BigDecimal("200.00"));
        reqVO.setItems(singletonList(item));

        // mock
        nextNoRef.set("QTYF002");
        when(otherPayableMapper.selectByNo("QTYF002")).thenReturn(null);

        // 调用
        otherPayableService.createOtherPayable(reqVO);

        // 断言
        ArgumentCaptor<ErpOtherPayableDO> captor = ArgumentCaptor.forClass(ErpOtherPayableDO.class);
        verify(otherPayableMapper).insert(captor.capture());
        assertThat(captor.getValue().getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(captor.getValue().getActualAmount()).isEqualByComparingTo("200.00");
    }

    @Test
    void testCreateOtherPayable_multipleItems_sumCorrect() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setDiscountAmount(new BigDecimal("10.00"));
        ErpOtherPayableSaveReqVO.Item item1 = new ErpOtherPayableSaveReqVO.Item();
        item1.setAmount(new BigDecimal("50.00"));
        ErpOtherPayableSaveReqVO.Item item2 = new ErpOtherPayableSaveReqVO.Item();
        item2.setAmount(new BigDecimal("80.00"));
        reqVO.setItems(asList(item1, item2));

        // mock
        nextNoRef.set("QTYF003");
        when(otherPayableMapper.selectByNo("QTYF003")).thenReturn(null);

        // 调用
        otherPayableService.createOtherPayable(reqVO);

        // 断言
        ArgumentCaptor<ErpOtherPayableDO> captor = ArgumentCaptor.forClass(ErpOtherPayableDO.class);
        verify(otherPayableMapper).insert(captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("130.00");
        assertThat(captor.getValue().getActualAmount()).isEqualByComparingTo("120.00");
    }

    @Test
    void testCreateOtherPayable_itemsSetPayableId() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setDiscountAmount(BigDecimal.ZERO);
        ErpOtherPayableSaveReqVO.Item item = new ErpOtherPayableSaveReqVO.Item();
        item.setAmount(new BigDecimal("10.00"));
        reqVO.setItems(singletonList(item));

        // mock
        nextNoRef.set("QTYF004");
        when(otherPayableMapper.selectByNo("QTYF004")).thenReturn(null);
        doAnswer(inv -> {
            ErpOtherPayableDO arg = inv.getArgument(0);
            arg.setId(999L);
            return null;
        }).when(otherPayableMapper).insert(any(ErpOtherPayableDO.class));

        // 调用
        otherPayableService.createOtherPayable(reqVO);

        // 断言子表 payableId 被设置
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpOtherPayableItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(otherPayableItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).allMatch(i -> i.getPayableId().equals(999L));
    }

    // ==================== update ====================

    @Test
    void testUpdateOtherPayable_success() {
        // 准备参数
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setId(100L);
        reqVO.setDiscountAmount(new BigDecimal("2.00"));
        ErpOtherPayableSaveReqVO.Item item = new ErpOtherPayableSaveReqVO.Item();
        item.setAmount(new BigDecimal("50.00"));
        reqVO.setItems(singletonList(item));

        // mock
        ErpOtherPayableDO existing = new ErpOtherPayableDO();
        existing.setId(100L);
        existing.setNo("QTYF100");
        existing.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(existing);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherPayableDO.class))).thenReturn(1);

        // 调用
        otherPayableService.updateOtherPayable(reqVO);

        // 断言：先删旧子表，再插新子表
        InOrder inOrder = inOrder(otherPayableItemMapper);
        inOrder.verify(otherPayableItemMapper).deleteByPayableId(100L);
        inOrder.verify(otherPayableItemMapper).insertBatch(anyList());

        // 断言主表更新（S12 修复：乐观锁 updateByIdAndStatus）
        verify(otherPayableMapper).updateByIdAndStatus(eq(100L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherPayableDO.class));
    }

    @Test
    void testUpdateOtherPayable_notExists_throwException() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setId(999L);
        reqVO.setItems(singletonList(new ErpOtherPayableSaveReqVO.Item()));

        when(otherPayableMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> otherPayableService.updateOtherPayable(reqVO), OTHER_PAYABLE_NOT_EXISTS);
    }

    @Test
    void testUpdateOtherPayable_alreadyApproved_throwException() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setId(100L);
        reqVO.setItems(singletonList(new ErpOtherPayableSaveReqVO.Item()));

        ErpOtherPayableDO existing = new ErpOtherPayableDO();
        existing.setId(100L);
        existing.setNo("QTYF100");
        existing.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> otherPayableService.updateOtherPayable(reqVO), OTHER_PAYABLE_UPDATE_FAIL_APPROVE, "QTYF100");
    }

    @Test
    void testUpdateOtherPayable_recalculateAmount() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setId(100L);
        reqVO.setDiscountAmount(new BigDecimal("15.00"));
        ErpOtherPayableSaveReqVO.Item item1 = new ErpOtherPayableSaveReqVO.Item();
        item1.setAmount(new BigDecimal("60.00"));
        ErpOtherPayableSaveReqVO.Item item2 = new ErpOtherPayableSaveReqVO.Item();
        item2.setAmount(new BigDecimal("40.00"));
        reqVO.setItems(asList(item1, item2));

        ErpOtherPayableDO existing = new ErpOtherPayableDO();
        existing.setId(100L);
        existing.setNo("QTYF100");
        existing.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(existing);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherPayableDO.class))).thenReturn(1);

        otherPayableService.updateOtherPayable(reqVO);

        ArgumentCaptor<ErpOtherPayableDO> captor = ArgumentCaptor.forClass(ErpOtherPayableDO.class);
        verify(otherPayableMapper).updateByIdAndStatus(eq(100L),
                eq(ErpAuditStatus.PROCESS.getStatus()), captor.capture());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getActualAmount()).isEqualByComparingTo("85.00");
    }

    @Test
    void testUpdateOtherPayable_itemsSetPayableId() {
        ErpOtherPayableSaveReqVO reqVO = new ErpOtherPayableSaveReqVO();
        reqVO.setId(100L);
        reqVO.setDiscountAmount(BigDecimal.ZERO);
        ErpOtherPayableSaveReqVO.Item item = new ErpOtherPayableSaveReqVO.Item();
        item.setAmount(new BigDecimal("30.00"));
        reqVO.setItems(singletonList(item));

        ErpOtherPayableDO existing = new ErpOtherPayableDO();
        existing.setId(100L);
        existing.setNo("QTYF100");
        existing.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(existing);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L),
                eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherPayableDO.class))).thenReturn(1);

        otherPayableService.updateOtherPayable(reqVO);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpOtherPayableItemDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(otherPayableItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).allMatch(i -> i.getPayableId().equals(100L));
    }

    // ==================== approve ====================

    @Test
    void testApprove_success_withVoucher() {
        // 准备
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        payable.setBizTime(LocalDateTime.of(2026, 5, 10, 9, 0));
        payable.setPartyName("供应商B");
        payable.setTotalAmount(new BigDecimal("500.00"));
        payable.setActualAmount(new BigDecimal("480.00"));
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(LocalDate.of(2026, 5, 10), VT)).thenReturn(true);
        when(autoVoucherBuilder.buildOtherPayableItems(payable)).thenReturn(singletonList(new ErpVoucherItemDO()));

        // 调用
        otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        // 断言
        verify(otherPayableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any());
        verify(voucherService).createVoucherFromBiz(
                eq(ErpVoucherSourceBizTypeEnum.OTHER_PAYABLE.getType()), eq(100L), eq("QTYF100"),
                any(), eq(LocalDate.of(2026, 5, 10)), eq("其他应付 - 供应商B"), anyList());
    }

    @Test
    void testApprove_success_voucherDisabled() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        payable.setBizTime(LocalDateTime.of(2026, 5, 10, 9, 0));
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(LocalDate.of(2026, 5, 10), VT)).thenReturn(false);

        otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    void testApprove_bizTimeNull_fallbackToNow() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        payable.setBizTime(null);
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(bookOpenService).isVoucherTypeEnabled(any(LocalDate.class), eq(VT));
    }

    @Test
    void testApprove_alreadyApproved_throwException() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        assertServiceException(
                () -> otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                OTHER_PAYABLE_APPROVE_FAIL);
    }

    @Test
    void testApprove_optimisticLockFail_throwException() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any()))
                .thenReturn(0);

        assertServiceException(
                () -> otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                OTHER_PAYABLE_APPROVE_FAIL);
    }

    // ==================== process (reverse audit) ====================

    @Test
    void testProcess_success_deleteUnauditedVoucher() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(500L);
        voucher.setVoucherNo("记-202605-000001");
        voucher.setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(singletonList(voucher));
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);

        otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherItemMapper).deleteByVoucherId(500L);
        verify(voucherMapper).deleteById(500L);
        verify(otherPayableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any());
    }

    @Test
    void testProcess_voucherApproved_throwException() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        ErpVoucherDO voucher = new ErpVoucherDO();
        voucher.setId(500L);
        voucher.setVoucherNo("记-202605-000001");
        voucher.setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(singletonList(voucher));

        assertServiceException(
                () -> otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "记-202605-000001");
    }

    @Test
    void testProcess_alreadyProcess_throwException() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        assertServiceException(
                () -> otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                OTHER_PAYABLE_PROCESS_FAIL);
    }

    @Test
    void testProcess_noVouchers_success() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(Collections.emptyList());
        when(otherPayableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any()))
                .thenReturn(1);

        otherPayableService.updateOtherPayableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherItemMapper, never()).deleteByVoucherId(anyLong());
        verify(voucherMapper, never()).deleteById(anyLong());
    }

    // ==================== delete ====================

    @Test
    void testDelete_success() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        otherPayableService.deleteOtherPayable(100L);

        verify(otherPayableMapper).deleteById(100L);
        verify(otherPayableItemMapper).deleteByPayableId(100L);
    }

    @Test
    void testDelete_notExists_throwException() {
        when(otherPayableMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> otherPayableService.deleteOtherPayable(999L), OTHER_PAYABLE_NOT_EXISTS);
    }

    @Test
    void testDelete_alreadyApproved_throwException() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        payable.setNo("QTYF100");
        payable.setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        assertServiceException(() -> otherPayableService.deleteOtherPayable(100L), OTHER_PAYABLE_DELETE_FAIL_APPROVE, "QTYF100");
    }

    // ==================== get / page / list ====================

    @Test
    void testGetOtherPayable() {
        ErpOtherPayableDO payable = new ErpOtherPayableDO();
        payable.setId(100L);
        when(otherPayableMapper.selectById(100L)).thenReturn(payable);

        ErpOtherPayableDO result = otherPayableService.getOtherPayable(100L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
    }

    @Test
    void testGetOtherPayablePage() {
        ErpOtherPayablePageReqVO pageReqVO = new ErpOtherPayablePageReqVO();
        PageResult<ErpOtherPayableDO> expected = new PageResult<>(singletonList(new ErpOtherPayableDO()), 1L);
        when(otherPayableMapper.selectPage(pageReqVO)).thenReturn(expected);

        PageResult<ErpOtherPayableDO> result = otherPayableService.getOtherPayablePage(pageReqVO);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getList()).hasSize(1);
    }

    @Test
    void testGetOtherPayableItemListByPayableId() {
        ErpOtherPayableItemDO item = new ErpOtherPayableItemDO();
        item.setPayableId(100L);
        when(otherPayableItemMapper.selectListByPayableId(100L)).thenReturn(singletonList(item));

        List<ErpOtherPayableItemDO> result = otherPayableService.getOtherPayableItemListByPayableId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPayableId()).isEqualTo(100L);
    }
}
