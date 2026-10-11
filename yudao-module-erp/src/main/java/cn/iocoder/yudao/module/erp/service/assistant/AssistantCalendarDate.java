package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Getter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-owned parser for an explicitly requested calendar day.  It deliberately keeps
 * the day and its year/month together so a clarification such as “上个月” can never
 * widen “29号” into a whole-month query.
 */
final class AssistantCalendarDate {

    private static final Pattern FULL_DATE = Pattern.compile(
            "(?<!\\d)(20\\d{2})[-/.年](\\d{1,2})[-/.月](\\d{1,2})(?:日|号)?(?!\\d)");
    private static final Pattern MONTH_DAY = Pattern.compile(
            "(?<![\\d年])([1-9]|1[0-2])月([1-9]|[12]\\d|3[01])(?:日|号)(?!\\d)");
    private static final Pattern DAY_ONLY = Pattern.compile(
            "(?<![\\d月])([1-9]|[12]\\d|3[01])(?:日|号)(?!\\d)");
    private static final Pattern DATE_RANGE_SEPARATOR = Pattern.compile("(?:至|到|~|～)");

    private AssistantCalendarDate() {
    }

    static Resolution resolve(String question, AssistantPlan previous) {
        return resolve(question, previous, LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE));
    }

    static Resolution resolve(String question, AssistantPlan previous, LocalDate today) {
        String text = compact(question);
        if (text.isEmpty()) return Resolution.none();

        Matcher full = FULL_DATE.matcher(text);
        if (full.find()) {
            int year = integer(full, 1), month = integer(full, 2), day = integer(full, 3);
            // A range belongs to the existing range parser rather than to the single-day parser.
            if (full.find() || DATE_RANGE_SEPARATOR.matcher(text).find()) return Resolution.none();
            return create(year, month, day, today, Source.CURRENT_QUESTION);
        }

        Matcher monthDay = MONTH_DAY.matcher(text);
        if (monthDay.find()) {
            int month = integer(monthDay, 1), day = integer(monthDay, 2);
            if (monthDay.find() || DATE_RANGE_SEPARATOR.matcher(text).find()) return Resolution.none();
            return create(today.getYear(), month, day, today, Source.CURRENT_QUESTION);
        }

        Matcher dayOnly = DAY_ONLY.matcher(text);
        if (!dayOnly.find()) return Resolution.none();
        int day = integer(dayOnly, 1);
        if (dayOnly.find())
            return Resolution.invalid(day, "检测到多个单日条件，请只保留一个日期");

        YearMonth month = supplementalMonth(text, today);
        Source source = month == null ? null : Source.CLARIFICATION_SUPPLEMENT;
        if (month == null && isSingleDay(previous)) {
            month = YearMonth.from(LocalDate.parse(previous.getStartDate()));
            source = Source.PREVIOUS_YEAR_MONTH;
        }
        if (month == null) return Resolution.missing(day);
        return create(month.getYear(), month.getMonthValue(), day, today, source);
    }

    static boolean isDayOnly(String question) {
        String text = compact(question);
        if (FULL_DATE.matcher(text).find() || MONTH_DAY.matcher(text).find()) return false;
        Matcher matcher = DAY_ONLY.matcher(text);
        return matcher.find() && !matcher.find();
    }

    static boolean isSingleDay(AssistantPlan plan) {
        if (plan == null || !"CUSTOM".equals(plan.getPeriod())
                || plan.getStartDate() == null || plan.getEndDate() == null) return false;
        try {
            return LocalDate.parse(plan.getStartDate()).equals(LocalDate.parse(plan.getEndDate()));
        } catch (DateTimeException ignored) {
            return false;
        }
    }

    private static YearMonth supplementalMonth(String text, LocalDate today) {
        if (text.matches(".*(?:上月|上个月).*")) return YearMonth.from(today).minusMonths(1);
        if (text.matches(".*(?:本月|这个月|这一个月).*")) return YearMonth.from(today);
        return null;
    }

    private static Resolution create(int year, int month, int day, LocalDate today, Source source) {
        try {
            LocalDate date = LocalDate.of(year, month, day);
            if (date.isAfter(today))
                return Resolution.invalid(day, "所选日期 " + date + " 尚未发生，请重新选择日期");
            return Resolution.resolved(date, source);
        } catch (DateTimeException ignored) {
            return Resolution.invalid(day, "日期 " + year + "-" + month + "-" + day + " 不存在，请重新选择日期");
        }
    }

    private static int integer(Matcher matcher, int group) {
        return Integer.parseInt(matcher.group(group));
    }

    private static String compact(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u3000，,。；;：:？?！!]", "");
    }

    enum Source {
        CURRENT_QUESTION,
        PREVIOUS_YEAR_MONTH,
        CLARIFICATION_SUPPLEMENT
    }

    @Getter
    static final class Resolution {
        private final boolean matched;
        private final boolean resolved;
        private final boolean missingContext;
        private final LocalDate date;
        private final Source source;
        private final Integer day;
        private final String error;

        private Resolution(boolean matched, boolean resolved, boolean missingContext, LocalDate date,
                           Source source, Integer day, String error) {
            this.matched = matched;
            this.resolved = resolved;
            this.missingContext = missingContext;
            this.date = date;
            this.source = source;
            this.day = day;
            this.error = error;
        }

        static Resolution none() {
            return new Resolution(false, false, false, null, null, null, null);
        }

        static Resolution missing(int day) {
            return new Resolution(true, false, true, null, null, day, null);
        }

        static Resolution invalid(int day, String error) {
            return new Resolution(true, false, false, null, null, day, error);
        }

        static Resolution resolved(LocalDate date, Source source) {
            return new Resolution(true, true, false, date, source, date.getDayOfMonth(), null);
        }

        void apply(AssistantPlan plan) {
            plan.setPeriod("CUSTOM");
            plan.setStartDate(date.toString());
            plan.setEndDate(date.toString());
        }

        void applyClarification(AssistantPlan plan) {
            plan.setPeriod(null);
            plan.setStartDate(null);
            plan.setEndDate(null);
            plan.setClarification("请确认“" + day + "号”是哪个月");
            plan.setChoices(new ArrayList<>());
            plan.getChoices().add("上个月");
            plan.getChoices().add("本月");
        }

        void requireValid() {
            if (matched && !resolved && !missingContext)
                throw new AssistantFailure("CLARIFY", error);
        }
    }
}
