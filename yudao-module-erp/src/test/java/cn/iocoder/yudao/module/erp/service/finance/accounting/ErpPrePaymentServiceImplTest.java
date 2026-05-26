package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment.ErpPrePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPrePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPrePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ErpPrePaymentServiceImpl} 单元测试。
 *
 * 覆盖：create / update / updateStatus 审核 / updateStatus 反审 / delete / get / page / list 共 25 用例。
 * 注意：本 Service 无 accountService 依赖；update 用 delete-then-insert；delete 入参为单个 Long；
 *      bizTime 为 null 时 fallback 到 LocalDate.now() 仍走凭证生成分支（与 OtherReceivable 不同）。
 */
@DisplayName("ErpPrePaymentServiceImpl 单元测试")
public class ErpPrePaymentServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPrePaymentServiceImpl service;

    @Mock
    private ErpPrePaymentMapper prePaymentMapper;
    @Mock
    private ErpPrePaymentItemMapper prePaymentItemMapper;
    private final java.util.concurrent.atomic.AtomicReference<String> nextNoRef =
            new java.util.concurrent.atomic.AtomicReference<>("YFKD-DEFAULT");
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
    @Mock
    private ErpAccountService accountService;

    private static final int VT = ErpVoucherTypeEnum.PRE_PAYMENT.getType();
    private static final int SBT = ErpVoucherSourceBizTypeEnum.PRE_PAYMENT.getType();

    @BeforeEach
    public void setupNoRedisDAO() {
        ReflectionTestUtils.setField(service, "noRedisDAO", new ErpNoRedisDAO() {
            @Override
            public String generate(String prefix) {
                return nextNoRef.get();
            }
        });
    }

    // ==================== create ====================

    private ErpPrePaymentSaveReqVO buildCreateReq() {
        ErpPrePaymentSaveReqVO req = new ErpPrePaymentSaveReqVO();
        req.setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        req.setPartyType(2);
        req.setPartyId(22L);
        req.setPartyName("某供应商");
        req.setAccountId(101L);
        req.setDiscountAmount(new BigDecimal("5.00"));
        ErpPrePaymentSaveReqVO.Item it1 = new ErpPrePaymentSaveReqVO.Item();
        it1.setSummary("项 1");
        it1.setAmount(new BigDecimal("60.00"));
        ErpPrePaymentSaveReqVO.Item it2 = new ErpPrePaymentSaveReqVO.Item();
        it2.setSummary("项 2");
        it2.setAmount(new BigDecimal("40.50"));
        req.setItems(asList(it1, it2));
        return req;
    }

    @Test
    @DisplayName("create：正常创建 - 单号生成 + 主表插入 + 子表 insertBatch")
    public void testCreate_normalCase() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        nextNoRef.set("YFKD20260510000001");
        when(prePaymentMapper.selectByNo("YFKD20260510000001")).thenReturn(null);
        when(prePaymentMapper.insert(any(ErpPrePaymentDO.class))).thenAnswer(inv -> {
            ((ErpPrePaymentDO) inv.getArgument(0)).setId(100L);
            return 1;
        });

        Long id = service.createPrePayment(req);

        assertEquals(100L, id);
        verify(prePaymentItemMapper).insertBatch(argThat(items -> items.size() == 2));
    }

    @Test
    @DisplayName("create：单号已存在 - 抛 PRE_PAYMENT_NO_EXISTS")
    public void testCreate_noAlreadyExists_throwException() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        nextNoRef.set("YFKD_DUP");
        when(prePaymentMapper.selectByNo("YFKD_DUP")).thenReturn(new ErpPrePaymentDO());

        assertServiceException(() -> service.createPrePayment(req), PRE_PAYMENT_NO_EXISTS);
        verify(prePaymentMapper, never()).insert(any(ErpPrePaymentDO.class));
    }

    @Test
    @DisplayName("create：totalAmount = items 求和，actualAmount = total - discount")
    public void testCreate_calculateTotalAmount() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        nextNoRef.set("YFKD_X");
        when(prePaymentMapper.selectByNo(any())).thenReturn(null);
        when(prePaymentMapper.insert(any(ErpPrePaymentDO.class))).thenAnswer(inv -> {
            ((ErpPrePaymentDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        service.createPrePayment(req);

        ArgumentCaptor<ErpPrePaymentDO> captor = ArgumentCaptor.forClass(ErpPrePaymentDO.class);
        verify(prePaymentMapper).insert(captor.capture());
        ErpPrePaymentDO inserted = captor.getValue();
        assertEquals(0, new BigDecimal("100.50").compareTo(inserted.getTotalAmount()));
        assertEquals(0, new BigDecimal("95.50").compareTo(inserted.getActualAmount()));
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
    }

    @Test
    @DisplayName("create：discountAmount 为 null - actualAmount = totalAmount，discountAmount 落库为 ZERO")
    public void testCreate_discountNull_actualEqualsTotal() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setDiscountAmount(null);
        nextNoRef.set("YFKD_Y");
        when(prePaymentMapper.selectByNo(any())).thenReturn(null);
        when(prePaymentMapper.insert(any(ErpPrePaymentDO.class))).thenAnswer(inv -> {
            ((ErpPrePaymentDO) inv.getArgument(0)).setId(201L);
            return 1;
        });

        service.createPrePayment(req);

        ArgumentCaptor<ErpPrePaymentDO> captor = ArgumentCaptor.forClass(ErpPrePaymentDO.class);
        verify(prePaymentMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getActualAmount()));
        // 注意 calculateTotalAmount 内部会兜底为 ZERO
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getDiscountAmount()));
    }

    @Test
    @DisplayName("create：插入子表时每个 item 的 prePaymentId 已被回填")
    public void testCreate_itemsSetPrePaymentId() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        nextNoRef.set("YFKD_Z");
        when(prePaymentMapper.selectByNo(any())).thenReturn(null);
        when(prePaymentMapper.insert(any(ErpPrePaymentDO.class))).thenAnswer(inv -> {
            ((ErpPrePaymentDO) inv.getArgument(0)).setId(300L);
            return 1;
        });

        service.createPrePayment(req);

        verify(prePaymentItemMapper).insertBatch(argThat(items ->
                items.size() == 2 && items.stream().allMatch(it -> ((ErpPrePaymentItemDO) it).getPrePaymentId().equals(300L))));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update：正常更新主表 + 子表 delete-then-insert")
    public void testUpdate_normalCase() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPrePaymentDO.class)))
                .thenReturn(1);

        service.updatePrePayment(req);

        verify(prePaymentMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPrePaymentDO.class));
        verify(prePaymentItemMapper).deleteByPrePaymentId(100L);
        verify(prePaymentItemMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("update：单据不存在 - 抛 PRE_PAYMENT_NOT_EXISTS")
    public void testUpdate_notExists() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setId(999L);
        when(prePaymentMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> service.updatePrePayment(req), PRE_PAYMENT_NOT_EXISTS);
    }

    @Test
    @DisplayName("update：已审核单据 - 抛 PRE_PAYMENT_UPDATE_FAIL_APPROVE")
    public void testUpdate_alreadyApproved_forbidden() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePrePayment(req), PRE_PAYMENT_UPDATE_FAIL_APPROVE, "YFKD");
        verify(prePaymentMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any(ErpPrePaymentDO.class));
    }

    @Test
    @DisplayName("update：子表先删后插 - deleteByPrePaymentId + insertBatch 严格顺序")
    public void testUpdate_deleteThenInsert() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPrePaymentDO.class)))
                .thenReturn(1);

        service.updatePrePayment(req);

        org.mockito.InOrder inOrder = inOrder(prePaymentItemMapper);
        inOrder.verify(prePaymentItemMapper).deleteByPrePaymentId(100L);
        inOrder.verify(prePaymentItemMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("update：明细变更后重新计算 totalAmount/actualAmount")
    public void testUpdate_recalculateTotalAmount() {
        ErpPrePaymentSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.setDiscountAmount(new BigDecimal("10.00"));
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPrePaymentDO.class)))
                .thenReturn(1);

        service.updatePrePayment(req);

        ArgumentCaptor<ErpPrePaymentDO> captor = ArgumentCaptor.forClass(ErpPrePaymentDO.class);
        verify(prePaymentMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getTotalAmount()));
        assertEquals(0, new BigDecimal("90.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== updateStatus 审核 ====================

    @Test
    @DisplayName("updateStatus 审核：正常审核 + 生成凭证")
    public void testApprove_normalCase() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .setPartyName("某供应商");
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPrePaymentDO.class)))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(eq(LocalDate.of(2026, 5, 10)), eq(VT))).thenReturn(true);
        when(autoVoucherBuilder.buildPrePaymentItems(existing)).thenReturn(Collections.emptyList());

        service.updatePrePaymentStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService).createVoucherFromBiz(eq(SBT), eq(100L), eq("YFKD"),
                any(), eq(LocalDate.of(2026, 5, 10)), startsWith("预付款 - "), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：BookOpen 未启用该凭证类型 - 跳过凭证生成")
    public void testApprove_voucherTypeDisabled_skipVoucher() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某供应商");
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        service.updatePrePaymentStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：bizTime 为 null - 使用 LocalDate.now() 兜底，仍走凭证检查")
    public void testApprove_bizTimeNull_useNowFallback() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(null)
                .setPartyName("某供应商");
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        // 注意：bizTime null 时仍会调用 isVoucherTypeEnabled(now(), VT)，与 OtherReceivable 不同
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        service.updatePrePaymentStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(bookOpenService).isVoucherTypeEnabled(any(LocalDate.class), eq(VT));
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：状态已是审核 - 抛 PRE_PAYMENT_APPROVE_FAIL")
    public void testApprove_alreadyApproved_throwException() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePrePaymentStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_PAYMENT_APPROVE_FAIL);
    }

    @Test
    @DisplayName("updateStatus 审核：updateByIdAndStatus 返回 0 - 抛 PRE_PAYMENT_APPROVE_FAIL（乐观锁失败）")
    public void testApprove_optimisticLockFail() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某供应商");
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(prePaymentMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> service.updatePrePaymentStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_PAYMENT_APPROVE_FAIL);
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    // ==================== updateStatus 反审 ====================

    @Test
    @DisplayName("updateStatus 反审：删除未审核凭证 + 状态回改（含 deleteByVoucherId）")
    public void testProcess_normalCase() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));
        when(prePaymentMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        service.updatePrePaymentStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherItemMapper).deleteByVoucherId(500L);
        verify(voucherMapper).deleteById(500L);
    }

    @Test
    @DisplayName("updateStatus 反审：关联凭证已审核 - 抛 BIZ_PROCESS_FAIL_VOUCHER_APPROVED")
    public void testProcess_voucherApproved_throwException() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));

        assertServiceException(() -> service.updatePrePaymentStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "V-001");
        verify(prePaymentMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：无关联凭证 - 直接改状态")
    public void testProcess_noRelatedVoucher_directRevert() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(Collections.emptyList());
        when(prePaymentMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updatePrePaymentStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper, never()).deleteById(anyLong());
        verify(prePaymentMapper).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：状态已是 PROCESS - 抛 PRE_PAYMENT_PROCESS_FAIL")
    public void testProcess_alreadyProcess_throwException() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePrePaymentStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                PRE_PAYMENT_PROCESS_FAIL);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete：正常删除 - 删主表 + deleteByPrePaymentId 子表")
    public void testDelete_normalCase() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);

        service.deletePrePayment(100L);

        verify(prePaymentMapper).deleteById(100L);
        verify(prePaymentItemMapper).deleteByPrePaymentId(100L);
    }

    @Test
    @DisplayName("delete：单据不存在 - 抛 PRE_PAYMENT_NOT_EXISTS")
    public void testDelete_notExists_throwException() {
        when(prePaymentMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> service.deletePrePayment(999L), PRE_PAYMENT_NOT_EXISTS);
        verify(prePaymentMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete：已审单据 - 抛 PRE_PAYMENT_DELETE_FAIL_APPROVE")
    public void testDelete_alreadyApproved_throwException() {
        ErpPrePaymentDO existing = new ErpPrePaymentDO().setId(100L).setNo("YFKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(prePaymentMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.deletePrePayment(100L),
                PRE_PAYMENT_DELETE_FAIL_APPROVE, "YFKD");
        verify(prePaymentMapper, never()).deleteById(anyLong());
    }

    // ==================== get / page / list ====================

    @Test
    @DisplayName("get：正常返回")
    public void testGetPrePayment_normal() {
        ErpPrePaymentDO d = new ErpPrePaymentDO().setId(100L);
        when(prePaymentMapper.selectById(100L)).thenReturn(d);

        assertSame(d, service.getPrePayment(100L));
    }

    @Test
    @DisplayName("getPage：透传 mapper.selectPage 结果")
    public void testGetPrePaymentPage_normal() {
        ErpPrePaymentPageReqVO req = new ErpPrePaymentPageReqVO();
        PageResult<ErpPrePaymentDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(prePaymentMapper.selectPage(req)).thenReturn(page);

        assertSame(page, service.getPrePaymentPage(req));
    }

    @Test
    @DisplayName("getItemListByPrePaymentId：透传 mapper 结果")
    public void testGetItemListByPrePaymentId_normal() {
        ErpPrePaymentItemDO item = new ErpPrePaymentItemDO();
        item.setId(20L);
        when(prePaymentItemMapper.selectListByPrePaymentId(100L)).thenReturn(asList(item));

        List<ErpPrePaymentItemDO> result = service.getPrePaymentItemListByPrePaymentId(100L);

        assertEquals(1, result.size());
        assertSame(item, result.get(0));
    }

}
