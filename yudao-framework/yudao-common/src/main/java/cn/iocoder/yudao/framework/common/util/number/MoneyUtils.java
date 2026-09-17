package cn.iocoder.yudao.framework.common.util.number;

import cn.hutool.core.math.Money;
import cn.hutool.core.util.NumberUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额工具类
 *
 * @author 芋道源码
 */
public class MoneyUtils {

    /**
     * 金额的小数位数
     */
    private static final int PRICE_SCALE = 2;
    private static final String[] CHINESE_UPPER_DIGITS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] CHINESE_UPPER_UNITS = {"", "拾", "佰", "仟"};
    private static final String[] CHINESE_UPPER_GROUP_UNITS = {"", "万", "亿", "兆"};

    /**
     * 百分比对应的 BigDecimal 对象
     */
    public static final BigDecimal PERCENT_100 = BigDecimal.valueOf(100);

    /**
     * 计算百分比金额，四舍五入
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePrice(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * 计算百分比金额，向下传入
     *
     * @param price 金额
     * @param rate  百分比，例如说 56.77% 则传入 56.77
     * @return 百分比金额
     */
    public static Integer calculateRatePriceFloor(Integer price, Double rate) {
        return calculateRatePrice(price, rate, 0, RoundingMode.FLOOR).intValue();
    }

    /**
     * 计算百分比金额
     *
     * @param price   金额（单位分）
     * @param count   数量
     * @param percent 折扣（单位分），列如 60.2%，则传入 6020
     * @return 商品总价
     */
    public static Integer calculator(Integer price, Integer count, Integer percent) {
        price = price * count;
        if (percent == null) {
            return price;
        }
        return MoneyUtils.calculateRatePriceFloor(price, (double) (percent / 100));
    }

    /**
     * 计算百分比金额
     *
     * @param price        金额
     * @param rate         百分比，例如说 56.77% 则传入 56.77
     * @param scale        保留小数位数
     * @param roundingMode 舍入模式
     */
    public static BigDecimal calculateRatePrice(Number price, Number rate, int scale, RoundingMode roundingMode) {
        return NumberUtil.toBigDecimal(price).multiply(NumberUtil.toBigDecimal(rate)) // 乘以
                .divide(BigDecimal.valueOf(100), scale, roundingMode); // 除以 100
    }

    /**
     * 分转元
     *
     * @param fen 分
     * @return 元
     */
    public static BigDecimal fenToYuan(int fen) {
        return new Money(0, fen).getAmount();
    }

    /**
     * 分转元（字符串）
     *
     * 例如说 fen 为 1 时，则结果为 0.01
     *
     * @param fen 分
     * @return 元
     */
    public static String fenToYuanStr(int fen) {
        return new Money(0, fen).toString();
    }

    /**
     * 金额相乘，默认进行四舍五入
     *
     * 位数：{@link #PRICE_SCALE}
     *
     * @param price 金额
     * @param count 数量
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiply(BigDecimal price, BigDecimal count) {
        if (price == null || count == null) {
            return null;
        }
        return price.multiply(count).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 金额相乘（百分比），默认进行四舍五入
     *
     * 位数：{@link #PRICE_SCALE}
     *
     * @param price  金额
     * @param percent 百分比
     * @return 金额相乘结果
     */
    public static BigDecimal priceMultiplyPercent(BigDecimal price, BigDecimal percent) {
        if (price == null || percent == null) {
            return null;
        }
        return price.multiply(percent).divide(PERCENT_100, PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 格式化为人民币大写金额。
     *
     * 例如：1420.00 -> 壹仟肆佰贰拾元整，100.01 -> 壹佰元零壹分。
     */
    public static String formatAmountUpper(BigDecimal value) {
        if (value == null) {
            return "";
        }
        BigDecimal amount = value.setScale(PRICE_SCALE, RoundingMode.HALF_UP);
        boolean negative = amount.signum() < 0;
        BigDecimal absoluteAmount = amount.abs();
        BigDecimal totalCentDecimal = absoluteAmount.movePointRight(PRICE_SCALE);
        String totalCent = totalCentDecimal.toBigIntegerExact().toString();
        String yuan = totalCent.length() <= 2 ? "0" : totalCent.substring(0, totalCent.length() - 2);
        int cent = Integer.parseInt(totalCent.length() == 1 ? totalCent : totalCent.substring(totalCent.length() - 2));
        int jiao = cent / 10;
        int fen = cent % 10;

        StringBuilder result = new StringBuilder();
        if (negative) {
            result.append("负");
        }
        result.append(formatIntegerUpper(yuan)).append("元");
        if (jiao == 0 && fen == 0) {
            return result.append("整").toString();
        }
        if (jiao > 0) {
            result.append(CHINESE_UPPER_DIGITS[jiao]).append("角");
        } else if (!"0".equals(stripLeadingZeros(yuan)) && fen > 0) {
            result.append("零");
        }
        if (fen > 0) {
            result.append(CHINESE_UPPER_DIGITS[fen]).append("分");
        }
        return result.toString();
    }

    private static String formatIntegerUpper(String value) {
        String integer = stripLeadingZeros(value);
        if ("0".equals(integer)) {
            return CHINESE_UPPER_DIGITS[0];
        }
        StringBuilder result = new StringBuilder();
        int groupCount = (integer.length() + 3) / 4;
        boolean zeroGroupPending = false;
        for (int groupIndex = 0; groupIndex < groupCount; groupIndex++) {
            int start = Math.max(0, integer.length() - (groupCount - groupIndex) * 4);
            int end = integer.length() - (groupCount - groupIndex - 1) * 4;
            int groupValue = Integer.parseInt(integer.substring(start, end));
            if (groupValue == 0) {
                zeroGroupPending = result.length() > 0;
                continue;
            }
            if (result.length() > 0 && (zeroGroupPending || groupValue < 1000)) {
                result.append(CHINESE_UPPER_DIGITS[0]);
            }
            result.append(formatGroupUpper(groupValue))
                    .append(CHINESE_UPPER_GROUP_UNITS[groupCount - groupIndex - 1]);
            zeroGroupPending = false;
        }
        return result.toString();
    }

    private static String formatGroupUpper(int value) {
        StringBuilder result = new StringBuilder();
        boolean zeroPending = false;
        for (int position = 3; position >= 0; position--) {
            int unitBase = (int) Math.pow(10, position);
            int digit = value / unitBase % 10;
            if (digit == 0) {
                zeroPending = result.length() > 0;
                continue;
            }
            if (zeroPending) {
                result.append(CHINESE_UPPER_DIGITS[0]);
            }
            result.append(CHINESE_UPPER_DIGITS[digit]).append(CHINESE_UPPER_UNITS[position]);
            zeroPending = false;
        }
        return result.toString();
    }

    private static String stripLeadingZeros(String value) {
        String stripped = value.replaceFirst("^0+(?!$)", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

}
