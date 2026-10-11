package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantEntryProtocol;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** 将已脱敏、已授权的问数事件转换为企业微信 Markdown。 */
@Component
public class AssistantWeComResultFormatter {

    private static final int MAX_BYTES = 20_480;
    private static final int SAFE_BYTES = 19_500;
    private static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> SAFE_DEFAULT_COLUMNS = new LinkedHashSet<>(Arrays.asList(
            "label", "amount", "unit", "period", "date", "document_count", "record_count",
            "total_amount", "total_quantity", "average_amount", "maximum_amount", "minimum_amount",
            "fulfilled_quantity", "return_quantity", "remaining_quantity", "unitRank", "unit_rank"));
    private static final Set<String> FORBIDDEN_COLUMNS = new HashSet<>(Arrays.asList(
            "sql", "parameters", "params", "trace", "audit", "audit_trace", "raw_data",
            "creator", "updater", "password", "token", "secret", "deleted"));

    public String result(Map<String, Object> result, String explanation) {
        if (result != null && "answer_company_knowledge".equals(result.get("toolName"))) {
            return knowledgeResult(result, explanation);
        }
        if (result != null && "query_product_stock".equals(result.get("toolName"))
                && result.get("productResults") instanceof Collection) {
            return productResult(result, explanation);
        }
        if (isAccountResult(result)) {
            return accountResult(result, explanation);
        }
        if (result != null && "MIXED".equals(result.get("queryType"))) {
            return mixedResult(result, explanation);
        }
        StringBuilder out = new StringBuilder();
        appendCorrection(out, result);
        Object presentation = result == null ? null : result.get("presentation");
        if (presentation instanceof Map) {
            Object headline = ((Map<?, ?>) presentation).get("headline");
            if (headline != null) out.append("### ").append(escape(headline)).append("\n\n");
        }
        List<Map<String, Object>> summary = rows(result == null ? null : result.get("summary"));
        List<Map<String, Object>> detail = rows(result == null ? null : result.get("rows"));
        if (summary.isEmpty() && detail.isEmpty()) {
            out.append(escape(AssistantEntryProtocol.message(result,
                    "未查询到当前权限范围内符合条件的数据。"))).append("\n");
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
        if (result != null) appendSources(out, result, "依据");
        String queriedAt = formatDateTime(result == null ? null : result.get("queriedAt"));
        if (queriedAt == null) queriedAt = LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER);
        out.append("\n查询时间：").append(escape(queriedAt));
        out.append("\n权限范围：").append(escape(result == null ? "当前 ERP 用户实时权限" : result.get("scope")));
        appendDetailHint(out, result);
        if (summary.size() > 10 || detail.size() > 10) {
            out.append("\n\n仅展示前 10 行，请前往 ERP 智能问数页面查看完整结果。");
        }
        return truncate(out.toString());
    }

    private String accountResult(Map<String,Object> result,String explanation) {
        StringBuilder out=new StringBuilder();appendCorrection(out,result);
        Object presentation=result.get("presentation");
        if(presentation instanceof Map && ((Map<?,?>)presentation).get("headline")!=null)
            out.append("### ").append(escape(((Map<?,?>)presentation).get("headline"))).append("\n\n");
        boolean receivable="RECEIVABLE".equals(accountMetric(result));
        String partyTitle=receivable?"客户":"供应商";
        String amountTitle=receivable?"应收余额":"应付余额";
        List<Map<String,Object>> summary=rows(result.get("summary"));
        List<Map<String,Object>> accounts=rows(result.get("accountResults"));
        if(summary.isEmpty() && accounts.isEmpty()) {
            out.append(escape(AssistantEntryProtocol.message(result,
                    "未查询到当前权限范围内符合条件的数据。"))).append("\n");
        } else {
            out.append("**查询结果**\n\n");
            if(!summary.isEmpty()) {
                Map<String,Object> total=summary.get(0);
                out.append("**").append(amountTitle).append("合计**：")
                        .append(formatNumber(total.get("amount")));
                if(total.get("unit")!=null) out.append(" ").append(escape(total.get("unit")));
                out.append("\n\n");
            }
            if(!accounts.isEmpty()) {
                out.append("|序号|").append(partyTitle).append("|单据所属部门|").append(amountTitle).append("|\n")
                        .append("|---:|---|---|---:|\n");
                for(int index=0;index<accounts.size();index++) {
                    Map<String,Object> account=accounts.get(index);
                    out.append("|").append(index+1).append("|")
                            .append(escape(account.get("partyName"))).append("|")
                            .append(escape(account.get("deptName"))).append("|")
                            .append(formatNumber(account.get("amount"))).append("|\n");
                }
            } else {
                out.append("当前没有非零部门余额。\n");
            }
            if(Boolean.TRUE.equals(result.get("accountPartyTruncated")))
                out.append("\n匹配结果较多，已展示前 10 个").append(partyTitle).append("及其部门明细。\n");
        }
        if(explanation!=null && !explanation.trim().isEmpty())
            out.append("\n**模型解释与统计口径**\n\n").append(escape(explanation)).append("\n");
        else out.append("\n**统计口径**：按当前实时权限，以业务单据所属部门汇总当前余额。\n");
        appendSources(out,result,"依据");
        String queriedAt=formatDateTime(result.get("queriedAt"));
        if(queriedAt==null) queriedAt=LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER);
        out.append("\n查询时间：").append(escape(queriedAt));
        out.append("\n权限范围：").append(escape(result.get("scope")));appendDetailHint(out,result);
        return truncateCompleteLines(out.toString());
    }

    private static boolean isAccountResult(Map<String,Object> result) {
        String metric=accountMetric(result);
        return result!=null && result.get("accountResults") instanceof Collection
                && ("RECEIVABLE".equals(metric) || "PAYABLE".equals(metric));
    }

    private static String accountMetric(Map<String,Object> result) {
        if(result==null) return null;
        Object plan=result.get("plan");
        if(plan instanceof AssistantPlan) return ((AssistantPlan)plan).getMetric().name();
        if(plan instanceof Map && ((Map<?,?>)plan).get("metric")!=null)
            return String.valueOf(((Map<?,?>)plan).get("metric"));
        return null;
    }

    private String productResult(Map<String,Object> result,String explanation) {
        StringBuilder out=new StringBuilder();appendCorrection(out,result);
        Object presentation=result.get("presentation");
        if(presentation instanceof Map && ((Map<?,?>)presentation).get("headline")!=null)
            out.append("### ").append(escape(((Map<?,?>)presentation).get("headline"))).append("\n\n");
        List<Map<String,Object>> products=rows(result.get("productResults"));
        if(products.isEmpty()) out.append(escape(AssistantEntryProtocol.message(result,
                "未查询到当前权限范围内符合条件的产品。"))).append("\n");
        else {
            out.append("**查询结果**\n\n|序号|产品|价格|仓库库存|\n|---:|---|---|---|\n");
            for(int index=0;index<Math.min(10,products.size());index++) {
                Map<String,Object> product=products.get(index);
                out.append("|").append(index+1).append("|")
                        .append(escape(product.get("productName"))).append("|")
                        .append(escape(productPriceText(product))).append("|")
                        .append(escape(productStockText(product,5))).append("|\n");
            }
            if(Boolean.TRUE.equals(result.get("productMatchTruncated")) || products.size()>10)
                out.append("\n匹配结果较多，已展示前 10 个产品。\n");
        }
        String priceNotice=string(result.get("priceNotice"));
        if(priceNotice!=null && !priceNotice.trim().isEmpty())
            out.append("\n> ").append(escape(priceNotice)).append("\n");
        if(explanation!=null && !explanation.trim().isEmpty())
            out.append("\n**模型解释与统计口径**\n\n").append(escape(explanation)).append("\n");
        else out.append("\n**统计口径**：产品价格按当前字段权限展示，库存按当前授权仓库汇总。\n");
        appendSources(out,result,"依据");
        String queriedAt=formatDateTime(result.get("queriedAt"));
        if(queriedAt==null) queriedAt=LocalDateTime.now().format(DISPLAY_DATE_TIME_FORMATTER);
        out.append("\n查询时间：").append(escape(queriedAt));
        out.append("\n权限范围：").append(escape(result.get("scope")));appendDetailHint(out,result);
        return truncate(out.toString());
    }

    private static String productPriceText(Map<String,Object> product) {
        String displayText=string(product.get("priceDisplayText"));
        if(displayText!=null && !displayText.trim().isEmpty()) return displayText;
        List<Map<String,Object>> prices=rows(product.get("prices"));List<String> values=new ArrayList<>();
        for(Map<String,Object> price:prices) {
            String title=string(price.get("title"));Object value=price.get("value");
            if(title!=null && value!=null) values.add(title+" "+formatNumber(value));
        }
        return values.isEmpty()?"当前账号可见的价格尚未维护":String.join("；",values);
    }

    private static String productStockText(Map<String,Object> product,int limit) {
        List<Map<String,Object>> stocks=rows(product.get("warehouseStocks"));
        if(stocks.isEmpty()) return "无库存";
        List<String> values=new ArrayList<>();
        for(int index=0;index<Math.min(limit,stocks.size());index++) {
            Map<String,Object> stock=stocks.get(index);
            values.add(String.valueOf(stock.get("warehouseName"))+" "+formatNumber(stock.get("amount")));
        }
        if(stocks.size()>limit) {
            BigDecimal other=BigDecimal.ZERO;
            for(int index=limit;index<stocks.size();index++) other=other.add(decimalValue(stocks.get(index).get("amount")));
            values.add("其他"+(stocks.size()-limit)+"仓 "+formatNumber(other));
        }
        return String.join("；",values);
    }

    private String knowledgeResult(Map<String, Object> result, String explanation) {
        String answer = explanation;
        if (answer == null || answer.trim().isEmpty()) answer = string(result.get("answerText"));
        if (answer == null || answer.trim().isEmpty()) answer = "当前公司知识库中没有找到足够相关的依据。";
        StringBuilder out = new StringBuilder("### 公司知识回答\n\n");
        appendCorrection(out, result);
        out.append(escape(answer)).append("\n");
        appendSources(out, result, "知识来源");
        appendWarnings(out, result);
        out.append("\n知识范围：公司公共知识");
        return truncate(out.toString());
    }

    private String mixedResult(Map<String, Object> result, String explanation) {
        StringBuilder out = new StringBuilder("### 公司知识与实时数据\n");
        appendCorrection(out, result);
        String answer = explanation;
        if (answer == null || answer.trim().isEmpty()) answer = string(result.get("answerText"));
        if (answer != null && !answer.trim().isEmpty()) {
            out.append("\n**知识回答**\n\n").append(escape(answer)).append("\n");
        }
        appendSources(out, result, "依据");
        appendWarnings(out, result);
        List<Map<String, Object>> summary = rows(result.get("summary"));
        List<Map<String, Object>> detail = rows(result.get("rows"));
        if (!summary.isEmpty() || !detail.isEmpty()) {
            out.append("\n**实时数据结果**\n\n");
            if (!summary.isEmpty()) appendTable(out, summary, result, 10);
            if (!detail.isEmpty()) appendTable(out, detail, result, 10);
        } else if ("EMPTY".equals(result.get("dataStatus"))) {
            out.append("\n实时数据部分在当前权限和条件下未查询到记录。\n");
        }
        out.append("\n权限范围：").append(escape(result.get("scope")));
        appendDetailHint(out, result);
        return truncate(out.toString());
    }

    public ChoiceResult clarification(Object value) {
        return clarification(value, null);
    }

    public ChoiceResult clarification(Object value, String originalQuestion) {
        String text = "需要补充信息后才能继续查询";
        String field = null;
        List<Long> ids = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Map<String, Object>> options = new ArrayList<>();
        if (value instanceof AssistantPlan) {
            AssistantPlan plan = (AssistantPlan) value;
            if (plan.getClarification() != null) text = plan.getClarification();
            field = plan.getPendingField();
            if (plan.getPendingIds() != null) ids.addAll(plan.getPendingIds());
            if (plan.getChoices() != null) labels.addAll(plan.getChoices());
            for (int index = 0; index < Math.min(ids.size(), labels.size()); index++) {
                options.add(choiceOption(field, ids.get(index), null, originalQuestion));
            }
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
                    String candidateField = string(item.get("field"));
                    options.add(choiceOption(candidateField == null ? field : candidateField, id, null, originalQuestion));
                }
            }
            Object choices = map.get("choices");
            if (choices instanceof Collection) {
                Map<?, ?> selected = map.get("selectedEntity") instanceof Map
                        ? (Map<?, ?>) map.get("selectedEntity") : Collections.emptyMap();
                String selectedType = string(selected.get("entityType"));
                String selectedField = string(selected.get("field"));
                if (selectedField == null && selectedType != null) selectedField = "resolved:" + selectedType;
                Long selectedId = longValue(selected.get("id"));
                for (Object choice : (Collection<?>) choices) {
                    if (choice == null || String.valueOf(choice).trim().isEmpty()) continue;
                    String label = String.valueOf(choice);
                    labels.add(label);
                    String prefix = selectedId == null ? string(originalQuestion)
                            : entityReferenceLabel(selectedType);
                    String question = (prefix == null ? "" : prefix) + "；补充：" + label;
                    options.add(choiceOption(selectedField, selectedId, label, question));
                }
            }
        }
        StringBuilder content = new StringBuilder(escape(text));
        int count = Math.min(options.size(), labels.size());
        for (int index = 0; index < count; index++) {
            content.append("\n").append(index + 1).append(". ").append(escape(labels.get(index)));
        }
        if (count > 0) content.append("\n\n请回复对应编号。");
        return new ChoiceResult(truncate(content.toString()), field, ids, options.subList(0, count));
    }

    public String error(String text) {
        return truncate(text == null || text.trim().isEmpty() ? "查询未完成，请稍后重试。" : escape(text));
    }

    private void appendSources(StringBuilder out, Map<String, Object> result, String title) {
        List<Map<String, Object>> sources = rows(result.get("sources"));
        if (!sources.isEmpty()) {
            out.append("\n**").append(title).append("**：");
            List<String> labels = new ArrayList<>();
            for (int index = 0; index < Math.min(5, sources.size()); index++) {
                Map<String, Object> source = sources.get(index);
                String citation = string(source.get("citation"));
                String sourceTitle = string(source.get("title"));
                String section = string(source.get("section"));
                StringBuilder label = new StringBuilder();
                if (citation != null) label.append("[").append(escape(citation)).append("]");
                if (sourceTitle != null) label.append("《").append(escape(sourceTitle)).append("》");
                if (section != null) label.append("·").append(escape(section));
                if ("UNVERIFIED".equals(source.get("verificationStatus"))) label.append("（待核实）");
                labels.add(label.toString());
            }
            out.append(String.join("、", labels)).append("\n");
            return;
        }
        Object basis = result.get("basis");
        if (basis instanceof Collection && !((Collection<?>) basis).isEmpty()) {
            out.append("\n**").append(title).append("**：");
            List<String> labels = new ArrayList<>();
            for (Object value : (Collection<?>) basis) labels.add(escape(value));
            out.append(String.join("、", labels)).append("\n");
        }
    }

    private void appendWarnings(StringBuilder out, Map<String, Object> result) {
        Object warnings = result.get("warnings");
        if (warnings instanceof Collection) for (Object warning : (Collection<?>) warnings) {
            out.append("\n> 注意：").append(escape(warning)).append("\n");
        }
    }

    private void appendCorrection(StringBuilder out, Map<String,Object> result) {
        if(result==null || result.get("correctionNotice")==null) return;
        String notice=String.valueOf(result.get("correctionNotice")).trim();
        if(!notice.isEmpty()) out.append("> ").append(escape(notice)).append("\n\n");
    }

    private void appendDetailHint(StringBuilder out, Map<String, Object> result) {
        if (AssistantEntryProtocol.detailAvailable(result)) {
            out.append("\n明细核对：请在 ERP 智能问数中打开“查看来源明细”。");
        }
    }

    private static Map<String, Object> choiceOption(String field, Long id, String text, String question) {
        Map<String, Object> option = new LinkedHashMap<>();
        if (field != null) option.put("field", field);
        if (id != null) option.put("id", id);
        if (text != null) option.put("text", text);
        if (question != null && !question.trim().isEmpty()) option.put("question", question);
        return option;
    }

    private static String entityReferenceLabel(String type) {
        if ("CUSTOMER".equals(type)) return "这个客户";
        if ("DEPARTMENT".equals(type)) return "这个部门";
        if ("PRODUCT".equals(type)) return "这个产品";
        if ("SUPPLIER".equals(type)) return "这个供应商";
        if ("WAREHOUSE".equals(type)) return "这个仓库";
        return "这个对象";
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
            columns.forEach(column -> out.append(escape(formatCellValue(row.get(column.key)))).append("|"));
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

    private static Object formatCellValue(Object value) {
        if (!(value instanceof Number)) return value;
        try {
            return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP).toPlainString();
        } catch (NumberFormatException ignored) {
            return value;
        }
    }

    private static String formatNumber(Object value) {
        return String.valueOf(formatCellValue(value));
    }

    private static BigDecimal decimalValue(Object value) {
        try { return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
    }

    private static String formatDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(DISPLAY_DATE_TIME_FORMATTER);
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .format(DISPLAY_DATE_TIME_FORMATTER);
        } catch (RuntimeException ignored) {
            return text;
        }
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

    private static String truncateCompleteLines(String text) {
        if(text.getBytes(StandardCharsets.UTF_8).length<=SAFE_BYTES) return text;
        String suffix="\n\n内容较长，已安全截断；请前往 ERP 智能问数页面查看完整结果。";
        int budget=SAFE_BYTES-suffix.getBytes(StandardCharsets.UTF_8).length;
        StringBuilder result=new StringBuilder();
        for(String line:text.split("\\n",-1)) {
            String complete=line+"\n";
            if(result.toString().getBytes(StandardCharsets.UTF_8).length
                    +complete.getBytes(StandardCharsets.UTF_8).length>budget) break;
            result.append(complete);
        }
        String truncated=result.toString().replaceFirst("\\n$","")+suffix;
        return truncated.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES?truncated:result.toString();
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
        private final List<Map<String, Object>> options;
        ChoiceResult(String content, String field, List<Long> ids, List<Map<String, Object>> options) {
            this.content = content; this.field = field; this.ids = new ArrayList<>(ids);
            this.options = new ArrayList<>(options);
        }
        public String getContent() { return content; }
        public String getField() { return field; }
        public List<Long> getIds() { return ids; }
        public boolean hasChoices() { return !options.isEmpty(); }
        public String toJson() {
            Map<String, Object> value = new LinkedHashMap<>(); value.put("field", field); value.put("ids", ids);
            value.put("options", options);
            return JsonUtils.toJsonString(value);
        }
    }
}
