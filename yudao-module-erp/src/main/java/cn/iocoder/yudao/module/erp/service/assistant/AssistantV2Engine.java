package cn.iocoder.yudao.module.erp.service.assistant;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * V2 独立执行边界。阶段 2 先复用已经验证的只读编排内核；阶段 3～5 只替换本边界内部实现，
 * V1 前台链路、入口协议和回退开关无需再次改动。
 */
@Component
public class AssistantV2Engine {

    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantCapabilityRegistry capabilities;
    @Resource private AssistantSemanticAnalyzer semantics;

    public AssistantOrchestrator.Outcome execute(String question, AssistantExecutionEnvelope previous,
                                                 List<Map<String, Object>> metricCatalog) {
        return execute(null, question, previous, metricCatalog);
    }

    public AssistantOrchestrator.Outcome execute(AssistantSemanticRequest request, String question,
                                                 AssistantExecutionEnvelope previous,
                                                 List<Map<String, Object>> metricCatalog) {
        if (request != null && semantics.needsContextClarification(request)) {
            AssistantExecutionEnvelope envelope = semanticEnvelope(request, question);
            semantics.mergeRuntime(request, envelope);
            throw new AssistantOrchestrationClarification(envelope, semantics.contextClarification(request));
        }
        AssistantExecutionEnvelope executionContext = request == null ? previous
                : semantics.executionContext(request, previous);
        try {
            return enrich(validate(orchestrator.execute(question, executionContext, metricCatalog)), request);
        } catch (AssistantOrchestrationClarification clarification) {
            enrich(clarification.getEnvelope(), request);
            AssistantClarification.normalize(clarification.getResponse());
            throw clarification;
        } catch (AssistantFailure failure) {
            AssistantExecutionEnvelope envelope = failure.getEnvelope();
            if (envelope == null) envelope = semanticEnvelope(request, question);
            enrich(envelope, request);
            AssistantOrchestrationClarification clarification = structured(failure, envelope, request);
            if (clarification != null) throw clarification;
            throw failure.attachEnvelope(envelope);
        }
    }

    public AssistantOrchestrator.Outcome resumeChoice(AssistantExecutionEnvelope previous, String field, Long id,
                                                      String question,
                                                      List<Map<String, Object>> metricCatalog) {
        return resumeChoice(null, previous, field, id, question, metricCatalog);
    }

    public AssistantOrchestrator.Outcome resumeChoice(AssistantSemanticRequest request,
                                                      AssistantExecutionEnvelope previous, String field, Long id,
                                                      String question,
                                                      List<Map<String, Object>> metricCatalog) {
        try {
            return enrich(validate(orchestrator.resumeChoice(previous, field, id, question, metricCatalog)), request);
        } catch (AssistantOrchestrationClarification clarification) {
            enrich(clarification.getEnvelope(), request);
            AssistantClarification.normalize(clarification.getResponse());
            throw clarification;
        } catch (AssistantFailure failure) {
            AssistantExecutionEnvelope envelope = failure.getEnvelope();
            if (envelope == null) envelope = semanticEnvelope(request, question);
            enrich(envelope, request);
            AssistantOrchestrationClarification clarification = structured(failure, envelope, request);
            if (clarification != null) throw clarification;
            throw failure.attachEnvelope(envelope);
        }
    }

    private static AssistantOrchestrationClarification structured(AssistantFailure failure,
                                                                  AssistantExecutionEnvelope envelope,
                                                                  AssistantSemanticRequest request) {
        if (failure instanceof AssistantEntityChoice) {
            return new AssistantOrchestrationClarification(envelope,
                    AssistantClarification.normalize(((AssistantEntityChoice) failure).response()));
        }
        if (!java.util.Arrays.asList("CLARIFY", "CLARIFY_ENTITY", "INVALID_CHOICE").contains(failure.getCode())) {
            return null;
        }
        java.util.List<String> missing = request == null || request.getMissingFields().isEmpty()
                ? java.util.Collections.singletonList("queryCondition") : request.getMissingFields();
        return new AssistantOrchestrationClarification(envelope,
                AssistantClarification.missing(failure.getCode(), failure.getMessage(), missing,
                        java.util.Collections.emptyList()));
    }

    private AssistantOrchestrator.Outcome enrich(AssistantOrchestrator.Outcome outcome,
                                                 AssistantSemanticRequest request) {
        if (outcome != null) enrich(outcome.getEnvelope(), request);
        return outcome;
    }

    private void enrich(AssistantExecutionEnvelope envelope, AssistantSemanticRequest request) {
        if (envelope == null || request == null) return;
        semantics.mergeRuntime(request, envelope);
        envelope.setSemanticRequest(request);
    }

    private static AssistantExecutionEnvelope semanticEnvelope(AssistantSemanticRequest request, String question) {
        AssistantExecutionEnvelope envelope = new AssistantExecutionEnvelope();
        envelope.setQuestion(AssistantModelClient.redact(question));
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        envelope.setToolName(request == null || request.getCapabilityId() == null
                ? "model_routed" : request.getCapabilityId());
        envelope.setSemanticRequest(request);
        return envelope;
    }

    private AssistantOrchestrator.Outcome validate(AssistantOrchestrator.Outcome outcome) {
        if (outcome == null || outcome.getEnvelope() == null) {
            throw new AssistantFailure("SYSTEM_ERROR", "V2 未返回结构化执行结果");
        }
        capabilities.getRequired(outcome.getEnvelope().getToolName());
        return outcome;
    }
}
