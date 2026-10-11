package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通道无关的智能问数执行器。网页 SSE、企业微信机器人等入口必须复用本服务，
 * 以保证权限、审计、历史、超时和澄清行为一致。
 */
@Service
@Slf4j
public class AssistantExecutionService {

    @Resource private AssistantStore store;
    @Resource private AssistantQueryService queries;
    @Resource private AssistantModelClient model;
    @Resource private AssistantKnowledge knowledge;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantExecutionCodec codec;
    @Resource private AssistantEntityResolver entityResolver;
    @Resource private AssistantCompanyKnowledge companyKnowledge;
    @Resource private AssistantEngineRouter engineRouter;
    @Resource private AssistantV2Engine v2Engine;
    @Resource private AssistantSemanticAnalyzer semanticAnalyzer;
    @Resource private AssistantPhoneticNormalizer phoneticNormalizer;

    private final ScheduledExecutorService deadlines = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService workers = new ThreadPoolExecutor(2, 4, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), new ThreadPoolExecutor.AbortPolicy());
    private final Map<String, Running> active = new ConcurrentHashMap<>();
    private final Set<String> busy = ConcurrentHashMap.newKeySet();

    @Data
    public static class Request {
        private String conversationId;
        private String question;
        private String choiceField;
        private Long choiceId;
        private AssistantExecutionEnvelope savedEnvelope;
        /** WEB_PAGE、WEB_DRAWER、WECOM 或 RERUN。 */
        private String entry;
        /** 发起问题时所在的前端路由；仅用于灰度和审计，不参与扩大权限。 */
        private String currentPage;
    }

    public interface EventSink {
        void emit(String event, Object data) throws Exception;
        void complete();
    }

    @AllArgsConstructor
    public class Handle {
        private final String requestId;
        public String getRequestId() { return requestId; }
        public void stop(boolean timeout) { stopInternal(requestId, timeout); }
    }

    private static class Running {
        final String conversation;
        final AssistantTask task = new AssistantTask();
        final AtomicBoolean cancelled = new AtomicBoolean();
        Running(String conversation) { this.conversation = conversation; }
    }

    @Data
    private static class EngineRun {
        private AssistantExecutionEnvelope envelope;
        private AssistantPlan plan;
        private Map<String, Object> result;
        private Object clarification;
        private String status;
        private long elapsedMillis;
    }

    @PreDestroy
    public void shutdown() {
        active.values().forEach(run -> run.task.cancel());
        deadlines.shutdownNow();
        workers.shutdownNow();
    }

    public boolean cancel(String requestId) {
        Running run = active.get(requestId);
        if (run == null) {
            return false;
        }
        store.owner(run.conversation);
        run.cancelled.set(true);
        run.task.cancel();
        return true;
    }

    public boolean isConversationBusy(String conversationId) {
        return busy.contains(conversationId);
    }

    public Handle execute(Request request, EventSink sink) {
        store.owner(request.getConversationId());
        if (!busy.add(request.getConversationId())) {
            throw new AssistantFailure("BUSY", "当前会话正在查询，请稍后重试");
        }
        SecurityContext captured = SecurityContextHolder.createEmptyContext();
        captured.setAuthentication(SecurityContextHolder.getContext().getAuthentication());
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String requestId = UUID.randomUUID().toString();
        Running running = new Running(request.getConversationId());
        active.put(requestId, running);
        ScheduledFuture<?> deadline = deadlines.schedule(() -> {
            running.cancelled.set(true);
            running.task.timeout();
        }, 60, TimeUnit.SECONDS);
        try {
            workers.submit(() -> run(request, sink, requestId, running, captured, tenantId, deadline));
        } catch (RejectedExecutionException exception) {
            deadline.cancel(false);
            active.remove(requestId);
            busy.remove(request.getConversationId());
            throw new AssistantFailure("BUSY", "当前查询较多，请稍后重试");
        }
        return new Handle(requestId);
    }

    private void run(Request request, EventSink sink, String requestId, Running running,
                     SecurityContext securityContext, Long tenantId, ScheduledFuture<?> deadline) {
        SecurityContextHolder.setContext(securityContext);
        TenantContextHolder.setTenantId(tenantId);
        TenantContextHolder.setIgnore(false);
        AssistantTask.attach(running.task);
        model.beginRequest();
        long start = System.currentTimeMillis();
        String message = null;
        AssistantPlan plan = null;
        AssistantExecutionEnvelope envelope = null;
        String status = "FAILED";
        int usedTokens = 0;
        AssistantSemanticRequest semanticRequest = null;
        AssistantExecutionAudit executionAudit = null;
        try {
            AssistantTask.checkCurrent();
            AssistantPhoneticNormalizer.Normalization normalization = phoneticNormalizer == null
                    ? null : phoneticNormalizer.normalize(request.getQuestion());
            if (phoneticNormalizer != null) phoneticNormalizer.begin(normalization);
            Request executionRequest = executionRequest(request, normalization == null
                    ? request.getQuestion() : normalization.getNormalizedQuestion());
            if (normalization != null && normalization.needsClarification()) {
                message = store.begin(request.getConversationId(), AssistantModelClient.redact(request.getQuestion()));
                emit(sink, "started", Collections.singletonMap("requestId", requestId), running, start);
                emit(sink, "progress", Collections.singletonMap("text", "正在确认同音词含义"), running, start);
                envelope = phoneticEnvelope(request.getQuestion(), normalization.getNormalizedQuestion());
                phoneticNormalizer.decorateEnvelope(envelope);
                throw new AssistantOrchestrationClarification(envelope, normalization.clarification());
            }
            AssistantExecutionEnvelope savedEnvelope = request.getSavedEnvelope();
            String prior = savedEnvelope == null ? store.previous(request.getConversationId()) : null;
            AssistantExecutionEnvelope previousEnvelope = prior == null ? null : codec.read(prior, null);
            AssistantPlan previous = previousEnvelope == null ? null : previousEnvelope.getLegacyPlan();
            if (previousEnvelope != null) {
                try {
                    orchestrator.validateAccess(previousEnvelope);
                } catch (AssistantFailure revoked) {
                    previousEnvelope = null;
                    previous = null;
                }
            }
            AssistantEngineRouter.Decision decision;
            if (engineRouter == null) {
                decision = new AssistantEngineRouter.Decision(AssistantEngineMode.V1, "V1", false, false, false,
                        "COMPATIBILITY_DEFAULT");
            } else {
                semanticRequest = engineRouter.request(requestId, executionRequest, tenantId, previousEnvelope);
                if (phoneticNormalizer != null) phoneticNormalizer.decorateSemanticRequest(semanticRequest);
                decision = engineRouter.decide(semanticRequest);
            }
            executionAudit = new AssistantExecutionAudit();
            executionAudit.setConfiguredMode(decision.getConfiguredMode().name());
            executionAudit.setSelectedEngine(decision.getSelectedEngine());
            executionAudit.setDecisionReason(decision.getReason());
            executionAudit.setCapabilityFallback(decision.isCapabilityFallback());
            executionAudit.setEntryFallback(decision.isEntryFallback());

            message = store.begin(request.getConversationId(), AssistantModelClient.redact(request.getQuestion()));
            emit(sink, "started", Collections.singletonMap("requestId", requestId), running, start);
            emit(sink, "progress", Collections.singletonMap("text", "正在理解问题"), running, start);
            AssistantPlan parsePrevious = AssistantModelClient.continuationContext(executionRequest.getQuestion(), previous);
            boolean hybridSchemaReady = store.hybridAuditSchemaReady();
            if (savedEnvelope != null
                    && savedEnvelope.getRouteType() != AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC
                    && savedEnvelope.getRouteType() != AssistantExecutionEnvelope.RouteType.KNOWLEDGE
                    && !hybridSchemaReady) {
                throw new AssistantFailure("MIGRATION_REQUIRED", "智能查询审计扩展尚未完成数据库迁移");
            }
            EngineRun foreground;
            try {
                foreground = runEngine(decision.getSelectedEngine(), executionRequest, semanticRequest, previousEnvelope, previous,
                        parsePrevious, hybridSchemaReady, sink, running, start, message, true);
            } catch (Exception v2Failure) {
                if (!"V2".equals(decision.getSelectedEngine()) || !propertiesFallbackOnError()
                        || !automaticFallbackAllowed(v2Failure)) throw v2Failure;
                usedTokens += model.takeTokens();
                model.beginRequest();
                executionAudit.setAutomaticFallback(true);
                executionAudit.setFallbackCode(failureCode(v2Failure));
                foreground = runEngine("V1", executionRequest, semanticRequest, previousEnvelope, previous, parsePrevious,
                        hybridSchemaReady, sink, running, start, message, true);
                decision.setSelectedEngine("V1");
                executionAudit.setSelectedEngine("V1");
                executionAudit.setDecisionReason("V2_ERROR_FALLBACK");
            }
            envelope = foreground.getEnvelope();
            plan = foreground.getPlan();
            status = foreground.getStatus();
            decorateEnvelope(envelope, semanticRequest, executionAudit, decision.getSelectedEngine());
            AssistantUnifiedResult foregroundSummary = foreground.getClarification() == null
                    ? AssistantUnifiedResult.success(decision.getSelectedEngine(), envelope,
                    foreground.getResult(), foreground.getElapsedMillis())
                    : AssistantUnifiedResult.clarification(decision.getSelectedEngine(), envelope,
                    foreground.getElapsedMillis());
            executionAudit.setForeground(foregroundSummary);
            usedTokens += model.takeTokens();

            if (decision.isShadowV2() && request.getChoiceField() == null && savedEnvelope == null
                    && !running.cancelled.get()) {
                model.beginRequest();
                AssistantUnifiedResult shadowSummary;
                try {
                    EngineRun shadow = runEngine("V2", executionRequest, semanticRequest, previousEnvelope, previous, parsePrevious,
                            hybridSchemaReady, sink, running, start, null, false);
                    shadowSummary = shadow.getClarification() == null
                            ? AssistantUnifiedResult.success("V2", shadow.getEnvelope(), shadow.getResult(), shadow.getElapsedMillis())
                            : AssistantUnifiedResult.clarification("V2", shadow.getEnvelope(), shadow.getElapsedMillis());
                } catch (Exception shadowFailure) {
                    AssistantExecutionEnvelope failed = shadowFailure instanceof AssistantFailure
                            ? ((AssistantFailure) shadowFailure).getEnvelope() : null;
                    shadowSummary = AssistantUnifiedResult.failure("V2", failed, failureCode(shadowFailure), 0L);
                } finally {
                    usedTokens += model.takeTokens();
                }
                executionAudit.setShadowComparison(engineRouter.compareShadow(foregroundSummary, shadowSummary));
            } else if (decision.isShadowV2()) {
                Map<String, Object> skipped = new LinkedHashMap<>();
                skipped.put("executed", false);
                skipped.put("reason", "CHOICE_OR_RERUN_NOT_SHADOWED");
                executionAudit.setShadowComparison(skipped);
            }
            decorateEnvelope(envelope, semanticRequest, executionAudit, decision.getSelectedEngine());
            store.finish(message, codec.write(envelope), plan == null || plan.getMetric() == null ? null : plan.getMetric().name(),
                    knowledgeVersion(envelope), status, System.currentTimeMillis() - start, usedTokens, envelope);
            message = null;
            emit(sink, "done", AssistantEntryProtocol.done(status), running, start);
            sink.complete();
        } catch (Exception exception) {
            if (exception instanceof AssistantFailure && ((AssistantFailure) exception).getEnvelope() != null) {
                envelope = ((AssistantFailure) exception).getEnvelope();
                plan = envelope.getLegacyPlan();
            }
            decorateEnvelope(envelope, semanticRequest, executionAudit,
                    executionAudit == null ? "V1" : executionAudit.getSelectedEngine());
            if (exception instanceof AssistantOrchestrationClarification) {
                AssistantOrchestrationClarification choice = (AssistantOrchestrationClarification) exception;
                envelope = choice.getEnvelope();
                plan = envelope.getLegacyPlan();
                // 回滚到 V1 时，澄清信封仍需保留与 V2 一致的标准化语义请求，便于后续上下文和审计。
                decorateEnvelope(envelope, semanticRequest, executionAudit,
                        executionAudit == null ? "V1" : executionAudit.getSelectedEngine());
                status = "CLARIFY";
                try {
                    if (phoneticNormalizer != null && choice.getResponse() instanceof Map) {
                        phoneticNormalizer.decorateClarification((Map<String, Object>) choice.getResponse());
                    }
                    emit(sink, "clarification", AssistantEntryProtocol.clarification(choice.getResponse()), running, start);
                    emit(sink, "done", AssistantEntryProtocol.done(status), running, start);
                    sink.complete();
                } catch (Exception ignored) {
                    sink.complete();
                }
            } else {
                try {
                    running.task.check();
                } catch (AssistantFailure stopped) {
                    exception = stopped;
                }
                if (running.cancelled.get()) {
                    status = exception instanceof AssistantFailure
                            ? ((AssistantFailure) exception).getCode() : "CANCELLED";
                    String text = exception instanceof AssistantFailure
                            ? exception.getMessage() : "查询已停止或超时";
                    try {
                        // 不再经过 emit() 的取消检查，确保机器人等非 SSE 通道也能收到终态。
                        sink.emit("error", AssistantEntryProtocol.error(status, text));
                        sink.complete();
                    } catch (Exception ignored) {
                        sink.complete();
                    }
                } else {
                    String code = exception instanceof AssistantFailure ? ((AssistantFailure) exception).getCode()
                            : exception instanceof IllegalArgumentException ? "CLARIFY" : "QUERY_FAILED";
                    String text = exception instanceof AssistantFailure || exception instanceof IllegalArgumentException
                            ? exception.getMessage() : "查询未完成，请稍后重试；未返回不完整总额";
                    status = code;
                    try {
                        if (exception instanceof AssistantEntityChoice) {
                            if (envelope == null && plan != null) {
                                envelope = AssistantExecutionEnvelope.metric(request.getQuestion(), plan);
                            }
                            status = "CLARIFY";
                            Map<String,Object> response=((AssistantEntityChoice) exception).response();
                            if(phoneticNormalizer!=null) phoneticNormalizer.decorateClarification(response);
                            emit(sink, "clarification",
                                    AssistantEntryProtocol.clarification(response),
                                    running, start);
                            emit(sink, "done", AssistantEntryProtocol.done(status), running, start);
                        } else {
                            sink.emit("error", AssistantEntryProtocol.error(code, text));
                        }
                        sink.complete();
                    } catch (Exception ignored) {
                        sink.complete();
                    }
                }
            }
        } finally {
            try {
                if (message != null) {
                    usedTokens += model.takeTokens();
                    store.finish(message, codec.write(envelope),
                            plan == null || plan.getMetric() == null ? null : plan.getMetric().name(),
                            knowledgeVersion(envelope), status, System.currentTimeMillis() - start, usedTokens, envelope);
                }
            } catch (Exception ignored) {
                log.error("Assistant audit persistence failed for message {}", message);
            }
            model.takeTokens();
            deadline.cancel(false);
            active.remove(requestId);
            AssistantTask.clear();
            busy.remove(request.getConversationId());
            if (phoneticNormalizer != null) phoneticNormalizer.clear();
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private EngineRun runEngine(String engine, Request request, AssistantSemanticRequest semanticRequest,
                                AssistantExecutionEnvelope previousEnvelope,
                                AssistantPlan previous, AssistantPlan parsePrevious, boolean hybridSchemaReady,
                                EventSink sink, Running running, long requestStart, String message,
                                boolean emitEvents) throws Exception {
        long engineStart = System.currentTimeMillis();
        boolean forceV2 = "V2".equals(engine);
        if (forceV2 && !hybridSchemaReady) {
            throw new AssistantFailure("MIGRATION_REQUIRED", "V2 所需的智能查询审计扩展尚未完成数据库迁移");
        }
        AssistantExecutionEnvelope savedEnvelope = request.getSavedEnvelope();
        boolean useNew = forceV2 || (orchestrator.enabledForCurrentUser() && hybridSchemaReady)
                || savedEnvelope != null
                && savedEnvelope.getRouteType() != AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC;
        boolean orchestrationChoice = request.getChoiceField() != null && previousEnvelope != null
                && previousEnvelope.getLegacyPlan() == null;
        EngineRun run = new EngineRun();
        if (useNew && (request.getChoiceField() == null || orchestrationChoice)) {
            emitIf(emitEvents, sink, "route", Collections.singletonMap("mode", forceV2 ? "V2" : "HYBRID"),
                    running, requestStart);
            emitIf(emitEvents, sink, "tool-progress", Collections.singletonMap("text", "正在选择安全查询工具"),
                    running, requestStart);
            AssistantOrchestrator.Outcome outcome;
            if (forceV2 && v2Engine != null) {
                outcome = orchestrationChoice
                        ? v2Engine.resumeChoice(semanticRequest, previousEnvelope, request.getChoiceField(), request.getChoiceId(),
                        request.getQuestion(), authorizedCatalog())
                        : v2Engine.execute(semanticRequest,
                        savedEnvelope != null ? savedEnvelope.getQuestion() : request.getQuestion(),
                        previousEnvelope, authorizedCatalog());
            } else {
                outcome = orchestrationChoice
                        ? orchestrator.resumeChoice(previousEnvelope, request.getChoiceField(), request.getChoiceId(),
                        request.getQuestion(), authorizedCatalog())
                        : orchestrator.execute(savedEnvelope != null ? savedEnvelope.getQuestion() : request.getQuestion(),
                        previousEnvelope, authorizedCatalog());
            }
            run.setEnvelope(outcome.getEnvelope());
            run.setPlan(run.getEnvelope().getLegacyPlan());
            if (outcome.getClarification() != null) {
                run.setStatus("CLARIFY");
                run.setClarification(AssistantClarification.fromPlan(outcome.getClarification()));
            } else {
                run.setResult(outcome.getResult());
                run.setStatus(String.valueOf(run.getResult().get("status")));
            }
        } else {
            AssistantPlan plan;
            if (savedEnvelope != null) {
                plan = savedEnvelope.getLegacyPlan();
            } else if (request.getChoiceField() != null) {
                if (previous == null) throw new AssistantFailure("NOT_FOUND", "请先发起查询再选择候选对象");
                if (!request.getChoiceField().equals(previous.getPendingField())
                        || !previous.getPendingIds().contains(request.getChoiceId())) {
                    throw new AssistantFailure("INVALID_CHOICE", "候选项已失效，请重新提问");
                }
                plan = previous;
                AssistantQueryService.assignChoice(plan, request.getChoiceField(), request.getChoiceId());
                plan.setPendingField(null);
                plan.setPendingIds(new ArrayList<>());
            } else {
                plan = model.parse(request.getQuestion(), parsePrevious,
                        knowledge.retrieve(request.getQuestion(), parsePrevious, authorizedCatalog()));
            }
            AssistantModelClient.normalizePlan(request.getQuestion(),
                    savedEnvelope != null ? savedEnvelope.getLegacyPlan() : previous, plan);
            AssistantPresentation.prepare(savedEnvelope != null ? savedEnvelope.getQuestion() : request.getQuestion(), plan);
            AssistantTask.checkCurrent();
            run.setPlan(plan);
            run.setEnvelope(AssistantExecutionEnvelope.metric(request.getQuestion(), plan));
            if (plan.getClarification() != null && !plan.getClarification().trim().isEmpty()) {
                run.setStatus("CLARIFY");
                run.setClarification(AssistantClarification.fromPlan(plan));
            } else {
                emitIf(emitEvents, sink, "progress", Collections.singletonMap("text", "正在核对权限并查询"),
                        running, requestStart);
                if (entityResolver != null) entityResolver.preparePlan(plan);
                run.setResult(queries.execute(plan));
                run.setStatus(String.valueOf(run.getResult().get("status")));
            }
        }
        if (phoneticNormalizer != null) {
            if (run.getResult() != null) phoneticNormalizer.decorateResult(run.getResult());
            if (run.getClarification() instanceof Map) {
                phoneticNormalizer.decorateClarification((Map<String, Object>) run.getClarification());
            }
            phoneticNormalizer.decorateEnvelope(run.getEnvelope());
        }
        emitEngineOutput(run, message, emitEvents, sink, running, requestStart);
        run.setElapsedMillis(System.currentTimeMillis() - engineStart);
        return run;
    }

    /** V1、V2 和三个入口共用同一套结果、澄清与解释输出规则。 */
    private void emitEngineOutput(EngineRun run, String message, boolean emitEvents, EventSink sink,
                                  Running running, long requestStart) throws Exception {
        if (run.getClarification() != null) {
            emitIf(emitEvents, sink, "clarification",
                    AssistantEntryProtocol.clarification(run.getClarification()), running, requestStart);
            return;
        }
        if (run.getResult() == null) throw new AssistantFailure("SYSTEM_ERROR", "查询引擎未返回结果");
        if (message != null) run.getResult().put("messageId", message);
        emitIf(emitEvents, sink, "result",
                AssistantEntryProtocol.result(run.getResult(), run.getEnvelope(), message), running, requestStart);
        if (!emitEvents || running.cancelled.get() || System.currentTimeMillis() - requestStart >= 38_000) return;
        Object answerText = run.getResult().get("answerText");
        String explanation = answerText instanceof String && !((String) answerText).trim().isEmpty()
                ? (String) answerText : model.explain(run.getResult());
        emit(sink, "explanation", Collections.singletonMap("text", explanation), running, requestStart);
    }

    private void emitIf(boolean enabled, EventSink sink, String event, Object data,
                        Running running, long start) throws Exception {
        if (enabled) emit(sink, event, data, running, start);
        else AssistantTask.checkCurrent();
    }

    private boolean propertiesFallbackOnError() {
        return engineRouter != null && engineRouter.propertiesFallbackOnError();
    }

    private static boolean automaticFallbackAllowed(Exception exception) {
        if (!(exception instanceof AssistantFailure)) return true;
        String code = ((AssistantFailure) exception).getCode();
        return Arrays.asList("QUERY_FAILED", "SYSTEM_ERROR", "CAPABILITY_UNAVAILABLE", "MODEL_TIMEOUT",
                "TEXT_TO_SQL_DISABLED", "MIGRATION_REQUIRED").contains(code);
    }

    private static String failureCode(Exception exception) {
        return exception instanceof AssistantFailure ? ((AssistantFailure) exception).getCode() : "SYSTEM_ERROR";
    }

    private void decorateEnvelope(AssistantExecutionEnvelope envelope, AssistantSemanticRequest request,
                                  AssistantExecutionAudit audit, String engine) {
        if (envelope == null) return;
        if (semanticAnalyzer != null) semanticAnalyzer.mergeRuntime(request, envelope);
        if (phoneticNormalizer != null) {
            phoneticNormalizer.decorateSemanticRequest(request);
            phoneticNormalizer.decorateEnvelope(envelope);
        }
        envelope.setSemanticRequest(request);
        envelope.setExecutionAudit(audit);
        envelope.setEngine(engine == null ? "V1" : engine);
    }

    private List<Map<String, Object>> authorizedCatalog() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : knowledge.catalog()) {
            try {
                queries.authorize(AssistantPlan.Metric.valueOf((String) item.get("id")));
                result.add(item);
            } catch (AssistantFailure ignored) {
                // 不向模型暴露无权限指标。
            }
        }
        return result;
    }

    private String knowledgeVersion(AssistantExecutionEnvelope envelope) {
        return envelope != null && (envelope.getRouteType() == AssistantExecutionEnvelope.RouteType.KNOWLEDGE
                || envelope.getRouteType() == AssistantExecutionEnvelope.RouteType.MIXED)
                ? companyKnowledge.version() : knowledge.version();
    }

    private void emit(EventSink sink, String event, Object data, Running running, long start) throws Exception {
        AssistantTask.checkCurrent();
        if (running.cancelled.get() || System.currentTimeMillis() - start >= 60_000) {
            throw new AssistantFailure("CANCELLED", "查询已停止或超时");
        }
        sink.emit(event, data);
    }

    private void stopInternal(String requestId, boolean timeout) {
        Running running = active.get(requestId);
        if (running == null) {
            return;
        }
        running.cancelled.set(true);
        if (timeout) {
            running.task.timeout();
        } else {
            running.task.cancel();
        }
    }

    private static Request executionRequest(Request source,String question) {
        Request result=new Request();result.setConversationId(source.getConversationId());result.setQuestion(question);
        result.setChoiceField(source.getChoiceField());result.setChoiceId(source.getChoiceId());
        result.setSavedEnvelope(source.getSavedEnvelope());result.setEntry(source.getEntry());result.setCurrentPage(source.getCurrentPage());
        return result;
    }

    private static AssistantExecutionEnvelope phoneticEnvelope(String original,String normalized) {
        AssistantExecutionEnvelope result=new AssistantExecutionEnvelope();result.setQuestion(normalized);
        result.setOriginalQuestion(AssistantModelClient.redact(original));result.setNormalizedQuestion(AssistantModelClient.redact(normalized));
        result.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);result.setToolName("phonetic_clarification");
        return result;
    }
}
