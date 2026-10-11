package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class AssistantRelativePeriodTest {

    private static final LocalDate TODAY=LocalDate.of(2026,10,7);
    private static final Clock QUERY_CLOCK=Clock.fixed(Instant.parse("2026-10-07T07:59:02Z"),ZoneOffset.UTC);

    @Test void resolvesRollingDaysWeeksAndMonthsFromBeijingBusinessDate() {
        assertRange("最近7天的销售额","2026-10-01","2026-10-07");
        assertRange("近七天的销售额","2026-10-01","2026-10-07");
        assertRange("过去2周的销售额","2026-09-24","2026-10-07");
        assertRange("最近一个月的销售额","2026-09-08","2026-10-07");
        assertRange("过去十二星期的采购额","2026-07-16","2026-10-07");
    }

    @Test void fixedClockProducesQueryTimeCutoffAndReadableHistoricalEnd() {
        AssistantRelativePeriod.Resolution rolling=AssistantRelativePeriod.resolve("最近7天",TODAY);
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);rolling.apply(plan);

        LocalDateTime[] range=plan.range(QUERY_CLOCK);

        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026,10,1,0,0),
                LocalDateTime.of(2026,10,7,15,59,2)},range);
        assertEquals(range[1],AssistantQueryService.responseEnd(range,LocalDateTime.of(2026,10,7,15,59,2)));

        plan.setStartDate("2026-07-03");plan.setEndDate("2026-07-09");
        range=plan.range(QUERY_CLOCK);
        assertEquals(LocalDateTime.of(2026,7,10,0,0),range[1],"SQL boundary remains exclusive");
        assertEquals(LocalDateTime.of(2026,7,9,23,59,59,999_999_000),
                AssistantQueryService.responseEnd(range,LocalDateTime.of(2026,10,7,15,59,2)));
    }

    @Test void rejectsZeroNegativeExcessiveAndConflictingRanges() {
        for(String question:new String[]{"最近0天","最近负一天","最近367天","最近53周","最近13个月",
                "本月最近7天","最近7天和过去2周"}) {
            AssistantRelativePeriod.Resolution result=AssistantRelativePeriod.resolve(question,TODAY);
            assertTrue(result.isMatched(),question);assertFalse(result.isValid(),question);assertNotNull(result.getError(),question);
            assertEquals("CLARIFY",assertThrows(AssistantFailure.class,result::requireValid).getCode(),question);
        }
    }

    @Test void normalizesCurrentWeekAndMonthAliasesIntoOneWindow() {
        LocalDateTime now=LocalDateTime.of(2026,10,7,15,59,2);
        for(String question:new String[]{"这个星期是否有入库记录","这一星期有没有入库","本星期有入库吗","这一周入库流水",
                "本礼拜销售额","这个礼拜采购额","这一礼拜收款"}) {
            AssistantRelativePeriod.Window window=AssistantRelativePeriod.window(question,now);
            assertNotNull(window,question);assertEquals("THIS_WEEK",window.getPeriod(),question);
            assertEquals(LocalDateTime.of(2026,10,5,0,0),window.getStart(),question);assertEquals(now,window.getEnd(),question);
            assertTrue(AssistantRelativePeriod.startsWithExplicitPeriod(question),question);
        }
        for(String question:new String[]{"这个月是否有入库记录","这一个月有没有入库","本月入库流水"}) {
            AssistantRelativePeriod.Window window=AssistantRelativePeriod.window(question,now);
            assertNotNull(window,question);assertEquals("THIS_MONTH",window.getPeriod(),question);
            assertEquals(LocalDateTime.of(2026,10,1,0,0),window.getStart(),question);assertEquals(now,window.getEnd(),question);
        }
        AssistantRelativePeriod.Window rolling=AssistantRelativePeriod.window("最近一个月入库记录",now);
        assertEquals("CUSTOM",rolling.getPeriod());assertEquals(LocalDateTime.of(2026,9,8,0,0),rolling.getStart());
    }

    @Test void normalizesPreviousWeekAliasesIntoTheLastNaturalWeek() {
        LocalDateTime now=LocalDateTime.of(2026,10,7,15,59,2);
        for(String question:new String[]{"上周销售情况","上一周采购额","上星期收款","上个星期的销售情况",
                "上一星期的入库流水","上礼拜付款","上个礼拜销量","上一礼拜入库情况"}) {
            AssistantRelativePeriod.Window window=AssistantRelativePeriod.window(question,now);
            assertNotNull(window,question);assertEquals("LAST_WEEK",window.getPeriod(),question);
            assertEquals(LocalDateTime.of(2026,9,28,0,0),window.getStart(),question);
            assertEquals(LocalDateTime.of(2026,10,5,0,0),window.getEnd(),question);
            assertTrue(AssistantRelativePeriod.startsWithExplicitPeriod(question),question);
        }
        AssistantRelativePeriod.Window rolling=AssistantRelativePeriod.window("最近一个星期销售情况",now);
        assertEquals("CUSTOM",rolling.getPeriod());assertEquals(LocalDateTime.of(2026,10,1,0,0),rolling.getStart());
        assertEquals(now,rolling.getEnd());
    }

    private static void assertRange(String question,String start,String end) {
        AssistantRelativePeriod.Resolution result=AssistantRelativePeriod.resolve(question,TODAY);
        assertTrue(result.isMatched(),question);assertTrue(result.isValid(),question);
        assertEquals(start,result.getStartDate().toString(),question);assertEquals(end,result.getEndDate().toString(),question);
    }
}
