package cn.iocoder.yudao.module.erp.dal.redis.no;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.iocoder.yudao.module.erp.dal.redis.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

/**
 * Erp 单号 Redis DAO
 */
@Repository
public class ErpNoRedisDAO {

    public static final String STOCK_IN_NO_PREFIX = "QTRK";
    public static final String STOCK_IN_BILL_NO_PREFIX = "RCD";
    public static final String STOCK_OUT_BILL_NO_PREFIX = "CCD";
    public static final String STOCK_OUT_NO_PREFIX = "QCKD";
    public static final String STOCK_MOVE_NO_PREFIX = "QCDB";
    public static final String STOCK_TRANSFER_IN_NO_PREFIX = "QDRK";
    public static final String WAREHOUSE_MOVE_NO_PREFIX = "YH";
    public static final String STOCK_CHECK_NO_PREFIX = "QCPD";
    public static final String SALE_ORDER_NO_PREFIX = "XSDD";
    public static final String SALE_QUOTE_NO_PREFIX = "XSBJ";
    public static final String SALE_CART_NO_PREFIX = "XSST";
    public static final String SALE_OUT_NO_PREFIX = "XSCK";
    public static final String SALE_RETURN_NO_PREFIX = "TH";
    public static final String PURCHASE_ORDER_NO_PREFIX = "CGDD";
    public static final String PURCHASE_IN_NO_PREFIX = "CGRK";
    public static final String PURCHASE_INVOICE_NO_PREFIX = "CGPJ";
    public static final String PURCHASE_RETURN_NO_PREFIX = "CGTH";
    public static final String FINANCE_PAYMENT_NO_PREFIX = "FKD";
    public static final String FINANCE_RECEIPT_NO_PREFIX = "SKD";
    public static final String FINANCE_TRANSFER_NO_PREFIX = "YHZZ";
    public static final String PURCHASE_PRICE_ADJUST_NO_PREFIX = "CGTJ";
    public static final String SALE_PRICE_ADJUST_NO_PREFIX = "XSTJ";
    public static final String CHAIN_ORDER_NO_PREFIX = "LSKD";
    public static final String PURCHASE_SUGGESTION_NO_PREFIX = "CGJY";
    public static final String OTHER_RECEIVABLE_NO_PREFIX = "QTYS";
    public static final String OTHER_INCOME_NO_PREFIX = "QTSR";
    public static final String PRE_RECEIVABLE_NO_PREFIX = "YSZK";
    public static final String BOOK_OPEN_NO_PREFIX = "KZ";
    public static final String VOUCHER_WORD_DEFAULT = "记";
    public static final String PRODUCT_CODE_PREFIX = "P";
    public static final String CUSTOMER_NO_PREFIX = "C";
    public static final String MEMBER_NO_PREFIX = "M";
    public static final String PLATFORM_NO_PREFIX = "P";
    public static final String WAREHOUSE_CODE_PREFIX = "WH";
    private static final DefaultRedisScript<Long> GENERATE_PLAIN_AFTER_SCRIPT = new DefaultRedisScript<>(
            "local current = redis.call('GET', KEYS[1]); "
                    + "local min = tonumber(ARGV[1]); "
                    + "if (not current) or tonumber(current) < min then redis.call('SET', KEYS[1], min); end; "
                    + "return redis.call('INCR', KEYS[1]);",
            Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 生成序号，格式：{PREFIX} + yyyyMMdd + 6 位自增
     */
    public String generate(String prefix) {
        String noPrefix = prefix + DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATE_PATTERN);
        String key = RedisKeyConstants.NO + noPrefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofDays(1L));
        return noPrefix + String.format("%06d", no);
    }

    /**
     * 生成纯流水号，格式：{PREFIX} + 6 位自增
     */
    public String generatePlain(String prefix) {
        String key = RedisKeyConstants.NO + prefix;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        if (no == null) {
            no = 1L;
        }
        return prefix + String.format("%06d", no);
    }

    /**
     * 生成大于指定流水的纯流水号，用于 Redis 流水落后于数据库已有编号时追平。
     */
    public String generatePlainAfter(String prefix, long minSequence) {
        String key = RedisKeyConstants.NO + prefix;
        Long no = stringRedisTemplate.execute(GENERATE_PLAIN_AFTER_SCRIPT, Collections.singletonList(key),
                String.valueOf(Math.max(0L, minSequence)));
        if (no == null) {
            no = minSequence + 1;
        }
        return prefix + String.format("%06d", no);
    }

    /**
     * 生成月度自增序号，格式：{PREFIX} + "-" + yyyyMM + "-" + 6 位月度自增
     */
    public String generateMonthly(String prefix) {
        return generateMonthly(prefix, YearMonth.now());
    }

    /**
     * 生成月度自增序号，格式：{PREFIX} + "-" + yyyyMM + "-" + 6 位月度自增
     */
    public String generateMonthly(String prefix, YearMonth yearMonth) {
        String ym = yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisKeyConstants.NO + prefix + "-" + ym;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, Duration.ofDays(35L));
        return prefix + "-" + ym + "-" + String.format("%06d", no);
    }

    /**
     * 生成按月流水，格式：{PREFIX} + yyyyMM + 6 位月度自增
     */
    public String generateMonthSequence(String prefix) {
        String ym = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = RedisKeyConstants.NO + prefix + "-" + ym;
        Long no = stringRedisTemplate.opsForValue().increment(key);
        if (no == null) {
            no = 1L;
        }
        stringRedisTemplate.expire(key, Duration.ofDays(35L));
        return prefix + ym + String.format("%06d", no);
    }

}
