package cn.iocoder.yudao.module.erp.service.common;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * ERP archive mnemonic-code utilities.
 */
@Slf4j
public final class ErpMnemonicCodeUtils {

    private static final int MAX_PINYIN_CODE_LENGTH = 64;
    private static final int MAX_WUBI_CODE_LENGTH = 32;
    private static final Collator PINYIN_COLLATOR = Collator.getInstance(Locale.CHINA);
    private static final HanyuPinyinOutputFormat PINYIN_OUTPUT_FORMAT = buildPinyinOutputFormat();
    private static final Boundary[] PINYIN_BOUNDARIES = new Boundary[]{
            new Boundary("A", "啊"),
            new Boundary("B", "芭"),
            new Boundary("C", "擦"),
            new Boundary("D", "搭"),
            new Boundary("E", "蛾"),
            new Boundary("F", "发"),
            new Boundary("G", "噶"),
            new Boundary("H", "哈"),
            new Boundary("J", "击"),
            new Boundary("K", "喀"),
            new Boundary("L", "垃"),
            new Boundary("M", "妈"),
            new Boundary("N", "拿"),
            new Boundary("O", "哦"),
            new Boundary("P", "啪"),
            new Boundary("Q", "期"),
            new Boundary("R", "然"),
            new Boundary("S", "撒"),
            new Boundary("T", "塌"),
            new Boundary("W", "挖"),
            new Boundary("X", "昔"),
            new Boundary("Y", "压"),
            new Boundary("Z", "匝"),
    };
    private static final Map<Integer, String> WUBI_INITIALS = loadWubiInitials();

    private ErpMnemonicCodeUtils() {
    }

    public static String buildPinyinCode(String name) {
        return buildCode(name, ErpMnemonicCodeUtils::resolvePinyinInitial, MAX_PINYIN_CODE_LENGTH);
    }

    static String buildLegacyPinyinCode(String name) {
        return buildCode(name, ErpMnemonicCodeUtils::resolveLegacyPinyinInitial, MAX_PINYIN_CODE_LENGTH);
    }

    public static String buildWubiCode(String name) {
        return buildCode(name, codePoint -> WUBI_INITIALS.getOrDefault(codePoint, ""), MAX_WUBI_CODE_LENGTH);
    }

    private static String buildCode(String name, InitialResolver resolver, int maxLength) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(maxLength);
        name.trim().codePoints().forEach(codePoint -> {
            if (builder.length() >= maxLength) {
                return;
            }
            if (isAsciiLetterOrDigit(codePoint)) {
                builder.appendCodePoint(Character.toUpperCase(codePoint));
                return;
            }
            if (isIgnoredCodePoint(codePoint)) {
                return;
            }
            String initial = resolver.resolve(codePoint);
            if (StringUtils.hasText(initial)) {
                builder.append(initial);
            }
        });
        return builder.length() <= maxLength
                ? builder.toString()
                : builder.substring(0, maxLength);
    }

    private static String resolvePinyinInitial(int codePoint) {
        if (!isHanCodePoint(codePoint) || !Character.isBmpCodePoint(codePoint)) {
            return "";
        }
        try {
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray((char) codePoint, PINYIN_OUTPUT_FORMAT);
            if (pinyinArray != null && pinyinArray.length > 0 && StringUtils.hasText(pinyinArray[0])) {
                return pinyinArray[0].substring(0, 1).toUpperCase(Locale.ROOT);
            }
        } catch (BadHanyuPinyinOutputFormatCombination ex) {
            log.warn("Failed to resolve ERP pinyin initial for code point: {}", Integer.toHexString(codePoint), ex);
        }
        return resolveLegacyPinyinInitial(codePoint);
    }

    private static String resolveLegacyPinyinInitial(int codePoint) {
        if (!isHanCodePoint(codePoint)) {
            return "";
        }
        String value = new String(Character.toChars(codePoint));
        for (int i = PINYIN_BOUNDARIES.length - 1; i >= 0; i--) {
            Boundary boundary = PINYIN_BOUNDARIES[i];
            if (PINYIN_COLLATOR.compare(value, boundary.boundary) >= 0) {
                return boundary.initial;
            }
        }
        return "";
    }

    private static HanyuPinyinOutputFormat buildPinyinOutputFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);
        return format;
    }

    private static boolean isAsciiLetterOrDigit(int codePoint) {
        return (codePoint >= 'A' && codePoint <= 'Z')
                || (codePoint >= 'a' && codePoint <= 'z')
                || (codePoint >= '0' && codePoint <= '9');
    }

    private static boolean isIgnoredCodePoint(int codePoint) {
        return Character.isWhitespace(codePoint)
                || codePoint == '-'
                || codePoint == '_'
                || codePoint == '.';
    }

    private static boolean isHanCodePoint(int codePoint) {
        return codePoint >= 0x3400 && codePoint <= 0x9fff;
    }

    private static Map<Integer, String> loadWubiInitials() {
        Properties properties = new Properties();
        try (InputStream input = new ClassPathResource("erp/wubi86-initials.properties").getInputStream()) {
            properties.load(input);
        } catch (IOException ex) {
            log.warn("Failed to load ERP Wubi code resource", ex);
            return Collections.emptyMap();
        }
        Map<Integer, String> map = new TreeMap<>();
        for (String key : properties.stringPropertyNames()) {
            try {
                map.put(Integer.parseInt(key, 16), properties.getProperty(key));
            } catch (NumberFormatException ex) {
                log.warn("Skipped invalid ERP Wubi code point: {}", key);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    @FunctionalInterface
    private interface InitialResolver {
        String resolve(int codePoint);
    }

    private static final class Boundary {
        private final String initial;
        private final String boundary;

        private Boundary(String initial, String boundary) {
            this.initial = initial;
            this.boundary = boundary;
        }
    }

}
