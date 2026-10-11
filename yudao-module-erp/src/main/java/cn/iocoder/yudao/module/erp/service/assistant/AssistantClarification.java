package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.*;

/** 统一的结构化澄清响应；保留旧前端已经使用的 clarification、choices 和 candidates 字段。 */
public final class AssistantClarification {

    private AssistantClarification() {
    }

    public static Map<String, Object> missing(String reasonCode, String text, Collection<String> missingFields,
                                              Collection<String> choices) {
        Map<String, Object> result = base("CLARIFY", reasonCode, text);
        result.put("missingFields", missingFields == null ? Collections.emptyList() : new ArrayList<>(missingFields));
        result.put("choices", choices == null ? Collections.emptyList() : new ArrayList<>(choices));
        result.put("candidates", Collections.emptyList());
        return result;
    }

    public static Map<String, Object> entityChoice(String text, Collection<Map<String, Object>> candidates) {
        Map<String, Object> result = base("CHOICE_REQUIRED", "ENTITY_AMBIGUOUS", text);
        result.put("field", "entity");
        result.put("missingFields", Collections.singletonList("entity"));
        result.put("choices", Collections.emptyList());
        result.put("candidates", candidates == null ? Collections.emptyList() : new ArrayList<>(candidates));
        return result;
    }

    public static Map<String, Object> intentChoice(String text, Map<String, Object> selectedEntity,
                                                   Collection<String> choices) {
        Map<String, Object> result = base("CHOICE_REQUIRED", "MISSING_INTENT", text);
        result.put("missingFields", Collections.singletonList("intent"));
        result.put("choices", choices == null ? Collections.emptyList() : new ArrayList<>(choices));
        result.put("candidates", Collections.emptyList());
        result.put("selectedEntity", selectedEntity);
        return result;
    }

    public static Map<String, Object> fromPlan(AssistantPlan plan) {
        if (plan == null) return missing("MISSING_PARAMETER", "请补充查询条件",
                Collections.singletonList("queryCondition"), Collections.emptyList());
        List<String> missing = new ArrayList<>();
        String reason = "MISSING_PARAMETER";
        if (plan.getMetric() == null) {
            missing.add("metric");
            reason = "MISSING_METRIC";
        }
        if (plan.getPeriod() == null && !plan.current()) {
            missing.add("timeRange");
            reason = "MISSING_TIME_RANGE";
        }
        if (missing.isEmpty()) missing.add("queryCondition");
        return missing(reason, plan.getClarification(), missing, plan.getChoices());
    }

    /** 给旧路径补齐结构化字段，但绝不覆盖其原有文案、候选项和选项。 */
    public static Map<String, Object> normalize(Map<String, Object> response) {
        if (response == null) return missing("MISSING_PARAMETER", "请补充查询条件",
                Collections.singletonList("queryCondition"), Collections.emptyList());
        response.putIfAbsent("status", response.containsKey("candidates") ? "CHOICE_REQUIRED" : "CLARIFY");
        response.putIfAbsent("reasonCode", response.containsKey("candidates") ? "ENTITY_AMBIGUOUS" : "MISSING_PARAMETER");
        response.putIfAbsent("missingFields", Collections.emptyList());
        response.putIfAbsent("choices", Collections.emptyList());
        response.putIfAbsent("candidates", Collections.emptyList());
        return response;
    }

    private static Map<String, Object> base(String status, String reasonCode, String text) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schemaVersion", 1);
        result.put("status", status);
        result.put("reasonCode", reasonCode);
        result.put("clarification", text);
        return result;
    }
}
