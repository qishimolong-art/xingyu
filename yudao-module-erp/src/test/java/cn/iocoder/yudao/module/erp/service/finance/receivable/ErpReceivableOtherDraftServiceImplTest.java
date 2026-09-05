package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_PROCESS_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_SAVE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpReceivableOtherDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpReceivableOtherServiceImpl service;

    @Mock
    private ErpReceivableOtherMapper receivableOtherMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
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

    @Test
    void createReceivableOther_rejectsZeroReceivableAmount() {
        assertServiceException(
                () -> service.createReceivableOther(new ErpReceivableOtherSaveReqVO()
                        .setBizTime(LocalDate.now())
                        .setCustomerId(1L)
                        .setDeptId(2L)
                        .setReceivableAmount(BigDecimal.ZERO)),
                OTHER_RECEIVABLE_SAVE_FAIL, "应收金额不能为 0");
        verify(receivableOtherMapper, never()).insert(any(ErpReceivableOtherDO.class));
    }

    @Test
    void createReceivableOther_allowsNegativeReceivableAmount() {
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO().setId(2L));
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX))
                .thenReturn("QTYS-NEGATIVE-1");
        when(receivableOtherMapper.insert(any(ErpReceivableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherDO) invocation.getArgument(0)).setId(8L);
            return 1;
        });

        Long id = service.createReceivableOther(new ErpReceivableOtherSaveReqVO()
                .setBizTime(LocalDate.now())
                .setCustomerId(1L)
                .setDeptId(2L)
                .setReceivableAmount(new BigDecimal("-12.50")));

        assertThat(id).isEqualTo(8L);
        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).insert(captor.capture());
        verify(customerService).validateCustomer(1L);
        assertThat(captor.getValue().getReceivableAmount()).isEqualByComparingTo("-12.50");
    }

    @Test
    void createDraft_requiresCustomer() {
        assertServiceException(
                () -> service.createReceivableOtherDraft(
                        new ErpReceivableOtherDraftSaveReqVO().setRemark("未完成")),
                OTHER_RECEIVABLE_DRAFT_SAVE_FAIL, "客户不能为空");
        verify(receivableOtherMapper, never()).insert(any(ErpReceivableOtherDO.class));
    }

    @Test
    void createDraft_allowsMissingOtherRequiredFieldsAfterCustomerSelected() {
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX))
                .thenReturn("QTYS-DRAFT-1");
        when(receivableOtherMapper.insert(any(ErpReceivableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createReceivableOtherDraft(
                new ErpReceivableOtherDraftSaveReqVO().setCustomerId(1L).setRemark("未完成"));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).insert(captor.capture());
        verify(customerService).validateCustomer(1L);
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getBizTime()).isNull();
        assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
        assertThat(captor.getValue().getReceivableAmount()).isNull();
    }

    @Test
    void createFromSaleCartFreight_validatesCustomerByGeneratedSaleDept() {
        when(deptApi.getDept(102L)).thenReturn(new DeptRespDTO().setId(102L));
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX))
                .thenReturn("QTYS-FREIGHT-1");
        when(receivableOtherMapper.insert(any(ErpReceivableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherDO) invocation.getArgument(0)).setId(2L);
            return 1;
        });

        Long id = service.createFromSaleCartFreight(new ErpSaleCartFreightDraftCreateReqBO()
                .setCartId(155L)
                .setCartNo("XSST20260726000009")
                .setBizTime(LocalDate.now())
                .setCustomerId(11L)
                .setDeptId(102L)
                .setHandlerId(290L)
                .setAmount(BigDecimal.TEN));

        assertThat(id).isEqualTo(2L);
        verify(customerService).validateCustomerForGeneratedSale(11L, 102L);
        verify(customerService, never()).validateCustomer(any());
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        when(receivableOtherMapper.selectById(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updateReceivableOtherDraft(
                        new ErpReceivableOtherDraftSaveReqVO().setId(10L)),
                OTHER_RECEIVABLE_DRAFT_UPDATE_FAIL, "QTYS10");
    }

    @Test
    void updateDraft_requiresCustomer() {
        when(receivableOtherMapper.selectById(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus()));

        assertServiceException(
                () -> service.updateReceivableOtherDraft(
                        new ErpReceivableOtherDraftSaveReqVO().setId(10L).setRemark("继续编辑")),
                OTHER_RECEIVABLE_DRAFT_SAVE_FAIL, "客户不能为空");
        verify(receivableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void updateDraft_usesStatusGuardAndPreservesIdentity() {
        when(receivableOtherMapper.selectById(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus()));
        when(receivableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updateReceivableOtherDraft(new ErpReceivableOtherDraftSaveReqVO()
                .setId(10L).setCustomerId(1L).setRemark("继续编辑"));

        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        verify(customerService).validateCustomer(1L);
        assertThat(captor.getValue().getNo()).isEqualTo("QTYS10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
        assertThat(captor.getValue().getRemark()).isEqualTo("继续编辑");
    }

    @Test
    void submitDraft_requiresCustomer() {
        when(receivableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.now())
                .setReceivableAmount(BigDecimal.ONE)
                .setRemark("测试"));

        assertServiceException(() -> service.submitReceivableOther(10L),
                OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "客户不能为空");
        verify(receivableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_rejectsZeroReceivableAmount() {
        when(receivableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.now())
                .setCustomerId(1L)
                .setReceivableAmount(BigDecimal.ZERO)
                .setRemark("测试"));

        assertServiceException(() -> service.submitReceivableOther(10L),
                OTHER_RECEIVABLE_DRAFT_SUBMIT_FAIL, "应收金额不能为 0");
        verify(receivableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithStatusGuard() {
        when(receivableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.now())
                .setCustomerId(1L)
                .setReceivableAmount(new BigDecimal("-1.00"))
                .setRemark("测试"));
        when(receivableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitReceivableOther(10L);

        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherStatusEnum.PROCESS.getStatus());
        verify(customerService).validateCustomer(1L);
    }

    @Test
    void approveDraft_isRejected() {
        when(receivableOtherMapper.selectById(10L)).thenReturn(new ErpReceivableOtherDO()
                .setId(10L).setNo("QTYS10")
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.updateReceivableOtherStatus(
                10L, ErpReceivableOtherStatusEnum.APPROVE.getStatus()),
                OTHER_RECEIVABLE_PROCESS_FAIL);
        verify(receivableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

}
