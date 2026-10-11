package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Getter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses server-owned rolling date expressions such as “最近7天” without asking the model to guess dates. */
final class AssistantRelativePeriod {

    static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    static final String CURRENT_WEEK_REGEX =
            "(?:本周|这周|这一周|本星期|这个星期|这一星期|本礼拜|这个礼拜|这一礼拜)";
    static final String LAST_WEEK_REGEX =
            "(?:上周|上一周|上星期|上个星期|上一星期|上礼拜|上个礼拜|上一礼拜)";
    static final String FIXED_PERIOD_REGEX =
            "(?:今天|今日|昨天|昨日|" + CURRENT_WEEK_REGEX + "|" + LAST_WEEK_REGEX + "|"
                    + "本月|这个月|这一个月|上月|上个月|本年|今年|今年以来|去年|上一年)";
    private static final Pattern ROLLING = Pattern.compile(
            "(?:最近|过去|近)([负+-]?[0-9零〇一二两三四五六七八九十百]+)(个月|个星期|星期|天|填|周|月)");
    private static final Pattern OTHER_EXPLICIT_PERIOD = Pattern.compile(
            "(?:" + FIXED_PERIOD_REGEX + "|"
                    + "20\\d{2}(?:年|[-/])\\d{1,2}|\\d{1,2}月份?|[一二三四五六七八九十]{1,3}月份?)");

    private AssistantRelativePeriod() {
    }

    static Resolution resolve(String question) {
        return resolve(question, LocalDate.now(BUSINESS_ZONE));
    }

    static Resolution resolve(String question, LocalDate today) {
        String text = compact(question);
        Matcher matcher = ROLLING.matcher(text);
        if (!matcher.find()) return Resolution.none();
        String numberText = matcher.group(1);
        String unitText = matcher.group(2);
        int firstStart = matcher.start();
        int firstEnd = matcher.end();
        if (matcher.find()) return Resolution.invalid("检测到多个滚动时间范围，请只保留一个“最近 N 天/周/月”条件");
        String withoutRolling = text.substring(0, firstStart) + text.substring(firstEnd);
        if (OTHER_EXPLICIT_PERIOD.matcher(withoutRolling).find())
            return Resolution.invalid("检测到相互冲突的时间范围，请确认本次要统计的期间");
        Integer amount = number(numberText);
        if (amount == null || amount <= 0)
            return Resolution.invalid("最近时间范围必须是大于 0 的整数");
        Unit unit = Unit.of(unitText);
        if (amount > unit.maximum)
            return Resolution.invalid(unit.limitMessage());
        LocalDate start;
        switch (unit) {
            case DAY:
                start = today.minusDays(amount - 1L);
                break;
            case WEEK:
                start = today.minusDays(amount * 7L - 1L);
                break;
            case MONTH:
                start = today.minusMonths(amount).plusDays(1L);
                break;
            default:
                throw new IllegalStateException("Unsupported rolling period unit");
        }
        return Resolution.resolved(start, today, amount, unit);
    }

    /** Returns the canonical fixed-period identifier shared by semantic and SQL layers. */
    static String explicitPeriod(String question) {
        String text = compact(question);
        if (text.matches(".*(?:今天|今日).*")) return "TODAY";
        if (text.matches(".*(?:昨天|昨日).*")) return "YESTERDAY";
        if (text.matches(".*" + CURRENT_WEEK_REGEX + ".*")) return "THIS_WEEK";
        if (text.matches(".*" + LAST_WEEK_REGEX + ".*")) return "LAST_WEEK";
        if (text.matches(".*(?:本月|这个月|这一个月).*")) return "THIS_MONTH";
        if (text.matches(".*(?:上月|上个月).*")) return "LAST_MONTH";
        if (text.matches(".*(?:本年|今年|今年以来).*")) return "THIS_YEAR";
        if (text.matches(".*(?:去年|上一年).*")) return "LAST_YEAR";
        return null;
    }

    /** A leading calendar phrase is a query condition, never an entity pronoun such as “这个产品”. */
    static boolean startsWithExplicitPeriod(String question) {
        String text = compact(question).replaceFirst("^(?:请问|帮我查一下|帮我查询|查询一下|查询|查看一下|查看|查一下)", "");
        return text.matches("^" + FIXED_PERIOD_REGEX + ".*" );
    }

    /** A period-only phrase may inherit a previous metric, but must never become an entity keyword. */
    static boolean isExplicitPeriodOnly(String question) {
        String text = compact(question);
        return text.matches("^(?:那|那么)?" + FIXED_PERIOD_REGEX + "(?:呢|吗)?$");
    }

    /** Resolves rolling and fixed calendar periods to the same half-open SQL time window. */
    static Window window(String question, LocalDateTime now) {
        Resolution rolling = resolve(question, now.toLocalDate());
        rolling.requireValid();
        if (rolling.isValid()) return new Window("CUSTOM", rolling.getStartDate().atStartOfDay(), now);
        String period = explicitPeriod(question);
        if (period == null) return null;
        LocalDate day = now.toLocalDate();
        LocalDateTime start;
        LocalDateTime end = now;
        switch (period) {
            case "TODAY":
                start = day.atStartOfDay();
                break;
            case "YESTERDAY":
                start = day.minusDays(1).atStartOfDay();
                end = day.atStartOfDay();
                break;
            case "THIS_WEEK":
                start = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                break;
            case "LAST_WEEK":
                end = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
                start = end.minusWeeks(1);
                break;
            case "THIS_MONTH":
                start = day.withDayOfMonth(1).atStartOfDay();
                break;
            case "LAST_MONTH":
                end = day.withDayOfMonth(1).atStartOfDay();
                start = end.minusMonths(1);
                break;
            case "THIS_YEAR":
                start = day.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                break;
            case "LAST_YEAR":
                end = day.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                start = end.minusYears(1);
                break;
            default:
                return null;
        }
        return new Window(period, start, end);
    }

    private static String compact(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u3000，,。；;：:？?！!]", "");
    }

    private static Integer number(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            if (value.matches("[+-]?\\d+")) return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
        boolean negative = value.startsWith("负");
        String text = negative ? value.substring(1) : value;
        if (text.isEmpty()) return null;
        int parsed = chineseNumber(text);
        return parsed < 0 ? null : negative ? -parsed : parsed;
    }

    private static int chineseNumber(String text) {
        if (text.indexOf('百') >= 0) {
            String[] parts = text.split("百", -1);
            if (parts.length != 2) return -1;
            int hundreds = parts[0].isEmpty() ? 1 : digit(parts[0]);
            int remainder = parts[1].isEmpty() ? 0 : underHundred(parts[1]);
            return hundreds < 0 || remainder < 0 ? -1 : hundreds * 100 + remainder;
        }
        return underHundred(text);
    }

    private static int underHundred(String text) {
        int ten = text.indexOf('十');
        if (ten >= 0) {
            if (text.indexOf('十', ten + 1) >= 0) return -1;
            int tens = ten == 0 ? 1 : digit(text.substring(0, ten));
            int ones = ten == text.length() - 1 ? 0 : digit(text.substring(ten + 1));
            return tens < 0 || ones < 0 ? -1 : tens * 10 + ones;
        }
        if (text.length() == 1) return digit(text);
        int result = 0;
        for (int i = 0; i < text.length(); i++) {
            int value = digit(text.substring(i, i + 1));
            if (value < 0) return -1;
            result = result * 10 + value;
        }
        return result;
    }

    private static int digit(String value) {
        if (value == null || value.length() != 1) return -1;
        switch (value.charAt(0)) {
            case '零': case '〇': return 0;
            case '一': return 1;
            case '二': case '两': return 2;
            case '三': return 3;
            case '四': return 4;
            case '五': return 5;
            case '六': return 6;
            case '七': return 7;
            case '八': return 8;
            case '九': return 9;
            default: return -1;
        }
    }

    enum Unit {
        DAY(366), WEEK(52), MONTH(12);

        private final int maximum;

        Unit(int maximum) {
            this.maximum = maximum;
        }

        static Unit of(String value) {
            if ("天".equals(value) || "填".equals(value)) return DAY;
            if ("周".equals(value) || "星期".equals(value) || "个星期".equals(value)) return WEEK;
            return MONTH;
        }

        String limitMessage() {
            if (this == DAY) return "最近天数需在 1 至 366 天之间";
            if (this == WEEK) return "最近周数需在 1 至 52 周之间";
            return "最近月数需在 1 至 12 个月之间";
        }
    }

    @Getter
    static final class Window {
        private final String period;
        private final LocalDateTime start;
        private final LocalDateTime end;

        Window(String period, LocalDateTime start, LocalDateTime end) {
            this.period = period;
            this.start = start;
            this.end = end;
        }
    }

    @Getter
    static final class Resolution {
        private final boolean matched;
        private final boolean valid;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final Integer amount;
        private final Unit unit;
        private final String error;

        private Resolution(boolean matched, boolean valid, LocalDate startDate, LocalDate endDate,
                           Integer amount, Unit unit, String error) {
            this.matched = matched;
            this.valid = valid;
            this.startDate = startDate;
            this.endDate = endDate;
            this.amount = amount;
            this.unit = unit;
            this.error = error;
        }

        static Resolution none() {
            return new Resolution(false, false, null, null, null, null, null);
        }

        static Resolution invalid(String error) {
            return new Resolution(true, false, null, null, null, null, error);
        }

        static Resolution resolved(LocalDate start, LocalDate end, int amount, Unit unit) {
            return new Resolution(true, true, start, end, amount, unit, null);
        }

        void apply(AssistantPlan plan) {
            plan.setPeriod("CUSTOM");
            plan.setStartDate(startDate.toString());
            plan.setEndDate(endDate.toString());
        }

        void requireValid() {
            if (matched && !valid) throw new AssistantFailure("CLARIFY", error);
        }
    }
}
