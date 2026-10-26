package cn.iocoder.yudao.module.erp.framework.web;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

public class StockDecimalFieldValidator implements ConstraintValidator<StockDecimalField, BigDecimal> {

    private int integer;
    private int scale;

    @Override
    public void initialize(StockDecimalField field) {
        integer = field.integer();
        scale = field.scale();
    }

    @Override
    public boolean isValid(BigDecimal value, ConstraintValidatorContext context) {
        return value == null || (value.stripTrailingZeros().scale() <= scale
                && value.abs().compareTo(BigDecimal.TEN.pow(integer)) < 0);
    }
}
