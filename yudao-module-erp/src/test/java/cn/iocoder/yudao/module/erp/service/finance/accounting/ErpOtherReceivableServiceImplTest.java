package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivablePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherreceivable.ErpOtherReceivableSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpOtherReceivableItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpOtherReceivableMapper;
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
 * {@link ErpOtherReceivableServiceImpl} 单元测试。
 *
 * 覆盖：create / update / updateStatus 审核 / updateStatus 反审 / delete / get / page / list 共 25 用例。
 */
@DisplayName("ErpOtherReceivableServiceImpl 单元测试")
public class ErpOtherReceivableServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpOtherReceivableServiceImpl service;

    @Mock
    private ErpOtherReceivableMapper otherReceivableMapper;
    @Mock
    private ErpOtherReceivableItemMapper otherReceivableItemMapper;
    private final java.util.concurrent.atomic.AtomicReference<String> nextNoRef =
            new java.util.concurrent.atomic.AtomicReference<>("QTYS-DEFAULT");
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpVoucherMapper voucherMapper;
    @Mock
    private ErpVoucherItemMapper voucherItemMapper;
    @Mock
    private ErpAutoVoucherBuilder autoVoucherBuilder;
    @Mock
    private ErpVoucherService voucherService;
    @Mock
    private ErpBookOpenService bookOpenService;

    private static final int VT = ErpVoucherTypeEnum.OTHER_RECEIVABLE.getType();
    private static final int SBT = ErpVoucherSourceBizTypeEnum.OTHER_RECEIVABLE.getType();

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

    private ErpOtherReceivableSaveReqVO buildCreateReq() {
        ErpOtherReceivableSaveReqVO req = new ErpOtherReceivableSaveReqVO();
        req.setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        req.setPartyType(1);
        req.setPartyId(11L);
        req.setPartyName("某客户");
        req.setAccountId(101L);
        req.setDiscountAmount(new BigDecimal("5.00"));
        ErpOtherReceivableSaveReqVO.Item it1 = new ErpOtherReceivableSaveReqVO.Item();
        it1.setSummary("项 1");
        it1.setAmount(new BigDecimal("60.00"));
        ErpOtherReceivableSaveReqVO.Item it2 = new ErpOtherReceivableSaveReqVO.Item();
        it2.setSummary("项 2");
        it2.setAmount(new BigDecimal("40.50"));
        req.setItems(asList(it1, it2));
        return req;
    }

    @Test
    @DisplayName("create：正常创建 - 单号生成 + 主表插入 + 子表 insertBatch + 校验账户")
    public void testCreate_normalCase() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("QTYS20260510000001");
        when(otherReceivableMapper.selectByNo("QTYS20260510000001")).thenReturn(null);
        when(otherReceivableMapper.insert(any(ErpOtherReceivableDO.class))).thenAnswer(inv -> {
            ((ErpOtherReceivableDO) inv.getArgument(0)).setId(100L);
            return 1;
        });

        Long id = service.createOtherReceivable(req);

        assertEquals(100L, id);
        verify(accountService).validateAccount(101L);
        verify(otherReceivableItemMapper).insertBatch(argThat(items -> items.size() == 2));
    }

    @Test
    @DisplayName("create：accountId 为 null 时不校验账户")
    public void testCreate_accountIdNull_skipValidate() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setAccountId(null);
        nextNoRef.set("QTYS20260510000002");
        when(otherReceivableMapper.selectByNo(any())).thenReturn(null);
        when(otherReceivableMapper.insert(any(ErpOtherReceivableDO.class))).thenAnswer(inv -> {
            ((ErpOtherReceivableDO) inv.getArgument(0)).setId(101L);
            return 1;
        });

        service.createOtherReceivable(req);

        verify(accountService, never()).validateAccount(any());
    }

    @Test
    @DisplayName("create：单号已存在 - 抛 OTHER_RECEIVABLE_NO_EXISTS")
    public void testCreate_noAlreadyExists_throwException() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("QTYS_DUP");
        when(otherReceivableMapper.selectByNo("QTYS_DUP")).thenReturn(new ErpOtherReceivableDO());

        assertServiceException(() -> service.createOtherReceivable(req), OTHER_RECEIVABLE_NO_EXISTS);
        verify(otherReceivableMapper, never()).insert(any(ErpOtherReceivableDO.class));
    }

    @Test
    @DisplayName("create：totalAmount = items 求和，actualAmount = total - discount")
    public void testCreate_calculateTotalAmount() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        nextNoRef.set("QTYS_X");
        when(otherReceivableMapper.selectByNo(any())).thenReturn(null);
        when(otherReceivableMapper.insert(any(ErpOtherReceivableDO.class))).thenAnswer(inv -> {
            ((ErpOtherReceivableDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        service.createOtherReceivable(req);

        ArgumentCaptor<ErpOtherReceivableDO> captor = ArgumentCaptor.forClass(ErpOtherReceivableDO.class);
        verify(otherReceivableMapper).insert(captor.capture());
        ErpOtherReceivableDO inserted = captor.getValue();
        assertEquals(0, new BigDecimal("100.50").compareTo(inserted.getTotalAmount()));
        assertEquals(0, new BigDecimal("95.50").compareTo(inserted.getActualAmount()));
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
    }

    @Test
    @DisplayName("create：discountAmount 为 null - actualAmount = totalAmount")
    public void testCreate_discountNull_actualEqualsTotal() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setDiscountAmount(null);
        nextNoRef.set("QTYS_Y");
        when(otherReceivableMapper.selectByNo(any())).thenReturn(null);
        when(otherReceivableMapper.insert(any(ErpOtherReceivableDO.class))).thenAnswer(inv -> {
            ((ErpOtherReceivableDO) inv.getArgument(0)).setId(201L);
            return 1;
        });

        service.createOtherReceivable(req);

        ArgumentCaptor<ErpOtherReceivableDO> captor = ArgumentCaptor.forClass(ErpOtherReceivableDO.class);
        verify(otherReceivableMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update：正常更新主表 + 子表 diff")
    public void testUpdate_normalCase() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.getItems().get(0).setId(10L);
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherReceivableDO.class)))
                .thenReturn(1);
        ErpOtherReceivableItemDO oldItem = new ErpOtherReceivableItemDO();
        oldItem.setId(10L);
        oldItem.setReceivableId(100L);
        when(otherReceivableItemMapper.selectListByReceivableId(100L)).thenReturn(asList(oldItem));

        service.updateOtherReceivable(req);

        verify(otherReceivableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherReceivableDO.class));
        // 旧的 1 条（id=10）保留更新，新增 1 条（无 id）
        verify(otherReceivableItemMapper, atLeastOnce()).insertBatch(anyList());
        verify(otherReceivableItemMapper, atLeastOnce()).updateBatch(anyList());
    }

    @Test
    @DisplayName("update：单据不存在 - 抛 OTHER_RECEIVABLE_NOT_EXISTS")
    public void testUpdate_notExists() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setId(999L);
        when(otherReceivableMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> service.updateOtherReceivable(req), OTHER_RECEIVABLE_NOT_EXISTS);
    }

    @Test
    @DisplayName("update：已审核单据 - 抛 OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE")
    public void testUpdate_alreadyApproved_forbidden() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updateOtherReceivable(req), OTHER_RECEIVABLE_UPDATE_FAIL_APPROVE, "QTYS");
        verify(otherReceivableMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any(ErpOtherReceivableDO.class));
    }

    @Test
    @DisplayName("update：子表 diff - insertBatch / updateBatch / deleteByIds 三分支")
    public void testUpdate_itemsDiff_addUpdateDelete() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.getItems().get(0).setId(10L); // 修改老的
        // 第 2 项无 id（新增）
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherReceivableDO.class)))
                .thenReturn(1);
        ErpOtherReceivableItemDO oldKeep = new ErpOtherReceivableItemDO();
        oldKeep.setId(10L);
        ErpOtherReceivableItemDO oldDel = new ErpOtherReceivableItemDO();
        oldDel.setId(11L);
        when(otherReceivableItemMapper.selectListByReceivableId(100L)).thenReturn(asList(oldKeep, oldDel));

        service.updateOtherReceivable(req);

        verify(otherReceivableItemMapper).insertBatch(anyList());
        verify(otherReceivableItemMapper).updateBatch(anyList());
        verify(otherReceivableItemMapper).deleteByIds(anyList());
    }

    @Test
    @DisplayName("update：明细变更后重新计算 totalAmount/actualAmount")
    public void testUpdate_recalculateTotalAmount() {
        ErpOtherReceivableSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.setDiscountAmount(new BigDecimal("10.00"));
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherReceivableDO.class)))
                .thenReturn(1);
        when(otherReceivableItemMapper.selectListByReceivableId(100L)).thenReturn(Collections.emptyList());

        service.updateOtherReceivable(req);

        ArgumentCaptor<ErpOtherReceivableDO> captor = ArgumentCaptor.forClass(ErpOtherReceivableDO.class);
        verify(otherReceivableMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getTotalAmount()));
        assertEquals(0, new BigDecimal("90.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== updateStatus 审核 ====================

    @Test
    @DisplayName("updateStatus 审核：正常审核 + 生成凭证")
    public void testApprove_normalCase() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .setPartyName("某客户");
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpOtherReceivableDO.class)))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(eq(LocalDate.of(2026, 5, 10)), eq(VT))).thenReturn(true);
        when(autoVoucherBuilder.buildOtherReceivableItems(existing)).thenReturn(Collections.emptyList());

        service.updateOtherReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService).createVoucherFromBiz(eq(SBT), eq(100L), eq("QTYS"),
                any(), eq(LocalDate.of(2026, 5, 10)), startsWith("其他应收 - "), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：BookOpen 未启用该凭证类型 - 跳过凭证生成")
    public void testApprove_voucherTypeDisabled_skipVoucher() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0));
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        service.updateOtherReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：bizTime 为 null - 跳过凭证生成")
    public void testApprove_bizTimeNull_skipVoucher() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(null);
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updateOtherReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：状态已是审核 - 抛 OTHER_RECEIVABLE_APPROVE_FAIL")
    public void testApprove_alreadyApproved_throwException() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updateOtherReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                OTHER_RECEIVABLE_APPROVE_FAIL);
    }

    @Test
    @DisplayName("updateStatus 审核：updateByIdAndStatus 返回 0 - 抛 OTHER_RECEIVABLE_APPROVE_FAIL（乐观锁失败）")
    public void testApprove_optimisticLockFail() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0));
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(otherReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> service.updateOtherReceivableStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                OTHER_RECEIVABLE_APPROVE_FAIL);
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    // ==================== updateStatus 反审 ====================

    @Test
    @DisplayName("updateStatus 反审：删除未审核凭证 + 状态回改")
    public void testProcess_normalCase() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));
        when(otherReceivableMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        service.updateOtherReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(500L);
        verify(voucherItemMapper).delete(any());
    }

    @Test
    @DisplayName("updateStatus 反审：关联凭证已审核 - 抛 BIZ_PROCESS_FAIL_VOUCHER_APPROVED")
    public void testProcess_voucherApproved_throwException() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));

        assertServiceException(() -> service.updateOtherReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED, "V-001");
        verify(otherReceivableMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：无关联凭证 - 直接改状态")
    public void testProcess_noRelatedVoucher_directRevert() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(Collections.emptyList());
        when(otherReceivableMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updateOtherReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper, never()).deleteById(anyLong());
        verify(otherReceivableMapper).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：状态已是 PROCESS - 抛 OTHER_RECEIVABLE_PROCESS_FAIL")
    public void testProcess_alreadyProcess_throwException() {
        ErpOtherReceivableDO existing = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherReceivableMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updateOtherReceivableStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                OTHER_RECEIVABLE_PROCESS_FAIL);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete：正常删除 - 删主表 + 子表")
    public void testDelete_normalCase() {
        ErpOtherReceivableDO d = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(otherReceivableMapper.selectByIds(asList(100L))).thenReturn(asList(d));

        service.deleteOtherReceivable(asList(100L));

        verify(otherReceivableMapper).deleteById(100L);
        verify(otherReceivableItemMapper).deleteByReceivableId(100L);
    }

    @Test
    @DisplayName("delete：selectByIds 返回空 - 静默返回不报错")
    public void testDelete_emptyResult_returnSilent() {
        when(otherReceivableMapper.selectByIds(asList(999L))).thenReturn(Collections.emptyList());

        service.deleteOtherReceivable(asList(999L));

        verify(otherReceivableMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete：包含已审单据 - 抛 OTHER_RECEIVABLE_DELETE_FAIL_APPROVE")
    public void testDelete_hasApproved_throwException() {
        ErpOtherReceivableDO d = new ErpOtherReceivableDO().setId(100L).setNo("QTYS")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(otherReceivableMapper.selectByIds(asList(100L))).thenReturn(asList(d));

        assertServiceException(() -> service.deleteOtherReceivable(asList(100L)),
                OTHER_RECEIVABLE_DELETE_FAIL_APPROVE, "QTYS");
        verify(otherReceivableMapper, never()).deleteById(anyLong());
    }

    // ==================== get / page / list ====================

    @Test
    @DisplayName("get：正常返回")
    public void testGetOtherReceivable_normal() {
        ErpOtherReceivableDO d = new ErpOtherReceivableDO().setId(100L);
        when(otherReceivableMapper.selectById(100L)).thenReturn(d);

        assertSame(d, service.getOtherReceivable(100L));
    }

    @Test
    @DisplayName("getPage：透传 mapper.selectPage 结果")
    public void testGetOtherReceivablePage_normal() {
        ErpOtherReceivablePageReqVO req = new ErpOtherReceivablePageReqVO();
        PageResult<ErpOtherReceivableDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(otherReceivableMapper.selectPage(req)).thenReturn(page);

        assertSame(page, service.getOtherReceivablePage(req));
    }

    @Test
    @DisplayName("getItemListByReceivableIds：传入空集合 - 返回 emptyList 且不调 mapper")
    public void testGetItemListByReceivableIds_emptyInput() {
        List<ErpOtherReceivableItemDO> result = service.getOtherReceivableItemListByReceivableIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(otherReceivableItemMapper, never()).selectListByReceivableIds(any());
    }

}
