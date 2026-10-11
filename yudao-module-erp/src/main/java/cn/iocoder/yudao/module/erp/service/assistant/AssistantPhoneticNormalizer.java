package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import javax.annotation.Resource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能问数受控同音/近音归一化器。
 *
 * <p>只自动改写词表或已发布知识元数据中的完整汉字词，不改写单字、编码、数字、日期、规格和英文。
 * 业务实体名称仍由 {@link AssistantEntityResolver} 在实时权限范围内解析。</p>
 */
@Component
@Slf4j
public class AssistantPhoneticNormalizer {

    private static final Pattern HAN_RUN = Pattern.compile("[\\p{IsHan}]{2,}");
    private static final Pattern ROLLING_DAY_TYPO = Pattern.compile(
            "((?:最近|过去|近)\\s*[0-9零〇一二两三四五六七八九十百]+\\s*)填");
    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = pinyinFormat();
    private static final ThreadLocal<RuntimeState> RUNTIME = new ThreadLocal<>();

    @Resource private AssistantProperties properties;
    @Resource private AssistantCompanyKnowledge companyKnowledge;

    private final List<Term> controlledTerms = new ArrayList<>();
    private final String version;
    private final String unavailableReason;
    private volatile String dynamicKnowledgeVersion;
    private volatile List<Term> dynamicKnowledgeTerms = Collections.emptyList();

    public AssistantPhoneticNormalizer() {
        this(loadClasspath());
    }

    AssistantPhoneticNormalizer(AssistantProperties properties, byte[] bytes, String expectedChecksum) {
        this(load(bytes, expectedChecksum));
        this.properties = properties;
    }

    private AssistantPhoneticNormalizer(Loaded loaded) {
        controlledTerms.addAll(loaded.terms);
        version = loaded.version;
        unavailableReason = loaded.error;
    }

    @Data
    public static class Normalization {
        private String originalQuestion;
        private String normalizedQuestion;
        private List<Map<String, Object>> corrections = new ArrayList<>();
        private List<String> ambiguousChoices = new ArrayList<>();

        public boolean needsClarification() {
            return !ambiguousChoices.isEmpty();
        }

        public Map<String, Object> clarification() {
            Map<String, Object> response = AssistantClarification.missing(
                    "PHONETIC_AMBIGUOUS", "检测到同音或近音词存在多种解释，请选择本次要表达的词语",
                    Collections.singletonList("phoneticTerm"), ambiguousChoices);
            response.put("originalQuestion", originalQuestion);
            response.put("normalizedQuestion", normalizedQuestion);
            return response;
        }
    }

    @Data
    @AllArgsConstructor
    public static class EntityMatch {
        private String matchType;
        private int rank;
        private int confidence;
    }

    public boolean enabled() {
        return properties != null && properties.getPhonetic().isEnabled() && unavailableReason == null;
    }

    public String version() {
        return version;
    }

    public String unavailableReason() {
        return unavailableReason;
    }

    public List<String> supportedEntityTypes() {
        return Arrays.asList("PRODUCT", "CUSTOMER", "SUPPLIER");
    }

    public Normalization normalize(String rawQuestion) {
        Normalization result = new Normalization();
        String original = rawQuestion == null ? "" : rawQuestion;
        String normalized = Normalizer.normalize(original, Normalizer.Form.NFKC);
        result.setOriginalQuestion(original);
        result.setNormalizedQuestion(normalized);
        if (!enabled() || normalized.trim().isEmpty()) return result;

        int maxCorrections = Math.max(1, Math.min(8, properties.getPhonetic().getMaxCorrections()));
        normalized = normalizeRollingDayTypo(normalized, result, maxCorrections);
        result.setNormalizedQuestion(normalized);

        List<Term> terms = allTerms();
        if (terms.isEmpty()) return result;
        List<Replacement> candidates = replacements(normalized, terms);
        if (candidates.isEmpty()) return result;

        List<Replacement> selected = new ArrayList<>();
        Set<String> ambiguity = new LinkedHashSet<>();
        boolean[] occupied = new boolean[normalized.length()];
        for (Replacement candidate : candidates) {
            if (overlaps(occupied, candidate.start, candidate.end)) continue;
            List<Replacement> tied = tied(candidates, candidate);
            String explicit = explicitChoice(normalized, tied);
            if (tied.size() > 1 && explicit == null) {
                for (Replacement value : tied) ambiguity.add(value.term.canonical);
                mark(occupied, candidate.start, candidate.end);
                continue;
            }
            Replacement chosen = explicit == null ? candidate : tied.stream()
                    .filter(value -> explicit.equals(value.term.canonical)).findFirst().orElse(candidate);
            selected.add(chosen);
            mark(occupied, chosen.start, chosen.end);
        }
        result.setAmbiguousChoices(new ArrayList<>(ambiguity));
        int remainingCorrections = Math.max(0, maxCorrections - result.getCorrections().size());
        if (selected.size() > remainingCorrections)
            selected = new ArrayList<>(selected.subList(0, remainingCorrections));
        selected.sort(Comparator.comparingInt((Replacement value) -> value.start).reversed());
        StringBuilder value = new StringBuilder(normalized);
        for (Replacement replacement : selected) {
            String source = value.substring(replacement.start, replacement.end);
            value.replace(replacement.start, replacement.end, replacement.term.canonical);
            result.getCorrections().add(correction(source, replacement.term.canonical,
                    replacement.matchType, replacement.confidence, replacement.term.category,
                    "CONTROLLED_LEXICON"));
        }
        Collections.reverse(result.getCorrections());
        result.setNormalizedQuestion(value.toString());
        return result;
    }

    private static String normalizeRollingDayTypo(String value, Normalization result, int limit) {
        Matcher matcher = ROLLING_DAY_TYPO.matcher(value);
        StringBuffer normalized = new StringBuffer();
        int count = 0;
        while (matcher.find() && count < limit) {
            matcher.appendReplacement(normalized, Matcher.quoteReplacement(matcher.group(1) + "天"));
            result.getCorrections().add(correction("填", "天", "PHONETIC", 100,
                    "TIME_UNIT", "CONTROLLED_TEMPORAL_PATTERN"));
            count++;
        }
        matcher.appendTail(normalized);
        return normalized.toString();
    }

    public void begin(Normalization normalization) {
        RUNTIME.set(new RuntimeState(normalization == null ? identity("") : normalization));
    }

    public void clear() {
        RUNTIME.remove();
    }

    public void recordEntityCorrection(String original, Map<String, Object> selected) {
        RuntimeState state = RUNTIME.get();
        if (state == null || selected == null || original == null) return;
        Object matchType = selected.get("matchType");
        if (matchType == null || !String.valueOf(matchType).contains("PHONETIC")) return;
        Object name = selected.get("name");
        if (name == null || String.valueOf(name).trim().isEmpty()) return;
        int confidence = selected.get("phoneticConfidence") instanceof Number
                ? ((Number) selected.get("phoneticConfidence")).intValue() : 90;
        state.add(correction(original, String.valueOf(name), String.valueOf(matchType), confidence,
                String.valueOf(selected.get("entityType")), "ENTITY_MASTER_DATA"));
    }

    public void decorateResult(Map<String, Object> result) {
        RuntimeState state = RUNTIME.get();
        if (result == null || state == null || state.corrections.isEmpty()) return;
        result.put("normalizedQuestion", state.normalizedQuestion);
        result.put("corrections", new ArrayList<>(state.corrections));
        result.put("correctionNotice", correctionNotice(state.corrections));
    }

    public void decorateClarification(Map<String, Object> response) {
        if (response == null) return;
        RuntimeState state = RUNTIME.get();
        if (state != null && !state.corrections.isEmpty()) {
            String notice = correctionNotice(state.corrections);
            response.put("normalizedQuestion", state.normalizedQuestion);
            response.put("corrections", new ArrayList<>(state.corrections));
            response.put("correctionNotice", notice);
            Object text = response.get("clarification");
            if (text != null && !String.valueOf(text).contains(notice)) {
                response.put("clarification", notice + "；" + text);
            }
        }
    }

    public void decorateEnvelope(AssistantExecutionEnvelope envelope) {
        RuntimeState state = RUNTIME.get();
        if (envelope == null || state == null) return;
        envelope.setOriginalQuestion(state.originalQuestion);
        envelope.setNormalizedQuestion(state.normalizedQuestion);
        envelope.setCorrections(new ArrayList<>(state.corrections));
        if (!state.corrections.isEmpty()) {
            envelope.getResultMetadata().put("correctionNotice", correctionNotice(state.corrections));
            envelope.getResultMetadata().put("corrections", new ArrayList<>(state.corrections));
        }
    }

    public void decorateSemanticRequest(AssistantSemanticRequest request) {
        RuntimeState state = RUNTIME.get();
        if (request == null || state == null) return;
        request.setOriginalQuestion(state.originalQuestion);
        request.setNormalizedQuestion(state.normalizedQuestion);
        request.setPhoneticCorrections(new ArrayList<>(state.corrections));
    }

    public String entityChoiceText(String original, Collection<Map<String, Object>> candidates, String fallback) {
        if (candidates == null || candidates.isEmpty()) return fallback;
        boolean phonetic = true;
        boolean near = false;
        for (Map<String, Object> candidate : candidates) {
            String type = String.valueOf(candidate.get("matchType"));
            phonetic &= type.contains("PHONETIC");
            near |= type.contains("NEAR");
        }
        if (!phonetic) return fallback;
        return "已按" + (near ? "常见近音" : "同音") + "识别“" + original + "”，匹配到多个有权限的业务对象，请选择";
    }

    public List<String> searchInitialCodes(String keyword) {
        if (!enabled() || !eligibleEntityKeyword(keyword)) return Collections.emptyList();
        String code = cn.iocoder.yudao.module.erp.service.common.ErpMnemonicCodeUtils.buildPinyinCode(keyword);
        if (code.length() < 2) return Collections.emptyList();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(code);
        if (properties.getPhonetic().isNearSoundEnabled()) expandInitials(code, 0, new StringBuilder(), result,
                Math.max(1, Math.min(16, properties.getPhonetic().getMaxInitialVariants())));
        return new ArrayList<>(result);
    }

    public EntityMatch matchEntity(String keyword, String candidateName) {
        if (!enabled() || !eligibleEntityKeyword(keyword) || candidateName == null) return null;
        String query = pinyin(keyword, false);
        String candidate = pinyin(candidateName, false);
        int relation = relation(query, candidate);
        if (relation >= 0) return new EntityMatch("PHONETIC", relation, relation == 0 ? 100 : relation == 1 ? 96 : 92);
        if (!properties.getPhonetic().isNearSoundEnabled()) return null;
        query = pinyin(keyword, true);
        candidate = pinyin(candidateName, true);
        relation = relation(query, candidate);
        return relation < 0 ? null : new EntityMatch("NEAR_PHONETIC", relation + 3,
                relation == 0 ? 86 : relation == 1 ? 82 : 78);
    }

    public static String pinyin(String value, boolean nearSound) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        Normalizer.normalize(value, Normalizer.Form.NFKC).codePoints().forEach(codePoint -> {
            if (isHan(codePoint) && Character.isBmpCodePoint(codePoint)) {
                try {
                    String[] values = PinyinHelper.toHanyuPinyinStringArray((char) codePoint, PINYIN_FORMAT);
                    if (values != null && values.length > 0 && values[0] != null) {
                        result.append(nearSound ? near(values[0]) : values[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    // The static output format is valid; an unconvertible character is simply skipped.
                }
            } else if (Character.isLetterOrDigit(codePoint)) {
                result.appendCodePoint(Character.toLowerCase(codePoint));
            }
        });
        return result.toString();
    }

    private List<Term> allTerms() {
        LinkedHashMap<String, Term> values = new LinkedHashMap<>();
        for (Term term : controlledTerms) values.put(term.canonical, term);
        for (Term term : knowledgeTerms()) values.putIfAbsent(term.canonical, term);
        List<Term> result = new ArrayList<>(values.values());
        result.sort(Comparator.comparingInt((Term value) -> value.canonical.length()).reversed()
                .thenComparing(value -> value.canonical));
        return result;
    }

    private List<Term> knowledgeTerms() {
        if (companyKnowledge == null || !companyKnowledge.available()) return Collections.emptyList();
        String current = companyKnowledge.version();
        List<Term> cached = dynamicKnowledgeTerms;
        if (Objects.equals(current, dynamicKnowledgeVersion)) return cached;
        synchronized (this) {
            if (!Objects.equals(current, dynamicKnowledgeVersion)) {
                List<Term> rebuilt = new ArrayList<>();
                for (String value : companyKnowledge.phoneticTerms()) {
                    if (isHanWord(value) && value.length() >= 3 && value.length() <= 12) {
                        rebuilt.add(new Term(value, "KNOWLEDGE", Collections.emptyList(), true));
                    }
                }
                dynamicKnowledgeTerms = Collections.unmodifiableList(rebuilt);
                dynamicKnowledgeVersion = current;
            }
            return dynamicKnowledgeTerms;
        }
    }

    private List<Replacement> replacements(String question, List<Term> terms) {
        List<Replacement> result = new ArrayList<>();
        Set<String> canonical = new HashSet<>();
        for (Term term : terms) canonical.add(term.canonical);
        for (Term term : terms) {
            for (String alias : term.aliases) {
                if (alias == null || alias.length() < 2 || alias.equals(term.canonical)) continue;
                for (int offset = question.indexOf(alias); offset >= 0; offset = question.indexOf(alias, offset + 1)) {
                    result.add(new Replacement(offset, offset + alias.length(), term, "ALIAS", 100, 0));
                }
            }
        }
        Matcher runs = HAN_RUN.matcher(question);
        while (runs.find()) {
            String run = runs.group();
            for (Term term : terms) {
                int length = term.canonical.length();
                if (length < 3 || length > run.length() || !isHanWord(term.canonical)) continue;
                String strict = pinyin(term.canonical, false);
                String near = term.allowNearSound ? pinyin(term.canonical, true) : null;
                for (int offset = 0; offset + length <= run.length(); offset++) {
                    String source = run.substring(offset, offset + length);
                    if (canonical.contains(source) || source.equals(term.canonical)) continue;
                    int start = runs.start() + offset;
                    if (strict.equals(pinyin(source, false))) {
                        result.add(new Replacement(start, start + length, term, "HOMOPHONE", 96, 1));
                    } else if (properties.getPhonetic().isNearSoundEnabled() && term.allowNearSound
                            && near.equals(pinyin(source, true))) {
                        result.add(new Replacement(start, start + length, term, "NEAR_SOUND", 82, 2));
                    }
                }
            }
        }
        result.sort(Comparator.comparingInt((Replacement value) -> value.start)
                .thenComparing((Replacement value) -> value.end - value.start, Comparator.reverseOrder())
                .thenComparingInt(value -> value.priority)
                .thenComparing(value -> value.term.canonical));
        return result;
    }

    private static List<Replacement> tied(List<Replacement> values, Replacement expected) {
        List<Replacement> result = new ArrayList<>();
        for (Replacement value : values) {
            if (value.start == expected.start && value.end == expected.end && value.priority == expected.priority) {
                boolean duplicate = result.stream().anyMatch(item -> item.term.canonical.equals(value.term.canonical));
                if (!duplicate) result.add(value);
            }
        }
        return result;
    }

    private static String explicitChoice(String question, List<Replacement> tied) {
        if (tied.size() < 2 || !question.contains("补充：")) return null;
        for (Replacement value : tied) if (question.contains("补充：" + value.term.canonical)
                || question.contains("补充：按“" + value.term.canonical + "”理解")) return value.term.canonical;
        return null;
    }

    private static int relation(String query, String candidate) {
        if (query.length() < 2 || candidate.length() < 2) return -1;
        if (candidate.equals(query)) return 0;
        if (candidate.startsWith(query)) return 1;
        return candidate.contains(query) ? 2 : -1;
    }

    private static boolean eligibleEntityKeyword(String value) {
        if (value == null || value.length() > 80) return false;
        int han = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            if (isHan(codePoint)) han++;
            offset += Character.charCount(codePoint);
        }
        return han >= 2;
    }

    private static boolean isHanWord(String value) {
        return value != null && !value.isEmpty() && value.codePoints().allMatch(AssistantPhoneticNormalizer::isHan);
    }

    private static boolean isHan(int codePoint) {
        return codePoint >= 0x3400 && codePoint <= 0x9fff;
    }

    private static String near(String syllable) {
        String value = syllable.toLowerCase(Locale.ROOT);
        if (value.startsWith("zh")) value = "z" + value.substring(2);
        else if (value.startsWith("ch")) value = "c" + value.substring(2);
        else if (value.startsWith("sh")) value = "s" + value.substring(2);
        if (value.startsWith("l")) value = "n" + value.substring(1);
        if (value.startsWith("h")) value = "f" + value.substring(1);
        value = value.replaceFirst("eng$", "en").replaceFirst("ing$", "in").replaceFirst("ang$", "an");
        return value;
    }

    private static void expandInitials(String code, int offset, StringBuilder value, Set<String> result, int limit) {
        if (result.size() >= limit) return;
        if (offset >= code.length()) {
            result.add(value.toString());
            return;
        }
        char current = code.charAt(offset);
        char alternative = current == 'N' ? 'L' : current == 'L' ? 'N'
                : current == 'F' ? 'H' : current == 'H' ? 'F' : 0;
        value.append(current);
        expandInitials(code, offset + 1, value, result, limit);
        value.setLength(value.length() - 1);
        if (alternative != 0 && result.size() < limit) {
            value.append(alternative);
            expandInitials(code, offset + 1, value, result, limit);
            value.setLength(value.length() - 1);
        }
    }

    private static Map<String, Object> correction(String original, String corrected, String matchType,
                                                   int confidence, String category, String source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("original", original);
        result.put("corrected", corrected);
        result.put("matchType", matchType);
        result.put("confidence", confidence);
        result.put("category", category);
        result.put("source", source);
        return result;
    }

    private static String correctionNotice(List<Map<String, Object>> corrections) {
        List<String> values = new ArrayList<>();
        for (Map<String, Object> correction : corrections) {
            values.add("“" + correction.get("original") + "”→“" + correction.get("corrected") + "”");
        }
        return "已按同音/近音识别：" + String.join("，", values);
    }

    private static Normalization identity(String question) {
        Normalization value = new Normalization();
        value.setOriginalQuestion(question);
        value.setNormalizedQuestion(question);
        return value;
    }

    private static boolean overlaps(boolean[] occupied, int start, int end) {
        for (int index = start; index < end; index++) if (occupied[index]) return true;
        return false;
    }

    private static void mark(boolean[] occupied, int start, int end) {
        for (int index = start; index < end; index++) occupied[index] = true;
    }

    private static HanyuPinyinOutputFormat pinyinFormat() {
        HanyuPinyinOutputFormat value = new HanyuPinyinOutputFormat();
        value.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        value.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        value.setVCharType(HanyuPinyinVCharType.WITH_V);
        return value;
    }

    private static Loaded loadClasspath() {
        try {
            byte[] bytes;
            try (InputStream input = new ClassPathResource("assistant/phonetic-lexicon.json").getInputStream()) {
                bytes = StreamUtils.copyToByteArray(input);
            }
            String checksum;
            try (InputStream input = new ClassPathResource("assistant/phonetic-lexicon.sha256").getInputStream()) {
                checksum = StreamUtils.copyToString(input, StandardCharsets.US_ASCII).trim();
            }
            return load(bytes, checksum);
        } catch (Exception error) {
            log.error("Assistant phonetic lexicon is unavailable: {}", error.getMessage());
            return new Loaded(Collections.emptyList(), null, "同音词表无法读取");
        }
    }

    private static Loaded load(byte[] bytes, String expectedChecksum) {
        try {
            if (!sha256(bytes).equalsIgnoreCase(expectedChecksum)) {
                throw new IllegalStateException("Assistant phonetic lexicon checksum mismatch");
            }
            JsonNode root = new ObjectMapper().readTree(bytes);
            if (root.path("schemaVersion").asInt() != 1) throw new IllegalStateException("Unsupported phonetic lexicon");
            String version = root.path("version").asText();
            List<Term> terms = new ArrayList<>();
            Set<String> canonicals = new HashSet<>();
            for (JsonNode node : root.path("terms")) {
                String canonical = node.path("canonical").asText().trim();
                String category = node.path("category").asText("BUSINESS").trim();
                if (canonical.length() < 2 || !canonicals.add(canonical)) {
                    throw new IllegalStateException("Invalid or duplicate phonetic term");
                }
                List<String> aliases = new ArrayList<>();
                for (JsonNode alias : node.path("aliases")) if (!alias.asText().trim().isEmpty()) aliases.add(alias.asText().trim());
                terms.add(new Term(canonical, category, aliases, node.path("allowNearSound").asBoolean(false)));
            }
            if (version.isEmpty() || terms.isEmpty()) throw new IllegalStateException("Empty phonetic lexicon");
            return new Loaded(Collections.unmodifiableList(terms), version, null);
        } catch (Exception error) {
            log.error("Assistant phonetic lexicon validation failed: {}", error.getMessage());
            return new Loaded(Collections.emptyList(), null, "同音词表校验失败");
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static final class Term {
        final String canonical;
        final String category;
        final List<String> aliases;
        final boolean allowNearSound;

        Term(String canonical, String category, List<String> aliases, boolean allowNearSound) {
            this.canonical = canonical;
            this.category = category;
            this.aliases = aliases == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(aliases));
            this.allowNearSound = allowNearSound;
        }
    }

    @AllArgsConstructor
    private static final class Loaded {
        final List<Term> terms;
        final String version;
        final String error;
    }

    @AllArgsConstructor
    private static final class Replacement {
        final int start;
        final int end;
        final Term term;
        final String matchType;
        final int confidence;
        final int priority;
    }

    private static final class RuntimeState {
        final String originalQuestion;
        final String normalizedQuestion;
        final List<Map<String, Object>> corrections = new ArrayList<>();
        final Set<String> keys = new LinkedHashSet<>();

        RuntimeState(Normalization normalization) {
            originalQuestion = normalization.getOriginalQuestion();
            normalizedQuestion = normalization.getNormalizedQuestion();
            for (Map<String, Object> correction : normalization.getCorrections()) add(correction);
        }

        void add(Map<String, Object> correction) {
            String key = correction.get("original") + "\u0000" + correction.get("corrected") + "\u0000" + correction.get("source");
            if (keys.add(key)) corrections.add(new LinkedHashMap<>(correction));
        }
    }
}
