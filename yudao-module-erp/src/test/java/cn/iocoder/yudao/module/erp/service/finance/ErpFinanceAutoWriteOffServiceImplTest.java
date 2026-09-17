package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.payable.ErpPayableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable.ErpReceivableMiscMapper;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchasePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSalePriceAdjustMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.config.ErpAutoWriteOffConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchasePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseReturnService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePriceAdjustService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpFinanceAutoWriteOffServiceImplTest extends BaseMockitoUnitTest {

    private static final Long LOGIN_USER_ID = 9L;

    @InjectMocks
    private ErpFinanceAutoWriteOffServiceImpl autoWriteOffService;

    @Mock
    private ErpFinanceReceiptMapper receiptMapper;
    @Mock
    private ErpFinanceReceiptItemMapper receiptItemMapper;
    @Mock
    private ErpFinancePaymentMapper paymentMapper;
    @Mock
    private ErpFinancePaymentItemMapper paymentItemMapper;
    @Mock
    private ErpSaleOutMapper saleOutMapper;
    @Mock
    private ErpSaleReturnMapper saleReturnMapper;
    @Mock
    private ErpSalePriceAdjustMapper salePriceAdjustMapper;
    @Mock
    private ErpPurchaseInMapper purchaseInMapper;
    @Mock
    private ErpPurchaseReturnMapper purchaseReturnMapper;
    @Mock
    private ErpPurchasePriceAdjustMapper purchasePriceAdjustMapper;
    @Mock
    private ErpReceivableMiscMapper receivableMiscMapper;
    @Mock
    private ErpPayableMiscMapper payableMiscMapper;
    @Mock
    private ErpAutoWriteOffConfigService autoWriteOffConfigService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpSaleReturnService saleReturnService;
    @Mock
    private ErpSalePriceAdjustService salePriceAdjustService;
    @Mock
    private ErpPurchaseInService purchaseInService;
    @Mock
    private ErpPurchaseReturnService purchaseReturnService;
    @Mock
    private ErpPurchasePriceAdjustService purchasePriceAdjustService;
    @Mock
    private ErpOperateLogService operateLogService;

    @BeforeEach
    void setUpMiscCandidates() {
        lenient().when(receivableMiscMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(payableMiscMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    void autoWriteOffReceipt_disabledDepartmentDoesNotGenerateItems() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        when(autoWriteOffConfigService.isAutoWriteOffDisabled(10L)).thenReturn(true);

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        verify(receiptItemMapper, never()).insertBatch(any());
        verify(saleOutMapper, never()).selectList(any());
        verify(saleReturnMapper, never()).selectList(any());
        verify(salePriceAdjustMapper, never()).selectList(any());
    }

    @Test
    void autoWriteOffReceipt_partiallyWritesFirstUncoveredSaleOutAndStops() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        ErpSaleOutDO first = createSaleOut(201L, "XS201", "60", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpSaleOutDO second = createSaleOut(202L, "XS202", "50", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(saleOutMapper.selectList(any())).thenReturn(Arrays.asList(second, first));
        when(saleOutMapper.selectOne(any())).thenReturn(first, second);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), eq(ErpBizTypeEnum.SALE_OUT.getType())))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(201L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("60"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(202L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("40"));

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinanceReceiptItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(receiptItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getBizId)
                .containsExactly(201L, 202L);
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getReceiptPrice)
                .containsExactly(new BigDecimal("60.00"), new BigDecimal("40.00"));
        assertThat(captor.getValue()).allSatisfy(item -> {
            assertThat(item.getRemark()).isEqualTo("系统自动核销");
            assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
            assertThat(item.getWriteOffUserId()).isEqualTo(LOGIN_USER_ID);
        });
        verify(saleOutService).updateSaleInReceiptPrice(201L, new BigDecimal("60"));
        verify(saleOutService).updateSaleInReceiptPrice(202L, new BigDecimal("40"));
    }

    @Test
    void autoWriteOffReceipt_sortsByBizTimeNoAndId() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        ErpSaleOutDO first = createSaleOut(200L, "XS001", "20", sameTime);
        ErpSaleOutDO second = createSaleOut(201L, "XS001", "20", sameTime);
        ErpSaleOutDO third = createSaleOut(202L, "XS002", "20", sameTime);
        when(saleOutMapper.selectList(any())).thenReturn(Arrays.asList(third, second, first));
        when(saleOutMapper.selectOne(any())).thenReturn(first, second, third);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), eq(ErpBizTypeEnum.SALE_OUT.getType())))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(any(), eq(ErpBizTypeEnum.SALE_OUT.getType())))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"));

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinanceReceiptItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(receiptItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getBizId)
                .containsExactly(200L, 201L, 202L);
    }

    @Test
    void autoWriteOffReceipt_respectsExistingAllocatedAmount() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("100"));
        ErpSaleOutDO first = createSaleOut(201L, "XS201", "20", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpSaleOutDO second = createSaleOut(202L, "XS202", "40", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(saleOutMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(saleOutMapper.selectOne(any())).thenReturn(first, second);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.singletonMap(100L, new BigDecimal("70")));
        when(receiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), eq(ErpBizTypeEnum.SALE_OUT.getType())))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(201L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("20"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(202L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("10"));

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinanceReceiptItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(receiptItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getBizId)
                .containsExactly(201L, 202L);
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getReceiptPrice)
                .containsExactly(new BigDecimal("20.00"), new BigDecimal("10.00"));
        verify(saleOutService).updateSaleInReceiptPrice(202L, new BigDecimal("10"));
    }

    @Test
    void autoWriteOffReceipt_allowsReturnAndAdjustWhenNetAmountRemainsValid() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("80"));
        ErpSaleOutDO saleOut = createSaleOut(201L, "XS201", "60", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpSaleReturnDO saleReturn = createSaleReturn(301L, "XSTH301", "20",
                LocalDateTime.of(2026, 1, 2, 0, 0));
        ErpSalePriceAdjustDO adjust = createSalePriceAdjust(401L, "XSTJ401", "40",
                LocalDateTime.of(2026, 1, 3, 0, 0));
        when(saleOutMapper.selectList(any())).thenReturn(Collections.singletonList(saleOut));
        when(saleReturnMapper.selectList(any())).thenReturn(Collections.singletonList(saleReturn));
        when(salePriceAdjustMapper.selectList(any())).thenReturn(Collections.singletonList(adjust));
        when(saleOutMapper.selectOne(any())).thenReturn(saleOut);
        when(saleReturnMapper.selectOne(any())).thenReturn(saleReturn);
        when(salePriceAdjustMapper.selectOne(any())).thenReturn(adjust);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), any()))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(201L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("60"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(301L, ErpBizTypeEnum.SALE_RETURN.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("-20"));
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(401L,
                ErpBizTypeEnum.SALE_PRICE_ADJUST.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("40"));

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinanceReceiptItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(receiptItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinanceReceiptItemDO::getBizId)
                .containsExactly(201L, 301L, 401L);
        assertThat(captor.getValue().stream().map(ErpFinanceReceiptItemDO::getReceiptPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("80");
        verify(saleOutService).updateSaleInReceiptPrice(201L, new BigDecimal("60"));
        verify(saleReturnService).updateSaleReturnRefundPrice(301L, new BigDecimal("20"));
        verify(salePriceAdjustService).updateSalePriceAdjustReceiptPrice(401L, new BigDecimal("40"));
    }

    @Test
    void autoWriteOffReceipt_skipsNegativeCandidateWhenItWouldReverseNetAmount() {
        when(receiptMapper.selectByIdForUpdate(100L)).thenReturn(createReceipt("80"));
        ErpSaleReturnDO saleReturn = createSaleReturn(301L, "XSTH301", "20",
                LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpSaleOutDO saleOut = createSaleOut(201L, "XS201", "60", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(saleOutMapper.selectList(any())).thenReturn(Collections.singletonList(saleOut));
        when(saleReturnMapper.selectList(any())).thenReturn(Collections.singletonList(saleReturn));
        when(saleOutMapper.selectOne(any())).thenReturn(saleOut);
        when(saleReturnMapper.selectOne(any())).thenReturn(saleReturn);
        when(receiptItemMapper.selectEffectivePriceSumMapByReceiptIds(Collections.singleton(100L)))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumMapByBizIdsAndBizType(any(), any()))
                .thenReturn(Collections.emptyMap());
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(301L, ErpBizTypeEnum.SALE_RETURN.getType()))
                .thenReturn(BigDecimal.ZERO);
        when(receiptItemMapper.selectReceiptPriceSumByBizIdAndBizType(201L, ErpBizTypeEnum.SALE_OUT.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("60"));

        autoWriteOffService.autoWriteOffReceipt(100L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinanceReceiptItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(receiptItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().iterator().next().getBizId()).isEqualTo(201L);
        verify(saleReturnService, never()).updateSaleReturnRefundPrice(any(), any());
    }

    @Test
    void autoWriteOffPayment_disabledDepartmentDoesNotGenerateItems() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        when(autoWriteOffConfigService.isAutoWriteOffDisabled(10L)).thenReturn(true);

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        verify(paymentItemMapper, never()).insertBatch(any());
        verify(purchaseInMapper, never()).selectList(any());
        verify(purchaseReturnMapper, never()).selectList(any());
        verify(purchasePriceAdjustMapper, never()).selectList(any());
    }

    @Test
    void autoWriteOffPayment_partiallyWritesFirstUncoveredPurchaseInAndStops() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        ErpPurchaseInDO first = createPurchaseIn(211L, "CG211", "60", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpPurchaseInDO second = createPurchaseIn(212L, "CG212", "50", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(purchaseInMapper.selectList(any())).thenReturn(Arrays.asList(second, first));
        when(purchaseInMapper.selectOne(any())).thenReturn(first, second);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(),
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(211L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("60"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(212L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("40"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getBizId)
                .containsExactly(211L, 212L);
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getPaymentPrice)
                .containsExactly(new BigDecimal("60.00"), new BigDecimal("40.00"));
        assertThat(captor.getValue()).allSatisfy(item -> {
            assertThat(item.getRemark()).isEqualTo("系统自动核销");
            assertThat(item.getWriteOffStatus()).isEqualTo(ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus());
            assertThat(item.getWriteOffUserId()).isEqualTo(LOGIN_USER_ID);
        });
        verify(purchaseInService).updatePurchaseInPaymentPrice(211L, new BigDecimal("60"));
        verify(purchaseInService).updatePurchaseInPaymentPrice(212L, new BigDecimal("40"));
    }

    @Test
    void autoWriteOffPayment_partiallyWritesLargeOldestPurchaseIn() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("50"));
        ErpPurchaseInDO purchaseIn = createPurchaseIn(211L, "CGRK20260902000001", "2564364.37",
                LocalDateTime.of(2026, 9, 2, 10, 0));
        when(purchaseInMapper.selectList(any())).thenReturn(Collections.singletonList(purchaseIn));
        when(purchaseInMapper.selectOne(any())).thenReturn(purchaseIn);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(),
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(211L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("50"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        ErpFinancePaymentItemDO item = captor.getValue().iterator().next();
        assertThat(item.getBizId()).isEqualTo(211L);
        assertThat(item.getTotalPrice()).isEqualByComparingTo("2564364.37");
        assertThat(item.getPaidPrice()).isEqualByComparingTo("0");
        assertThat(item.getPaymentPrice()).isEqualByComparingTo("50");
        verify(purchaseInService).updatePurchaseInPaymentPrice(211L, new BigDecimal("50"));
    }

    @Test
    void autoWriteOffPayment_sortsByBizTimeNoAndId() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        LocalDateTime sameTime = LocalDateTime.of(2026, 1, 1, 0, 0);
        ErpPurchaseInDO first = createPurchaseIn(210L, "CG001", "20", sameTime);
        ErpPurchaseInDO second = createPurchaseIn(211L, "CG001", "20", sameTime);
        ErpPurchaseInDO third = createPurchaseIn(212L, "CG002", "20", sameTime);
        when(purchaseInMapper.selectList(any())).thenReturn(Arrays.asList(third, second, first));
        when(purchaseInMapper.selectOne(any())).thenReturn(first, second, third);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(),
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(any(), eq(ErpBizTypeEnum.PURCHASE_IN.getType())))
                .thenReturn(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getBizId)
                .containsExactly(210L, 211L, 212L);
    }

    @Test
    void autoWriteOffPayment_respectsExistingAllocatedAmount() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("100"));
        ErpPurchaseInDO first = createPurchaseIn(211L, "CG211", "20", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpPurchaseInDO second = createPurchaseIn(212L, "CG212", "40", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(purchaseInMapper.selectList(any())).thenReturn(Arrays.asList(first, second));
        when(purchaseInMapper.selectOne(any())).thenReturn(first, second);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.singletonMap(110L, new BigDecimal("70")));
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(),
                eq(ErpBizTypeEnum.PURCHASE_IN.getType()))).thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(211L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("20"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(212L,
                ErpBizTypeEnum.PURCHASE_IN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("10"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getBizId)
                .containsExactly(211L, 212L);
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getPaymentPrice)
                .containsExactly(new BigDecimal("20.00"), new BigDecimal("10.00"));
        verify(purchaseInService).updatePurchaseInPaymentPrice(212L, new BigDecimal("10"));
    }

    @Test
    void autoWriteOffPayment_allowsReturnAndAdjustWhenNetAmountRemainsValid() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("80"));
        ErpPurchaseInDO purchaseIn = createPurchaseIn(211L, "CG211", "60", LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpPurchaseReturnDO purchaseReturn = createPurchaseReturn(301L, "CGTH301", "20",
                LocalDateTime.of(2026, 1, 2, 0, 0));
        ErpPurchasePriceAdjustDO adjust = createPurchasePriceAdjust(401L, "CGTJ401", "40",
                LocalDateTime.of(2026, 1, 3, 0, 0));
        when(purchaseInMapper.selectList(any())).thenReturn(Collections.singletonList(purchaseIn));
        when(purchaseReturnMapper.selectList(any())).thenReturn(Collections.singletonList(purchaseReturn));
        when(purchasePriceAdjustMapper.selectList(any())).thenReturn(Collections.singletonList(adjust));
        when(purchaseInMapper.selectOne(any())).thenReturn(purchaseIn);
        when(purchaseReturnMapper.selectOne(any())).thenReturn(purchaseReturn);
        when(purchasePriceAdjustMapper.selectOne(any())).thenReturn(adjust);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(), any()))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(211L, ErpBizTypeEnum.PURCHASE_IN.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("60"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(301L,
                ErpBizTypeEnum.PURCHASE_RETURN.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("-20"));
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(401L,
                ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType())).thenReturn(BigDecimal.ZERO, new BigDecimal("40"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).extracting(ErpFinancePaymentItemDO::getBizId)
                .containsExactly(211L, 301L, 401L);
        assertThat(captor.getValue().stream().map(ErpFinancePaymentItemDO::getPaymentPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("80");
        verify(purchaseInService).updatePurchaseInPaymentPrice(211L, new BigDecimal("60"));
        verify(purchaseReturnService).updatePurchaseReturnRefundPrice(301L, new BigDecimal("20"));
        verify(purchasePriceAdjustService).updatePurchasePriceAdjustPaymentPrice(401L, new BigDecimal("40"));
    }

    @Test
    void autoWriteOffPayment_skipsNegativeCandidateWhenItWouldReverseNetAmount() {
        when(paymentMapper.selectByIdForUpdate(110L)).thenReturn(createPayment("80"));
        ErpPurchaseReturnDO purchaseReturn = createPurchaseReturn(301L, "CGTH301", "20",
                LocalDateTime.of(2026, 1, 1, 0, 0));
        ErpPurchaseInDO purchaseIn = createPurchaseIn(211L, "CG211", "60", LocalDateTime.of(2026, 1, 2, 0, 0));
        when(purchaseInMapper.selectList(any())).thenReturn(Collections.singletonList(purchaseIn));
        when(purchaseReturnMapper.selectList(any())).thenReturn(Collections.singletonList(purchaseReturn));
        when(purchaseInMapper.selectOne(any())).thenReturn(purchaseIn);
        when(purchaseReturnMapper.selectOne(any())).thenReturn(purchaseReturn);
        when(paymentItemMapper.selectEffectivePriceSumMapByPaymentIds(Collections.singleton(110L)))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumMapByBizIdsAndBizType(any(), any()))
                .thenReturn(Collections.emptyMap());
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(301L,
                ErpBizTypeEnum.PURCHASE_RETURN.getType())).thenReturn(BigDecimal.ZERO);
        when(paymentItemMapper.selectPaymentPriceSumByBizIdAndBizType(211L, ErpBizTypeEnum.PURCHASE_IN.getType()))
                .thenReturn(BigDecimal.ZERO, new BigDecimal("60"));

        autoWriteOffService.autoWriteOffPayment(110L, LOGIN_USER_ID);

        ArgumentCaptor<Collection<ErpFinancePaymentItemDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(paymentItemMapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().iterator().next().getBizId()).isEqualTo(211L);
        verify(purchaseReturnService, never()).updatePurchaseReturnRefundPrice(any(), any());
    }

    private ErpFinanceReceiptDO createReceipt(String totalPrice) {
        return ErpFinanceReceiptDO.builder().id(100L).no("SK100")
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpSaleOutDO createSaleOut(Long id, String no, String totalPrice, LocalDateTime outTime) {
        return ErpSaleOutDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .outTime(outTime).totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpSaleReturnDO createSaleReturn(Long id, String no, String totalPrice, LocalDateTime returnTime) {
        return ErpSaleReturnDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .returnTime(returnTime).totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpSalePriceAdjustDO createSalePriceAdjust(Long id, String no, String totalPrice, LocalDateTime adjustDate) {
        return ErpSalePriceAdjustDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).customerId(1L).deptId(10L)
                .adjustDate(adjustDate).totalAdjustPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpFinancePaymentDO createPayment(String totalPrice) {
        return ErpFinancePaymentDO.builder().id(110L).no("FK110")
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpPurchaseInDO createPurchaseIn(Long id, String no, String totalPrice, LocalDateTime inTime) {
        return ErpPurchaseInDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .inTime(inTime).totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpPurchaseReturnDO createPurchaseReturn(Long id, String no, String totalPrice, LocalDateTime returnTime) {
        return ErpPurchaseReturnDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .returnTime(returnTime).totalPrice(new BigDecimal(totalPrice)).build();
    }

    private ErpPurchasePriceAdjustDO createPurchasePriceAdjust(Long id, String no, String totalPrice,
                                                              LocalDateTime adjustTime) {
        return ErpPurchasePriceAdjustDO.builder().id(id).no(no)
                .status(ErpAuditStatus.APPROVE.getStatus()).supplierId(2L).deptId(10L)
                .adjustTime(adjustTime).totalAdjustPrice(new BigDecimal(totalPrice)).build();
    }

}

