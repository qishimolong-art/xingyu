package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpPayableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_UPDATE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPayableOtherDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPayableOtherServiceImpl service;

    @Mock
    private ErpPayableOtherMapper payableOtherMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void createDraft_allowsMissingRequiredFields() {
        when(noRedisDAO.generate("QTFK")).thenReturn("QTFK-DRAFT-1");
        when(payableOtherMapper.insert(any(ErpPayableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpPayableOtherDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createPayableOtherDraft(new ErpPayableOtherDraftSaveReqVO());

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getBizTime()).isNull();
        assertThat(captor.getValue().getSupplierId()).isNull();
        assertThat(captor.getValue().getPayableAmount()).isNull();
        assertThat(captor.getValue().getRemark()).isNull();
        assertThat(captor.getValue().getVoucherNo()).isEmpty();
        assertThat(captor.getValue().getSettledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(supplierService, never()).validateSupplier(any());
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updatePayableOtherDraft(
                        new ErpPayableOtherDraftSaveReqVO().setId(10L)),
                OTHER_PAYABLE_DRAFT_UPDATE_FAIL, "QTFK10");
    }

    @Test
    void updateDraft_usesStatusGuardAndPreservesIdentity() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus()));
        when(payableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updatePayableOtherDraft(new ErpPayableOtherDraftSaveReqVO()
                .setId(10L).setRemark("继续编辑"));

        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("QTFK10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getRemark()).isEqualTo("继续编辑");
    }

    @Test
    void submitDraft_requiresSupplier() {
        when(payableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setBizTime(LocalDate.now())
                .setDeptId(2L)
                .setPayableAmount(BigDecimal.ONE)
                .setRemark("测试"));

        assertServiceException(() -> service.submitPayableOther(10L),
                OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "供应商不能为空");
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_requiresDept() {
        when(payableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setSupplierId(1L)
                .setPayableAmount(BigDecimal.ONE));

        assertServiceException(() -> service.submitPayableOther(10L),
                OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "部门不能为空");
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithoutBizTimeOrRemark() {
        when(payableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setSupplierId(1L)
                .setDeptId(2L)
                .setPayableAmount(BigDecimal.ONE));
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO());
        when(payableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitPayableOther(10L);

        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableOtherStatusEnum.PROCESS.getStatus());
        verify(supplierService).validateSupplier(1L);
    }

    @Test
    void approveDraft_isRejected() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.updatePayableOtherStatus(
                10L, ErpPayableOtherStatusEnum.APPROVE.getStatus()),
                OTHER_PAYABLE_APPROVE_FAIL);
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

}
