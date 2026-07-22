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
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
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

    private static final Long LOGIN_USER_ID = 9L;

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
    private ErpFinancePaymentMapper paymentMapper;
    @Mock
    private ErpFinancePaymentItemMapper paymentItemMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpOperateLogService operateLogService;

    @Test
    void approveFinanceReceipt_activatesPendingItemsAndRefreshesSaleOut() {
        ErpFinanceReceiptDO receipt = createReceipt("100").setStatus(ErpAuditStatus.PROCESS.getStatus());
        ErpFinanceReceiptItemDO item = new ErpFinanceReceiptItemDO().setId(300L).setReceiptId(100L)
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                .setReceiptPrice(new BigDecimal("40"))
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
    void reverseFinanceReceiptWriteOff_marksItemReversedAndRefreshesSaleOut() {
        ErpFinanceReceiptItemDO item = new ErpFinanceReceiptItemDO().setId(300L).setReceiptId(100L)
                .setBizType(ErpBizTypeEnum.SALE_OUT.getType()).setBizId(200L)
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
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
                .setPaymentPrice(new BigDecimal("40"))
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
        verify(paymentItemMapper).updateBatch(Collections.singletonList(item));
        verify(purchaseInService).updatePurchaseInPaymentPrice(210L, new BigDecimal("50"));
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
    void reverseFinancePaymentWriteOff_marksItemReversedAndRefreshesPurchaseIn() {
        ErpFinancePaymentItemDO item = new ErpFinancePaymentItemDO().setId(310L).setPaymentId(110L)
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                .setWriteOffStatus(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
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

    private ErpFinancePaymentWriteOffReqVO createPaymentWriteOffReq(String amount) {
        ErpFinancePaymentWriteOffReqVO.Item item = new ErpFinancePaymentWriteOffReqVO.Item()
                .setBizType(ErpBizTypeEnum.PURCHASE_IN.getType()).setBizId(210L)
                .setWriteOffAmount(new BigDecimal(amount));
        return new ErpFinancePaymentWriteOffReqVO().setPaymentId(110L)
                .setItems(Collections.singletonList(item));
    }

}
