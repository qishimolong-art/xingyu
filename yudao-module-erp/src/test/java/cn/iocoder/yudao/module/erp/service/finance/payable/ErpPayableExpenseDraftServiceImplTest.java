package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.base.ErpBaseDataDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableExpenseMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableExpenseStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseDataService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DEPT_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.PAYABLE_EXPENSE_OPTION_INVALID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPayableExpenseDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableExpenseServiceImpl service;

    @Mock
    private ErpPayableExpenseMapper expenseMapper;
    @Mock
    private ErpPayableExpenseItemMapper expenseItemMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private ErpBaseDataService baseDataService;
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
        mockBaseOptions("payable_expense_biz_type", "代收代付支出", "资金支出", "一般费用", "管理费用");
        mockBaseOptions("payable_expense_type", "其他");
        mockBaseOptions("payable_expense_doc_type", "正常单据");
        mockBaseOptions("payable_expense_item_project", "销售减收", "差旅费用", "通信费用", "销售产生运费",
                "销售退货运费", "调拨产生运费", "员工工资", "员工社保", "返利");
    }

    @Test
    void create_requiresDeptIdAfterDefaultFill() {
        assertServiceException(
                () -> service.createPayableExpense(new ErpPayableExpenseSaveReqVO()
                        .setBizTime(LocalDate.of(2026, 8, 17))
                        .setSettleMethod("现金")
                        .setAccountId(2L)
                        .setExpenseType("其他")
                        .setHandlerId(3L)
                        .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                                .setItemName("差旅费用")
                                .setAmount(new BigDecimal("12.30"))))),
                PAYABLE_EXPENSE_DEPT_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
    }

    @Test
    void create_savesExpenseBizType() {
        when(noRedisDAO.generate("FYZF")).thenReturn("FYZF1");
        when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));
        when(expenseMapper.insert(any(ErpPayableExpenseDO.class))).thenAnswer(invocation -> {
            ((ErpPayableExpenseDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createPayableExpense(new ErpPayableExpenseSaveReqVO()
                .setBizTime(LocalDate.of(2026, 8, 17))
                .setSettleMethod("现金")
                .setAccountId(2L)
                .setExpenseBizType("一般费用")
                .setExpenseType("其他")
                .setDeptId(4L)
                .setHandlerId(3L)
                .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                        .setItemName("差旅费用")
                        .setAmount(new BigDecimal("12.30")))));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpPayableExpenseDO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).insert(captor.capture());
        assertThat(captor.getValue().getExpenseBizType()).isEqualTo("一般费用");
        assertThat(captor.getValue().getDeptId()).isEqualTo(4L);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("12.30");
    }

    @Test
    void update_savesExpenseBizType() {
        when(expenseMapper.selectById(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.PROCESS.getStatus()));
        when(expenseItemMapper.selectListByExpenseId(10L)).thenReturn(
                Collections.singletonList(new ErpPayableExpenseItemDO().setId(20L)));
        when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));
        when(expenseMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.PROCESS.getStatus()), any())).thenReturn(1);

        service.updatePayableExpense(new ErpPayableExpenseSaveReqVO()
                .setId(10L)
                .setBizTime(LocalDate.of(2026, 8, 17))
                .setSettleMethod("现金")
                .setAccountId(2L)
                .setExpenseBizType("管理费用")
                .setExpenseType("其他")
                .setDeptId(4L)
                .setHandlerId(3L)
                .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                        .setItemName("差旅费用")
                        .setAmount(new BigDecimal("20.00")))));

        ArgumentCaptor<ErpPayableExpenseDO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.PROCESS.getStatus()), captor.capture());
        assertThat(captor.getValue().getExpenseBizType()).isEqualTo("管理费用");
    }

    @Test
    void createFromSaleCartFreight_usesConfiguredExpenseOptions() {
        when(expenseMapper.selectBySource("销售手推车", 100L)).thenReturn(null);
        when(noRedisDAO.generate("FYZF")).thenReturn("FYZF-SALE-1");
        when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));
        when(expenseMapper.insert(any(ErpPayableExpenseDO.class))).thenAnswer(invocation -> {
            ((ErpPayableExpenseDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createFromSaleCartFreight(new ErpSaleCartFreightDraftCreateReqBO()
                .setCartId(100L)
                .setCartNo("XSC100")
                .setBizTime(LocalDate.of(2026, 8, 17))
                .setSettleMethod("现金")
                .setAccountId(2L)
                .setDeptId(4L)
                .setHandlerId(3L)
                .setParty("客户A")
                .setAmount(new BigDecimal("28.50")));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpPayableExpenseDO> mainCaptor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).insert(mainCaptor.capture());
        assertThat(mainCaptor.getValue().getExpenseType()).isEqualTo("其他");
        ArgumentCaptor<List<ErpPayableExpenseItemDO>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(expenseItemMapper).insertBatch(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).singleElement()
                .extracting(ErpPayableExpenseItemDO::getItemName)
                .isEqualTo("销售产生运费");
    }

    @Test
    void createDraft_withoutValidItems_throwException() {
        assertServiceException(
                () -> service.createPayableExpenseDraft(new ErpPayableExpenseDraftSaveReqVO()),
                PAYABLE_EXPENSE_DRAFT_ITEMS_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
        verify(expenseMapper, never()).insert(any(ErpPayableExpenseDO.class));
        verify(expenseItemMapper, never()).insertBatch(any());
    }

    @Test
    void createDraft_allowsMissingFormalFieldsWithValidItems() {
        when(noRedisDAO.generate("FYZF")).thenReturn("FYZF-DRAFT-1");
        when(expenseMapper.insert(any(ErpPayableExpenseDO.class))).thenAnswer(invocation -> {
            ((ErpPayableExpenseDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createPayableExpenseDraft(
                new ErpPayableExpenseDraftSaveReqVO()
                        .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                                .setItemName("差旅费用")
                                .setAmount(BigDecimal.ZERO))));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpPayableExpenseDO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableExpenseStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getBizTime()).isNull();
        assertThat(captor.getValue().getAccountId()).isNull();
        assertThat(captor.getValue().getHandlerId()).isNull();
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(expenseItemMapper).insertBatch(any());
    }


    @Test
    void updateDraft_rejectsNonDraft() {
        when(expenseMapper.selectById(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updatePayableExpenseDraft(
                        new ErpPayableExpenseDraftSaveReqVO().setId(10L)),
                PAYABLE_EXPENSE_DRAFT_UPDATE_FAIL, "FYZF10");
    }

    @Test
    void updateDraft_preservesIdentitySourceAndUsesStatusGuard() {
        when(expenseMapper.selectById(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus())
                .setSourceType("销售手推车").setSourceId(2L).setSourceNo("XSC2"));
        when(expenseItemMapper.selectListByExpenseId(10L)).thenReturn(Collections.emptyList());
        when(expenseMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);
        ErpPayableExpenseSaveReqVO.Item item = new ErpPayableExpenseSaveReqVO.Item()
                .setItemName("差旅费用").setAmount(new BigDecimal("12.50"));

        service.updatePayableExpenseDraft(new ErpPayableExpenseDraftSaveReqVO()
                .setId(10L).setItems(Collections.singletonList(item)));

        ArgumentCaptor<ErpPayableExpenseDO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("FYZF10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableExpenseStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getSourceType()).isEqualTo("销售手推车");
        assertThat(captor.getValue().getSourceId()).isEqualTo(2L);
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("12.50");
        verify(expenseItemMapper).deleteByExpenseId(10L);
        verify(expenseItemMapper).insertBatch(any());
    }

    @Test
    void submitDraft_requiresFormalMainFields() {
        when(expenseMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.submitPayableExpense(10L),
                PAYABLE_EXPENSE_DRAFT_SUBMIT_FAIL, "单据日期不能为空");
        verify(expenseMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithRecalculatedTotal() {
        when(expenseMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.of(2026, 7, 27))
                .setSettleMethod("现金").setAccountId(2L)
                .setExpenseType("其他").setDeptId(4L).setHandlerId(3L));
        when(expenseItemMapper.selectListByExpenseId(10L)).thenReturn(
                Collections.singletonList(new ErpPayableExpenseItemDO()
                        .setItemName("差旅费用").setAmount(new BigDecimal("88.60"))));
        when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));
        when(expenseMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitPayableExpense(10L);

        ArgumentCaptor<ErpPayableExpenseDO> captor =
                ArgumentCaptor.forClass(ErpPayableExpenseDO.class);
        verify(expenseMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableExpenseStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableExpenseStatusEnum.PROCESS.getStatus());
        assertThat(captor.getValue().getTotalAmount()).isEqualByComparingTo("88.60");
        verify(accountService).validateAccount(2L);
        verify(adminUserApi).validateUser(3L);
    }

    @Test
    void approveDraft_isRejected() {
        when(expenseMapper.selectById(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.updatePayableExpenseStatus(10L, 20),
                PAYABLE_EXPENSE_APPROVE_FAIL);
        verify(expenseMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void create_rejectsInvalidConfiguredOption() {
        assertServiceException(
                () -> service.createPayableExpense(new ErpPayableExpenseSaveReqVO()
                        .setBizTime(LocalDate.of(2026, 8, 17))
                        .setSettleMethod("银行转账")
                        .setAccountId(2L)
                        .setExpenseType("其他")
                        .setDeptId(4L)
                        .setHandlerId(3L)
                        .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                                .setItemName("差旅费用")
                                .setAmount(new BigDecimal("12.30"))))),
                PAYABLE_EXPENSE_OPTION_INVALID, "结算方式", "银行转账");
        verify(noRedisDAO, never()).generate(any());
    }

    @Test
    void create_rejectsInvalidItemProject() {
        assertServiceException(
                () -> service.createPayableExpense(new ErpPayableExpenseSaveReqVO()
                        .setBizTime(LocalDate.of(2026, 8, 17))
                        .setSettleMethod("现金")
                        .setAccountId(2L)
                        .setExpenseType("其他")
                        .setDeptId(4L)
                        .setHandlerId(3L)
                        .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                                .setItemName("旧项目")
                                .setAmount(new BigDecimal("12.30"))))),
                PAYABLE_EXPENSE_OPTION_INVALID, "第 1 条明细的项目名称", "旧项目");
        verify(noRedisDAO, never()).generate(any());
    }

    @Test
    void createDraft_rejectsFilledInvalidItemProject() {
        assertServiceException(
                () -> service.createPayableExpenseDraft(new ErpPayableExpenseDraftSaveReqVO()
                        .setItems(Collections.singletonList(new ErpPayableExpenseSaveReqVO.Item()
                                .setItemName("旧项目")
                                .setAmount(BigDecimal.ONE)))),
                PAYABLE_EXPENSE_OPTION_INVALID, "第 1 条明细的项目名称", "旧项目");
        verify(noRedisDAO, never()).generate(any());
    }

    @Test
    void submitDraft_rejectsInvalidItemProject() {
        when(expenseMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableExpenseDO()
                .setId(10L).setNo("FYZF10")
                .setStatus(ErpPayableExpenseStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.of(2026, 7, 27))
                .setSettleMethod("现金").setAccountId(2L)
                .setExpenseType("其他").setDeptId(4L).setHandlerId(3L));
        when(expenseItemMapper.selectListByExpenseId(10L)).thenReturn(
                Collections.singletonList(new ErpPayableExpenseItemDO()
                        .setItemName("旧项目").setAmount(new BigDecimal("88.60"))));
        when(deptApi.getDept(4L)).thenReturn(new DeptRespDTO().setId(4L));

        assertServiceException(() -> service.submitPayableExpense(10L),
                PAYABLE_EXPENSE_OPTION_INVALID, "第 1 条明细的项目名称", "旧项目");
        verify(expenseMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    private void mockBaseOptions(String type, String... names) {
        List<ErpBaseDataDO> list = java.util.Arrays.stream(names)
                .map(name -> ErpBaseDataDO.builder().type(type).name(name).build())
                .collect(Collectors.toList());
        lenient().when(baseDataService.getBaseDataSimpleListByType(type)).thenReturn(list);
    }

}
