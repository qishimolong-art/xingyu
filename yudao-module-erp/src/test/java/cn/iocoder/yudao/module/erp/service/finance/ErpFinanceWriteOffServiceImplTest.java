package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptWriteOffReverseReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_WRITEOFF_AMOUNT_EXCEED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceWriteOffServiceImplTest extends BaseMockitoUnitTest {

    @Mock
    private cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper receivableMiscMapper;

    @Test
    void approveReceiptTransfer_countsPriorSettlementAndNeverCreatesMiscDocument() {
        ErpFinanceReceiptDO document = new ErpFinanceReceiptDO().setId(100L).setStatus(10)
                .setCustomerId(1L).setSourceReceivableMiscId(200L).setReceiptPrice(new BigDecimal("700"));
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(receivableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO()
                        .setId(200L).setStatus(20).setCustomerId(1L).setAmount(new BigDecimal("1000")));
        when(receivableMiscMapper.selectSettlementAmountSumMapBySourceMiscIds(any(), any()))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("300")));
        when(receiptItemMapper.selectListByReceiptId(100L)).thenReturn(Collections.emptyList());
        when(receiptMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any())).thenReturn(1);
        receiptService.approveFinanceReceipt(100L);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(receivableMiscMapper, receiptMapper);
        order.verify(receivableMiscMapper).selectByIdForUpdate(200L);
        order.verify(receivableMiscMapper).selectSettlementAmountSumMapBySourceMiscIds(any(), any());
        order.verify(receiptMapper).updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(10), any());
        verify(receivableMiscMapper, never()).insert(any(cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO.class));
        verify(financeAutoWriteOffService, never()).autoWriteOffReceipt(any(), any());
    }

    @Test
    void approveReceiptTransfer_rejectsOverpaymentBeforeStatusChange() {
        ErpFinanceReceiptDO document = new ErpFinanceReceiptDO().setId(100L).setStatus(10)
                .setCustomerId(1L).setSourceReceivableMiscId(200L).setReceiptPrice(new BigDecimal("701"));
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(receivableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO()
                        .setId(200L).setStatus(20).setCustomerId(1L).setAmount(new BigDecimal("1000")));
        when(receivableMiscMapper.selectSettlementAmountSumMapBySourceMiscIds(any(), any()))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("300")));
        when(receiptItemMapper.selectListByReceiptId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> receiptService.approveFinanceReceipt(100L), cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID, "本次转款金额超过剩余可转金额，请刷新后调整");
        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approveReceiptTransfer_rejectsRepeatedApproval() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(new ErpFinanceReceiptDO().setId(100L).setStatus(20));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> receiptService.approveFinanceReceipt(100L));
        verify(receivableMiscMapper, never()).selectByIdForUpdate(any());
        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Mock
    private cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper payableMiscMapper;

    @Test
    void approvePaymentTransfer_countsPriorSettlementAndNeverCreatesMiscDocument() {
        ErpFinancePaymentDO document = new ErpFinancePaymentDO().setId(100L).setStatus(10)
                .setSupplierId(1L).setSourcePayableMiscId(200L).setPaymentPrice(new BigDecimal("700"));
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(payableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO()
                        .setId(200L).setStatus(20).setSupplierId(1L).setAmount(new BigDecimal("1000")));
        when(payableMiscMapper.selectSettlementAmountSumMapBySourceMiscIds(any(), any()))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("300")));
        when(paymentItemMapper.selectListByPaymentId(100L)).thenReturn(Collections.emptyList());
        when(paymentMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any())).thenReturn(1);
        paymentService.approveFinancePayment(100L);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(payableMiscMapper, paymentMapper);
        order.verify(payableMiscMapper).selectByIdForUpdate(200L);
        order.verify(payableMiscMapper).selectSettlementAmountSumMapBySourceMiscIds(any(), any());
        order.verify(paymentMapper).updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.eq(10), any());
        verify(payableMiscMapper, never()).insert(any(cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO.class));
        verify(financeAutoWriteOffService, never()).autoWriteOffPayment(any(), any());
    }

    @Test
    void approvePaymentTransfer_rejectsOverpaymentBeforeStatusChange() {
        ErpFinancePaymentDO document = new ErpFinancePaymentDO().setId(100L).setStatus(10)
                .setSupplierId(1L).setSourcePayableMiscId(200L).setPaymentPrice(new BigDecimal("701"));
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(payableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO()
                        .setId(200L).setStatus(20).setSupplierId(1L).setAmount(new BigDecimal("1000")));
        when(payableMiscMapper.selectSettlementAmountSumMapBySourceMiscIds(any(), any()))
                .thenReturn(Collections.singletonMap(200L, new BigDecimal("300")));
        when(paymentItemMapper.selectListByPaymentId(100L)).thenReturn(Collections.emptyList());
        assertServiceException(() -> paymentService.approveFinancePayment(100L), cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID, "本次转款金额超过剩余可转金额，请刷新后调整");
        verify(paymentMapper, never()).updateByIdAndStatus(any(), any(), any());
    }

    @Test
    void approvePaymentTransfer_rejectsRepeatedApproval() {
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(new ErpFinancePaymentDO().setId(100L).setStatus(20));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> paymentService.approveFinancePayment(100L));
        verify(payableMiscMapper, never()).selectByIdForUpdate(any());
        verify(paymentMapper, never()).updateByIdAndStatus(any(), any(), any());
    }


    @Test
    void receiptTransfer_respectsOtherPendingReservationsAndExcludesSelf() {
        ErpFinanceReceiptDO document = new ErpFinanceReceiptDO().setId(100L).setStatus(10)
                .setCustomerId(1L).setSourceReceivableMiscId(200L).setReceiptPrice(new BigDecimal("701"));
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(receivableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO()
                        .setId(200L).setStatus(20).setCustomerId(1L).setAmount(new BigDecimal("1000")));
        when(receivableMiscMapper.selectPendingTransferAmount(200L, 100L)).thenReturn(new BigDecimal("300"));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> receiptService.approveFinanceReceipt(100L));
        verify(receiptMapper, never()).updateByIdAndStatus(any(), any(), any());
        document.setReceiptPrice(new BigDecimal("700"));
        when(receiptMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any())).thenReturn(1);
        receiptService.approveFinanceReceipt(100L);
        verify(receiptMapper).updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any());
    }


    @Test
    void paymentTransfer_respectsOtherPendingReservationsAndExcludesSelf() {
        ErpFinancePaymentDO document = new ErpFinancePaymentDO().setId(100L).setStatus(10)
                .setSupplierId(1L).setSourcePayableMiscId(200L).setPaymentPrice(new BigDecimal("701"));
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(document);
        when(payableMiscMapper.selectByIdForUpdate(200L)).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO()
                        .setId(200L).setStatus(20).setSupplierId(1L).setAmount(new BigDecimal("1000")));
        when(payableMiscMapper.selectPendingTransferAmount(200L, 100L)).thenReturn(new BigDecimal("300"));
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> paymentService.approveFinancePayment(100L));
        verify(paymentMapper, never()).updateByIdAndStatus(any(), any(), any());
        document.setPaymentPrice(new BigDecimal("700"));
        when(paymentMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any())).thenReturn(1);
        paymentService.approveFinancePayment(100L);
        verify(paymentMapper).updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(10), any());
    }


    @Test
    void receiptManualWriteOff_cannotConsumeReservedTransferAmount() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(new ErpFinanceReceiptDO()
                .setId(100L).setStatus(20).setCustomerId(1L).setTotalPrice(new BigDecimal("1000")));
        when(receivableMiscMapper.selectOne(any())).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO()
                        .setId(200L).setStatus(20).setCustomerId(1L).setAmount(new BigDecimal("1000")));
        when(receivableMiscMapper.selectPendingTransferAmount(200L, null)).thenReturn(new BigDecimal("300"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.RECEIVABLE_MISC.getType())).thenReturn(BigDecimal.ZERO);
        ErpFinanceReceiptWriteOffReqVO request = new ErpFinanceReceiptWriteOffReqVO().setReceiptId(100L)
                .setItems(Collections.singletonList(new ErpFinanceReceiptWriteOffReqVO.Item()
                        .setBizType(ErpBizTypeEnum.RECEIVABLE_MISC.getType()).setBizId(200L).setWriteOffAmount(new BigDecimal("701"))));
        assertServiceException(() -> receiptService.writeOffFinanceReceipt(request),
                cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_RECEIPT_WRITEOFF_AMOUNT_INVALID,
                "核销金额超过业务单据未核销金额或符号不一致");
        verify(receiptItemMapper, never()).insertBatch(any());
    }

    @Test
    void receiptDelete_locksAndRechecksApprovalBeforeReleasingReservation() {
        ErpFinanceReceiptDO document = new ErpFinanceReceiptDO().setId(100L).setStatus(20).setSourceReceivableMiscId(200L);
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(document);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> receiptService.deleteFinanceReceipt(Collections.singletonList(100L)));
        verify(receiptMapper, never()).deleteById(100L);
        document.setStatus(10);
        receiptService.deleteFinanceReceipt(Collections.singletonList(100L));
        verify(receiptMapper).deleteById(100L);
    }

    @Test
    void paymentManualWriteOff_cannotConsumeReservedTransferAmount() {
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(new ErpFinancePaymentDO()
                .setId(100L).setStatus(20).setSupplierId(1L).setTotalPrice(new BigDecimal("1000")));
        when(payableMiscMapper.selectOne(any())).thenReturn(
                new cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO()
                        .setId(200L).setStatus(20).setSupplierId(1L).setAmount(new BigDecimal("1000")));
        when(payableMiscMapper.selectPendingTransferAmount(200L, null)).thenReturn(new BigDecimal("300"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.PAYABLE_MISC.getType())).thenReturn(BigDecimal.ZERO);
        ErpFinancePaymentWriteOffReqVO request = new ErpFinancePaymentWriteOffReqVO().setPaymentId(100L)
                .setItems(Collections.singletonList(new ErpFinancePaymentWriteOffReqVO.Item()
                        .setBizType(ErpBizTypeEnum.PAYABLE_MISC.getType()).setBizId(200L).setWriteOffAmount(new BigDecimal("701"))));
        assertServiceException(() -> paymentService.writeOffFinancePayment(request),
                cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.FINANCE_PAYMENT_WRITEOFF_AMOUNT_INVALID,
                "核销金额超过业务单据未核销金额或符号不一致");
        verify(paymentItemMapper, never()).insertBatch(any());
    }

    @Test
    void paymentDelete_locksAndRechecksApprovalBeforeReleasingReservation() {
        ErpFinancePaymentDO document = new ErpFinancePaymentDO().setId(100L).setStatus(20).setSourcePayableMiscId(200L);
        when(paymentMapper.selectByIdForUpdate(100L)).thenReturn(document);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> paymentService.deleteFinancePayment(Collections.singletonList(100L)));
        verify(paymentMapper, never()).deleteById(100L);
        document.setStatus(10);
        paymentService.deleteFinancePayment(Collections.singletonList(100L));
        verify(paymentMapper).deleteById(100L);
    }
    private static final Long LOGIN_USER_ID = 9L;

    @Mock private cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableOtherService payableOtherService;
    @Mock private cn.iocoder.yudao.module.erp.service.finance.receivable.ErpReceivableOtherService receivableOtherService;
    @InjectMocks
    private ErpFinanceReceiptServiceImpl receiptService;
    @InjectMocks
    private ErpFinancePaymentServiceImpl paymentService;

    @Mock
    private ErpFinanceReceiptMapper receiptMapper;
    @Mock
    private ErpFinanceReceiptItemMapper receiptItemMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSaleReturnService saleReturnService;
    @Mock
    private ErpFinanceAutoWriteOffService financeAutoWriteOffService;
    @Mock
    private ErpFinancePaymentMapper paymentMapper;
    @Mock
    private ErpFinancePaymentItemMapper paymentItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchaseReturnService purchaseReturnService;
    @Mock
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Mock
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void approveFinanceReceipt_activatesPendingItemsAndRefreshesSaleOut() {
        ErpFinanceReceiptDO receipt = createReceipt("100").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpFinanceReceiptItemDO item = new ErpFinanceReceiptItemDO().setId(300L).setReceiptId(100L)
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                .setReceiptPrice(new BigDecimal("40.00000000000001"))
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus());
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(receipt);
        when(receiptItemMapper.selectListByReceiptId(100L)).thenReturn(Collections.singletonList(item));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("80"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(new BigDecimal("10"), new BigDecimal("50"));
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());
        when(receiptMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(1);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.approveFinanceReceipt(100L);
        }

        assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
        assertThat(item.getWriteOffUserId()).isEqualTo(LOGIN_USER_ID);
        assertThat(item.getReceiptPrice()).isEqualByComparingTo("40.00");
        verify(receiptItemMapper).updateBatch(Collections.singletonList(item));
        verify(saleOutService).updateSaleInReceiptPrice(200L, new BigDecimal("50"));
    }

    @Test
    void writeOffFinanceReceipt_createsEffectiveItemAndRefreshesSaleOut() {
        ErpFinanceReceiptDO receipt = createReceipt("100");
        ErpSaleOutDO saleOut = createSaleOut("80");
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(receipt);
        when(saleOutMapper.selectOne(any())).thenReturn(saleOut);
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(new BigDecimal("10"), new BigDecimal("50"));
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.singletonMap(100L, new BigDecimal("20")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.writeOffFinanceReceipt(createReceiptWriteOffReq("40"));
        }

        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items -> {
            ErpFinanceReceiptItemDO item = items.iterator().next();
            return items.size() == 1
                    && item.getWriteOffStatus().equals(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    && item.getWriteOffUserId().equals(LOGIN_USER_ID)
                    && item.getReceiptedPrice().compareTo(new BigDecimal("10")) == 0
                    && item.getReceiptPrice().compareTo(new BigDecimal("40")) == 0;
        }));
        verify(saleOutService).updateSaleInReceiptPrice(200L, new BigDecimal("50"));
    }

    @Test
    void writeOffFinanceReceipt_normalizesFrontendFloatingPointTail() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("99.99"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("99.99"));
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.writeOffFinanceReceipt(createReceiptWriteOffReq("99.99000000000001"));
        }

        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.iterator().next().getReceiptPrice().equals(new BigDecimal("99.99"))));
    }

    @Test
    void writeOffFinanceReceipt_usesOriginalSaleOutAmountAfterPriceAdjustment() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("120"));
        when(saleOutService.getSaleOutItemListByOutIds(Collections.singleton(200L)))
                .thenReturn(Collections.singletonList(ErpSaleOutItemDO.builder().outId(200L)
                        .count(BigDecimal.ONE).productPrice(new BigDecimal("120"))
                        .originalProductPrice(new BigDecimal("100")).build()));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("100"));
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.writeOffFinanceReceipt(createReceiptWriteOffReq("100"));
        }

        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.iterator().next().getTotalPrice().compareTo(new BigDecimal("100")) == 0));
    }

    @Test
    void writeOffFinanceReceipt_rejectsAmountBeyondReceiptPool() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("50"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("100"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(BigDecimal.ZERO);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.singletonMap(100L, new BigDecimal("40")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            assertServiceException(() -> receiptService.writeOffFinanceReceipt(createReceiptWriteOffReq("20")),
                    FINANCE_RECEIPT_WRITEOFF_AMOUNT_EXCEED);
        }

        verify(receiptItemMapper, never()).insertBatch(any());
        verify(saleOutService, never()).updateSaleInReceiptPrice(any(), any());
    }

    @Test
    void writeOffFinanceReceipt_acceptsSaleReturnNegativeAmount() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("-20"));
        when(saleReturnMapper.selectOne(any())).thenReturn(createSaleReturn("20"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(212L,
                ErpBizTypeEnum.SALE_RETURN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("-20"));
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());

        ErpFinanceReceiptWriteOffReqVO reqVO = new ErpFinanceReceiptWriteOffReqVO()
                .setReceiptId(100L)
                .setItems(Collections.singletonList(new ErpFinanceReceiptWriteOffReqVO.Item()
                        .setBizType(ErpBizTypeEnum.SALE_RETURN.getType()).setBizId(212L)
                        .setWriteOffAmount(new BigDecimal("-20"))));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.writeOffFinanceReceipt(reqVO);
        }

        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items -> {
            ErpFinanceReceiptItemDO item = items.iterator().next();
            return items.size() == 1
                    && item.getBizType().equals(ErpBizTypeEnum.SALE_RETURN.getType())
                    && item.getTotalPrice().compareTo(new BigDecimal("-20")) == 0
                    && item.getReceiptPrice().compareTo(new BigDecimal("-20")) == 0;
        }));
        verify(saleReturnService).updateSaleReturnRefundPrice(212L, new BigDecimal("20"));
    }

    @Test
    void writeOffFinanceReceipt_acceptsSaleOutAndReturnTogether() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("80"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("100"));
        when(saleReturnMapper.selectOne(any())).thenReturn(createSaleReturn("20"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());

        ErpFinanceReceiptWriteOffReqVO reqVO = new ErpFinanceReceiptWriteOffReqVO()
                .setReceiptId(100L)
                .setItems(Arrays.asList(
                        new ErpFinanceReceiptWriteOffReqVO.Item()
                                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                                .setWriteOffAmount(new BigDecimal("100")),
                        new ErpFinanceReceiptWriteOffReqVO.Item()
                                .setBizType(ErpBizTypeEnum.SALE_RETURN.getType()).setBizId(212L)
                                .setWriteOffAmount(new BigDecimal("-20"))));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.writeOffFinanceReceipt(reqVO);
        }

        verify(receiptItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.size() == 2
                        && items.stream().map(ErpFinanceReceiptItemDO::getReceiptPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("80")) == 0
                        && items.stream().filter(item -> item.getBizId().equals(212L)).findFirst()
                        .get().getTotalPrice().compareTo(new BigDecimal("-20")) == 0));
    }

    @Test
    void writeOffFinanceReceipt_rejectsNetAmountWithDifferentReceiptSign() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("-50"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("100"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(BigDecimal.ZERO);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            assertServiceException(() -> receiptService.writeOffFinanceReceipt(createReceiptWriteOffReq("20")),
                    FINANCE_RECEIPT_WRITEOFF_AMOUNT_EXCEED);
        }

        verify(receiptItemMapper, never()).insertBatch(any());
    }

    @Test
    void reverseFinanceReceiptWriteOff_marksItemReversedAndRefreshesSaleOut() {
        ErpFinanceReceiptItemDO item = new ErpFinanceReceiptItemDO().setId(300L).setReceiptId(100L)
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
        when(receiptItemMapper.selectById(300L)).thenReturn(item);
        when(receiptItemMapper.selectByIdForUpdate(300L)).thenReturn(item);
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        when(saleOutMapper.selectOne(any())).thenReturn(createSaleOut("80"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(200L,
                ErpBizTypeEnum.SALE_OUT.getType())).thenReturn(BigDecimal.ZERO);

        ErpFinanceReceiptWriteOffReverseReqVO reqVO = new ErpFinanceReceiptWriteOffReverseReqVO()
                .setItemId(300L).setReason("录入错误");
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            receiptService.reverseFinanceReceiptWriteOff(reqVO);
        }

        assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus());
        assertThat(item.getReverseUserId()).isEqualTo(LOGIN_USER_ID);
        assertThat(item.getReverseReason()).isEqualTo("录入错误");
        verify(receiptItemMapper).updateById(item);
        verify(saleOutService).updateSaleInReceiptPrice(200L, BigDecimal.ZERO);
    }

    @Test
    void approveFinancePayment_activatesPendingItemsAndRefreshesPurchaseIn() {
        ErpFinancePaymentDO payment = createPayment("100").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpFinancePaymentItemDO item = new ErpFinancePaymentItemDO().setId(310L).setPaymentId(110L)
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                .setPaymentPrice(new BigDecimal("40.00000000000001"))
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.PENDING.getStatus());
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(payment);
        when(paymentItemMapper.selectListByPaymentId(110L)).thenReturn(Collections.singletonList(item));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("80"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(new BigDecimal("10"), new BigDecimal("50"));
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentMapper.updateByIdAndStatus(org.mockito.ArgumentMatchers.eq(110L),
                org.mockito.ArgumentMatchers.eq(ErpAuditStatus.PROCESS.getStatus()), any())).thenReturn(1);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.approveFinancePayment(110L);
        }

        assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
        assertThat(item.getWriteOffUserId()).isEqualTo(LOGIN_USER_ID);
        assertThat(item.getPaymentPrice()).isEqualByComparingTo("40.00");
        verify(paymentItemMapper).updateBatch(Collections.singletonList(item));
        verify(purchaseInService).updatePurchaseInPaymentPrice(210L, new BigDecimal("50"));
        verify(financeAutoWriteOffService).autoWriteOffPayment(110L, LOGIN_USER_ID);
    }

    @Test
    void writeOffFinancePayment_createsEffectiveItemAndRefreshesPurchaseIn() {
        ErpFinancePaymentDO payment = createPayment("100");
        ErpPurchaseInDO purchaseIn = createPurchaseIn("80");
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(payment);
        when(purchaseInMapper.selectOne(any())).thenReturn(purchaseIn);
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(new BigDecimal("10"), new BigDecimal("50"));
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.singletonMap(110L, new BigDecimal("20")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.writeOffFinancePayment(createPaymentWriteOffReq("40"));
        }

        verify(paymentItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items -> {
            ErpFinancePaymentItemDO item = items.iterator().next();
            return items.size() == 1
                    && item.getWriteOffStatus().equals(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                    && item.getWriteOffUserId().equals(LOGIN_USER_ID)
                    && item.getPaidPrice().compareTo(new BigDecimal("10")) == 0
                    && item.getPaymentPrice().compareTo(new BigDecimal("40")) == 0;
        }));
        verify(purchaseInService).updatePurchaseInPaymentPrice(210L, new BigDecimal("50"));
    }

    @Test
    void writeOffFinancePayment_normalizesFrontendFloatingPointTail() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("99.99"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("99.99"));
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.writeOffFinancePayment(createPaymentWriteOffReq("99.99000000000001"));
        }

        verify(paymentItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.iterator().next().getPaymentPrice().equals(new BigDecimal("99.99"))));
    }

    @Test
    void writeOffFinancePayment_usesOriginalPurchaseInAmountAfterPriceAdjustment() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("120"));
        when(purchaseInService.getPurchaseInItemListByInIds(Collections.singleton(210L)))
                .thenReturn(Collections.singletonList(ErpPurchaseInItemDO.builder().inId(210L)
                        .count(BigDecimal.ONE).productPrice(new BigDecimal("120"))
                        .originalProductPrice(new BigDecimal("100")).build()));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("100"));
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.writeOffFinancePayment(createPaymentWriteOffReq("100"));
        }

        verify(paymentItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.iterator().next().getTotalPrice().compareTo(new BigDecimal("100")) == 0));
    }

    @Test
    void writeOffFinancePayment_acceptsPurchaseInAdjustmentAndReturnTogether() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("85"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("120"));
        when(purchaseInService.getPurchaseInItemListByInIds(Collections.singleton(210L)))
                .thenReturn(Collections.singletonList(ErpPurchaseInItemDO.builder().inId(210L)
                        .count(BigDecimal.ONE).productPrice(new BigDecimal("120"))
                        .originalProductPrice(new BigDecimal("100")).build()));
        when(purchasePriceAdjustMapper.selectOne(any())).thenReturn(
                ErpPurchasePriceAdjustDO.builder().id(211L).no("TJ211")
                        .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                        .totalAdjustPrice(new BigDecimal("5")).build());
        when(purchaseReturnMapper.selectOne(any())).thenReturn(
                ErpPurchaseReturnDO.builder().id(212L).no("TH212")
                        .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                        .totalPrice(new BigDecimal("20")).build());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        ErpFinancePaymentWriteOffReqVO reqVO = new ErpFinancePaymentWriteOffReqVO()
                .setPaymentId(110L)
                .setItems(Arrays.asList(
                        new ErpFinancePaymentWriteOffReqVO.Item()
                                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                                .setWriteOffAmount(new BigDecimal("100")),
                        new ErpFinancePaymentWriteOffReqVO.Item()
                                .setBizType(ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType()).setBizId(211L)
                                .setWriteOffAmount(new BigDecimal("5")),
                        new ErpFinancePaymentWriteOffReqVO.Item()
                                .setBizType(ErpBizTypeEnum.PURCHASE_RETURN.getType()).setBizId(212L)
                                .setWriteOffAmount(new BigDecimal("-20"))));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.writeOffFinancePayment(reqVO);
        }

        verify(paymentItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items ->
                items.size() == 3
                        && items.stream().map(ErpFinancePaymentItemDO::getPaymentPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add).compareTo(new BigDecimal("85")) == 0
                        && items.stream().filter(item -> item.getBizId().equals(210L)).findFirst()
                        .get().getTotalPrice().compareTo(new BigDecimal("100")) == 0
                        && items.stream().filter(item -> item.getBizId().equals(212L)).findFirst()
                        .get().getTotalPrice().compareTo(new BigDecimal("-20")) == 0));
    }

    @Test
    void writeOffFinancePayment_acceptsNegativePaymentForPurchaseReturn() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("-20"));
        when(purchaseReturnMapper.selectOne(any())).thenReturn(createPurchaseReturn("20"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(212L,
                ErpBizTypeEnum.PURCHASE_RETURN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("-20"));
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        ErpFinancePaymentWriteOffReqVO reqVO = new ErpFinancePaymentWriteOffReqVO()
                .setPaymentId(110L)
                .setItems(Collections.singletonList(new ErpFinancePaymentWriteOffReqVO.Item()
                        .setBizType(ErpBizTypeEnum.PURCHASE_RETURN.getType()).setBizId(212L)
                        .setWriteOffAmount(new BigDecimal("-20"))));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.writeOffFinancePayment(reqVO);
        }

        verify(paymentItemMapper).insertBatch(org.mockito.ArgumentMatchers.argThat(items -> {
            ErpFinancePaymentItemDO item = items.iterator().next();
            return items.size() == 1
                    && item.getBizType().equals(ErpBizTypeEnum.PURCHASE_RETURN.getType())
                    && item.getTotalPrice().compareTo(new BigDecimal("-20")) == 0
                    && item.getPaymentPrice().compareTo(new BigDecimal("-20")) == 0;
        }));
        verify(purchaseReturnService).updatePurchaseReturnRefundPrice(212L, new BigDecimal("20"));
    }

    @Test
    void writeOffFinancePayment_rejectsAmountBeyondPaymentPool() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("50"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("100"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.singletonMap(110L, new BigDecimal("40")));

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            assertServiceException(() -> paymentService.writeOffFinancePayment(createPaymentWriteOffReq("20")),
                    FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED);
        }

        verify(paymentItemMapper, never()).insertBatch(any());
        verify(purchaseInService, never()).updatePurchaseInPaymentPrice(any(), any());
    }

    @Test
    void writeOffFinancePayment_rejectsNetAmountWithDifferentPaymentSign() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("-50"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("100"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            assertServiceException(() -> paymentService.writeOffFinancePayment(createPaymentWriteOffReq("20")),
                    FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED);
        }

        verify(paymentItemMapper, never()).insertBatch(any());
    }

    @Test
    void writeOffFinancePayment_rejectsNegativeNetAmountBeyondPaymentPool() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("-50"));
        when(purchaseReturnMapper.selectOne(any())).thenReturn(createPurchaseReturn("100"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(212L,
                ErpBizTypeEnum.PURCHASE_RETURN.getType())).thenReturn(BigDecimal.ZERO);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());

        ErpFinancePaymentWriteOffReqVO reqVO = new ErpFinancePaymentWriteOffReqVO()
                .setPaymentId(110L)
                .setItems(Collections.singletonList(new ErpFinancePaymentWriteOffReqVO.Item()
                        .setBizType(ErpBizTypeEnum.PURCHASE_RETURN.getType()).setBizId(212L)
                        .setWriteOffAmount(new BigDecimal("-60"))));
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            assertServiceException(() -> paymentService.writeOffFinancePayment(reqVO),
                    FINANCE_PAYMENT_WRITEOFF_AMOUNT_EXCEED);
        }

        verify(paymentItemMapper, never()).insertBatch(any());
    }

    @Test
    void reverseFinancePaymentWriteOff_marksItemReversedAndRefreshesPurchaseIn() {
        ErpFinancePaymentItemDO item = new ErpFinancePaymentItemDO().setId(310L).setPaymentId(110L)
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
        when(paymentItemMapper.selectById(310L)).thenReturn(item);
        when(paymentItemMapper.selectByIdForUpdate(310L)).thenReturn(item);
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        when(purchaseInMapper.selectOne(any())).thenReturn(createPurchaseIn("80"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(210L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO);

        ErpFinancePaymentWriteOffReverseReqVO reqVO = new ErpFinancePaymentWriteOffReverseReqVO()
                .setItemId(310L).setReason("录入错误");
        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(LOGIN_USER_ID);
            paymentService.reverseFinancePaymentWriteOff(reqVO);
        }

        assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.REVERSED.getStatus());
        assertThat(item.getReverseUserId()).isEqualTo(LOGIN_USER_ID);
        assertThat(item.getReverseReason()).isEqualTo("录入错误");
        verify(paymentItemMapper).updateById(item);
        verify(purchaseInService).updatePurchaseInPaymentPrice(210L, BigDecimal.ZERO);
    }

    @Test
    void itemMapperAmountConversion_keepsDecimalPrecision() {
        String amount = "123456789.123456789";

        assertThat(ErpFinanceReceiptItemMapper.toBigDecimal(amount)).isEqualByComparingTo(amount);
        assertThat(ErpFinancePaymentItemMapper.toBigDecimal(amount)).isEqualByComparingTo(amount);
    }

    private ErpFinanceReceiptDO createReceipt(String totalPrice) {
        return ErpFinanceReceiptDO.builder().id(100L).no("SK100")
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpSaleOutDO createSaleOut(String totalPrice) {
        return ErpSaleOutDO.builder().id(200L).no("XS200")
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpSaleReturnDO createSaleReturn(String totalPrice) {
        return ErpSaleReturnDO.builder().id(212L).no("XSTH212")
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpFinanceReceiptWriteOffReqVO createReceiptWriteOffReq(String amount) {
        ErpFinanceReceiptWriteOffReqVO.Item item = new ErpFinanceReceiptWriteOffReqVO.Item()
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                .setWriteOffAmount(new BigDecimal(amount));
        return new ErpFinanceReceiptWriteOffReqVO().setReceiptId(100L)
                .setItems(Collections.singletonList(item));
    }

    private ErpFinancePaymentDO createPayment(String totalPrice) {
        return ErpFinancePaymentDO.builder().id(110L).no("FK110")
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpPurchaseInDO createPurchaseIn(String totalPrice) {
        return ErpPurchaseInDO.builder().id(210L).no("CG210")
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpPurchaseReturnDO createPurchaseReturn(String totalPrice) {
        return ErpPurchaseReturnDO.builder().id(212L).no("CGTH212")
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpFinancePaymentWriteOffReqVO createPaymentWriteOffReq(String amount) {
        ErpFinancePaymentWriteOffReqVO.Item item = new ErpFinancePaymentWriteOffReqVO.Item()
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                .setWriteOffAmount(new BigDecimal(amount));
        return new ErpFinancePaymentWriteOffReqVO().setPaymentId(110L)
                .setItems(Collections.singletonList(item));
    }

}
