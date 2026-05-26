package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpVoucherItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.ErpPreReceiptMapper;
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
 * {@link ErpPreReceiptServiceImpl} 单元测试。
 *
 * 覆盖：create / update / updateStatus 审核 / updateStatus 反审 / delete / get / page / list 共 25 用例。
 */
@DisplayName("ErpPreReceiptServiceImpl 单元测试")
public class ErpPreReceiptServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPreReceiptServiceImpl service;

    @Mock
    private ErpPreReceiptMapper preReceiptMapper;
    @Mock
    private ErpPreReceiptItemMapper preReceiptItemMapper;
    private final java.util.concurrent.atomic.AtomicReference<String> nextNoRef =
            new java.util.concurrent.atomic.AtomicReference<>("YSKD-DEFAULT");
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

    private static final int VT = ErpVoucherTypeEnum.PRE_RECEIPT.getType();
    private static final int SBT = ErpVoucherSourceBizTypeEnum.PRE_RECEIPT.getType();

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

    private ErpPreReceiptSaveReqVO buildCreateReq() {
        ErpPreReceiptSaveReqVO req = new ErpPreReceiptSaveReqVO();
        req.setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0));
        req.setPartyType(1);
        req.setPartyId(11L);
        req.setPartyName("某客户");
        req.setAccountId(101L);
        req.setDiscountAmount(new BigDecimal("5.00"));
        ErpPreReceiptSaveReqVO.Item it1 = new ErpPreReceiptSaveReqVO.Item();
        it1.setSummary("项 1");
        it1.setAmount(new BigDecimal("60.00"));
        ErpPreReceiptSaveReqVO.Item it2 = new ErpPreReceiptSaveReqVO.Item();
        it2.setSummary("项 2");
        it2.setAmount(new BigDecimal("40.50"));
        req.setItems(asList(it1, it2));
        return req;
    }

    @Test
    @DisplayName("create：正常创建 - 单号生成 + 主表插入 + 子表 insertBatch + 校验账户")
    public void testCreate_normalCase() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSKD20260510000001");
        when(preReceiptMapper.selectByNo("YSKD20260510000001")).thenReturn(null);
        when(preReceiptMapper.insert(any(ErpPreReceiptDO.class))).thenAnswer(inv -> {
            ((ErpPreReceiptDO) inv.getArgument(0)).setId(100L);
            return 1;
        });

        Long id = service.createPreReceipt(req);

        assertEquals(100L, id);
        verify(accountService).validateAccount(101L);
        verify(preReceiptItemMapper).insertBatch(argThat(items -> items.size() == 2));
    }

    @Test
    @DisplayName("create：accountId 为 null 时不校验账户")
    public void testCreate_accountIdNull_skipValidate() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setAccountId(null);
        nextNoRef.set("YSKD20260510000002");
        when(preReceiptMapper.selectByNo(any())).thenReturn(null);
        when(preReceiptMapper.insert(any(ErpPreReceiptDO.class))).thenAnswer(inv -> {
            ((ErpPreReceiptDO) inv.getArgument(0)).setId(101L);
            return 1;
        });

        service.createPreReceipt(req);

        verify(accountService, never()).validateAccount(any());
    }

    @Test
    @DisplayName("create：单号已存在 - 抛 PRE_RECEIPT_NO_EXISTS")
    public void testCreate_noAlreadyExists_throwException() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSKD_DUP");
        when(preReceiptMapper.selectByNo("YSKD_DUP")).thenReturn(new ErpPreReceiptDO());

        assertServiceException(() -> service.createPreReceipt(req), PRE_RECEIPT_NO_EXISTS);
        verify(preReceiptMapper, never()).insert(any(ErpPreReceiptDO.class));
    }

    @Test
    @DisplayName("create：totalAmount = items 求和，actualAmount = total - discount")
    public void testCreate_calculateTotalAmount() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        nextNoRef.set("YSKD_X");
        when(preReceiptMapper.selectByNo(any())).thenReturn(null);
        when(preReceiptMapper.insert(any(ErpPreReceiptDO.class))).thenAnswer(inv -> {
            ((ErpPreReceiptDO) inv.getArgument(0)).setId(200L);
            return 1;
        });

        service.createPreReceipt(req);

        ArgumentCaptor<ErpPreReceiptDO> captor = ArgumentCaptor.forClass(ErpPreReceiptDO.class);
        verify(preReceiptMapper).insert(captor.capture());
        ErpPreReceiptDO inserted = captor.getValue();
        assertEquals(0, new BigDecimal("100.50").compareTo(inserted.getTotalAmount()));
        assertEquals(0, new BigDecimal("95.50").compareTo(inserted.getActualAmount()));
        assertEquals(ErpAuditStatus.PROCESS.getStatus(), inserted.getStatus());
    }

    @Test
    @DisplayName("create：discountAmount 为 null - actualAmount = totalAmount")
    public void testCreate_discountNull_actualEqualsTotal() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setDiscountAmount(null);
        nextNoRef.set("YSKD_Y");
        when(preReceiptMapper.selectByNo(any())).thenReturn(null);
        when(preReceiptMapper.insert(any(ErpPreReceiptDO.class))).thenAnswer(inv -> {
            ((ErpPreReceiptDO) inv.getArgument(0)).setId(201L);
            return 1;
        });

        service.createPreReceipt(req);

        ArgumentCaptor<ErpPreReceiptDO> captor = ArgumentCaptor.forClass(ErpPreReceiptDO.class);
        verify(preReceiptMapper).insert(captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update：正常更新主表 + 子表 diff")
    public void testUpdate_normalCase() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.getItems().get(0).setId(10L);
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceiptDO.class)))
                .thenReturn(1);
        ErpPreReceiptItemDO oldItem = new ErpPreReceiptItemDO();
        oldItem.setId(10L);
        oldItem.setPreReceiptId(100L);
        when(preReceiptItemMapper.selectListByPreReceiptId(100L)).thenReturn(asList(oldItem));

        service.updatePreReceipt(req);

        verify(preReceiptMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceiptDO.class));
        verify(preReceiptItemMapper, atLeastOnce()).insertBatch(anyList());
        verify(preReceiptItemMapper, atLeastOnce()).updateBatch(anyList());
    }

    @Test
    @DisplayName("update：单据不存在 - 抛 PRE_RECEIPT_NOT_EXISTS")
    public void testUpdate_notExists() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setId(999L);
        when(preReceiptMapper.selectById(999L)).thenReturn(null);

        assertServiceException(() -> service.updatePreReceipt(req), PRE_RECEIPT_NOT_EXISTS);
    }

    @Test
    @DisplayName("update：已审核单据 - 抛 PRE_RECEIPT_UPDATE_FAIL_APPROVE")
    public void testUpdate_alreadyApproved_forbidden() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setId(100L);
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceipt(req), PRE_RECEIPT_UPDATE_FAIL_APPROVE, "YSKD");
        verify(preReceiptMapper, never()).updateByIdAndStatus(anyLong(), anyInt(), any(ErpPreReceiptDO.class));
    }

    @Test
    @DisplayName("update：子表 diff - insertBatch / updateBatch / deleteByIds 三分支")
    public void testUpdate_itemsDiff_addUpdateDelete() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.getItems().get(0).setId(10L); // 修改老的
        // 第 2 项无 id（新增）
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceiptDO.class)))
                .thenReturn(1);
        ErpPreReceiptItemDO oldKeep = new ErpPreReceiptItemDO();
        oldKeep.setId(10L);
        ErpPreReceiptItemDO oldDel = new ErpPreReceiptItemDO();
        oldDel.setId(11L);
        when(preReceiptItemMapper.selectListByPreReceiptId(100L)).thenReturn(asList(oldKeep, oldDel));

        service.updatePreReceipt(req);

        verify(preReceiptItemMapper).insertBatch(anyList());
        verify(preReceiptItemMapper).updateBatch(anyList());
        verify(preReceiptItemMapper).deleteByIds(anyList());
    }

    @Test
    @DisplayName("update：明细变更后重新计算 totalAmount/actualAmount")
    public void testUpdate_recalculateTotalAmount() {
        ErpPreReceiptSaveReqVO req = buildCreateReq();
        req.setId(100L);
        req.setDiscountAmount(new BigDecimal("10.00"));
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceiptDO.class)))
                .thenReturn(1);
        when(preReceiptItemMapper.selectListByPreReceiptId(100L)).thenReturn(Collections.emptyList());

        service.updatePreReceipt(req);

        ArgumentCaptor<ErpPreReceiptDO> captor = ArgumentCaptor.forClass(ErpPreReceiptDO.class);
        verify(preReceiptMapper).updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), captor.capture());
        assertEquals(0, new BigDecimal("100.50").compareTo(captor.getValue().getTotalAmount()));
        assertEquals(0, new BigDecimal("90.50").compareTo(captor.getValue().getActualAmount()));
    }

    // ==================== updateStatus 审核 ====================

    @Test
    @DisplayName("updateStatus 审核：正常审核 + 生成凭证")
    public void testApprove_normalCase() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 10, 0))
                .setPartyName("某客户");
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.PROCESS.getStatus()), any(ErpPreReceiptDO.class)))
                .thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(eq(LocalDate.of(2026, 5, 10)), eq(VT))).thenReturn(true);
        when(autoVoucherBuilder.buildPreReceiptItems(existing)).thenReturn(Collections.emptyList());

        service.updatePreReceiptStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService).createVoucherFromBiz(eq(SBT), eq(100L), eq("YSKD"),
                any(), eq(LocalDate.of(2026, 5, 10)), startsWith("预收款 - "), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：BookOpen 未启用该凭证类型 - 跳过凭证生成")
    public void testApprove_voucherTypeDisabled_skipVoucher() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某客户");
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);
        when(bookOpenService.isVoucherTypeEnabled(any(LocalDate.class), eq(VT))).thenReturn(false);

        service.updatePreReceiptStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：bizTime 为 null - 跳过凭证生成")
    public void testApprove_bizTimeNull_skipVoucher() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(null)
                .setPartyName("某客户");
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updatePreReceiptStatus(100L, ErpAuditStatus.APPROVE.getStatus());

        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("updateStatus 审核：状态已是审核 - 抛 PRE_RECEIPT_APPROVE_FAIL")
    public void testApprove_alreadyApproved_throwException() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceiptStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_RECEIPT_APPROVE_FAIL);
    }

    @Test
    @DisplayName("updateStatus 审核：updateByIdAndStatus 返回 0 - 抛 PRE_RECEIPT_APPROVE_FAIL（乐观锁失败）")
    public void testApprove_optimisticLockFail() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setBizTime(LocalDateTime.of(2026, 5, 10, 0, 0))
                .setPartyName("某客户");
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(preReceiptMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(0);

        assertServiceException(() -> service.updatePreReceiptStatus(100L, ErpAuditStatus.APPROVE.getStatus()),
                PRE_RECEIPT_APPROVE_FAIL);
        verify(voucherService, never()).createVoucherFromBiz(anyInt(), anyLong(), anyString(), any(), any(), anyString(), anyList());
    }

    // ==================== updateStatus 反审 ====================

    @Test
    @DisplayName("updateStatus 反审：删除未审核凭证 + 状态回改")
    public void testProcess_normalCase() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.PROCESS.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));
        when(preReceiptMapper.updateByIdAndStatus(eq(100L), eq(ErpAuditStatus.APPROVE.getStatus()), any())).thenReturn(1);

        service.updatePreReceiptStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper).deleteById(500L);
        verify(voucherItemMapper).delete(any());
    }

    @Test
    @DisplayName("updateStatus 反审：关联凭证已审核 - 抛 BIZ_PROCESS_FAIL_VOUCHER_APPROVED")
    public void testProcess_voucherApproved_throwException() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        ErpVoucherDO v = new ErpVoucherDO().setId(500L).setVoucherNo("V-001")
                .setAuditStatus(ErpVoucherAuditStatusEnum.APPROVE.getStatus());
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(asList(v));

        assertServiceException(() -> service.updatePreReceiptStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                BIZ_PROCESS_FAIL_VOUCHER_APPROVED);
        verify(preReceiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：无关联凭证 - 直接改状态")
    public void testProcess_noRelatedVoucher_directRevert() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);
        when(voucherMapper.selectListByBiz(SBT, 100L)).thenReturn(Collections.emptyList());
        when(preReceiptMapper.updateByIdAndStatus(any(), any(), any())).thenReturn(1);

        service.updatePreReceiptStatus(100L, ErpAuditStatus.PROCESS.getStatus());

        verify(voucherMapper, never()).deleteById(anyLong());
        verify(preReceiptMapper).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateStatus 反审：状态已是 PROCESS - 抛 PRE_RECEIPT_PROCESS_FAIL")
    public void testProcess_alreadyProcess_throwException() {
        ErpPreReceiptDO existing = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceiptMapper.selectById(100L)).thenReturn(existing);

        assertServiceException(() -> service.updatePreReceiptStatus(100L, ErpAuditStatus.PROCESS.getStatus()),
                PRE_RECEIPT_PROCESS_FAIL);
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete：正常删除 - 删主表 + 子表（按 selectListByPreReceiptId + deleteByIds）")
    public void testDelete_normalCase() {
        ErpPreReceiptDO d = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.PROCESS.getStatus());
        when(preReceiptMapper.selectByIds(asList(100L))).thenReturn(asList(d));
        ErpPreReceiptItemDO subItem = new ErpPreReceiptItemDO();
        subItem.setId(20L);
        subItem.setPreReceiptId(100L);
        when(preReceiptItemMapper.selectListByPreReceiptId(100L)).thenReturn(asList(subItem));

        service.deletePreReceipt(asList(100L));

        verify(preReceiptMapper).deleteById(100L);
        verify(preReceiptItemMapper).deleteByIds(anyCollection());
    }

    @Test
    @DisplayName("delete：selectByIds 返回空 - 静默返回不报错")
    public void testDelete_emptyResult_returnSilent() {
        when(preReceiptMapper.selectByIds(asList(999L))).thenReturn(Collections.emptyList());

        service.deletePreReceipt(asList(999L));

        verify(preReceiptMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("delete：包含已审单据 - 抛 PRE_RECEIPT_DELETE_FAIL_APPROVE")
    public void testDelete_hasApproved_throwException() {
        ErpPreReceiptDO d = new ErpPreReceiptDO().setId(100L).setNo("YSKD")
                .setStatus(ErpAuditStatus.APPROVE.getStatus());
        when(preReceiptMapper.selectByIds(asList(100L))).thenReturn(asList(d));

        assertServiceException(() -> service.deletePreReceipt(asList(100L)),
                PRE_RECEIPT_DELETE_FAIL_APPROVE, "YSKD");
        verify(preReceiptMapper, never()).deleteById(anyLong());
    }

    // ==================== get / page / list ====================

    @Test
    @DisplayName("get：正常返回")
    public void testGetPreReceipt_normal() {
        ErpPreReceiptDO d = new ErpPreReceiptDO().setId(100L);
        when(preReceiptMapper.selectById(100L)).thenReturn(d);

        assertSame(d, service.getPreReceipt(100L));
    }

    @Test
    @DisplayName("getPage：透传 mapper.selectPage 结果")
    public void testGetPreReceiptPage_normal() {
        ErpPreReceiptPageReqVO req = new ErpPreReceiptPageReqVO();
        PageResult<ErpPreReceiptDO> page = new PageResult<>(Collections.emptyList(), 0L);
        when(preReceiptMapper.selectPage(req)).thenReturn(page);

        assertSame(page, service.getPreReceiptPage(req));
    }

    @Test
    @DisplayName("getItemListByPreReceiptIds：传入空集合 - 返回 emptyList 且不调 mapper")
    public void testGetItemListByPreReceiptIds_emptyInput() {
        List<ErpPreReceiptItemDO> result = service.getPreReceiptItemListByPreReceiptIds(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(preReceiptItemMapper, never()).selectListByPreReceiptIds(any());
    }

}
