package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Versioned high-frequency intent/entity contracts compiled from the Obsidian vault. */
@Component
public class AssistantSemanticKnowledge {

    private static final Pattern PRODUCT_UNIT_PATTERN = Pattern.compile(
            "([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*"
                    + "(摄氏度|毫升|千克|公斤|℃|升|克|度|"
                    + "°C(?![A-Za-z])|mL(?![A-Za-z])|kg(?![A-Za-z])|L(?![A-Za-z])|g(?![A-Za-z]))",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_PRODUCT_UNIT_VARIANTS = 64;

    public static final class Match {
        private final String intentId;
        private final String capabilityId;
        private final String route;
        private final String defaultPeriod;
        private final List<String> entityTypes;
        private final String outcomeStatus;

        Match(String intentId, String capabilityId, String route, String defaultPeriod,
              Collection<String> entityTypes, String outcomeStatus) {
            this.intentId = intentId;
            this.capabilityId = capabilityId;
            this.route = route;
            this.defaultPeriod = defaultPeriod;
            this.entityTypes = Collections.unmodifiableList(new ArrayList<>(entityTypes));
            this.outcomeStatus = outcomeStatus;
        }

        public String getIntentId() { return intentId; }
        public String getCapabilityId() { return capabilityId; }
        public String getRoute() { return route; }
        public String getDefaultPeriod() { return defaultPeriod; }
        public List<String> getEntityTypes() { return entityTypes; }
        public String getOutcomeStatus() { return outcomeStatus; }
        public boolean unverified() { return "CAPABILITY_UNVERIFIED".equals(outcomeStatus); }
    }

    private static final class Card {
        private final String intentId, capability, route, defaultPeriod, verificationStatus;
        private final List<String> aliases;

        Card(JsonNode node) {
            intentId = node.path("intentId").asText();
            capability = node.path("capability").asText();
            route = node.path("route").asText();
            defaultPeriod = node.path("defaultPeriod").asText();
            verificationStatus = node.path("verificationStatus").asText();
            List<String> values = new ArrayList<>();
            for (JsonNode value : node.path("aliases")) values.add(value.asText());
            aliases = Collections.unmodifiableList(values);
        }
    }

    private final Map<String, Card> intents = new LinkedHashMap<>();
    private final String checksum;

    public AssistantSemanticKnowledge() {
        try {
            byte[] bytes;
            try (InputStream input = new ClassPathResource("assistant/semantic-index.json").getInputStream()) {
                bytes = readAll(input);
            }
            checksum = sha256(bytes);
            String expected;
            try (InputStream input = new ClassPathResource("assistant/semantic-index.sha256").getInputStream()) {
                expected = new String(readAll(input), StandardCharsets.US_ASCII).trim().split("\\s+")[0];
            }
            if (!checksum.equalsIgnoreCase(expected)) throw new IllegalStateException("Semantic index checksum mismatch");
            JsonNode root = new ObjectMapper().readTree(bytes);
            if (root.path("schemaVersion").asInt() != 1) throw new IllegalStateException("Unsupported semantic index version");
            for (JsonNode node : root.path("cards")) {
                if (!"INTENT".equals(node.path("cardType").asText())) continue;
                Card card = new Card(node);
                if (card.intentId.isEmpty() || intents.put(card.intentId, card) != null)
                    throw new IllegalStateException("Duplicate semantic intent: " + card.intentId);
            }
            for (String required : Arrays.asList("PRODUCT_PRICE_STOCK", "SALE", "PURCHASE", "RECEIPT", "PAYMENT",
                    "RECEIVABLE", "CAPABILITY_UNVERIFIED"))
                if (!intents.containsKey(required)) throw new IllegalStateException("Missing semantic intent: " + required);
        } catch (Exception error) {
            throw new IllegalStateException("Unable to load assistant semantic index", error);
        }
    }

    public Match match(String question) {
        String text = normalize(question);
        if (text.isEmpty()) return match("NO_INTENT", "NO_INTENT", "REJECT", "NONE", Collections.<String>emptyList(), "CLARIFY");
        if (containsAny(text, "账龄", "跨账套", "其他账套", "毛利", "毛利率", "利润", "利润率",
                "库存金额", "库存成本", "库存货值", "存货金额", "存货成本")) {
            return fromCard("CAPABILITY_UNVERIFIED", entityTypes(text), "CAPABILITY_UNVERIFIED");
        }
        if (containsAny(text, "销售订单", "销售单", "客户订单", "订单客户")
                || text.contains("客户") && containsAny(text,"订单","下单","订货")) {
            return fromCard("SALE_ORDER_RELATION",entityTypes(text),"ROUTED");
        }
        if (containsAny(text, "欠款", "欠钱", "当前应收", "客户余额", "应收余额")) {
            List<String> types = new ArrayList<>();
            if (containsAny(text, "业务员", "销售员", "业务经理")) types.add("SALESPERSON");
            if (!containsAny(text, "哪个客户", "哪些客户", "所有客户", "客户排行")) types.add("CUSTOMER");
            return fromCard("RECEIVABLE", types, "ROUTED");
        }
        if (containsAny(text, "采购额", "采购金额", "采购总金额", "采购总额")) {
            return fromCard("PURCHASE", entityTypes(text), "ROUTED");
        }
        if ((containsAny(text, "收款", "回款") || looksLikeReceiptAmountQuestion(text))
                && !containsAny(text, "应收")) {
            return fromCard("RECEIPT", entityTypes(text), "ROUTED");
        }
        if (containsAny(text, "付款") && !containsAny(text, "应付")) {
            return fromCard("PAYMENT", entityTypes(text), "ROUTED");
        }
        boolean price = containsAny(text, "价格", "报价", "多少钱", "股份价", "零售价", "销售价", "采购价", "批发价", "参考价");
        boolean sale = containsAny(text, "销量", "销售量", "销售情况", "销售了多少", "卖了多少", "销售额", "卖得多", "卖的多");
        if (sale && !price) {
            List<String> types = new ArrayList<>();
            if (containsAny(text, "业务员", "销售员", "业务经理") || looksLikeSalespersonSales(text)) types.add("SALESPERSON");
            if (containsAny(text, "客户") || looksLikeCustomerSales(text)) types.add("CUSTOMER");
            if(containsAny(text,"产品","配件","机油","轮胎","液压油","防冻液","制动液")) types.add("PRODUCT");
            return fromCard("SALE", types, "ROUTED");
        }
        boolean stockOrAvailability = containsAny(text, "库存", "有货", "缺货", "没货");
        if (price || stockOrAvailability || looksLikeBareProduct(text)) {
            List<String> types = new ArrayList<>(Collections.singletonList("PRODUCT"));
            if (text.contains("仓")) types.add("WAREHOUSE");
            return fromCard("PRODUCT_PRICE_STOCK", types, "ROUTED");
        }
        return match("NO_INTENT", "NO_INTENT", "REJECT", "NONE", Collections.<String>emptyList(), "CLARIFY");
    }

    private Match fromCard(String intentId, Collection<String> entityTypes, String outcome) {
        Card card = intents.get(intentId);
        if (card == null) return match(intentId, intentId, "REJECT", "NONE", entityTypes, outcome);
        return match(card.intentId, card.capability, card.route, card.defaultPeriod, entityTypes, outcome);
    }

    private static Match match(String intent, String capability, String route, String period,
                               Collection<String> types, String outcome) {
        return new Match(intent, capability, route, period, new LinkedHashSet<>(types), outcome);
    }

    public String version() { return checksum.substring(0, 16); }

    public List<String> openedIntents() {
        List<String> result = new ArrayList<>();
        for (Card card : intents.values())
            if (!"UNVERIFIED".equals(card.verificationStatus)) result.add(card.intentId);
        return result;
    }

    public List<String> unverifiedCapabilities() {
        List<String> result = new ArrayList<>();
        for (Card card : intents.values())
            if ("UNVERIFIED".equals(card.verificationStatus)) result.addAll(card.aliases);
        return result;
    }

    public static List<String> productKeywordVariants(String raw) {
        if (raw == null) return Collections.emptyList();
        String value = normalize(raw);
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (!value.isEmpty()) result.add(value);
        String compact = value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        java.util.regex.Matcher tire = java.util.regex.Pattern.compile("^(\\d{3})[/\\-]?(\\d{2})R?(\\d{2})(.*)$").matcher(compact);
        if (tire.matches()) {
            result.add(tire.group(1) + "/" + tire.group(2) + "R" + tire.group(3) + tire.group(4));
            result.add(tire.group(1) + " " + tire.group(2) + " " + tire.group(3) + tire.group(4));
        }
        addViscosityVariants(result, value);
        if (!compact.equals(value)) addViscosityVariants(result, compact);
        return new ArrayList<>(result);
    }

    /**
     * Returns assistant-only measurement-unit aliases while retaining the normalized original value.
     * A unit is recognized only when it follows a numeric literal; ordinary text such as "abl" is
     * therefore left untouched. Whitespace between the number and unit is accepted, and Latin units
     * are not recognized when another Latin letter follows them.
     */
    public static List<String> productUnitVariants(String raw) {
        if (raw == null) return Collections.emptyList();
        String value = normalize(raw);
        if (value.isEmpty()) return Collections.emptyList();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.add(value);
        appendProductUnitVariants(value, 0, new StringBuilder(value.length()), result);
        return new ArrayList<>(result);
    }

    /** Removes only whitespace inside recognized numeric-unit expressions before keyword splitting. */
    public static String compactProductUnitSpacing(String raw) {
        if (raw == null) return "";
        String value = normalize(raw);
        Matcher matcher = PRODUCT_UNIT_PATTERN.matcher(value);
        StringBuffer result = new StringBuffer(value.length());
        while (matcher.find()) {
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + matcher.group(2)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Builds an ordered-character pattern while keeping converted numeric-unit aliases atomic.
     * For example, the token {@code 18升 -> 18L} remains {@code 18L}; when its caller joins it
     * after an ordered {@code k40} token, the result is {@code k%4%0%18L}. The generated alias
     * therefore cannot match a name where {@code 1}, {@code 8}, and {@code L} are separated.
     * Numeric units unchanged from the user's input retain the existing character-order fallback.
     */
    public static String productUnitAwareOrderedPattern(String original, String variant) {
        String source = compactProductUnitSpacing(original);
        String candidate = compactProductUnitSpacing(variant);
        if (candidate.isEmpty()) return null;

        Matcher sourceMatcher = PRODUCT_UNIT_PATTERN.matcher(source);
        Matcher candidateMatcher = PRODUCT_UNIT_PATTERN.matcher(candidate);
        List<String> fragments = new ArrayList<>();
        int sourceOffset = 0;
        int candidateOffset = 0;
        while (true) {
            boolean sourceFound = sourceMatcher.find();
            boolean candidateFound = candidateMatcher.find();
            if (sourceFound != candidateFound) return orderedCharactersPattern(candidate);
            if (!sourceFound) break;

            String sourcePrefix = source.substring(sourceOffset, sourceMatcher.start());
            String candidatePrefix = candidate.substring(candidateOffset, candidateMatcher.start());
            if (!sourcePrefix.equals(candidatePrefix)
                    || !sourceMatcher.group(1).equals(candidateMatcher.group(1))) {
                return orderedCharactersPattern(candidate);
            }
            appendOrderedFragments(fragments, candidatePrefix);

            String candidateUnitExpression = candidateMatcher.group(1) + candidateMatcher.group(2);
            if (sourceMatcher.group(2).equals(candidateMatcher.group(2))) {
                appendOrderedFragments(fragments, candidateUnitExpression);
            } else {
                fragments.add(candidateUnitExpression);
            }
            sourceOffset = sourceMatcher.end();
            candidateOffset = candidateMatcher.end();
        }
        String sourceSuffix = source.substring(sourceOffset);
        String candidateSuffix = candidate.substring(candidateOffset);
        if (!sourceSuffix.equals(candidateSuffix)) return orderedCharactersPattern(candidate);
        appendOrderedFragments(fragments, candidateSuffix);
        return String.join("%", fragments);
    }

    private static String orderedCharactersPattern(String value) {
        List<String> fragments = new ArrayList<>();
        appendOrderedFragments(fragments, value);
        return String.join("%", fragments);
    }

    private static void appendOrderedFragments(List<String> fragments, String value) {
        value.codePoints().forEach(codePoint -> fragments.add(new String(Character.toChars(codePoint))));
    }

    private static void appendProductUnitVariants(String value, int offset, StringBuilder current,
                                                  Set<String> result) {
        if (result.size() >= MAX_PRODUCT_UNIT_VARIANTS) return;
        Matcher matcher = PRODUCT_UNIT_PATTERN.matcher(value);
        matcher.region(offset, value.length());
        if (!matcher.find()) {
            int length = current.length();
            current.append(value, offset, value.length());
            result.add(current.toString());
            current.setLength(length);
            return;
        }
        int length = current.length();
        current.append(value, offset, matcher.start()).append(matcher.group(1));
        int numberEnd = current.length();
        for (String alias : productUnitAliases(matcher.group(2))) {
            if (result.size() >= MAX_PRODUCT_UNIT_VARIANTS) break;
            current.setLength(numberEnd);
            current.append(alias);
            appendProductUnitVariants(value, matcher.end(), current, result);
        }
        current.setLength(length);
    }

    private static List<String> productUnitAliases(String unit) {
        String normalized = unit.toLowerCase(Locale.ROOT);
        if ("升".equals(unit) || "l".equals(normalized)) return Arrays.asList("升", "L");
        if ("毫升".equals(unit) || "ml".equals(normalized)) return Arrays.asList("毫升", "mL");
        if ("千克".equals(unit) || "公斤".equals(unit) || "kg".equals(normalized))
            return Arrays.asList("千克", "公斤", "kg");
        if ("克".equals(unit) || "g".equals(normalized)) return Arrays.asList("克", "g");
        if ("摄氏度".equals(unit) || "度".equals(unit) || "℃".equals(unit) || "°c".equals(normalized))
            return Arrays.asList("摄氏度", "度", "℃", "°C");
        return Collections.singletonList(unit);
    }

    private static void addViscosityVariants(Set<String> result, String keyword) {
        java.util.regex.Matcher viscosity = java.util.regex.Pattern.compile("^(.*?)(0|5|10|15|20)[W\\-]?(20|30|40|50|60)(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(keyword);
        if (viscosity.matches()) {
            String prefix = viscosity.group(1), low = viscosity.group(2), high = viscosity.group(3), suffix = viscosity.group(4);
            result.add(prefix + low + "W-" + high + suffix);
            result.add(prefix + low + "W" + high + suffix);
            result.add(prefix + low + "-" + high + suffix);
            result.add(prefix + low + high + suffix);
        }
    }

    public static String normalize(String raw) {
        if (raw == null) return "";
        String value = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
        value = value.replace('，', ',').replace('：', ':').replace('／', '/').replace('－', '-');
        value = value.replaceAll("[\\t\\r\\n]+", " ").replaceAll(" {2,}", " ");
        return value.replaceAll("^[\\s,。；;:：\\\"'“”‘’]+|[\\s,。；;:：\\\"'“”‘’？?！!]+$", "").trim();
    }

    private static List<String> entityTypes(String text) {
        List<String> result = new ArrayList<>();
        if (containsAny(text, "产品", "配件", "商品")) result.add("PRODUCT");
        if (text.contains("客户")) result.add("CUSTOMER");
        if (text.contains("供应商")) result.add("SUPPLIER");
        if (text.contains("仓")) result.add("WAREHOUSE");
        if (containsAny(text, "业务员", "销售员", "业务经理")) result.add("SALESPERSON");
        if (containsAny(text, "采购员", "采购人员", "采购经办人")) result.add("PURCHASER");
        return result;
    }

    private static boolean looksLikeBareProduct(String text) {
        if (text.length() < 2 || text.length() > 80 || text.matches(".*(?:怎么|为什么|查询|查看|帮我|哪个|哪些|多少|情况|"
                + "谁|什么|如何|工作|负责|分管|部门|人员|协作|协同|职责).*$")) return false;
        if (AssistantKnowledgeContext.containsOrganizationLanguage(text)
                || AssistantKnowledgeContext.isOrganizationKnowledgeQuestion(text)) return false;
        return text.matches("[\\p{L}\\p{N}_./\\-·*+# ]+");
    }

    private static boolean looksLikeSalespersonSales(String text) {
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("^([\\p{IsHan}]{2,4})(?:" + AssistantRelativePeriod.FIXED_PERIOD_REGEX + "|\\d{1,2}月份?).*(?:销量|销售量|销售情况)$").matcher(text);
        if(!matcher.matches()) return false;
        String prefix=matcher.group(1);
        return !containsAny(prefix,"机油","轮胎","液压油","防冻液","制动液","汽修","汽配","公司","门市");
    }

    private static boolean looksLikeCustomerSales(String text) {
        return text.matches(".*(?:公司|汽修|汽配|门市|修理厂|服务站).*(?:销量|销售量|销售情况|销售了多少|卖了多少).*");
    }

    private static boolean looksLikeReceiptAmountQuestion(String text) {
        return text.matches(".*(?:收了|收回)(?:多少钱|多少款|多少)(?:回来)?.*");
    }

    private static boolean containsAny(String value, String... choices) {
        for (String choice : choices) if (value.contains(choice)) return true;
        return false;
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int length;
        while ((length = input.read(buffer)) != -1) out.write(buffer, 0, length);
        return out.toByteArray();
    }

    private static String sha256(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format("%02x", value));
        return result.toString();
    }
}
