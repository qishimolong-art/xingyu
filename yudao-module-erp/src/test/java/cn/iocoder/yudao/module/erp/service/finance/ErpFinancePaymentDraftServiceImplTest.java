package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinancePaymentStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_DRAFT_UPDATE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinancePaymentDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinancePaymentServiceImpl service;

    @Mock
    private ErpFinancePaymentMapper paymentMapper;
    @Mock
    private ErpFinancePaymentItemMapper paymentItemMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpSupplierService supplierService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void createDraft_withoutValidItems_throwException() {
        assertServiceException(
                () -> service.createFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO()),
                FINANCE_PAYMENT_DRAFT_ITEMS_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
        verify(paymentMapper, never()).insert(any(ErpFinancePaymentDO.class));
        verify(paymentItemMapper, never()).insertBatch(any());
    }

    @Test
    void createDraft_allowsMissingSupplierAndAccountWithValidItems() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_PAYMENT_NO_PREFIX)).thenReturn("FKD-DRAFT-1");
        when(paymentMapper.insert(any(ErpFinancePaymentDO.class))).thenAnswer(invocation -> {
            ((ErpFinancePaymentDO) invocation.getArgument(0)).setId(1L);
            return 1;
        });

        Long id = service.createFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO()
                .setItems(Collections.singletonList(new ErpFinancePaymentSaveReqVO.Item()
                        .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType())
                        .setBizId(21L)
                        .setPaymentPrice(BigDecimal.ZERO))));

        assertThat(id).isEqualTo(1L);
        ArgumentCaptor<ErpFinancePaymentDO> captor = ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinancePaymentStatusEnum.DRAFT.getStatus());
        assertThat(captor.getValue().getSupplierId()).isNull();
        assertThat(captor.getValue().getAccountId()).isNull();
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(paymentItemMapper).insertBatch(any());
    }


    @Test
    void createDraft_persistsSelectedItemsAndCalculatesTotalFromThem() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_PAYMENT_NO_PREFIX)).thenReturn("FKD-DRAFT-3");
        when(paymentMapper.insert(any(ErpFinancePaymentDO.class))).thenAnswer(invocation -> {
            ((ErpFinancePaymentDO) invocation.getArgument(0)).setId(3L);
            return 1;
        });
        ErpFinancePaymentSaveReqVO.Item item = new ErpFinancePaymentSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType())
                .setBizId(21L)
                .setBizNo("RKD21")
                .setTotalPrice(new BigDecimal("200"))
                .setPaidPrice(new BigDecimal("20"))
                .setPaymentPrice(new BigDecimal("80"))
                .setRemark("本次付款");

        service.createFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO()
                .setTotalPrice(new BigDecimal("999"))
                .setItems(Collections.singletonList(item)));

        ArgumentCaptor<ErpFinancePaymentDO> paymentCaptor =
                ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).insert(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getTotalPrice()).isEqualByComparingTo("80.00");
        assertThat(paymentCaptor.getValue().getPaymentPrice()).isEqualByComparingTo("80.00");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpFinancePaymentItemDO>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(paymentItemMapper).insertBatch(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).singleElement().satisfies(savedItem -> {
            assertThat(savedItem.getPaymentId()).isEqualTo(3L);
            assertThat(savedItem.getBizType()).isEqualTo(ErpBizTypeEnum.PURCHASE_IN.getType());
            assertThat(savedItem.getBizId()).isEqualTo(21L);
            assertThat(savedItem.getPaymentPrice()).isEqualByComparingTo("80.00");
            assertThat(savedItem.getRemark()).isEqualTo("本次付款");
        });
    }

    @Test
    void updateDraft_rejectsNonDraft() {
        when(paymentMapper.selectById(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.PROCESS.getStatus()));

        assertServiceException(
                () -> service.updateFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO().setId(10L)),
                FINANCE_PAYMENT_DRAFT_UPDATE_FAIL, "FKD10");
    }

    @Test
    void updateDraft_usesStatusGuard() {
        when(paymentMapper.selectById(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus())
                .setPaymentTime(LocalDateTime.now()));
        when(paymentItemMapper.selectListByPaymentId(10L)).thenReturn(Collections.emptyList());
        when(paymentMapper.updateByIdAndStatus(eq(10L),
                eq(ErpFinancePaymentStatusEnum.DRAFT.getStatus()), any())).thenReturn(0);

        assertServiceException(
                () -> service.updateFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO().setId(10L)),
                FINANCE_PAYMENT_DRAFT_UPDATE_FAIL, "FKD10");
        verify(paymentItemMapper, never()).deleteByPaymentId(10L);
    }

    @Test
    void updateDraft_preservesManualTotalWhenThereAreNoItems() {
        LocalDateTime paymentTime = LocalDateTime.now();
        when(paymentMapper.selectById(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus())
                .setPaymentTime(paymentTime));
        when(paymentItemMapper.selectListByPaymentId(10L)).thenReturn(Collections.emptyList());
        when(paymentMapper.updateByIdAndStatus(eq(10L),
                eq(ErpFinancePaymentStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.updateFinancePaymentDraft(new ErpFinancePaymentDraftSaveReqVO()
                .setId(10L)
                .setTotalPrice(new BigDecimal("100"))
                .setDiscountPrice(BigDecimal.ZERO));

        ArgumentCaptor<ErpFinancePaymentDO> captor = ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).updateByIdAndStatus(eq(10L),
                eq(ErpFinancePaymentStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getNo()).isEqualTo("FKD10");
        assertThat(captor.getValue().getPaymentTime()).isEqualTo(paymentTime);
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getPaymentPrice()).isEqualByComparingTo("100.00");
        verify(paymentItemMapper).deleteByPaymentId(10L);
    }

    @Test
    void submitDraft_requiresSupplier() {
        when(paymentMapper.selectByIdForUpdate(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus())
                .setPaymentTime(LocalDateTime.now()).setAccountId(2L)
                .setTotalPrice(BigDecimal.ONE).setDiscountPrice(BigDecimal.ZERO));

        assertServiceException(() -> service.submitFinancePayment(10L),
                FINANCE_PAYMENT_DRAFT_SUBMIT_FAIL, "供应商不能为空");
        verify(paymentMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void submitDraft_movesToProcessWithStatusGuard() {
        when(paymentMapper.selectByIdForUpdate(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus())
                .setPaymentTime(LocalDateTime.now()).setSupplierId(1L).setAccountId(2L)
                .setTotalPrice(new BigDecimal("100")).setDiscountPrice(BigDecimal.ZERO));
        when(paymentItemMapper.selectListByPaymentId(10L)).thenReturn(Collections.emptyList());
        when(paymentMapper.updateByIdAndStatus(eq(10L),
                eq(ErpFinancePaymentStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        service.submitFinancePayment(10L);

        ArgumentCaptor<ErpFinancePaymentDO> captor = ArgumentCaptor.forClass(ErpFinancePaymentDO.class);
        verify(paymentMapper).updateByIdAndStatus(eq(10L),
                eq(ErpFinancePaymentStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ErpFinancePaymentStatusEnum.PROCESS.getStatus());
        verify(paymentItemMapper).deleteByPaymentId(10L);
    }

    @Test
    void approveDraft_isRejected() {
        when(paymentMapper.selectByIdForUpdate(10L)).thenReturn(new ErpFinancePaymentDO()
                .setId(10L).setNo("FKD10").setStatus(ErpFinancePaymentStatusEnum.DRAFT.getStatus()));

        assertServiceException(() -> service.approveFinancePayment(10L), FINANCE_PAYMENT_APPROVE_FAIL);
        verify(paymentMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

}
