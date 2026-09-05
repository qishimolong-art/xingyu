package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
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
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_SAVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_DRAFT_UPDATE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_PAYABLE_SAVE_FAIL;
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
    void createDraft_requiresSupplier() {
        assertServiceException(() -> service.createPayableOtherDraft(new ErpPayableOtherDraftSaveReqVO()),
                OTHER_PAYABLE_DRAFT_SAVE_FAIL, "供应商不能为空");
        verify(noRedisDAO, never()).generate(any());
        verify(payableOtherMapper, never()).insert(any(ErpPayableOtherDO.class));
    }

    @Test
    void createDraft_allowsMissingOtherFormalFieldsWithSupplier() {
        when(noRedisDAO.generate("QTFK")).thenReturn("QTFK-DRAFT-1");
        when(payableOtherMapper.insert(any(ErpPayableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpPayableOtherDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createPayableOtherDraft(new ErpPayableOtherDraftSaveReqVO().setSupplierId(1L));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getBizTime()).isNull();
        assertThat(captor.getValue().getSupplierId()).isEqualTo(1L);
        assertThat(captor.getValue().getPayableAmount()).isNull();
        assertThat(captor.getValue().getRemark()).isNull();
        assertThat(captor.getValue().getVoucherNo()).isEmpty();
        assertThat(captor.getValue().getSettledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(supplierService).validateSupplier(1L);
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
    void updateDraft_requiresSupplier() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus()));

        assertServiceException(
                () -> service.updatePayableOtherDraft(
                        new ErpPayableOtherDraftSaveReqVO().setId(10L)),
                OTHER_PAYABLE_DRAFT_SAVE_FAIL, "供应商不能为空");
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void updateDraft_usesStatusGuardAndPreservesIdentity() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus()));
        when(payableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updatePayableOtherDraft(new ErpPayableOtherDraftSaveReqVO()
                .setId(10L).setSupplierId(1L).setRemark("继续编辑"));

        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("QTFK10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpPayableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getSupplierId()).isEqualTo(1L);
        assertThat(captor.getValue().getRemark()).isEqualTo("继续编辑");
        verify(supplierService).validateSupplier(1L);
    }

    @Test
    void createPayableOther_rejectsZeroPayableAmount() {
        assertServiceException(
                () -> service.createPayableOther(new ErpPayableOtherSaveReqVO()
                        .setSupplierId(1L)
                        .setDeptId(2L)
                        .setPayableAmount(BigDecimal.ZERO)),
                OTHER_PAYABLE_SAVE_FAIL, "应付金额不能为 0");
        verify(supplierService, never()).validateSupplier(any());
        verify(payableOtherMapper, never()).insert(any(ErpPayableOtherDO.class));
    }

    @Test
    void createPayableOther_allowsNegativePayableAmount() {
        when(noRedisDAO.generate("QTFK")).thenReturn("QTFK-NEG-1");
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO());
        when(payableOtherMapper.insert(any(ErpPayableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpPayableOtherDO) invocation.getArgument(0)).setId(2L);
            return 1;
        });

        Long id = service.createPayableOther(new ErpPayableOtherSaveReqVO()
                .setSupplierId(1L)
                .setDeptId(2L)
                .setPayableAmount(new BigDecimal("-1.00")));

        assertThat(id).isEqualTo(2L);
        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayableAmount()).isEqualByComparingTo("-1.00");
        verify(supplierService).validateSupplier(1L);
    }

    @Test
    void updatePayableOther_rejectsZeroPayableAmount() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updatePayableOther(new ErpPayableOtherSaveReqVO()
                        .setId(10L)
                        .setSupplierId(1L)
                        .setDeptId(2L)
                        .setPayableAmount(BigDecimal.ZERO)),
                OTHER_PAYABLE_SAVE_FAIL, "应付金额不能为 0");
        verify(supplierService, never()).validateSupplier(any());
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void updatePayableOther_allowsNegativePayableAmount() {
        when(payableOtherMapper.selectById(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.PROCESS.getStatus()));
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO());
        when(payableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.PROCESS.getStatus()), any())).thenReturn(1);

        service.updatePayableOther(new ErpPayableOtherSaveReqVO()
                .setId(10L)
                .setSupplierId(1L)
                .setDeptId(2L)
                .setPayableAmount(new BigDecimal("-1.00")));

        ArgumentCaptor<ErpPayableOtherDO> captor =
                ArgumentCaptor.forClass(ErpPayableOtherDO.class);
        verify(payableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpPayableOtherStatusEnum.PROCESS.getStatus()), captor.capture());
        assertThat(captor.getValue().getPayableAmount()).isEqualByComparingTo("-1.00");
        verify(supplierService).validateSupplier(1L);
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
    void submitDraft_rejectsZeroPayableAmount() {
        when(payableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setSupplierId(1L)
                .setDeptId(2L)
                .setPayableAmount(BigDecimal.ZERO));

        assertServiceException(() -> service.submitPayableOther(10L),
                OTHER_PAYABLE_DRAFT_SUBMIT_FAIL, "应付金额不能为 0");
        verify(payableOtherMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithoutBizTimeOrRemark() {
        when(payableOtherMapper.selectByIdForUpdate(10L)).thenReturn(new ErpPayableOtherDO()
                .setId(10L).setNo("QTFK10")
                .setStatus(ErpPayableOtherStatusEnum.DRAFT.getStatus())
                .setSupplierId(1L)
                .setDeptId(2L)
                .setPayableAmount(new BigDecimal("-1.00")));
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
