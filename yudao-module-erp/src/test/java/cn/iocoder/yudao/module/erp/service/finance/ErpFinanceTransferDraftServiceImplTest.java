package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceTransferMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceTransferStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_ACCOUNTS_SAME;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_TRANSFER_DRAFT_UPDATE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceTransferDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceTransferServiceImpl service;

    @Mock
    private ErpFinanceTransferMapper financeTransferMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void createDraft_requiresCompleteFieldsAndKeepsDraftStatus() {
        when(noRedisDAO.generateMonthSequence(ErpNoRedisDAO.FINANCE_TRANSFER_NO_PREFIX))
                .thenReturn("YHZZ-DRAFT-1");
        when(financeTransferMapper.insert(any(ErpFinanceTransferDO.class))).thenAnswer(invocation -> {
            ((ErpFinanceTransferDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createFinanceTransferDraft(completeDraft());

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(ErpFinanceTransferDO.class);
        verify(financeTransferMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinanceTransferStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getTransferTime()).isNotNull();
        assertThat(captor.getValue().getOutAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getInAccountId()).isEqualTo(2L);
        assertThat(captor.getValue().getTransferPrice()).isEqualByComparingTo("100");
        assertThat(captor.getValue().getExchangeRate()).isEqualByComparingTo("1");
        verify(accountService).validateAccount(1L);
        verify(accountService).validateAccount(2L);
        verify(adminUserApi).validateUser(9L);
    }

    @Test
    void draftCreateAndUpdate_rejectMissingRequiredFieldsBeforeWriting() {
        assertDraftSaveRejected(req -> req.setTransferTime(null), "转账时间不能为空");
        assertDraftSaveRejected(req -> req.setOutAccountId(null), "转出账户不能为空");
        assertDraftSaveRejected(req -> req.setInAccountId(null), "转入账户不能为空");
        assertDraftSaveRejected(req -> req.setTransferPrice(null), "转账金额不能为空");
        assertDraftSaveRejected(req -> req.setFinanceUserId(null), "经手人不能为空");
        assertDraftSaveRejected(req -> req.setDeptId(null), "所属部门不能为空");
        verify(noRedisDAO, never()).generateMonthSequence(any());
        verify(financeTransferMapper, never()).insert(any(ErpFinanceTransferDO.class));
        verify(financeTransferMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void draftCreateAndUpdate_rejectInvalidAmountsBeforeWriting() {
        assertDraftSaveRejected(req -> req.setTransferPrice(BigDecimal.ZERO), "转账金额必须大于 0");
        assertDraftSaveRejected(req -> req.setTransferPrice(BigDecimal.ONE.negate()), "转账金额必须大于 0");
        assertDraftSaveRejected(req -> req.setExchangeRate(BigDecimal.ZERO), "汇率必须大于 0");
        assertDraftSaveRejected(req -> req.setFeePrice(BigDecimal.ONE.negate()), "手续费不能小于 0");
        verify(noRedisDAO, never()).generateMonthSequence(any());
        verify(financeTransferMapper, never()).insert(any(ErpFinanceTransferDO.class));
        verify(financeTransferMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void draftCreateAndUpdate_rejectSameAccounts() {
        ErpFinanceTransferDraftSaveReqVO req = completeDraft().setInAccountId(1L);
        assertServiceException(() -> service.createFinanceTransferDraft(req), FINANCE_TRANSFER_ACCOUNTS_SAME);
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus()));
        req.setId(10L);
        assertServiceException(() -> service.updateFinanceTransferDraft(req), FINANCE_TRANSFER_ACCOUNTS_SAME);
        verify(financeTransferMapper, never()).insert(any(ErpFinanceTransferDO.class));
        verify(financeTransferMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    private void assertDraftSaveRejected(Consumer<ErpFinanceTransferDraftSaveReqVO> change, String message) {
        ErpFinanceTransferDraftSaveReqVO req = completeDraft();
        change.accept(req);
        assertServiceException(() -> service.createFinanceTransferDraft(req), FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, message);
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus()));
        req.setId(10L);
        assertServiceException(() -> service.updateFinanceTransferDraft(req), FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, message);
    }

    private ErpFinanceTransferDraftSaveReqVO completeDraft() {
        return new ErpFinanceTransferDraftSaveReqVO().setTransferTime(LocalDateTime.now())
                .setOutAccountId(1L).setInAccountId(2L).setTransferPrice(new BigDecimal("100"))
                .setFinanceUserId(9L).setDeptId(3L);
    }

    @Test
    void createAndSubmit_createsProcessDocument() {
        when(noRedisDAO.generateMonthSequence(ErpNoRedisDAO.FINANCE_TRANSFER_NO_PREFIX))
                .thenReturn("YHZZ-1");
        when(financeTransferMapper.insert(any(ErpFinanceTransferDO.class))).thenAnswer(invocation -> {
            ((ErpFinanceTransferDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createFinanceTransferAndSubmit(new ErpFinanceTransferSaveReqVO()
                .setTransferTime(LocalDateTime.now())
                .setOutAccountId(1L)
                .setInAccountId(2L)
                .setTransferPrice(new BigDecimal("100"))
                .setFinanceUserId(9L)
                .setDeptId(3L));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(ErpFinanceTransferDO.class);
        verify(financeTransferMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinanceTransferStatusEnum.PROCESS.getStatus());
        verify(accountService).validateAccount(1L);
        verify(accountService).validateAccount(2L);
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(ErpFinanceTransferStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updateFinanceTransferDraft(new ErpFinanceTransferDraftSaveReqVO().setId(10L)),
                FINANCE_TRANSFER_DRAFT_UPDATE_FAIL, "YHZZ10");
    }

    @Test
    void updateDraft_preservesIdentityAndUsesStatusGuard() {
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus())
                .setDeptId(3L));
        when(financeTransferMapper.updateByIdAndStatus(eq(10L),
                eq(ErpFinanceTransferStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updateFinanceTransferDraft(completeDraft()
                .setId(10L).setDeptId(null).setRemark("继续编辑"));

        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(ErpFinanceTransferDO.class);
        verify(financeTransferMapper).updateByIdAndStatus(eq(10L),
                eq(ErpFinanceTransferStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("YHZZ10");
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinanceTransferStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getDeptId()).isEqualTo(3L);
    }

    @Test
    void updateDraft_preservesHiddenFieldsBeforeRequiredValidation() {
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(0).setOutAccountId(1L).setDeptId(3L));
        doAnswer(invocation -> {
            ((ErpFinanceTransferDraftSaveReqVO) invocation.getArgument(1)).setOutAccountId(1L);
            return null;
        }).when(fieldPermissionMasker).preserveHiddenFields(any(), any(), any());
        when(financeTransferMapper.updateByIdAndStatus(eq(10L), eq(0), any())).thenReturn(1);

        service.updateFinanceTransferDraft(completeDraft().setId(10L).setOutAccountId(null));

        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(ErpFinanceTransferDO.class);
        verify(financeTransferMapper).updateByIdAndStatus(eq(10L), eq(0), captor.capture());
        assertThat(captor.getValue().getOutAccountId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isZero();
    }

    @Test
    void submitDraft_requiresOutAccount() {
        when(financeTransferMapper.selectByIdForUpdate(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus())
                .setTransferTime(LocalDateTime.now()).setInAccountId(2L)
                .setTransferPrice(BigDecimal.ONE));

        assertServiceException(() -> service.submitFinanceTransfer(10L),
                FINANCE_TRANSFER_DRAFT_SUBMIT_FAIL, "转出账户不能为空");
        verify(financeTransferMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithStatusGuard() {
        when(financeTransferMapper.selectByIdForUpdate(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus())
                .setTransferTime(LocalDateTime.now()).setOutAccountId(1L).setInAccountId(2L)
                .setTransferPrice(new BigDecimal("100"))
                .setFinanceUserId(9L)
                .setDeptId(3L));
        when(financeTransferMapper.updateByIdAndStatus(eq(10L),
                eq(ErpFinanceTransferStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitFinanceTransfer(10L);

        ArgumentCaptor<ErpFinanceTransferDO> captor = ArgumentCaptor.forClass(ErpFinanceTransferDO.class);
        verify(financeTransferMapper).updateByIdAndStatus(eq(10L),
                eq(ErpFinanceTransferStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinanceTransferStatusEnum.PROCESS.getStatus());
    }

    @Test
    void approveDraft_isRejected() {
        when(financeTransferMapper.selectById(10L)).thenReturn(new ErpFinanceTransferDO()
                .setId(10L).setNo("YHZZ10").setStatus(ErpFinanceTransferStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.updateFinanceTransferStatus(10L, 20),
                FINANCE_TRANSFER_APPROVE_FAIL);
        verify(financeTransferMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

}
