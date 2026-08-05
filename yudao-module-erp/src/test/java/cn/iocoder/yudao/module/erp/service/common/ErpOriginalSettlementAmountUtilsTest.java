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
    void calculatePurchaseIn_usesOriginalPriceForAdjustedItemsAndCurrentPriceForOtherItems() {
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder()
                .totalPrice(new BigDecimal("200.00"))
                .discountPercent(new BigDecimal("10"))
                .feeAmount(new BigDecimal("5.00"))
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
    void calculatePurchaseIn_withoutAdjustmentKeepsStoredTotal() {
        ErpPurchaseInDO purchaseIn = ErpPurchaseInDO.builder().totalPrice(new BigDecimal("88.88")).build();
        ErpPurchaseInItemDO item = ErpPurchaseInItemDO.builder()
                .count(BigDecimal.ONE).productPrice(new BigDecimal("99.99")).build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculatePurchaseIn(
                purchaseIn, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("88.88");
    }

    @Test
    void calculateSaleOut_roundsEachLineAndDiscountLikeDocumentCalculation() {
        ErpSaleOutDO saleOut = ErpSaleOutDO.builder()
                .totalPrice(BigDecimal.ZERO).discountPercent(new BigDecimal("10")).build();
        ErpSaleOutItemDO item = ErpSaleOutItemDO.builder()
                .count(new BigDecimal("3"))
                .productPrice(new BigDecimal("1.00"))
                .originalProductPrice(new BigDecimal("0.335"))
                .build();

        BigDecimal result = ErpOriginalSettlementAmountUtils.calculateSaleOut(
                saleOut, Collections.singletonList(item));

        assertThat(result).isEqualByComparingTo("0.91");
    }

}
