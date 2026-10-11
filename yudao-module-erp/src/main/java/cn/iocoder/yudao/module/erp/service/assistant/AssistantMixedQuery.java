package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministically separates one knowledge question and one authorized realtime-data question. */
public final class AssistantMixedQuery {

    public static final String TOOL_NAME = "answer_mixed_question";
    private static final Pattern CONNECTOR = Pattern.compile("(?:，|,|；|;)?\\s*(?:并且|同时|以及|还要|再查|顺便|另外|并)\\s*");
    private static final Pattern COMBINED_EXPLANATION = Pattern.compile("^结合(.{2,80}?)(?:来)?(?:解释|分析|说明)(.+)$");
    private static final Pattern KNOWLEDGE = Pattern.compile(
            ".*(?:介绍|是什么|做什么|主营|区别|标准|认证|怎么选|如何选|选型|卖点|话术|学习路径|培训|用途|适用|"
                    + "核心优势|合作关系|渠道模式|销售渠道|渠道网络|业务覆盖|市场布局|区域布局|客户类型).*");
    private static final Pattern REALTIME = Pattern.compile(
            ".*(?:库存|价格|有货|缺货|销量|销售额|采购额|订单|单据|收款|付款|应收|应付|金额|数量|排名).*");
    private static final Pattern REALTIME_CUE = Pattern.compile(".*(?:查|查询|当前|实时|今天|本周|本月|多少|有没有|有货|缺货).*");

    private AssistantMixedQuery() {
    }

    public static boolean matches(String question) {
        if (question == null) return false;
        String compact = compact(question);
        boolean connected = CONNECTOR.matcher(question).find() || COMBINED_EXPLANATION.matcher(question.trim()).matches();
        return connected && KNOWLEDGE.matcher(compact).matches()
                && REALTIME.matcher(compact).matches() && REALTIME_CUE.matcher(compact).matches();
    }

    public static Split split(String question) {
        if (!matches(question)) return null;
        Matcher combined = COMBINED_EXPLANATION.matcher(question.trim());
        if (combined.matches()) {
            String knowledge = "请说明" + combined.group(1).trim();
            String data = combined.group(2).trim();
            return new Split(question, knowledge, data, null);
        }
        String[] raw = CONNECTOR.split(question);
        List<String> knowledgeParts = new ArrayList<>(), dataParts = new ArrayList<>();
        for (String value : raw) {
            String part = value.trim().replaceAll("^[，,；;]+|[，,；;]+$", "");
            if (part.isEmpty()) continue;
            boolean knowledge = KNOWLEDGE.matcher(compact(part)).matches();
            boolean data = REALTIME.matcher(compact(part)).matches() && REALTIME_CUE.matcher(compact(part)).matches();
            if (knowledge && !data) knowledgeParts.add(part);
            else if (data && !knowledge) dataParts.add(part);
            else if (data) dataParts.add(part);
            else knowledgeParts.add(part);
        }
        if (knowledgeParts.isEmpty() || dataParts.isEmpty()) return null;
        String knowledge = String.join("，", knowledgeParts);
        String data = String.join("，", dataParts);
        String subject = sharedSubject(knowledge);
        if (subject != null && productDataQuestion(data) && !compact(data).contains(compact(subject))) {
            data = "查询" + subject + data.replaceFirst("^(?:请)?(?:帮我)?(?:再)?(?:查|查询)", "");
        }
        return new Split(question, knowledge, data, subject);
    }

    private static boolean productDataQuestion(String value) {
        String compact = compact(value);
        return compact.matches(".*(?:库存|价格|有货|缺货|多少钱).*")
                && !compact.matches(".*(?:销售额|采购额|应收|应付|收款|付款).*");
    }

    private static String sharedSubject(String knowledge) {
        String value = knowledge.replaceAll("^(?:请)?(?:帮我)?(?:简单)?(?:介绍|说明|讲讲|说说)(?:一下)?", "")
                .replaceAll("(?:是什么|做什么|有什么卖点|的卖点|的用途|如何选|怎么选|选型)$", "")
                .replaceAll("[？?。！!]", "").trim();
        if (value.length() < 2 || value.length() > 60
                || value.matches(".*(?:兴宇公司|公司|集团|学习路径|培训|产品选型|产品知识|产品介绍|公司产品).*$")
                || "产品".equals(value)) {
            return null;
        }
        return value;
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    @Data
    @AllArgsConstructor
    public static class Split {
        private String originalQuestion;
        private String knowledgeQuestion;
        private String dataQuestion;
        private String sharedSubject;
    }
}
