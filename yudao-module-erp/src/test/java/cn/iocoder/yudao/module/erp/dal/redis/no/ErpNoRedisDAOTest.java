package cn.iocoder.yudao.module.erp.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.redis.RedisKeyConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpNoRedisDAO} 单元测试。
 *
 * 覆盖：
 *  - generate(prefix)：常规日期序号
 *  - generatePlain(prefix)：纯流水号
 *  - generateMonthly(prefix)：月度自增凭证号（财务核算核心算法）
 *
 * 重点 Bug 暴露：S3 凭证号月份使用 LocalDateTime.now()，不接受业务月份参数 →
 * 跨月归属时无法生成与 voucherDate 对齐的凭证号。
 */
public class ErpNoRedisDAOTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpNoRedisDAO noRedisDAO;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private void mockIncrement(String expectKey, Long returnNo) {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq(expectKey))).thenReturn(returnNo);
        lenient().when(stringRedisTemplate.expire(any(String.class), any(Duration.class))).thenReturn(true);
    }

    // ==================== generate ====================

    @Test
    @DisplayName("generate：返回 prefix + yyyyMMdd + 6 位序号，并设置 1 天过期")
    public void testGenerate_normalCase() {
        String prefix = "CGRK";
        String today = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String expectKey = RedisKeyConstants.NO + prefix + today;
        mockIncrement(expectKey, 1L);

        String no = noRedisDAO.generate(prefix);

        assertEquals(prefix + today + "000001", no);
        verify(stringRedisTemplate).expire(eq(expectKey), eq(Duration.ofDays(1L)));
    }

    @Test
    @DisplayName("generate：序号 999999 应正确格式化为 6 位")
    public void testGenerate_largeNumber() {
        String prefix = "QTRK";
        String today = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        mockIncrement(RedisKeyConstants.NO + prefix + today, 999_999L);

        String no = noRedisDAO.generate(prefix);

        assertEquals(prefix + today + "999999", no);
    }

    @Test
    @DisplayName("generate：序号超过 6 位（1_000_000）仍能输出（不截断）")
    public void testGenerate_overflowNumber() {
        String prefix = "QTRK";
        String today = DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        mockIncrement(RedisKeyConstants.NO + prefix + today, 1_000_000L);

        String no = noRedisDAO.generate(prefix);

        // %06d 不会截断，只会保证最少 6 位
        assertTrue(no.endsWith("1000000"), "实际输出：" + no);
    }

    // ==================== generatePlain ====================

    @Test
    @DisplayName("generatePlain：不带日期，跨日累加；不调用 expire")
    public void testGeneratePlain_noExpire() {
        String prefix = "P";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq(RedisKeyConstants.NO + prefix))).thenReturn(42L);

        String no = noRedisDAO.generatePlain(prefix);

        assertEquals("P000042", no);
        verify(stringRedisTemplate, times(0)).expire(any(String.class), any(Duration.class));
    }

    @Test
    @DisplayName("generatePlain：increment 返回 null 时兜底为 1（防 NPE）")
    public void testGeneratePlain_incrementReturnNull() {
        String prefix = "P";
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq(RedisKeyConstants.NO + prefix))).thenReturn(null);

        String no = noRedisDAO.generatePlain(prefix);

        assertEquals("P000001", no);
    }

    @Test
    @DisplayName("generatePlainAfter：Redis 流水落后时先追到指定流水再自增")
    public void testGeneratePlainAfter() {
        String prefix = "WH";
        when(stringRedisTemplate.execute(any(), eq(Collections.singletonList(RedisKeyConstants.NO + prefix)),
                eq("123"))).thenReturn(124L);

        String no = noRedisDAO.generatePlainAfter(prefix, 123L);

        assertEquals("WH000124", no);
    }

    // ==================== generateMonthly（财务凭证号） ====================

    @Test
    @DisplayName("generateMonthly：格式正确 - 记-yyyyMM-000001")
    public void testGenerateMonthly_normalFormat() {
        String prefix = "记";
        String yearMonth = DateUtil.format(LocalDateTime.now(), "yyyyMM");
        String expectKey = RedisKeyConstants.NO + prefix + "-" + yearMonth;
        mockIncrement(expectKey, 1L);

        String no = noRedisDAO.generateMonthly(prefix);

        assertEquals(prefix + "-" + yearMonth + "-000001", no);
        verify(stringRedisTemplate).expire(eq(expectKey), eq(Duration.ofDays(35L)));
    }

    @Test
    @DisplayName("generateMonthly：本月连续生成 序号自增 000001 → 000002")
    public void testGenerateMonthly_increment() {
        String prefix = "记";
        String yearMonth = DateUtil.format(LocalDateTime.now(), "yyyyMM");
        String expectKey = RedisKeyConstants.NO + prefix + "-" + yearMonth;
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(eq(expectKey))).thenReturn(1L, 2L);
        lenient().when(stringRedisTemplate.expire(any(String.class), any(Duration.class))).thenReturn(true);

        String no1 = noRedisDAO.generateMonthly(prefix);
        String no2 = noRedisDAO.generateMonthly(prefix);

        assertEquals(prefix + "-" + yearMonth + "-000001", no1);
        assertEquals(prefix + "-" + yearMonth + "-000002", no2);
    }

    @Test
    @DisplayName("generateMonthly：35 天过期，跨月自然失效（防止极端时区漏数）")
    public void testGenerateMonthly_expire35Days() {
        String prefix = "记";
        String yearMonth = DateUtil.format(LocalDateTime.now(), "yyyyMM");
        String expectKey = RedisKeyConstants.NO + prefix + "-" + yearMonth;
        mockIncrement(expectKey, 100L);

        noRedisDAO.generateMonthly(prefix);

        verify(stringRedisTemplate).expire(eq(expectKey), eq(Duration.ofDays(35L)));
    }

    @Test
    @DisplayName("generateMonthly：自定义凭证字（如 转）也能正常生成")
    public void testGenerateMonthly_customPrefix() {
        String prefix = "转";
        String yearMonth = DateUtil.format(LocalDateTime.now(), "yyyyMM");
        String expectKey = RedisKeyConstants.NO + prefix + "-" + yearMonth;
        mockIncrement(expectKey, 5L);

        String no = noRedisDAO.generateMonthly(prefix);

        assertEquals(prefix + "-" + yearMonth + "-000005", no);
    }

    @Test
    @DisplayName("S3 修复：generateMonthly(prefix, YearMonth) 重载支持业务月份，避免跨月凭证号穿越")
    public void testGenerateMonthly_bugS3_crossMonthAttribution() {
        // S3 业务场景：voucherDate = 2026-05-31（业务月份 202605），即使系统当前时间已跨月，
        // 凭证号应与业务月份对齐 → 记-202605-000001。
        // S3 修复后：ErpNoRedisDAO 新增 generateMonthly(prefix, YearMonth) 重载，
        // 调用方传入业务月份；老接口 generateMonthly(prefix) 委托到新重载并传入 YearMonth.now()。
        String prefix = "记";
        YearMonth bizYearMonth = YearMonth.of(2026, 5);
        String expectKey = RedisKeyConstants.NO + prefix + "-202605";
        mockIncrement(expectKey, 1L);

        String no = noRedisDAO.generateMonthly(prefix, bizYearMonth);

        // 修复后：凭证号月份 = 业务月份（不再依赖系统当前月份）
        assertEquals(prefix + "-202605-000001", no);
        verify(stringRedisTemplate).expire(eq(expectKey), eq(Duration.ofDays(35L)));
    }

}
