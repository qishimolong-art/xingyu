package cn.iocoder.yudao.module.erp.service.sale.returncost;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 按累计目标分摊，整批退完精确归还原总额；展示单价不参与回乘。 */
public final class ErpSaleReturnCostCalculator {
    private ErpSaleReturnCostCalculator() { }

    public static BigDecimal allocate(BigDecimal totalQuantity, BigDecimal totalAmount,
                                      BigDecimal priorQuantity, BigDecimal priorAmount, BigDecimal quantity) {
        if (totalQuantity == null || totalQuantity.signum() <= 0 || totalAmount == null || totalAmount.signum() < 0
                || priorQuantity == null || priorQuantity.signum() < 0 || priorAmount == null || priorAmount.signum() < 0
                || quantity == null || quantity.signum() <= 0) {
            throw error("原销售成本或退货数量缺失、方向无效");
        }
        BigDecimal cumulative = priorQuantity.add(quantity);
        if (cumulative.compareTo(totalQuantity) > 0 || priorAmount.compareTo(totalAmount) > 0) {
            throw error("累计退货超过原销售实际过账数量或成本");
        }
        BigDecimal target = cumulative.compareTo(totalQuantity) == 0 ? totalAmount
                : totalAmount.multiply(cumulative).divide(totalQuantity, 6, RoundingMode.HALF_UP);
        BigDecimal allocated = target.subtract(priorAmount);
        if (allocated.signum() < 0) {
            throw error("原销售成本累计分摊不一致，请先核对");
        }
        try {
            return allocated.setScale(6, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException error) {
            throw error("原销售成本超出六位金额精度");
        }
    }

    private static ServiceException error(String message) {
        return new ServiceException(409, message);
    }
}
