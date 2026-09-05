package cn.iocoder.yudao.framework.excel.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记导入模板中的任选一字段。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelChoiceRequired {

    String value() default "三选一字段：本列与同组字段至少填写一个；多列都填时必须对应同一条资料";

}
