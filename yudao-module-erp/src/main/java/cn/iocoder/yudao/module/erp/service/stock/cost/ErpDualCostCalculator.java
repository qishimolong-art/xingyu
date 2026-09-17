package cn.iocoder.yudao.module.erp.service.stock.cost;

import lombok.Value;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** 双成本共用的金额守恒算法。金额保留 6 位，清仓时释放全部尾差。 */
public final class ErpDualCostCalculator {
    private ErpDualCostCalculator() { }

    public static Change calculateWithAmount(BigDecimal quantity, BigDecimal amount, BigDecimal delta,
                                             BigDecimal inboundAmount) {
        if(delta==null || delta.signum()<=0 || inboundAmount==null || inboundAmount.signum()<0)
            throw new IllegalArgumentException("金额优先入库需要正数量及明确非负总成本");
        // 复用余额条件校验，但不使用展示单价反算确认总额。
        calculate(quantity,amount,delta,BigDecimal.ZERO);
        requireStoragePrecision(inboundAmount);
        requireStoragePrecision(amount.add(inboundAmount));
        return new Change(quantity.add(delta),amount.add(inboundAmount),inboundAmount);
    }

    public static Change calculate(BigDecimal quantity, BigDecimal amount, BigDecimal delta,
                                   BigDecimal inboundPrice) {
        if (quantity == null || amount == null || delta == null) {
            throw new IllegalArgumentException("缺少已核对的库存数量或成本，不能按零成本过账");
        }
        requireStoragePrecision(quantity);
        requireStoragePrecision(amount);
        requireStoragePrecision(delta);
        if (quantity.signum() < 0 || amount.signum() < 0 || quantity.add(delta).signum() < 0
                || (quantity.signum() == 0 && amount.signum() != 0)) {
            throw new IllegalArgumentException("库存数量或成本余额不符合核算条件");
        }
        BigDecimal movement;
        if (delta.signum() > 0) {
            if (inboundPrice == null || inboundPrice.signum() < 0) {
                throw new IllegalArgumentException("入库成本必须明确确认，真实零成本允许过账");
            }
            movement = delta.multiply(inboundPrice).setScale(6, RoundingMode.HALF_UP);
        } else if (delta.signum() < 0) {
            movement = quantity.add(delta).signum() == 0 ? amount.negate()
                    : amount.multiply(delta).divide(quantity, 6, RoundingMode.HALF_UP);
        } else {
            throw new IllegalArgumentException("零数量成本调整必须使用独立调整事件");
        }
        requireStoragePrecision(quantity.add(delta));
        requireStoragePrecision(amount.add(movement));
        return new Change(quantity.add(delta), amount.add(movement), movement);
    }

    static void requireStoragePrecision(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        if (normalized.scale() > 6 || normalized.precision() - normalized.scale() > 18) {
            throw new IllegalArgumentException("数量和成本超出核算精度，最多18位整数和6位小数");
        }
    }

    @Value
    public static class Change {
        BigDecimal quantity;
        BigDecimal amount;
        BigDecimal movement;
    }
}
