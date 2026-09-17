package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherIncomeMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherIncomeStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_DEPT_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_INCOME_OPTION_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpReceivableOtherIncomeDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReceivableOtherIncomeServiceImpl service;

    @Mock
    private ErpReceivableOtherIncomeMapper otherIncomeMapper;
    @Mock
    private ErpReceivableOtherIncomeItemMapper otherIncomeItemMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpBaseDataService baseDataService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;

    @BeforeEach
    void setUpOptions() {
        mockBaseOptions("settle_method", "挂账", "汇款", "网上支付", "现金");
        mockBaseOptions("receivable_other_income_type", "支出", "成本", "其他");
        mockBaseOptions("receivable_other_income_doc_type", "正常单据", "代付款", "期初余额");
        mockBaseOptions("receivable_other_income_item_project", "项目", "temporary");
        lenient().when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));
    }

    @Test
    void createDraft_allowsMissingRequiredFieldsAndCalculatesTotal() {
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_INCOME_NO_PREFIX)).thenReturn("QTSR-DRAFT-1");
        when(otherIncomeMapper.insert(any(ErpReceivableOtherIncomeDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherIncomeDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createOtherIncomeDraft(new ErpReceivableOtherIncomeDraftSaveReqVO()
                .setRemark("未完成")
                .setItems(Collections.singletonList(
                        new ErpReceivableOtherIncomeDraftSaveReqVO.Item()
                                .setItemName("temporary")
                                .setAmount(BigDecimal.ZERO))));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getAccountId()).isNull();
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(otherIncomeItemMapper).insertBatch(any());
    }


    @Test
    void createDraft_withoutValidItems_throwException() {
        assertServiceException(
                () -> service.createOtherIncomeDraft(new ErpReceivableOtherIncomeDraftSaveReqVO()
                        .setItems(Collections.singletonList(new ErpReceivableOtherIncomeDraftSaveReqVO.Item()
                                .setItemName("temporary")))),
                OTHER_INCOME_DRAFT_ITEMS_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
        verify(otherIncomeMapper, never()).insert(any(ErpReceivableOtherIncomeDO.class));
        verify(otherIncomeItemMapper, never()).insertBatch(any());
    }
    @Test
    void createDraft_replacesEpochBizTime() {
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_INCOME_NO_PREFIX)).thenReturn("QTSR-DRAFT-2");
        when(otherIncomeMapper.insert(any(ErpReceivableOtherIncomeDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherIncomeDO) invocation.getArgument(0)).setId(2L);
            return 1;
        });
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        service.createOtherIncomeDraft(new ErpReceivableOtherIncomeDraftSaveReqVO()
                .setBizTime(LocalDateTime.of(1970, 1, 1, 8, 0))
                .setItems(Collections.singletonList(new ErpReceivableOtherIncomeDraftSaveReqVO.Item()
                        .setItemName("temporary")
                        .setAmount(BigDecimal.ZERO))));

        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).insert(captor.capture());
        assertThat(captor.getValue().getBizTime()).isAfter(before);
    }

    @Test
    void createAndSubmit_createsProcessDocument() {
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_INCOME_NO_PREFIX)).thenReturn("QTSR-1");
        when(otherIncomeMapper.insert(any(ErpReceivableOtherIncomeDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherIncomeDO) invocation.getArgument(0)).setId(2L);
            return 1;
        });
        ErpReceivableOtherIncomeSaveReqVO reqVO = new ErpReceivableOtherIncomeSaveReqVO()
                .setBizTime(LocalDateTime.now())
                .setSettleMethod("挂账")
                .setAccountId(2L)
                .setIncomeType("其他")
                .setDeptId(4L)
                .setHandlerId(3L)
                .setItems(Collections.singletonList(new ErpReceivableOtherIncomeSaveReqVO.Item()
                        .setItemName("项目").setAmount(new BigDecimal("8.00")).setCustomerId(88L)));

        Long id = service.createOtherIncomeAndSubmit(reqVO);

        assertThat(id).isEqualTo(2L);
        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("8.00");
        verify(customerService).validateCustomer(88L);
    }

    @Test
    void createAndSubmit_requiresDeptIdAfterDefaultFill() {
        ErpReceivableOtherIncomeSaveReqVO reqVO = new ErpReceivableOtherIncomeSaveReqVO()
                .setBizTime(LocalDateTime.now())
                .setSettleMethod("挂账")
                .setAccountId(2L)
                .setIncomeType("其他")
                .setHandlerId(3L)
                .setItems(Collections.singletonList(new ErpReceivableOtherIncomeSaveReqVO.Item()
                        .setItemName("项目").setAmount(new BigDecimal("8.00"))));

        assertServiceException(() -> service.createOtherIncomeAndSubmit(reqVO),
                OTHER_INCOME_DEPT_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
    }

    @Test
    void createAndSubmit_rejectsInvalidOptions() {
        ErpReceivableOtherIncomeSaveReqVO reqVO = new ErpReceivableOtherIncomeSaveReqVO()
                .setBizTime(LocalDateTime.now())
                .setSettleMethod("其他方式")
                .setAccountId(2L)
                .setIncomeType("其他")
                .setDeptId(4L)
                .setHandlerId(3L)
                .setItems(Collections.singletonList(new ErpReceivableOtherIncomeSaveReqVO.Item()
                        .setItemName("项目").setAmount(new BigDecimal("8.00"))));

        assertServiceException(() -> service.createOtherIncomeAndSubmit(reqVO),
                OTHER_INCOME_OPTION_INVALID, "结算方式", "其他方式");
        verify(otherIncomeMapper, never()).insert(any(ErpReceivableOtherIncomeDO.class));
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        when(otherIncomeMapper.selectById(10L)).thenReturn(new ErpReceivableOtherIncomeDO()
                .setId(10L).setNo("QTSR10")
                .setStatus(ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> service.updateOtherIncomeDraft(
                        new ErpReceivableOtherIncomeDraftSaveReqVO().setId(10L)),
                OTHER_INCOME_DRAFT_UPDATE_FAIL, "QTSR10");
    }

    @Test
    void updateDraft_usesStatusGuardAndPreservesIdentity() {
        LocalDateTime persistedBizTime = LocalDateTime.of(2026, 7, 27, 16, 52, 26);
        when(otherIncomeMapper.selectById(10L)).thenReturn(new ErpReceivableOtherIncomeDO()
                .setId(10L).setNo("QTSR10")
                .setBizTime(persistedBizTime)
                .setStatus(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()));
        when(otherIncomeMapper.updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updateOtherIncomeDraft(new ErpReceivableOtherIncomeDraftSaveReqVO()
                .setId(10L).setBizTime(LocalDateTime.of(1970, 1, 1, 8, 0))
                .setRemark("继续编辑"));

        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("QTSR10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getBizTime()).isEqualTo(persistedBizTime);
        verify(otherIncomeItemMapper, never()).deleteByIncomeId(10L);
    }

    @Test
    void submitDraft_requiresAccount() {
        when(otherIncomeMapper.selectByIdForUpdate(10L)).thenReturn(validDraft().setAccountId(null));
        when(otherIncomeItemMapper.selectListByIncomeId(10L)).thenReturn(Collections.singletonList(
                new ErpReceivableOtherIncomeItemDO().setItemName("项目").setAmount(BigDecimal.ONE)));

        assertServiceException(() -> service.submitOtherIncome(10L),
                OTHER_INCOME_DRAFT_SUBMIT_FAIL, "账户不能为空");
        verify(otherIncomeMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_requiresDeptId() {
        when(otherIncomeMapper.selectByIdForUpdate(10L)).thenReturn(validDraft().setDeptId(null));
        when(otherIncomeItemMapper.selectListByIncomeId(10L)).thenReturn(Collections.singletonList(
                new ErpReceivableOtherIncomeItemDO().setItemName("项目").setAmount(BigDecimal.ONE)));

        assertServiceException(() -> service.submitOtherIncome(10L),
                OTHER_INCOME_DRAFT_SUBMIT_FAIL, "开单部门不能为空");
        verify(otherIncomeMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithRecalculatedTotal() {
        when(otherIncomeMapper.selectByIdForUpdate(10L)).thenReturn(validDraft());
        when(otherIncomeItemMapper.selectListByIncomeId(10L)).thenReturn(Collections.singletonList(
                new ErpReceivableOtherIncomeItemDO().setItemName("项目")
                        .setAmount(new BigDecimal("12.50"))));
        when(otherIncomeMapper.updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitOtherIncome(10L);

        ArgumentCaptor<ErpReceivableOtherIncomeDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherIncomeDO.class);
        verify(otherIncomeMapper).updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("12.50");
        verify(accountService).validateAccount(2L);
        verify(adminUserApi).validateUser(3L);
    }

    @Test
    void submitDraft_rejectsLegacyItemName() {
        when(otherIncomeMapper.selectByIdForUpdate(10L)).thenReturn(validDraft());
        when(otherIncomeItemMapper.selectListByIncomeId(10L)).thenReturn(Collections.singletonList(
                new ErpReceivableOtherIncomeItemDO().setItemName("旧项目")
                        .setAmount(new BigDecimal("12.50"))));

        assertServiceException(() -> service.submitOtherIncome(10L),
                OTHER_INCOME_OPTION_INVALID, "第 1 条明细的项目名称", "旧项目");
        verify(otherIncomeMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approveDraft_isRejected() {
        when(otherIncomeMapper.selectById(10L)).thenReturn(new ErpReceivableOtherIncomeDO()
                .setId(10L).setNo("QTSR10")
                .setStatus(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.updateOtherIncomeStatus(
                        10L, ErpReceivableOtherIncomeStatusEnum.APPROVE.getStatus()),
                OTHER_RECEIVABLE_PROCESS_FAIL);
        verify(otherIncomeMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approveProcess_requiresDeptId() {
        when(otherIncomeMapper.selectById(10L)).thenReturn(new ErpReceivableOtherIncomeDO()
                .setId(10L).setNo("QTSR10")
                .setStatus(ErpReceivableOtherIncomeStatusEnum.PROCESS.getStatus()));

        assertServiceException(() -> service.updateOtherIncomeStatus(
                        10L, ErpReceivableOtherIncomeStatusEnum.APPROVE.getStatus()),
                OTHER_INCOME_DEPT_REQUIRED);
        verify(otherIncomeMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    private ErpReceivableOtherIncomeDO validDraft() {
        return new ErpReceivableOtherIncomeDO()
                .setId(10L)
                .setNo("QTSR10")
                .setStatus(ErpReceivableOtherIncomeStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDateTime.now())
                .setSettleMethod("挂账")
                .setAccountId(2L)
                .setIncomeType("其他")
                .setDeptId(4L)
                .setHandlerId(3L);
    }

    private void mockBaseOptions(String type, String... names) {
        List<ErpBaseDataDO> list = Arrays.stream(names)
                .map(name -> new ErpBaseDataDO().setType(type).setName(name).setStatus(0))
                .collect(Collectors.toList());
        lenient().when(baseDataService.getBaseDataSimpleListByType(type)).thenReturn(list);
    }
}
