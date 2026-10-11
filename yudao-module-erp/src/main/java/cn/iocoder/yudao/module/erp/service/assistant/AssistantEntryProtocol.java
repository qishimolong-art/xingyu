package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.*;

/**
 * 页面、右侧抽屉和企业微信共用的终态消息协议。
 *
 * <p>保留既有顶层字段以兼容旧前端，同时在 response 中提供稳定的状态、表格、来源和明细动作元数据。</p>
 */
public final class AssistantEntryProtocol {

    public static final int SCHEMA_VERSION = 2;

    private AssistantEntryProtocol() {
    }

    public static Map<String, Object> result(Map<String, Object> value,
                                             AssistantExecutionEnvelope envelope,
                                             String messageId) {
        Map<String, Object> result = value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
        result.putIfAbsent("status", "SYSTEM_ERROR");
        result.putIfAbsent("summary", Collections.emptyList());
        result.putIfAbsent("rows", Collections.emptyList());
        result.putIfAbsent("columns", Collections.emptyList());
        result.putIfAbsent("basis", Collections.emptyList());
        result.putIfAbsent("sources", Collections.emptyList());
        result.putIfAbsent("warnings", Collections.emptyList());
        if (messageId != null && !messageId.trim().isEmpty()) result.put("messageId", messageId);

        String status = string(result.get("status"), "SYSTEM_ERROR");
        Map<String, Object> response = base("RESULT", status, severity(status), statusMessage(result, status));

        Map<String, Object> table = new LinkedHashMap<>();
        table.put("visible", !list(result.get("summary")).isEmpty() || !list(result.get("rows")).isEmpty());
        table.put("summaryRows", list(result.get("summary")).size());
        table.put("detailRows", list(result.get("rows")).size());
        table.put("columnCount", list(result.get("columns")).size());
        response.put("table", table);

        List<?> sources = list(result.get("sources"));
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("count", sources.size());
        source.put("available", !sources.isEmpty() || !list(result.get("basis")).isEmpty());
        response.put("source", source);
        response.put("detail", detail(envelope, messageId));
        result.put("response", response);
        return result;
    }

    public static Map<String, Object> clarification(Object value) {
        Map<String, Object> result;
        if (value instanceof Map) {
            result = new LinkedHashMap<>((Map<String, Object>) value);
            AssistantClarification.normalize(result);
        } else if (value instanceof AssistantPlan) {
            result = AssistantClarification.fromPlan((AssistantPlan) value);
        } else {
            result = AssistantClarification.missing("MISSING_PARAMETER", "请补充查询条件",
                    Collections.singletonList("queryCondition"), Collections.emptyList());
        }
        result.put("response", base("CLARIFICATION", string(result.get("status"), "CLARIFY"),
                "info", string(result.get("clarification"), "需要补充信息后才能继续查询")));
        return result;
    }

    public static Map<String, Object> error(String code, String text) {
        String status = string(code, "QUERY_FAILED");
        String message = string(text, "查询未完成，请稍后重试。");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", status);
        result.put("text", message);
        result.put("response", base("ERROR", status, "error", message));
        return result;
    }

    public static Map<String, Object> done(String status) {
        String code = string(status, "SUCCESS");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", code);
        result.put("response", base("DONE", code, severity(code), terminalMessage(code)));
        return result;
    }

    public static String message(Map<String, Object> value, String fallback) {
        if (value != null && value.get("response") instanceof Map) {
            Object message = ((Map<?, ?>) value.get("response")).get("message");
            if (message != null && !String.valueOf(message).trim().isEmpty()) return String.valueOf(message);
        }
        return fallback;
    }

    public static boolean detailAvailable(Map<String, Object> value) {
        if (value == null || !(value.get("response") instanceof Map)) return false;
        Object detail = ((Map<?, ?>) value.get("response")).get("detail");
        return detail instanceof Map && Boolean.TRUE.equals(((Map<?, ?>) detail).get("available"));
    }

    private static Map<String, Object> base(String type, String status, String severity, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemaVersion", SCHEMA_VERSION);
        response.put("type", type);
        response.put("status", status);
        response.put("severity", severity);
        response.put("message", message);
        return response;
    }

    private static Map<String, Object> detail(AssistantExecutionEnvelope envelope, String messageId) {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean available = supportsDetails(envelope) && messageId != null && !messageId.trim().isEmpty();
        detail.put("available", available);
        detail.put("label", "查看来源明细");
        detail.put("kind", "ERP_ASSISTANT_DETAIL");
        if (available) detail.put("messageId", messageId);
        else detail.put("reason", detailUnavailableReason(envelope));
        return detail;
    }

    private static boolean supportsDetails(AssistantExecutionEnvelope envelope) {
        if (envelope == null) return false;
        if (AssistantCompanyKnowledge.TOOL_NAME.equals(envelope.getToolName())
                || AssistantMasterDataQueryService.TOOL_NAME.equals(envelope.getToolName())) return false;
        if (envelope.getLegacyPlan() != null) return true;
        return nonBlank(envelope.getArguments().get("detailSql")) || nonBlank(envelope.getArguments().get("sql"));
    }

    private static String detailUnavailableReason(AssistantExecutionEnvelope envelope) {
        if (envelope == null) return "NO_EXECUTION_CONTEXT";
        if (AssistantCompanyKnowledge.TOOL_NAME.equals(envelope.getToolName())) return "KNOWLEDGE_SOURCE_ONLY";
        if (AssistantMasterDataQueryService.TOOL_NAME.equals(envelope.getToolName())) return "AGGREGATE_ONLY";
        return "DETAIL_NOT_REGISTERED";
    }

    private static String statusMessage(Map<String, Object> result, String status) {
        if ("EMPTY".equals(status) || "NO_DATA".equals(status))
            return string(result.get("emptyText"), "在指定条件和当前权限范围内未查询到记录");
        if ("KNOWLEDGE_NOT_FOUND".equals(status))
            return string(result.get("emptyText"), "当前已发布的公司知识中没有找到足够相关的依据");
        if ("UNVERIFIED_KNOWLEDGE".equals(status))
            return string(result.get("verificationNotice"), "当前回答包含待核实资料，不能作为确定结论。");
        if ("PARTIAL_SUCCESS".equals(status)) {
            List<?> warnings = list(result.get("warnings"));
            if (!warnings.isEmpty()) return join(warnings, "；");
            return "问题仅完成了其中一部分";
        }
        if ("SUCCESS".equals(status)) return "查询完成";
        return terminalMessage(status);
    }

    private static String terminalMessage(String status) {
        if ("CLARIFY".equals(status) || "CHOICE_REQUIRED".equals(status)) return "需要补充信息后才能继续查询";
        if ("CANCELLED".equals(status)) return "查询已停止";
        if ("TIMEOUT".equals(status)) return "查询超时，请缩小范围后重试";
        if ("FORBIDDEN".equals(status) || "FIELD_FORBIDDEN".equals(status)) return "没有当前查询所需的权限";
        if ("SUCCESS".equals(status)) return "查询完成";
        return "查询未完成，请稍后重试";
    }

    private static String severity(String status) {
        if ("SUCCESS".equals(status)) return "success";
        if (Arrays.asList("EMPTY", "NO_DATA", "KNOWLEDGE_NOT_FOUND", "CLARIFY", "CHOICE_REQUIRED").contains(status))
            return "info";
        if (Arrays.asList("PARTIAL_SUCCESS", "UNVERIFIED_KNOWLEDGE").contains(status)) return "warning";
        return "error";
    }

    private static boolean nonBlank(Object value) {
        return value != null && !String.valueOf(value).trim().isEmpty();
    }

    private static List<?> list(Object value) {
        return value instanceof List ? (List<?>) value : Collections.emptyList();
    }

    private static String join(Collection<?> values, String delimiter) {
        List<String> strings = new ArrayList<>();
        for (Object value : values) if (value != null) strings.add(String.valueOf(value));
        return String.join(delimiter, strings);
    }

    private static String string(Object value, String fallback) {
        return value == null || String.valueOf(value).trim().isEmpty() ? fallback : String.valueOf(value);
    }
}
