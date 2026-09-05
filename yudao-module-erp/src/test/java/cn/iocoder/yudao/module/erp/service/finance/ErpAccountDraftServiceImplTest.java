package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpAccountMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountDocumentStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpAccountingSubjectService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_FORMAL_UPDATE_FAIL_DRAFT;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ACCOUNT_NOT_SUBMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpAccountDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpAccountServiceImpl accountService;

    @Mock
    private ErpAccountMapper accountMapper;
    @Mock
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpAccountingSubjectService accountingSubjectService;

    @Test
    void createDraft_allowsMissingNameAndUsesSafeDefaults() {
        doAnswer(invocation -> {
            invocation.<ErpAccountDO>getArgument(0).setId(1L);
            return 1;
        }).when(accountMapper).insert(any(ErpAccountDO.class));

        Long id = accountService.createAccountDraft(new ErpAccountDraftSaveReqVO());

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpAccountDO> captor = ArgumentCaptor.forClass(ErpAccountDO.class);
        verify(accountMapper).insert(captor.capture());
        ErpAccountDO account = captor.getValue();
        assertThat(account.getName()).isNull();
        assertThat(account.getAccountType()).isEqualTo(1);
        assertThat(account.getStatus()).isEqualTo(CommonStatusEnum.ENABLE.getStatus());
        assertThat(account.getSort()).isZero();
        assertThat(account.getDefaultStatus()).isFalse();
        assertThat(account.getDocumentStatus())
                .isEqualTo(ErpAccountDocumentStatusEnum.DRAFT.getStatus());
        verify(accountingSubjectService, never()).ensureFundAccountSubject(any(), any());
    }

    @Test
    void createAndSubmit_keepsOrdinaryFormalCreateBehavior() {
        doAnswer(invocation -> {
            invocation.<ErpAccountDO>getArgument(0).setId(2L);
            return 1;
        }).when(accountMapper).insert(any(ErpAccountDO.class));
        ErpAccountSaveReqVO request = new ErpAccountSaveReqVO()
                .setName(" 基本户 ").setAccountType(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSort(0)
                .setDefaultStatus(false);

        Long id = accountService.createAndSubmitAccount(request);

        assertThat(id).isEqualTo(2L);
        ArgumentCaptor<ErpAccountDO> captor = ArgumentCaptor.forClass(ErpAccountDO.class);
        verify(accountMapper).insert(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("基本户");
        assertThat(captor.getValue().getDocumentStatus())
                .isEqualTo(ErpAccountDocumentStatusEnum.SUBMITTED.getStatus());
        verify(accountingSubjectService).ensureFundAccountSubject(eq(1), eq("基本户"));
    }

    @Test
    void updateDraft_rejectsSubmittedAccount() {
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).name("正式账户")
                .documentStatus(ErpAccountDocumentStatusEnum.SUBMITTED.getStatus())
                .build());

        assertServiceException(
                () -> accountService.updateAccountDraft(
                        new ErpAccountDraftSaveReqVO().setId(1L)),
                ACCOUNT_DRAFT_UPDATE_FAIL, "当前账户不是草稿");

        verify(accountMapper, never()).updateDraftByIdAndDocumentStatus(any(), any(), any());
    }

    @Test
    void updateDraft_preservesNotNullFieldsAndUsesOptimisticStatusGuard() {
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).name("原账户").accountType(2)
                .status(CommonStatusEnum.DISABLE.getStatus()).sort(8)
                .defaultStatus(false)
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build());
        when(accountMapper.updateDraftByIdAndDocumentStatus(eq(1L),
                eq(ErpAccountDocumentStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        accountService.updateAccountDraft(new ErpAccountDraftSaveReqVO()
                .setId(1L).setName(null).setRemark(" 草稿备注 "));

        ArgumentCaptor<ErpAccountDO> captor = ArgumentCaptor.forClass(ErpAccountDO.class);
        verify(accountMapper).updateDraftByIdAndDocumentStatus(eq(1L),
                eq(ErpAccountDocumentStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getName()).isNull();
        assertThat(captor.getValue().getAccountType()).isEqualTo(2);
        assertThat(captor.getValue().getStatus()).isEqualTo(CommonStatusEnum.DISABLE.getStatus());
        assertThat(captor.getValue().getSort()).isEqualTo(8);
        assertThat(captor.getValue().getRemark()).isEqualTo("草稿备注");
        verify(accountingSubjectService, never()).ensureFundAccountSubject(any(), any());
    }

    @Test
    void submitDraft_rejectsMissingNameBeforeStatusChange() {
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).accountType(1).status(CommonStatusEnum.ENABLE.getStatus())
                .sort(0).defaultStatus(false)
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build());

        assertServiceException(() -> accountService.submitAccountDraft(1L),
                ACCOUNT_DRAFT_SUBMIT_FAIL, "账户名称不能为空");

        verify(accountMapper, never()).updateByIdAndDocumentStatus(any(), any(), any());
    }

    @Test
    void submitDraft_validatesPersistedDataAndTransitionsToSubmitted() {
        ErpAccountDO draft = ErpAccountDO.builder()
                .id(1L).name("基本户").accountType(1)
                .status(CommonStatusEnum.ENABLE.getStatus()).sort(0)
                .defaultStatus(false)
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build();
        when(accountMapper.selectById(1L)).thenReturn(draft);
        when(accountMapper.updateByIdAndDocumentStatus(eq(1L),
                eq(ErpAccountDocumentStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        accountService.submitAccountDraft(1L);

        verify(accountMapper).updateByIdAndDocumentStatus(eq(1L),
                eq(ErpAccountDocumentStatusEnum.DRAFT.getStatus()),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpAccountDocumentStatusEnum.SUBMITTED.getStatus()
                                .equals(update.getDocumentStatus())));
        verify(accountingSubjectService).ensureFundAccountSubject(eq(1), eq("基本户"));
    }

    @Test
    void submitDraft_whenSubjectEnsureFails_doesNotTransitionStatus() {
        ErpAccountDO draft = ErpAccountDO.builder()
                .id(1L).name("基本户").accountType(1)
                .status(CommonStatusEnum.ENABLE.getStatus()).sort(0)
                .defaultStatus(false)
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build();
        when(accountMapper.selectById(1L)).thenReturn(draft);
        doThrow(new IllegalStateException("科目补建失败")).when(accountingSubjectService)
                .ensureFundAccountSubject(eq(1), eq("基本户"));

        assertThatThrownBy(() -> accountService.submitAccountDraft(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("科目补建失败");

        verify(accountMapper, never()).updateByIdAndDocumentStatus(any(), any(), any());
    }

    @Test
    void formalUpdate_cannotBypassDraftLifecycle() {
        ErpAccountSaveReqVO request = new ErpAccountSaveReqVO()
                .setId(1L).setName("基本户").setAccountType(1)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSort(0);
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).name("草稿账户")
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build());

        assertServiceException(() -> accountService.updateAccount(request),
                ACCOUNT_FORMAL_UPDATE_FAIL_DRAFT);

        verify(accountMapper, never()).updateById(any(ErpAccountDO.class));
    }

    @Test
    void formalUpdate_keepsSubmittedAccountOnOrdinaryUpdatePath() {
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).name("原账户").accountType(1)
                .status(CommonStatusEnum.ENABLE.getStatus()).sort(0)
                .defaultStatus(false)
                .documentStatus(ErpAccountDocumentStatusEnum.SUBMITTED.getStatus())
                .build());
        ErpAccountSaveReqVO request = new ErpAccountSaveReqVO()
                .setId(1L).setName(" 更新账户 ").setAccountType(2)
                .setStatus(CommonStatusEnum.DISABLE.getStatus()).setSort(3)
                .setDefaultStatus(false);

        accountService.updateAccount(request);

        ArgumentCaptor<ErpAccountDO> captor = ArgumentCaptor.forClass(ErpAccountDO.class);
        verify(accountMapper).updateById(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("更新账户");
        assertThat(captor.getValue().getAccountType()).isEqualTo(2);
        assertThat(captor.getValue().getDocumentStatus()).isNull();
        verify(accountingSubjectService).ensureFundAccountSubject(eq(2), eq("更新账户"));
    }

    @Test
    void validateAccount_rejectsDraftEvenWhenEnableStatusIsSet() {
        when(accountMapper.selectById(1L)).thenReturn(ErpAccountDO.builder()
                .id(1L).name("草稿账户")
                .status(CommonStatusEnum.ENABLE.getStatus())
                .documentStatus(ErpAccountDocumentStatusEnum.DRAFT.getStatus())
                .build());

        assertServiceException(() -> accountService.validateAccount(1L),
                ACCOUNT_NOT_SUBMITTED, "草稿账户");
    }

}
