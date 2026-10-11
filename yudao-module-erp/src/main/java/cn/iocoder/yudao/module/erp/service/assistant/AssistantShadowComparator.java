package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 只比较结构化摘要，避免把影子结果明细或敏感字段写入审计。 */
public final class AssistantShadowComparator {
    private AssistantShadowComparator() {}

    public static Map<String, Object> compare(AssistantUnifiedResult v1, AssistantUnifiedResult v2) {
        return compare(v1, v2, 3.0D, 5000L);
    }

    public static Map<String, Object> compare(AssistantUnifiedResult v1, AssistantUnifiedResult v2,
                                              double maxLatencyRatio, long maxLatencyDeltaMillis) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("executed", v2 != null);
        if (v2 == null) return result;
        result.put("v1Status", v1 == null ? null : v1.getStatus());
        result.put("v2Status", v2.getStatus());
        result.put("statusMatched", v1 != null && Objects.equals(v1.getStatus(), v2.getStatus()));
        result.put("v1Capability", v1 == null ? null : v1.getCapabilityId());
        result.put("v2Capability", v2.getCapabilityId());
        result.put("capabilityMatched", v1 != null && Objects.equals(v1.getCapabilityId(), v2.getCapabilityId()));
        result.put("v1RowCount", v1 == null ? null : v1.getRowCount());
        result.put("v2RowCount", v2.getRowCount());
        result.put("rowCountMatched", v1 != null && Objects.equals(v1.getRowCount(), v2.getRowCount()));
        boolean answerMatched = v1 != null && Objects.equals(v1.getOutcomeType(), v2.getOutcomeType())
                && (!"RESULT".equals(v2.getOutcomeType())
                || v1.getResultFingerprint() != null
                && Objects.equals(v1.getResultFingerprint(), v2.getResultFingerprint()));
        result.put("answerMatched", answerMatched);
        result.put("permissionMatched", v1 != null && isPermissionResult(v1) == isPermissionResult(v2));
        result.put("failureMatched", v1 != null && isTechnicalFailure(v1) == isTechnicalFailure(v2));
        result.put("semanticMatched", v1 != null && Objects.equals(v1.getIntentId(), v2.getIntentId())
                && Objects.equals(v1.getMetric(), v2.getMetric())
                && Objects.equals(v1.getBusinessObjects(), v2.getBusinessObjects())
                && Objects.equals(v1.getDimensions(), v2.getDimensions())
                && Objects.equals(v1.getDocumentStatuses(), v2.getDocumentStatuses())
                && Objects.equals(v1.getContextMode(), v2.getContextMode())
                && Objects.equals(v1.getInheritedFields(), v2.getInheritedFields()));
        long v1Elapsed = v1 == null ? 0L : Math.max(0L, v1.getElapsedMillis());
        long v2Elapsed = Math.max(0L, v2.getElapsedMillis());
        long latencyDelta = v2Elapsed - v1Elapsed;
        Double latencyRatio = v1Elapsed <= 0L ? null : (double) v2Elapsed / (double) v1Elapsed;
        boolean performanceWithinThreshold = latencyDelta <= Math.max(0L, maxLatencyDeltaMillis)
                || latencyRatio == null || latencyRatio <= Math.max(1.0D, maxLatencyRatio);
        result.put("v1ElapsedMillis", v1Elapsed);
        result.put("v2ElapsedMillis", v2Elapsed);
        result.put("latencyDeltaMillis", latencyDelta);
        result.put("latencyRatio", latencyRatio);
        result.put("performanceWithinThreshold", performanceWithinThreshold);
        result.put("matched", Boolean.TRUE.equals(result.get("statusMatched"))
                && Boolean.TRUE.equals(result.get("capabilityMatched"))
                && Boolean.TRUE.equals(result.get("rowCountMatched"))
                && Boolean.TRUE.equals(result.get("answerMatched"))
                && Boolean.TRUE.equals(result.get("permissionMatched"))
                && Boolean.TRUE.equals(result.get("failureMatched"))
                && Boolean.TRUE.equals(result.get("semanticMatched")));
        result.put("releaseEligible", Boolean.TRUE.equals(result.get("matched"))
                && performanceWithinThreshold);
        return result;
    }

    private static boolean isPermissionResult(AssistantUnifiedResult value) {
        return value != null && value.getStatus() == AssistantResultStatus.NO_PERMISSION;
    }

    private static boolean isTechnicalFailure(AssistantUnifiedResult value) {
        return value != null && (value.getStatus() == AssistantResultStatus.SYSTEM_ERROR
                || value.getStatus() == AssistantResultStatus.CAPABILITY_UNAVAILABLE);
    }
}
