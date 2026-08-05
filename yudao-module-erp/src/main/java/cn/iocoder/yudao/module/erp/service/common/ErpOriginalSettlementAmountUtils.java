package cn.iocoder.yudao.module.erp.service.common;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;

import java.math.BigDecimal;
import java.util.List;

/**
 * ERP 原单结算金额工具。
 *
 * <p>调价审批会更新原单明细的当前价格，因此付款、收款结算原单时需要使用首次调价前的价格快照；
 * 调价差额由调价单独立结算。</p>
 */
public final class ErpOriginalSettlementAmountUtils {

    private ErpOriginalSettlementAmountUtils() {
    }

    public static BigDecimal calculatePurchaseIn(ErpPurchaseInDO purchaseIn,
                                                  List<ErpPurchaseInItemDO> items) {
        if (!hasPurchaseOriginalPrice(items)) {
            return zeroIfNull(purchaseIn.getTotalPrice());
        }
        BigDecimal productPrice = items.stream()
                .map(item -> MoneyUtils.priceMultiply(
                        item.getOriginalProductPrice() != null ? item.getOriginalProductPrice() : item.getProductPrice(),
                        item.getCount()))
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calculateTotal(productPrice, purchaseIn.getDiscountPercent(),
                firstNonNull(purchaseIn.getFeeAmount(), purchaseIn.getOtherPrice()));
    }

    public static BigDecimal calculateSaleOut(ErpSaleOutDO saleOut, List<ErpSaleOutItemDO> items) {
        if (!hasSaleOriginalPrice(items)) {
            return zeroIfNull(saleOut.getTotalPrice());
        }
        BigDecimal productPrice = items.stream()
                .map(item -> MoneyUtils.priceMultiply(
                        item.getOriginalProductPrice() != null ? item.getOriginalProductPrice() : item.getProductPrice(),
                        item.getCount()))
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calculateTotal(productPrice, saleOut.getDiscountPercent(),
                firstNonNull(saleOut.getFeeAmount(), saleOut.getOtherPrice(), saleOut.getExtraFee()));
    }

    private static boolean hasPurchaseOriginalPrice(List<ErpPurchaseInItemDO> items) {
        return CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> item.getOriginalProductPrice() != null);
    }

    private static boolean hasSaleOriginalPrice(List<ErpSaleOutItemDO> items) {
        return CollUtil.isNotEmpty(items) && items.stream()
                .anyMatch(item -> item.getOriginalProductPrice() != null);
    }

    private static BigDecimal calculateTotal(BigDecimal productPrice, BigDecimal discountPercent,
                                             BigDecimal feeAmount) {
        BigDecimal discountPrice = MoneyUtils.priceMultiplyPercent(productPrice, zeroIfNull(discountPercent));
        return productPrice.subtract(discountPrice).add(zeroIfNull(feeAmount));
    }

    private static BigDecimal firstNonNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return value;
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
