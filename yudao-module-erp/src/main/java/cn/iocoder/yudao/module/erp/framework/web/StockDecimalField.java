package cn.iocoder.yudao.module.erp.framework.web;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StockDecimalFieldValidator.class)
public @interface StockDecimalField {

    String message() default "超出字段保存精度（整数最多{integer}位、小数最多{scale}位；末尾零不计）";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int integer() default 18;

    int scale() default 6;
}
