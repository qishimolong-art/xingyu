package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 将已脱敏、已授权的问数事件转换为企业微信 Markdown。 */
@Component
public class AssistantWeComResultFormatter {

    private static final int MAX_BYTES = 20_480;
    private static final int SAFE_BYTES = 19_500;
    private static final Set<String> SAFE_DEFAULT_COLUMNS = new LinkedHashSet<>(Arrays.asList(
            "label", "amount", "unit", "period", "date", "document_count", "record_count",
            "total_amount", "total_quantity", "average_amount", "maximum_amount", "minimum_amount",
            "fulfilled_quantity", "return_quantity", "remaining_quantity", "unitRank", "unit_rank"));
    private static final Set<String> FORBIDDEN_COLUMNS = new HashSet<>(Arrays.asList(
            "sql", "parameters", "params", "trace", "audit", "audit_trace", "raw_data",
            "creator", "updater", "password", "token", "secret", "deleted"));

    public String result(Map<String, Object> result, String explanation) {
        StringBuilder out = new StringBuilder();
        Object presentation = result == null ? null : result.get("presentation");
        if (presentation instanceof Map) {
            Object headline = ((Map<?, ?>) presentation).get("headline");
            if (headline != null) out.append("### ").append(escape(headline)).append("\n\n");
        }
        List<Map<String, Object>> summary = rows(result == null ? null : result.get("summary"));
        List<Map<String, Object>> detail = rows(result == null ? null : result.get("rows"));
        if (summary.isEmpty() && detail.isEmpty()) {
            out.append("未查询到当前权限范围内符合条件的数据。\n");
        } else {
            out.append("**查询结果**\n\n");
            if (!summary.isEmpty()) appendTable(out, summary, result, 10);
            if (!detail.isEmpty()) appendTable(out, detail, result, 10);
        }
        if (explanation != null && !explanation.trim().isEmpty()) {
            out.append("\n**模型解释与统计口径**\n\n").append(escape(explanation)).append("\n");
        } else {
            out.append("\n**统计口径**：按 ERP 智能问数已发布指标及当前实时权限计算。\n");
        }
        String queriedAt = result == null ? null : string(result.get("queriedAt"));
        if (queriedAt == null) queriedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        out.append("\n查询时间：").append(escape(queriedAt));
        out.append("\n权限范围：").append(escape(result == null ? "当前 ERP 用户实时权限" : result.get("scope")));
        if (summary.size() > 10 || detail.size() > 10) {
            out.append("\n\n仅展示前 10 行，请前往 ERP 智能问数页面查看完整结果。");
        }
        return truncate(out.toString());
    }

    public ChoiceResult clarification(Object value) {
        String text = "需要补充信息后才能继续查询";
        String field = null;
        List<Long> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (value instanceof AssistantPlan) {
            AssistantPlan plan = (AssistantPlan) value;
            if (plan.getClarification() != null) text = plan.getClarification();
            field = plan.getPendingField();
            if (plan.getPendingIds() != null) ids.addAll(plan.getPendingIds());
            if (plan.getChoices() != null) labels.addAll(plan.getChoices());
        } else if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.get("clarification") != null) text = String.valueOf(map.get("clarification"));
            if (map.get("field") != null) field = String.valueOf(map.get("field"));
            Object candidates = map.get("candidates");
            if (candidates instanceof Collection) {
                for (Object candidate : (Collection<?>) candidates) {
                    if (!(candidate instanceof Map)) continue;
                    Map<?, ?> item = (Map<?, ?>) candidate;
                    Long id = longValue(item.get("id"));
                    if (id == null) continue;
                    ids.add(id);
                    Object label = item.get("label");
                    if (label == null) label = item.get("name");
                    labels.add(label == null ? String.valueOf(id) : String.valueOf(label));
                }
            }
        }
        StringBuilder content = new StringBuilder(escape(text));
        int count = Math.min(ids.size(), labels.size());
        for (int index = 0; index < count; index++) {
            content.append("\n").append(index + 1).append(". ").append(escape(labels.get(index)));
        }
        if (count > 0) content.append("\n\n请回复对应编号。");
        return new ChoiceResult(truncate(content.toString()), field, ids.subList(0, count));
    }

    public String error(String text) {
        return truncate(text == null || text.trim().isEmpty() ? "查询未完成，请稍后重试。" : escape(text));
    }

    private void appendTable(StringBuilder out, List<Map<String, Object>> rows,
                             Map<String, Object> result, int limit) {
        List<Column> columns = columns(result, rows);
        if (columns.isEmpty()) return;
        out.append("|");
        columns.forEach(column -> out.append(escape(column.title)).append("|"));
        out.append("\n|");
        columns.forEach(column -> out.append("---|"));
        out.append("\n");
        for (int index = 0; index < Math.min(limit, rows.size()); index++) {
            Map<String, Object> row = rows.get(index);
            out.append("|");
            columns.forEach(column -> out.append(escape(row.get(column.key))).append("|"));
            out.append("\n");
        }
    }

    private List<Column> columns(Map<String, Object> result, List<Map<String, Object>> rows) {
        LinkedHashMap<String, String> allowed = new LinkedHashMap<>();
        Object configured = result == null ? null : result.get("columns");
        if (configured instanceof Collection) {
            for (Object value : (Collection<?>) configured) {
                if (!(value instanceof Map)) continue;
                Map<?, ?> column = (Map<?, ?>) value;
                String key = string(column.get("key"));
                String title = string(column.get("title"));
                if (isSafeColumn(key, rows) && title != null) allowed.put(key, title);
            }
        }
        if (!rows.isEmpty()) {
            for (String key : SAFE_DEFAULT_COLUMNS) {
                if (rows.get(0).containsKey(key)) allowed.putIfAbsent(key, defaultTitle(key));
            }
        }
        List<Column> columns = new ArrayList<>();
        allowed.forEach((key, title) -> columns.add(new Column(key, title)));
        return columns;
    }

    private static boolean isSafeColumn(String key, List<Map<String, Object>> rows) {
        if (key == null || !key.matches("[A-Za-z][A-Za-z0-9_]{0,63}")
                || FORBIDDEN_COLUMNS.contains(key.toLowerCase(Locale.ROOT))) {
            return false;
        }
        return rows.isEmpty() || rows.stream().anyMatch(row -> row.containsKey(key));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object value) {
        if (!(value instanceof List)) return Collections.emptyList();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object row : (List<?>) value) if (row instanceof Map) result.add((Map<String, Object>) row);
        return result;
    }

    private static String defaultTitle(String key) {
        Map<String, String> titles = new HashMap<>();
        titles.put("label", "对象"); titles.put("amount", "数值"); titles.put("unit", "单位");
        titles.put("period", "日期"); titles.put("date", "日期"); titles.put("unitRank", "排名");
        titles.put("unit_rank", "排名"); titles.put("document_count", "单据数"); titles.put("record_count", "记录数");
        return titles.getOrDefault(key, key);
    }

    private static String escape(Object value) {
        if (value == null) return "";
        String text = String.valueOf(value).replace("\\", "\\\\").replace("|", "\\|")
                .replace("\r", " ").replace("\n", " ");
        return text.replace("`", "\\`").replace("*", "\\*").replace("_", "\\_");
    }

    private static String truncate(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= SAFE_BYTES) return text;
        String suffix = "\n\n内容较长，已安全截断；仅展示前 10 行，请前往 ERP 智能问数页面查看完整结果。";
        int budget = SAFE_BYTES - suffix.getBytes(StandardCharsets.UTF_8).length;
        StringBuilder result = new StringBuilder();
        int used = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String character = new String(Character.toChars(codePoint));
            int size = character.getBytes(StandardCharsets.UTF_8).length;
            if (used + size > budget) break;
            result.append(character); used += size; offset += Character.charCount(codePoint);
        }
        String truncated = result + suffix;
        if (truncated.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return result.toString();
        return truncated;
    }

    private static String string(Object value) { return value == null ? null : String.valueOf(value); }
    private static Long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return value == null ? null : Long.valueOf(String.valueOf(value)); } catch (NumberFormatException ignored) { return null; }
    }

    private static class Column {
        final String key; final String title;
        Column(String key, String title) { this.key = key; this.title = title; }
    }

    public static class ChoiceResult {
        private final String content; private final String field; private final List<Long> ids;
        ChoiceResult(String content, String field, List<Long> ids) {
            this.content = content; this.field = field; this.ids = new ArrayList<>(ids);
        }
        public String getContent() { return content; }
        public String getField() { return field; }
        public List<Long> getIds() { return ids; }
        public boolean hasChoices() { return field != null && !ids.isEmpty(); }
        public String toJson() {
            Map<String, Object> value = new LinkedHashMap<>(); value.put("field", field); value.put("ids", ids);
            return JsonUtils.toJsonString(value);
        }
    }
}
