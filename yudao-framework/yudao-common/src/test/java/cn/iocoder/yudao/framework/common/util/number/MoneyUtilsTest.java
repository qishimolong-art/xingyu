package cn.iocoder.yudao.framework.common.util.number;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link MoneyUtils} 的单元测试
 */
public class MoneyUtilsTest {

    @Test
    public void testFormatAmountUpper() {
        assertEquals("", MoneyUtils.formatAmountUpper(null));
        assertEquals("零元整", MoneyUtils.formatAmountUpper(new BigDecimal("0")));
        assertEquals("壹仟肆佰贰拾元整", MoneyUtils.formatAmountUpper(new BigDecimal("1420.00")));
        assertEquals("壹佰贰拾元伍角", MoneyUtils.formatAmountUpper(new BigDecimal("120.50")));
        assertEquals("壹佰元零壹分", MoneyUtils.formatAmountUpper(new BigDecimal("100.01")));
        assertEquals("壹仟零壹元壹角", MoneyUtils.formatAmountUpper(new BigDecimal("1001.10")));
        assertEquals("负壹元贰角叁分", MoneyUtils.formatAmountUpper(new BigDecimal("-1.23")));
    }

}
