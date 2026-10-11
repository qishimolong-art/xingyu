package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AssistantCalendarDateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 8);

    @Test
    void resolvesFullMonthDayAndPreviousSingleDayFollowup() {
        assertResolved("2026年9月28号的销售情况", null, "2026-09-28",
                AssistantCalendarDate.Source.CURRENT_QUESTION);
        assertResolved("9月28号的销售情况", null, "2026-09-28",
                AssistantCalendarDate.Source.CURRENT_QUESTION);

        AssistantPlan previous = singleDay("2026-09-28", AssistantPlan.Metric.SALE);
        assertResolved("29号的销售情况", previous, "2026-09-29",
                AssistantCalendarDate.Source.PREVIOUS_YEAR_MONTH);
        assertResolved("29号呢", previous, "2026-09-29",
                AssistantCalendarDate.Source.PREVIOUS_YEAR_MONTH);
    }

    @Test
    void clarificationSupplementKeepsTheRequestedDay() {
        assertResolved("29号的销售情况；补充：上个月", null, "2026-09-29",
                AssistantCalendarDate.Source.CLARIFICATION_SUPPLEMENT);
        AssistantCalendarDate.Resolution current = AssistantCalendarDate.resolve(
                "29号的销售情况；补充：本月", null, TODAY);
        assertTrue(current.isMatched());
        assertFalse(current.isResolved());
        assertFalse(current.isMissingContext());
        assertTrue(current.getError().contains("尚未发生"));
        assertEquals("CLARIFY", assertThrows(AssistantFailure.class, current::requireValid).getCode());
    }

    @Test
    void standaloneDayRequiresMonthInsteadOfExpandingToAnyMonth() {
        AssistantCalendarDate.Resolution result = AssistantCalendarDate.resolve("29号的销售情况", null, TODAY);
        assertTrue(result.isMatched());
        assertFalse(result.isResolved());
        assertTrue(result.isMissingContext());

        AssistantPlan plan = new AssistantPlan();
        result.applyClarification(plan);
        assertEquals("请确认“29号”是哪个月", plan.getClarification());
        assertEquals(java.util.Arrays.asList("上个月", "本月"), plan.getChoices());
        assertNull(plan.getPeriod());
    }

    @Test
    void validatesImpossibleLeapAndCrossYearDates() {
        for (String question : new String[]{"9月31号销售额", "2025年2月29日销售额"}) {
            AssistantCalendarDate.Resolution result = AssistantCalendarDate.resolve(question, null, TODAY);
            assertTrue(result.isMatched(), question);
            assertFalse(result.isResolved(), question);
            assertTrue(result.getError().contains("不存在"), question);
        }
        assertResolved("2024年2月29日销售额", null, "2024-02-29",
                AssistantCalendarDate.Source.CURRENT_QUESTION);

        AssistantCalendarDate.Resolution december = AssistantCalendarDate.resolve(
                "29号销售情况；补充：上个月", null, LocalDate.of(2026, 1, 8));
        assertEquals(LocalDate.of(2025, 12, 29), december.getDate());
    }

    @Test
    void leavesExplicitRangesToTheExistingRangeParser() {
        assertFalse(AssistantCalendarDate.resolve(
                "2026年9月1日至2026年9月29日销售额", null, TODAY).isMatched());
    }

    @Test
    void singleDayUsesTheExistingHalfOpenCustomRange() {
        AssistantCalendarDate.Resolution resolution=AssistantCalendarDate.resolve(
                "9月29号销售情况",null,TODAY);
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);
        resolution.apply(plan);

        LocalDateTime[] range=plan.range(Clock.fixed(
                Instant.parse("2026-10-08T01:00:00Z"),ZoneOffset.UTC));

        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026,9,29,0,0),
                LocalDateTime.of(2026,9,30,0,0)},range);
    }

    private static void assertResolved(String question, AssistantPlan previous, String expected,
                                       AssistantCalendarDate.Source source) {
        AssistantCalendarDate.Resolution result = AssistantCalendarDate.resolve(question, previous, TODAY);
        assertTrue(result.isMatched(), question);
        assertTrue(result.isResolved(), question);
        assertEquals(LocalDate.parse(expected), result.getDate(), question);
        assertEquals(source, result.getSource(), question);
    }

    private static AssistantPlan singleDay(String date, AssistantPlan.Metric metric) {
        AssistantPlan result = new AssistantPlan();
        result.setMetric(metric);
        result.setPeriod("CUSTOM");
        result.setStartDate(date);
        result.setEndDate(date);
        return result;
    }
}
