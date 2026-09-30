package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.biz.system.logger.OperateLogCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableOtherMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableOtherStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.common.ErpFormOperateLogAspect;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.OTHER_RECEIVABLE_CUSTOMER_DEPT_NOT_ALLOWED;
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
    @Mock
    private ErpCustomerDeptPermissionService customerDeptPermissionService;

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
        when(customerDeptPermissionService.hasAvailableDept(1L, 2L, "erp_receivable_other"))
                .thenReturn(true);
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
    void createReceivableOther_rejectsCustomerDeptOutsideAvailableScope() {
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO().setId(2L));
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX))
                .thenReturn("QTYS-DEPT-1");

        assertServiceException(
                () -> service.createReceivableOther(new ErpReceivableOtherSaveReqVO()
                        .setBizTime(LocalDate.now())
                        .setCustomerId(1L)
                        .setDeptId(2L)
                        .setReceivableAmount(BigDecimal.ONE)),
                OTHER_RECEIVABLE_CUSTOMER_DEPT_NOT_ALLOWED);
        verify(receivableOtherMapper, never()).insert(any(ErpReceivableOtherDO.class));
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
    void createFromSaleCartFreight_createsApprovedReceivableAdjustmentWithExpectedFields() {
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
                .setAmount(new BigDecimal("88.50")));

        assertThat(id).isEqualTo(2L);
        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).insert(captor.capture());
        verify(customerService).validateCustomerForGeneratedSale(11L, 102L);
        verify(customerService, never()).validateCustomer(any());
        ErpReceivableOtherDO created = captor.getValue();
        assertThat(created.getNo()).isEqualTo("QTYS-FREIGHT-1");
        assertThat(created.getStatus()).isEqualTo(ErpAuditStatus.APPROVE.getStatus());
        assertThat(created.getBizTime()).isEqualTo(LocalDate.now());
        assertThat(created.getCustomerId()).isEqualTo(11L);
        assertThat(created.getDeptId()).isEqualTo(102L);
        assertThat(created.getHandlerId()).isEqualTo(290L);
        assertThat(created.getReceivableAmount()).isEqualByComparingTo("88.50");
        assertThat(created.getSettledAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.getProject()).isEqualTo("代客户付运费");
        assertThat(created.getReceivableType()).isEqualTo("客户运费");
        assertThat(created.getCostAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.getSourceType()).isEqualTo("销售手推车");
        assertThat(created.getSourceId()).isEqualTo(155L);
        assertThat(created.getSourceNo()).isEqualTo("XSST20260726000009");
        assertThat(created.getRemark()).isEqualTo("销售手推车终审自动生成，来源单号：XSST20260726000009");
        verify(operateLogService).recordCreate(any(), eq(2L),
                org.mockito.ArgumentMatchers.argThat(snapshot -> ((ErpReceivableOtherDO) snapshot).getStatus() == 10),
                eq(created.getNo()));
        verify(operateLogService).recordStatus(any(), eq(2L),
                org.mockito.ArgumentMatchers.argThat(snapshot -> ((ErpReceivableOtherDO) snapshot).getStatus() == 10),
                eq(created), eq(created.getNo()), eq(true));
    }

    @Test
    void createFromSaleCartFreight_returnsExistingReceivableAdjustmentForSameSaleCartSource() {
        when(receivableOtherMapper.selectBySource("销售手推车", 155L))
                .thenReturn(new ErpReceivableOtherDO().setId(9L));

        Long id = service.createFromSaleCartFreight(new ErpSaleCartFreightDraftCreateReqBO()
                .setCartId(155L)
                .setCartNo("XSST20260726000009")
                .setBizTime(LocalDate.now())
                .setCustomerId(11L)
                .setDeptId(102L)
                .setHandlerId(290L)
                .setAmount(new BigDecimal("88.50")));

        assertThat(id).isEqualTo(9L);
        verify(receivableOtherMapper, never()).insert(any(ErpReceivableOtherDO.class));
        verify(noRedisDAO, never()).generate(any());
        verify(customerService, never()).validateCustomerForGeneratedSale(any(), any());
        org.mockito.Mockito.verifyNoInteractions(operateLogService);
    }

    @Test
    void createFromSaleCartFreight_withLogAspect_recordsCreationAndApproval() {
        OperateLogCommonApi logApi = org.mockito.Mockito.mock(OperateLogCommonApi.class);
        ErpOperateLogService logger = new ErpOperateLogService();
        ReflectionTestUtils.setField(logger, "operateLogApi", logApi);
        ReflectionTestUtils.setField(service, "operateLogService", logger);
        ErpFormOperateLogAspect aspect = new ErpFormOperateLogAspect();
        ReflectionTestUtils.setField(aspect, "operateLogService", logger);
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.addAspect(aspect);
        ErpReceivableOtherService proxy = factory.getProxy();
        when(noRedisDAO.generate(ErpNoRedisDAO.OTHER_RECEIVABLE_NO_PREFIX)).thenReturn("QTYS-FREIGHT-LOG");
        when(receivableOtherMapper.insert(any(ErpReceivableOtherDO.class))).thenAnswer(invocation -> {
            ((ErpReceivableOtherDO) invocation.getArgument(0)).setId(2L);
            return 1;
        });
        proxy.createFromSaleCartFreight(new ErpSaleCartFreightDraftCreateReqBO()
                .setCartId(100L).setCartNo("XSC100").setCustomerId(11L).setAmount(new BigDecimal("100")));
        ArgumentCaptor<OperateLogCreateReqDTO> logs = ArgumentCaptor.forClass(OperateLogCreateReqDTO.class);
        verify(logApi, org.mockito.Mockito.times(2)).createOperateLogAsync(logs.capture());
        assertThat(logs.getAllValues()).allSatisfy(log -> assertThat(log.getBizId()).isEqualTo(2L));
        assertThat(logs.getAllValues().get(0).getSubType()).isEqualTo("新增");
        assertThat(logs.getAllValues().get(1).getAction()).contains("status：10->20");
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
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setSourceType("调账")
                .setSourceNo("OLD-SOURCE"));

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
                .setStatus(ErpReceivableOtherStatusEnum.DRAFT.getStatus())
                .setSourceType("调账")
                .setSourceNo("OLD-SOURCE"));
        when(receivableOtherMapper.updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updateReceivableOtherDraft(new ErpReceivableOtherDraftSaveReqVO()
                .setId(10L).setCustomerId(1L).setSourceType("恶意修改").setRemark("继续编辑"));

        ArgumentCaptor<ErpReceivableOtherDO> captor =
                ArgumentCaptor.forClass(ErpReceivableOtherDO.class);
        verify(receivableOtherMapper).updateByIdAndStatus(eq(10L),
                eq(ErpReceivableOtherStatusEnum.DRAFT.getStatus()), captor.capture());
        verify(customerService).validateCustomer(1L);
        assertThat(captor.getValue().getNo()).isEqualTo("QTYS10");
        assertThat(captor.getValue().getStatus())
                .isEqualTo(ErpReceivableOtherStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getCustomerId()).isEqualTo(1L);
        assertThat(captor.getValue().getSourceType()).isEqualTo("调账");
        assertThat(captor.getValue().getSourceNo()).isEqualTo("OLD-SOURCE");
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
                .setDeptId(2L)
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
                .setDeptId(2L)
                .setReceivableAmount(new BigDecimal("-1.00"))
                .setRemark("测试"));
        when(deptApi.getDept(2L)).thenReturn(new DeptRespDTO().setId(2L));
        when(customerDeptPermissionService.hasAvailableDept(1L, 2L, "erp_receivable_other"))
                .thenReturn(true);
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
