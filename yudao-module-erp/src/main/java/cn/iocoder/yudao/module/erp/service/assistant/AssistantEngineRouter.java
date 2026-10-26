package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/** 构造统一请求，并依据配置选择前台引擎、影子执行和能力级回退。 */
@Component
public class AssistantEngineRouter {

    @Resource private AssistantProperties properties;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantCapabilityRegistry registry;
    @Resource private AssistantSemanticAnalyzer semanticAnalyzer;

    public AssistantSemanticRequest request(String requestId, AssistantExecutionService.Request source,
                                            Long tenantId, AssistantExecutionEnvelope previous) {
        AssistantSemanticRequest result = new AssistantSemanticRequest();
        result.setRequestId(requestId);
        result.setConversationId(source.getConversationId());
        result.setQuestion(AssistantModelClient.redact(source.getQuestion()));
        result.setSource(parseSource(source.getEntry()));
        result.setCurrentPage(safePage(source.getCurrentPage()));
        result.setTenantId(tenantId);
        result.setUserId(SecurityFrameworkUtils.getLoginUserId());
        result.setDepartmentId(SecurityFrameworkUtils.getLoginUserDeptId());
        if (source.getUnderstanding() != null) {
            AssistantUnderstandingResult understanding = source.getUnderstanding();
            AssistantUnderstandingResult.Subtask task = understanding.getTasks().get(0);
            result.setUnderstanding(understanding); result.setQueryMode(understanding.getQueryMode());
            String capability = understanding.getTasks().size() > 1 ? AssistantMixedQuery.TOOL_NAME : task.getCapability();
            if (!registry.find(capability).isPresent()) capability = "model_routed";
            result.setCapabilityId(capability);
            // 入口关闭在模型调用前由执行入口检查；具体能力逐任务检查，保留其他成功结果。
            if (containsIgnoreCase(properties.getV2().getDisabledEntries(), result.getSource().name()))
                throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "当前入口的智能问数已临时关闭");
            result.setDomain(understanding.getQueryMode().name()); result.setIntentId(task.getCapability());
            result.setRoutingReason("DEEPSEEK_UNDERSTANDING"); result.setEntityMentions(task.getEntities());
            result.setMetric(task.getConditions().getMetric() == null ? null : task.getConditions().getMetric().name());
            result.setDimensions(Collections.singletonList(task.getConditions().getGroup()));
            AssistantSemanticRequest.TimeRange time = new AssistantSemanticRequest.TimeRange();
            time.setPeriod(task.getConditions().getPeriod()); time.setStartDate(task.getConditions().getStartDate());
            time.setEndDate(task.getConditions().getEndDate()); time.setSource("DEEPSEEK_VALIDATED"); result.setTimeRange(time);
            result.getContext().setMode(understanding.getTasks().stream().anyMatch(t -> "INHERIT".equals(t.getContextMode())) ? "INHERIT" : "NEW_TOPIC");
            result.getContext().setReason("DEEPSEEK_UNDERSTANDING");
            return result;
        }
        semanticAnalyzer.analyzeFrame(result, previous, source.getQuestion());
        String capability = previewCapability(source.getQuestion(), previous);
        AssistantCapability registered = registry.getRequired(capability);
        ensureEnabled(result.getSource(), registered);
        result.setCapabilityId(capability);
        result.setQueryMode(registered.getQueryMode());
        semanticAnalyzer.finalizeRouting(result, previous, source.getQuestion());
        return result;
    }

    public Decision decide(AssistantSemanticRequest request) {
        AssistantProperties.V2 config = properties.getV2();
        AssistantEngineMode mode = AssistantEngineMode.parse(config.getMode());
        if (containsIgnoreCase(config.getFallbackEntries(), request.getSource().name())) {
            return new Decision(mode, "V1", false, false, true, "ENTRY_FALLBACK");
        }
        if (containsIgnoreCase(config.getFallbackCapabilities(), request.getCapabilityId())) {
            return new Decision(mode, "V1", false, true, false, "CAPABILITY_FALLBACK");
        }
        switch (mode) {
            case V2:
                return new Decision(mode, "V2", false, false, false, "FULL_V2");
            case V2_SHADOW:
                return new Decision(mode, "V1", true, false, false, "SHADOW_COMPARE");
            case V2_GRAY:
                boolean selected = matchesGray(config, request);
                return new Decision(mode, selected ? "V2" : "V1", false, false, false,
                        selected ? "GRAY_MATCH" : "GRAY_MISS");
            case V1:
            default:
                return new Decision(mode, "V1", false, false, false, "V2_DISABLED");
        }
    }

    public Map<String, Object> readiness() {
        AssistantProperties.V2 config = properties.getV2();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", AssistantEngineMode.parse(config.getMode()).name());
        result.put("grayTenantCount", config.getGrayTenantIds().size());
        result.put("grayUserCount", config.getGrayUserIds().size());
        result.put("grayDepartmentCount", config.getGrayDepartmentIds().size());
        result.put("grayEntries", new ArrayList<>(config.getGrayEntries()));
        result.put("grayCapabilities", new ArrayList<>(config.getGrayCapabilities()));
        result.put("fallbackEntries", new ArrayList<>(config.getFallbackEntries()));
        result.put("fallbackCapabilities", new ArrayList<>(config.getFallbackCapabilities()));
        result.put("disabledEntries", new ArrayList<>(config.getDisabledEntries()));
        result.put("disabledCapabilities", new ArrayList<>(config.getDisabledCapabilities()));
        result.put("knowledgeEnabled", config.isKnowledgeEnabled());
        result.put("mixedEnabled", config.isMixedEnabled());
        result.put("fallbackOnError", config.isFallbackOnError());
        result.put("maxShadowLatencyRatio", config.getMaxShadowLatencyRatio());
        result.put("maxShadowLatencyDeltaMillis", config.getMaxShadowLatencyDeltaMillis());
        result.put("registeredCapabilityCount", registry.all().size());
        return result;
    }

    public boolean propertiesFallbackOnError() {
        return properties.getV2().isFallbackOnError();
    }

    public Map<String, Object> compareShadow(AssistantUnifiedResult foreground, AssistantUnifiedResult shadow) {
        AssistantProperties.V2 config = properties.getV2();
        return AssistantShadowComparator.compare(foreground, shadow,
                Math.max(1.0D, config.getMaxShadowLatencyRatio()),
                Math.max(0L, config.getMaxShadowLatencyDeltaMillis()));
    }

    private String previewCapability(String question, AssistantExecutionEnvelope previous) {
        if (orchestrator == null) return "model_routed";
        String value = previous == null ? orchestrator.previewCapability(question)
                : orchestrator.previewCapability(question, previous);
        return value == null || value.trim().isEmpty() ? "model_routed" : value;
    }

    private static boolean matchesGray(AssistantProperties.V2 config, AssistantSemanticRequest request) {
        boolean configured = false;
        if (!config.getGrayTenantIds().isEmpty()) {
            configured = true;
            if (!config.getGrayTenantIds().contains(request.getTenantId())) return false;
        }
        if (!config.getGrayUserIds().isEmpty()) {
            configured = true;
            if (!config.getGrayUserIds().contains(request.getUserId())) return false;
        }
        if (!config.getGrayDepartmentIds().isEmpty()) {
            configured = true;
            if (!config.getGrayDepartmentIds().contains(request.getDepartmentId())) return false;
        }
        if (!config.getGrayEntries().isEmpty()) {
            configured = true;
            if (!containsIgnoreCase(config.getGrayEntries(), request.getSource().name())) return false;
        }
        if (!config.getGrayCapabilities().isEmpty()) {
            configured = true;
            if (!containsIgnoreCase(config.getGrayCapabilities(), request.getCapabilityId())) return false;
        }
        return configured;
    }

    private void ensureEnabled(AssistantSemanticRequest.Source source, AssistantCapability capability) {
        AssistantProperties.V2 config = properties.getV2();
        if (containsIgnoreCase(config.getDisabledEntries(), source.name())) {
            throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "当前入口的智能问数已临时关闭");
        }
        if (containsIgnoreCase(config.getDisabledCapabilities(), capability.getId())) {
            throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "该问数能力已临时关闭");
        }
        if (capability.getQueryMode() == AssistantSemanticRequest.QueryMode.KNOWLEDGE
                && !config.isKnowledgeEnabled()) {
            throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "公司知识问答已临时关闭");
        }
        if (capability.getQueryMode() == AssistantSemanticRequest.QueryMode.MIXED
                && (!config.isKnowledgeEnabled() || !config.isMixedEnabled())) {
            throw new AssistantFailure("CAPABILITY_UNAVAILABLE", "数据与知识混合问答已临时关闭");
        }
    }

    private static boolean containsIgnoreCase(Collection<String> values, String expected) {
        if (expected == null) return false;
        for (String value : values) if (expected.equalsIgnoreCase(value)) return true;
        return false;
    }

    private static AssistantSemanticRequest.Source parseSource(String value) {
        if (value == null) return AssistantSemanticRequest.Source.UNKNOWN;
        try {
            return AssistantSemanticRequest.Source.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AssistantSemanticRequest.Source.UNKNOWN;
        }
    }

    private static String safePage(String value) {
        if (value == null) return null;
        String page = value.trim();
        if (page.isEmpty()) return null;
        return page.substring(0, Math.min(500, page.length()));
    }

    @Data
    @AllArgsConstructor
    public static class Decision {
        private AssistantEngineMode configuredMode;
        private String selectedEngine;
        private boolean shadowV2;
        private boolean capabilityFallback;
        private boolean entryFallback;
        private String reason;
    }
}
