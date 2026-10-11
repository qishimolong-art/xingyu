package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Data;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** V1/V2 共用的结构化执行结果摘要，不保存业务明细。 */
@Data
public class AssistantUnifiedResult {
    private String engine;
    private AssistantResultStatus status;
    /** 保留 V1 原始状态码，便于定位映射差异。 */
    private String statusCode;
    private String outcomeType;
    private String capabilityId;
    private String routeType;
    private Integer rowCount;
    /** 仅保存去除运行时字段后的结果哈希，不保存业务明细。 */
    private String resultFingerprint;
    private String intentId;
    private String metric;
    private List<String> businessObjects = new ArrayList<>();
    private List<String> dimensions = new ArrayList<>();
    private List<String> documentStatuses = new ArrayList<>();
    private String contextMode;
    private List<String> inheritedFields = new ArrayList<>();
    private long elapsedMillis;

    public static AssistantUnifiedResult success(String engine, AssistantExecutionEnvelope envelope,
                                                 Map<String, Object> result, long elapsedMillis) {
        AssistantUnifiedResult value = base(engine, envelope, elapsedMillis);
        value.setOutcomeType("RESULT");
        value.setStatusCode(string(result == null ? null : result.get("status"), "SYSTEM_ERROR"));
        value.setStatus(AssistantResultStatus.from(value.getStatusCode()));
        value.setRowCount(rowCount(result));
        value.setResultFingerprint(fingerprint(result));
        return value;
    }

    public static AssistantUnifiedResult clarification(String engine, AssistantExecutionEnvelope envelope,
                                                       long elapsedMillis) {
        AssistantUnifiedResult value = base(engine, envelope, elapsedMillis);
        value.setOutcomeType("CLARIFICATION");
        value.setStatusCode("CLARIFY");
        value.setStatus(AssistantResultStatus.CLARIFY);
        return value;
    }

    public static AssistantUnifiedResult failure(String engine, AssistantExecutionEnvelope envelope,
                                                 String status, long elapsedMillis) {
        AssistantUnifiedResult value = base(engine, envelope, elapsedMillis);
        value.setOutcomeType("ERROR");
        value.setStatusCode(string(status, "SYSTEM_ERROR"));
        value.setStatus(AssistantResultStatus.from(value.getStatusCode()));
        return value;
    }

    private static AssistantUnifiedResult base(String engine, AssistantExecutionEnvelope envelope, long elapsedMillis) {
        AssistantUnifiedResult value = new AssistantUnifiedResult();
        value.setEngine(engine);
        value.setElapsedMillis(elapsedMillis);
        if (envelope != null) {
            value.setCapabilityId(envelope.getToolName());
            value.setRouteType(envelope.getRouteType() == null ? null : envelope.getRouteType().name());
            AssistantSemanticRequest request = envelope.getSemanticRequest();
            if (request != null) {
                value.setIntentId(request.getIntentId());
                value.setMetric(request.getMetric());
                value.setBusinessObjects(copy(request.getBusinessObjects()));
                value.setDimensions(copy(request.getDimensions()));
                value.setDocumentStatuses(copy(request.getDocumentStatuses()));
                if (request.getContext() != null) {
                    value.setContextMode(request.getContext().getMode());
                    for (AssistantSemanticRequest.InheritedField field : request.getContext().getInheritedFields()) {
                        if (field != null && field.getField() != null) value.getInheritedFields().add(field.getField());
                    }
                }
            }
        }
        return value;
    }

    private static List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private static Integer rowCount(Map<String, Object> result) {
        if (result == null) return null;
        Object total = result.get("total");
        if (total instanceof Number) return ((Number) total).intValue();
        Object rows = result.get("rows");
        return rows instanceof java.util.Collection ? ((java.util.Collection<?>) rows).size() : null;
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static String fingerprint(Map<String, Object> result) {
        if (result == null) return null;
        try {
            Map<String, Object> comparable = new LinkedHashMap<>();
            for (String key : java.util.Arrays.asList("status", "queryType", "toolName", "summary", "total",
                    "rows", "answerText", "sources", "warnings")) {
                if (result.containsKey(key)) comparable.put(key, canonical(result.get(key)));
            }
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(mapper.writeValueAsString(comparable).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return hex.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object canonical(Object value) {
        if (value instanceof Map) {
            Map<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (java.util.Arrays.asList("messageId", "detailSql", "sql", "response").contains(key)) continue;
                sorted.put(key, canonical(entry.getValue()));
            }
            return sorted;
        }
        if (value instanceof Collection) {
            List<Object> values = new ArrayList<>();
            for (Object item : (Collection<?>) value) values.add(canonical(item));
            return values;
        }
        return value;
    }
}
