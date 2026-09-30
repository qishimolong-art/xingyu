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

/** Versioned high-frequency intent/entity contracts compiled from the Obsidian vault. */
@Component
public class AssistantSemanticKnowledge {

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
        if (containsAny(text, "收款", "回款") && !containsAny(text, "应收")) {
            return fromCard("RECEIPT", entityTypes(text), "ROUTED");
        }
        if (containsAny(text, "付款") && !containsAny(text, "应付")) {
            return fromCard("PAYMENT", entityTypes(text), "ROUTED");
        }
        boolean price = containsAny(text, "价格", "报价", "多少钱", "股份价", "零售价", "销售价", "采购价", "批发价", "参考价");
        boolean sale = containsAny(text, "销量", "销售量", "销售情况", "卖了多少", "销售额", "卖得多", "卖的多");
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
        java.util.regex.Matcher viscosity = java.util.regex.Pattern.compile("^(.*?)(0|5|10|15|20)[W\\-]?(20|30|40|50|60)(.*)$", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(compact);
        if (viscosity.matches()) {
            String prefix = viscosity.group(1), low = viscosity.group(2), high = viscosity.group(3), suffix = viscosity.group(4);
            result.add(prefix + low + "W-" + high + suffix);
            result.add(prefix + low + "W" + high + suffix);
            result.add(prefix + low + "-" + high + suffix);
            result.add(prefix + low + high + suffix);
        }
        return new ArrayList<>(result);
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
        return result;
    }

    private static boolean looksLikeBareProduct(String text) {
        if (text.length() < 2 || text.length() > 80 || text.matches(".*(?:怎么|为什么|查询|查看|帮我|哪个|哪些|多少|情况).*$")) return false;
        return text.matches("[\\p{L}\\p{N}_./\\-·*+# ]+");
    }

    private static boolean looksLikeSalespersonSales(String text) {
        java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("^([\\p{IsHan}]{2,4})(?:今年|本年|\\d{1,2}月份?|本月|上月|本周|上周).*(?:销量|销售量|销售情况)$").matcher(text);
        if(!matcher.matches()) return false;
        String prefix=matcher.group(1);
        return !containsAny(prefix,"机油","轮胎","液压油","防冻液","制动液","汽修","汽配","公司","门市");
    }

    private static boolean looksLikeCustomerSales(String text) {
        return text.matches(".*(?:公司|汽修|汽配|门市|修理厂|服务站).*(?:销量|销售量|销售情况|卖了多少).*");
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
