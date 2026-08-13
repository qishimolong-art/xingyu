package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceReceiptStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerDeptPermissionService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_APPROVE_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_CUSTOMER_DEPT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_DRAFT_ITEMS_REQUIRED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_DRAFT_UPDATE_FAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceReceiptDraftServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpFinanceReceiptServiceImpl receiptService;

    @Mock
    private ErpFinanceReceiptMapper receiptMapper;
    @Mock
    private ErpFinanceReceiptItemMapper receiptItemMapper;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private ErpFinancePermissionFieldFiller permissionFieldFiller;
    @Mock
    private ErpFinanceFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpCustomerDeptPermissionService customerDeptPermissionService;
    @Mock
    private ErpAccountService accountService;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void createDraft_withoutValidItems_throwException() {
        ErpFinanceReceiptSaveReqVO.Item incompleteItem = new ErpFinanceReceiptSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType());
        ErpFinanceReceiptDraftSaveReqVO request = new ErpFinanceReceiptDraftSaveReqVO()
                .setDiscountPrice(new BigDecimal("5"))
                .setItems(Collections.singletonList(incompleteItem));

        assertServiceException(() -> receiptService.createFinanceReceiptDraft(request),
                FINANCE_RECEIPT_DRAFT_ITEMS_REQUIRED);
        verify(noRedisDAO, never()).generate(any());
        verify(receiptMapper, never()).insert(any(ErpFinanceReceiptDO.class));
        verify(receiptItemMapper, never()).insertBatch(any());
    }

    @Test
    void createDraft_withCustomerDeptNotAllowed_throwException() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("SK001");
        ErpFinanceReceiptSaveReqVO.Item validItem = new ErpFinanceReceiptSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(10L)
                .setBizNo("XS001").setTotalPrice(new BigDecimal("80"))
                .setReceiptedPrice(new BigDecimal("20")).setReceiptPrice(new BigDecimal("50"));
        ErpFinanceReceiptDraftSaveReqVO request = new ErpFinanceReceiptDraftSaveReqVO()
                .setCustomerId(2L).setDeptId(99L).setDiscountPrice(new BigDecimal("5"))
                .setItems(Collections.singletonList(validItem));
        when(customerDeptPermissionService.hasAvailableDept(2L, 99L, "erp_finance_receipt")).thenReturn(false);

        assertServiceException(() -> receiptService.createFinanceReceiptDraft(request),
                FINANCE_RECEIPT_CUSTOMER_DEPT_NOT_ALLOWED);

        verify(receiptMapper, never()).insert(any(ErpFinanceReceiptDO.class));
        verify(receiptItemMapper, never()).insertBatch(any());
    }

    @Test
    void createDraft_withoutCustomerOrDept_skipsCustomerDeptValidation() {
        when(noRedisDAO.generate(ErpNoRedisDAO.FINANCE_RECEIPT_NO_PREFIX)).thenReturn("SK001");
        ErpFinanceReceiptSaveReqVO.Item validItem = new ErpFinanceReceiptSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(10L)
                .setBizNo("XS001").setTotalPrice(new BigDecimal("80"))
                .setReceiptedPrice(new BigDecimal("20")).setReceiptPrice(new BigDecimal("50"));
        ErpFinanceReceiptDraftSaveReqVO request = new ErpFinanceReceiptDraftSaveReqVO()
                .setDiscountPrice(new BigDecimal("5"))
                .setItems(Collections.singletonList(validItem));

        receiptService.createFinanceReceiptDraft(request);

        verify(customerDeptPermissionService, never()).hasAvailableDept(any(), any(), any());
        verify(receiptMapper).insert(any(ErpFinanceReceiptDO.class));
        verify(receiptItemMapper).insertBatch(any());
    }


    @Test
    void updateDraft_rejectsNonDraftStatus() {
        when(receiptMapper.selectById(1L)).thenReturn(ErpFinanceReceiptDO.builder()
                .id(1L).no("SK001").status(ErpAuditStatus.PROCESS.getStatus()).build());

        assertServiceException(
                () -> receiptService.updateFinanceReceiptDraft(
                        new ErpFinanceReceiptDraftSaveReqVO().setId(1L)),
                FINANCE_RECEIPT_DRAFT_UPDATE_FAIL,
                "SK001");

        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void updateDraft_filtersIncompleteItemsAndRecalculatesAmounts() {
        when(receiptMapper.selectById(1L)).thenReturn(ErpFinanceReceiptDO.builder()
                .id(1L).no("SK001").status(ErpFinanceReceiptStatusEnum.DRAFT.getStatus())
                .receiptTime(LocalDateTime.now()).build());
        when(receiptItemMapper.selectListByReceiptId(1L)).thenReturn(Collections.emptyList());
        when(receiptMapper.updateByIdAndStatus(eq(1L),
                eq(ErpFinanceReceiptStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);
        ErpFinanceReceiptSaveReqVO.Item validItem = new ErpFinanceReceiptSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(10L)
                .setBizNo("XS001").setTotalPrice(new BigDecimal("80"))
                .setReceiptedPrice(new BigDecimal("20")).setReceiptPrice(new BigDecimal("50"));
        ErpFinanceReceiptSaveReqVO.Item incompleteItem = new ErpFinanceReceiptSaveReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setReceiptPrice(BigDecimal.TEN);
        ErpFinanceReceiptDraftSaveReqVO request = new ErpFinanceReceiptDraftSaveReqVO()
                .setId(1L).setDiscountPrice(new BigDecimal("5"))
                .setItems(Arrays.asList(validItem, incompleteItem));

        receiptService.updateFinanceReceiptDraft(request);

        ArgumentCaptor<ErpFinanceReceiptDO> captor = ArgumentCaptor.forClass(ErpFinanceReceiptDO.class);
        verify(receiptMapper).updateByIdAndStatus(eq(1L),
                eq(ErpFinanceReceiptStatusEnum.DRAFT.getStatus()), captor.capture());
        assertThat(captor.getValue().getTotalPrice()).isEqualByComparingTo("50");
        assertThat(captor.getValue().getReceiptPrice()).isEqualByComparingTo("45");
        verify(receiptItemMapper).deleteByReceiptId(1L);
        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.size() == 1 && items.iterator().next().getBizId().equals(10L)));
    }

    @Test
    void submitDraft_strictlyValidatesAndTransitionsToProcess() {
        ErpFinanceReceiptDO receipt = ErpFinanceReceiptDO.builder()
                .id(1L).no("SK001").status(ErpFinanceReceiptStatusEnum.DRAFT.getStatus())
                .receiptTime(LocalDateTime.now()).customerId(2L).accountId(3L)
                .totalPrice(new BigDecimal("100")).discountPrice(new BigDecimal("5"))
                .receiptPrice(new BigDecimal("95")).build();
        when(receiptMapper.selectByIdForUpdate(1L)).thenReturn(receipt);
        when(receiptItemMapper.selectListByReceiptId(1L)).thenReturn(Collections.emptyList());
        when(receiptMapper.updateByIdAndStatus(eq(1L),
                eq(ErpFinanceReceiptStatusEnum.DRAFT.getStatus()), any())).thenReturn(1);

        receiptService.submitFinanceReceipt(1L);

        verify(customerService).validateCustomer(2L);
        verify(accountService).validateAccount(3L);
        verify(receiptMapper).updateByIdAndStatus(eq(1L),
                eq(ErpFinanceReceiptStatusEnum.DRAFT.getStatus()),
                org.mockito.ArgumentMatchers.argThat(update ->
                        ErpFinanceReceiptStatusEnum.PROCESS.getStatus().equals(update.getStatus())
                                && new BigDecimal("95").compareTo(update.getReceiptPrice()) == 0));
        verify(receiptItemMapper).deleteByReceiptId(1L);
    }

    @Test
    void submitDraft_rejectsMissingCustomerBeforeStatusChange() {
        when(receiptMapper.selectByIdForUpdate(1L)).thenReturn(ErpFinanceReceiptDO.builder()
                .id(1L).no("SK001").status(ErpFinanceReceiptStatusEnum.DRAFT.getStatus())
                .receiptTime(LocalDateTime.now()).accountId(3L).build());

        assertServiceException(() -> receiptService.submitFinanceReceipt(1L),
                FINANCE_RECEIPT_DRAFT_SUBMIT_FAIL, "客户不能为空");

        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approveDraft_cannotBypassProcessStatus() {
        when(receiptMapper.selectByIdForUpdate(1L)).thenReturn(ErpFinanceReceiptDO.builder()
                .id(1L).no("SK001").status(ErpFinanceReceiptStatusEnum.DRAFT.getStatus()).build());

        assertServiceException(() -> receiptService.approveFinanceReceipt(1L),
                FINANCE_RECEIPT_APPROVE_FAIL);

        verify(receiptItemMapper, never()).selectListByReceiptId(any());
        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

}
