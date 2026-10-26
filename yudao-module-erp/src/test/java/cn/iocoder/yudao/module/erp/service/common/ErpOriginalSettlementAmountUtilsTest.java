package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ErpOriginalSettlementAmountUtilsTest {

    @Test
    void calculatePurchaseIn_usesOriginalPriceAndExcludesFreightFromSettlement() {
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder()
                .totalPrice(new BigDecimal("200.00"))
                .discountPercent(new BigDecimal("10"))
                .feeAmount(new BigDecimal("5.00"))
                .totalFreight1(new BigDecimal("8.00"))
                .build();
        ErpPurchaseInItemDO adjustedItem = ErpPurchaseInItemDO.builder()
                .count(new BigDecimal("2"))
                .productPrice(new BigDecimal("60"))
                .originalProductPrice(new BigDecimal("50"))
                .build();
        ErpPurchaseInItemDO normalItem = ErpPurchaseInItemDO.builder()
                .count(BigDecimal.ONE)
                .productPrice(new BigDecimal("30"))
                .build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(
                purchaseIn, Arrays.asList(adjustedItem, normalItem));

        assertThat(result).isEqualByComparingTo("122.00");
    }

    @Test
    void calculateSaleOut_usesOriginalPriceAndFeeFallback() {
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(new BigDecimal("90.00"))
                .discountPercent(new BigDecimal("12.5"))
                .otherPrice(new BigDecimal("3.00"))
                .build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("40"))
                .originalProductPrice(new BigDecimal("32"))
                .build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculateSaleOut(
                saleOut, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("87.00");
    }

    @Test
    void calculateSaleOutReceivableAccountAmount_usesOriginalProductAmountWithoutFee() {
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(new BigDecimal("90.00"))
                .discountPercent(new BigDecimal("12.5"))
                .otherPrice(new BigDecimal("3.00"))
                .freight(new BigDecimal("8.00"))
                .build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("40"))
                .originalProductPrice(new BigDecimal("32"))
                .build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculateSaleOutReceivableAccountAmount(
                saleOut, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("84.00");
    }

    @Test
    void calculateSaleOutReceivableAccountAmount_withoutAdjustmentSubtractsCompatibleFee() {
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(new BigDecimal("110.00"))
                .extraFee(new BigDecimal("10.00"))
                .build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .count(BigDecimal.ONE).productPrice(new BigDecimal("100.00")).build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculateSaleOutReceivableAccountAmount(
                saleOut, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatePurchaseIn_withoutAdjustmentKeepsStoredTotal() {
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder().totalPrice(new BigDecimal("88.88")).build();
        ErpPurchaseInItemDO item = ErpPurchaseInItemDO.builder()
                .count(BigDecimal.ONE).productPrice(new BigDecimal("99.99")).build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(
                purchaseIn, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("88.88");
    }

    @Test
    void calculateSaleOut_keepsSixDecimalLineAndDiscountPrecision() {
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(BigDecimal.ZERO).discountPercent(new BigDecimal("10")).build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("1.00"))
                .originalProductPrice(new BigDecimal("0.335"))
                .build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculateSaleOut(
                saleOut, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("0.9045");
    }

    @Test
    void calculateFromAggregatedOriginalProductPrice_keepsLegacySettlementFormula() {
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder()
                .totalPrice(new BigDecimal("200.00"))
                .discountPercent(new BigDecimal("10"))
                .feeAmount(new BigDecimal("5.00"))
                .build();
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(new BigDecimal("90.00"))
                .discountPercent(new BigDecimal("12.5"))
                .otherPrice(new BigDecimal("3.00"))
                .build();

        assertThat(ErpOriginalSettlementAmountUtils.calculatePurchaseIn(
                purchaseIn, new BigDecimal("130.00"))).isEqualByComparingTo("122.00");
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOut(
                saleOut, new BigDecimal("96.00"))).isEqualByComparingTo("87.00");
        assertThat(ErpOriginalSettlementAmountUtils.calculatePurchaseIn(purchaseIn, (BigDecimal) null))
                .isEqualByComparingTo("200.00");
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOut(saleOut, (BigDecimal) null))
                .isEqualByComparingTo("90.00");
    }

    @Test
    void calculateSaleOutSettlementStatus_usesUnreceivedAmountAtFinancialPrecision() {
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutSettlementStatus(
                new BigDecimal("700.00"), BigDecimal.ZERO)).isEqualTo(0);
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutSettlementStatus(
                new BigDecimal("700.00"), new BigDecimal("300"))).isEqualTo(1);
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutSettlementStatus(
                new BigDecimal("700.00"), new BigDecimal("700"))).isEqualTo(2);
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutSettlementStatus(
                BigDecimal.ZERO, BigDecimal.ZERO)).isEqualTo(2);
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutSettlementStatus(
                new BigDecimal("700.004"), new BigDecimal("700.00"))).isEqualTo(1);
    }

    @Test
    void calculateSaleOutUnreceivedAmount_normalizesScaleAndSign() {
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutUnreceivedAmount(
                new BigDecimal("700"), new BigDecimal("300.0"))).isEqualByComparingTo("400.00");
        assertThat(ErpOriginalSettlementAmountUtils.calculateSaleOutUnreceivedAmount(
                new BigDecimal("-700.00"), new BigDecimal("-700"))).isEqualByComparingTo("0.00");
    }

}
