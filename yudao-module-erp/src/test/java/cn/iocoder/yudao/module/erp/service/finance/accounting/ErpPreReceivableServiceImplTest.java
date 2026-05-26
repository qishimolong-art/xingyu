package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable.ErpPreReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceivableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceivableMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpVoucherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
 * {@link ErpPreReceivableServiceImpl} 单元测试。
 *
 * 覆盖：create / update / updateStatus 审核 / updateStatus 反审 / delete / get / page / list 共 25 用例。
 * 注意：本 Service 有 accountService 依赖；update 用 delete-then-insert（LambdaQueryWrapper）；
 *      delete 入参为 List<Long>，子表删除走 LambdaQueryWrapper；VO 子项无 id 字段。
 */
@DisplayName("ErpPreReceivableServiceImpl 单元测试")
public class ErpPreReceivableServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPreReceivableServiceImpl service;

    @Mock
    private ErpPreReceivableMapper preReceivableMapper;
    @Mock
    private ErpPreReceivableItemMapper preReceivableItemMapper;
    private final java.util.concurrent.atomic.AtomicReference<String> nextNoRef =
            new java.util.concurrent.atomic.AtomicReference<>("YSZK-DEFAULT");
    @Mock
    private ErpAccountService accountService;
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

    private static final int VT = ErpVoucherTypeEnum.PRE_RECEIVABLE.getType();
    private static final int SBT = ErpVoucherSourceBizTypeEnum.PRE_RECEIVABLE.getType();

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

    private ErpPreReceivableSaveReqVO buildCreateReq() {
        ErpPreReceivableSaveReqVO req = new ErpPreReceivableSaveReqVO();
        req.setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        req.setPartyType(1);
        req.setPartyId(11L);
        req.setPartyName("某客户");
        req.setAccountId(101L);
        req.setDiscountAmount(new BigDecimal("5.00"));
        ErpPreReceivableSaveReqVO.Item it1 = new ErpPreReceivableSaveReqVO.Item();
        it1.setSummary("项 1");
        it1.setAmount(new BigDecimal("60.00"));
        ErpPreReceivableSaveReqVO.Item it2 = new ErpPreReceivableSaveReqVO.Item();
        it2.setSummary("项 2");
        it2.setAmount(new BigDecimal("40.50"));
        req.setItems(asList(it1, it2));
        return req;
    }

    @Test
    @DisplayName("create：正常创建 - 单号生成 + 主表插入 + 子表 insertBatch + 校验账户")
    public void testCreate_normalCase() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSZK20260510000001");
        when(preReceivableMapper.selectByNo("YSZK20260510000001")).thenReturn(null);
        when(preReceivableMapper.insert(any(ErpPreReceivableDO.class))).thenAnswer(inv -> {
            ((ErpPreReceivableDO) inv.getArgument(0)).setId(100L);
            return 1;
        });

        Long id = service.createPreReceivable(req);

        assertEquals(100L, id);
        verify(accountService).validateAccount(101L);
        verify(preReceivableItemMapper).insertBatch(argThat(items -> items.size() == 2));
    }

    @Test
    @DisplayName("create：accountId 为 null 时不校验账户")
    public void testCreate_accountIdNull_skipValidate() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setAccountId(null);
        nextNoRef.set("YSZK20260510000002");
        when(preReceivableMapper.selectByNo(any())).thenReturn(null);
        when(preReceivableMapper.insert(any(ErpPreReceivableDO.class))).thenAnswer(inv -> {
            ((ErpPreReceivableDO) inv.getArgument(0)).setId(101L);
            return 1;
        });

        service.createPreReceivable(req);

        verify(accountService, never()).validateAccount(any());
    }

    @Test
    @DisplayName("create：单号已存在 - 抛 PRE_RECEIVABLE_NO_EXISTS")
    public void testCreate_noAlreadyExists_throwException() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSZK_DUP");
        when(preReceivableMapper.selectByNo("YSZK_DUP")).thenReturn(new ErpPreReceivableDO());

        assertServiceException(() -> service.createPreReceivable(req), PRE_RECEIVABLE_NO_EXISTS);
        verify(preReceivableMapper, never()).insert(any(ErpPreReceivableDO.class));
    }

    @Test
    @DisplayName("create：totalAmount = items 求和，actualAmount = total - discount")
    public void testCreate_calculateTotalAmount() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSZK_X");
        when(preReceivableMapper.selectByNo(any())).thenReturn(null);
        when(preReceivableMapper.insert(any(ErpPreReceivableDO.class))).thenAnswer(inv -> {
            ((ErpPreReceivableDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        service.createPreReceivable(req);

        ArgumentCaptor<ErpPreReceivableDO> captor = ArgumentCaptor.forClass(ErpPreReceivableDO.class);
        verify(preReceivableMapper).insert(captor.capture());
        ErpPreReceivableDO inserted = captor.getValue();
        assertEquals(0, new BigDecimal("100.50").compareTo(inserted.getTotalAmount()));
        assertEquals(0, new BigDecimal("95.50").compareTo(inserted.getActualAmount()));
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
    }

    @Test
    @DisplayName("create：discountAmount 为 null - actualAmount = totalAmount")
    public void testCreate_discountNull_actualEqualsTotal() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setDiscountAmount(null);
        nextNoRef.set("YSZK_Y");
        when(preReceivableMapper.selectByNo(any())).thenReturn(null);
        when(preReceivableMapper.insert(any(ErpPreReceivableDO.class))).thenAnswer(inv -> {
            ((ErpPreReceivableDO) inv.getArgument(0)).setId(201L);
            return 1;
        });

        service.createPreReceivable(req);

        ArgumentCaptor<ErpPreReceivableDO> captor = ArgumentCaptor.forClass(ErpPreReceivableDO.class);
        verify(preReceivableMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update：正常更新主表 + 子表 delete-then-insert（LambdaQueryWrapper）")
    public void testUpdate_normalCase() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceivableDO.class)))
                .thenReturn(1);

        service.updatePreReceivable(req);

        verify(preReceivableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceivableDO.class));
        verify(preReceivableItemMapper).delete(any(Wrapper.class));
        verify(preReceivableItemMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("update：单据不存在 - 抛 PRE_RECEIVABLE_NOT_EXISTS")
    public void testUpdate_notExists() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setId(999L);
        when(preReceivableMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> service.updatePreReceivable(req), PRE_RECEIVABLE_NOT_EXISTS);
    }

    @Test
    @DisplayName("update：已审核单据 - 抛 PRE_RECEIVABLE_UPDATE_FAIL_APPROVE")
    public void testUpdate_alreadyApproved_forbidden() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceivable(req), PRE_RECEIVABLE_UPDATE_FAIL_APPROVE, "YSZK");
        verify(preReceivableMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any(ErpPreReceivableDO.class));
    }

    @Test
    @DisplayName("update：先删后插 - delete + insertBatch 严格顺序")
    public void testUpdate_deleteThenInsert() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceivableDO.class)))
                .thenReturn(1);

        service.updatePreReceivable(req);

        org.mockito.InOrder inOrder = inOrder(preReceivableItemMapper);
        inOrder.verify(preReceivableItemMapper).delete(any(Wrapper.class));
        inOrder.verify(preReceivableItemMapper).insertBatch(anyList());
    }

    @Test
    @DisplayName("update：明细变更后重新计算 totalAmount/actualAmount")
    public void testUpdate_recalculateTotalAmount() {
        ErpPreReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.setDiscountAmount(new BigDecimal("10.00"));
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceivableDO.class)))
                .thenReturn(1);

        service.updatePreReceivable(req);

        ArgumentCaptor<ErpPreReceivableDO> captor = ArgumentCaptor.forClass(ErpPreReceivableDO.class);
        verify(preReceivableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getTotalAmount()));
        assertEquals(0, new BigDecimal("90.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== updateStatus 审核 ====================

    @Test
    @DisplayName("updateStatus 审核：正常审核 + 生成凭证")
    public void testApprove_normalCase() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .setPartyName("某客户");
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceivableDO.class)))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(eq(LocalDate.of(2026, 5, 10)), eq(VT))).thenReturn(true);
        when(autoVoucherBuilder.buildPreReceivableItems(existing)).thenReturn(Collections.emptyList());

        service.updatePreReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService).createVoucherFromBiz(eq(SBT), eq(100L), eq("YSZK"),
                any(), eq(LocalDate.of(2026, 5, 10)), startsWith("预收账款 - "), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：BookOpen 未启用该凭证类型 - 跳过凭证生成")
    public void testApprove_voucherTypeDisabled_skipVoucher() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某客户");
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        service.updatePreReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：bizTime 为 null - 跳过凭证生成")
    public void testApprove_bizTimeNull_skipVoucher() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(null)
                .setPartyName("某客户");
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updatePreReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：状态已是审核 - 抛 PRE_RECEIVABLE_APPROVE_FAIL")
    public void testApprove_alreadyApproved_throwException() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_RECEIVABLE_APPROVE_FAIL);
    }

    @Test
    @DisplayName("updateStatus 审核：updateByIdAndStatus 返回 0 - 抛 PRE_RECEIVABLE_APPROVE_FAIL（乐观锁失败）")
    public void testApprove_optimisticLockFail() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某客户");
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(preReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> service.updatePreReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_RECEIVABLE_APPROVE_FAIL);
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    // ==================== updateStatus 反审 ====================

    @Test
    @DisplayName("updateStatus 反审：删除未审核凭证 + 状态回改")
    public void testProcess_normalCase() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));
        when(preReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        service.updatePreReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(500L);
        verify(voucherItemMapper).delete(any(Wrapper.class));
    }

    @Test
    @DisplayName("updateStatus 反审：关联凭证已审核 - 抛 BIZ_PROCESS_FAIL_VOUCHER_APPROVED")
    public void testProcess_voucherApproved_throwException() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));

        assertServiceException(() -> service.updatePreReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "V-001");
        verify(preReceivableMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：无关联凭证 - 直接改状态")
    public void testProcess_noRelatedVoucher_directRevert() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(Collections.emptyList());
        when(preReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updatePreReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper, never()).deleteById(anyLong());
        verify(preReceivableMapper).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：状态已是 PROCESS - 抛 PRE_RECEIVABLE_PROCESS_FAIL")
    public void testProcess_alreadyProcess_throwException() {
        ErpPreReceivableDO existing = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                PRE_RECEIVABLE_PROCESS_FAIL);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete：正常删除 - 删主表 + 每个 id 走 LambdaQueryWrapper 删子表")
    public void testDelete_normalCase() {
        ErpPreReceivableDO d = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceivableMapper.selectByIds(asList(100L))).thenReturn(asList(d));

        service.deletePreReceivable(asList(100L));

        verify(preReceivableMapper).deleteByIds(asList(100L));
        verify(preReceivableItemMapper).delete(any(Wrapper.class));
    }

    @Test
    @DisplayName("delete：selectByIds 返回空 - 静默返回不报错")
    public void testDelete_emptyResult_returnSilent() {
        when(preReceivableMapper.selectByIds(asList(999L))).thenReturn(Collections.emptyList());

        service.deletePreReceivable(asList(999L));

        verify(preReceivableMapper, never()).deleteByIds(any());
    }

    @Test
    @DisplayName("delete：包含已审单据 - 抛 PRE_RECEIVABLE_DELETE_FAIL_APPROVE")
    public void testDelete_hasApproved_throwException() {
        ErpPreReceivableDO d = new ErpPreReceivableDO().setId(100L).setNo("YSZK")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceivableMapper.selectByIds(asList(100L))).thenReturn(asList(d));

        assertServiceException(() -> service.deletePreReceivable(asList(100L)),
                PRE_RECEIVABLE_DELETE_FAIL_APPROVE, "YSZK");
        verify(preReceivableMapper, never()).deleteByIds(any());
    }

    // ==================== get / page / list ====================

    @Test
    @DisplayName("get：正常返回")
    public void testGetPreReceivable_normal() {
        ErpPreReceivableDO d = new ErpPreReceivableDO().setId(100L);
        when(preReceivableMapper.selectById(100L)).thenReturn(d);

        assertSame(d, service.getPreReceivable(100L));
    }

    @Test
    @DisplayName("getPage：透传 mapper.selectPage 结果")
    public void testGetPreReceivablePage_normal() {
        ErpPreReceivablePageReqVO req = new ErpPreReceivablePageReqVO();
        PageResult<ErpPreReceivableDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(preReceivableMapper.selectPage(req)).thenReturn(page);

        assertSame(page, service.getPreReceivablePage(req));
    }

    @Test
    @DisplayName("getItemListByPreReceivableIds：传入空集合 - 返回 emptyList 且不调 mapper")
    public void testGetItemListByPreReceivableIds_emptyInput() {
        List<ErpPreReceivableItemDO> result = service.getPreReceivableItemListByPreReceivableIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(preReceivableItemMapper, never()).selectListByPreReceivableIds(any());
    }

}
