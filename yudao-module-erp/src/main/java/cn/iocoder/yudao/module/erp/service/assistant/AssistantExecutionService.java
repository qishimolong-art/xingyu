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
        try {
            AssistantTask.checkCurrent();
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
            message = store.begin(request.getConversationId(), AssistantModelClient.redact(request.getQuestion()));
            emit(sink, "started", Collections.singletonMap("requestId", requestId), running, start);
            emit(sink, "progress", Collections.singletonMap("text", "正在理解问题"), running, start);
            AssistantPlan parsePrevious = AssistantModelClient.continuationContext(request.getQuestion(), previous);
            boolean hybridSchemaReady = store.hybridAuditSchemaReady();
            if (savedEnvelope != null
                    && savedEnvelope.getRouteType() != AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC
                    && !hybridSchemaReady) {
                throw new AssistantFailure("MIGRATION_REQUIRED", "智能查询审计扩展尚未完成数据库迁移");
            }
            boolean useNew = (orchestrator.enabledForCurrentUser() && hybridSchemaReady)
                    || savedEnvelope != null
                    && savedEnvelope.getRouteType() != AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC;
            boolean orchestrationChoice = request.getChoiceField() != null && previousEnvelope != null
                    && previousEnvelope.getLegacyPlan() == null;
            if (useNew && (request.getChoiceField() == null || orchestrationChoice)) {
                emit(sink, "route", Collections.singletonMap("mode", "HYBRID"), running, start);
                emit(sink, "tool-progress", Collections.singletonMap("text", "正在选择安全查询工具"), running, start);
                AssistantOrchestrator.Outcome outcome = orchestrationChoice
                        ? orchestrator.resumeChoice(previousEnvelope, request.getChoiceField(), request.getChoiceId(),
                        request.getQuestion(), authorizedCatalog())
                        : orchestrator.execute(savedEnvelope != null ? savedEnvelope.getQuestion() : request.getQuestion(),
                        previousEnvelope, authorizedCatalog());
                envelope = outcome.getEnvelope();
                plan = envelope.getLegacyPlan();
                if (outcome.getClarification() != null) {
                    status = "CLARIFY";
                    emit(sink, "clarification", outcome.getClarification(), running, start);
                } else {
                    Map<String, Object> result = outcome.getResult();
                    result.put("messageId", message);
                    status = (String) result.get("status");
                    emit(sink, "result", result, running, start);
                    if (!running.cancelled.get() && System.currentTimeMillis() - start < 38_000) {
                        Object answerText = result.get("answerText");
                        String explanation = answerText instanceof String && !((String) answerText).trim().isEmpty()
                                ? (String) answerText : model.explain(result);
                        emit(sink, "explanation", Collections.singletonMap("text", explanation), running, start);
                    }
                }
            } else {
                if (savedEnvelope != null) {
                    plan = savedEnvelope.getLegacyPlan();
                } else if (request.getChoiceField() != null) {
                    if (previous == null) {
                        throw new AssistantFailure("NOT_FOUND", "请先发起查询再选择候选对象");
                    }
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
                        savedEnvelope != null ? savedEnvelope.getLegacyPlan() : parsePrevious, plan);
                AssistantPresentation.prepare(savedEnvelope != null ? savedEnvelope.getQuestion() : request.getQuestion(), plan);
                AssistantTask.checkCurrent();
                if (plan.getClarification() != null && !plan.getClarification().trim().isEmpty()) {
                    status = "CLARIFY";
                    emit(sink, "clarification", plan, running, start);
                } else {
                    emit(sink, "progress", Collections.singletonMap("text", "正在核对权限并查询"), running, start);
                    if (entityResolver != null) {
                        entityResolver.preparePlan(plan);
                    }
                    Map<String, Object> result = queries.execute(plan);
                    result.put("messageId", message);
                    status = (String) result.get("status");
                    emit(sink, "result", result, running, start);
                    if (!running.cancelled.get() && System.currentTimeMillis() - start < 38_000) {
                        emit(sink, "explanation", Collections.singletonMap("text", model.explain(result)), running, start);
                    }
                }
                envelope = AssistantExecutionEnvelope.metric(request.getQuestion(), plan);
            }
            store.finish(message, codec.write(envelope), plan == null || plan.getMetric() == null ? null : plan.getMetric().name(),
                    knowledge.version(), status, System.currentTimeMillis() - start, model.takeTokens(), envelope);
            message = null;
            emit(sink, "done", Collections.singletonMap("status", status), running, start);
            sink.complete();
        } catch (Exception exception) {
            if (exception instanceof AssistantFailure && ((AssistantFailure) exception).getEnvelope() != null) {
                envelope = ((AssistantFailure) exception).getEnvelope();
                plan = envelope.getLegacyPlan();
            }
            if (exception instanceof AssistantOrchestrationClarification) {
                AssistantOrchestrationClarification choice = (AssistantOrchestrationClarification) exception;
                envelope = choice.getEnvelope();
                plan = envelope.getLegacyPlan();
                status = "CLARIFY";
                try {
                    emit(sink, "clarification", choice.getResponse(), running, start);
                    emit(sink, "done", Collections.singletonMap("status", status), running, start);
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
                        Map<String, String> error = new HashMap<>();
                        error.put("code", status);
                        error.put("text", text);
                        // 不再经过 emit() 的取消检查，确保机器人等非 SSE 通道也能收到终态。
                        sink.emit("error", error);
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
                            emit(sink, "clarification", ((AssistantEntityChoice) exception).response(), running, start);
                            emit(sink, "done", Collections.singletonMap("status", status), running, start);
                        } else {
                            Map<String, String> error = new HashMap<>();
                            error.put("code", code);
                            error.put("text", text);
                            sink.emit("error", error);
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
                    store.finish(message, codec.write(envelope),
                            plan == null || plan.getMetric() == null ? null : plan.getMetric().name(),
                            knowledge.version(), status, System.currentTimeMillis() - start, model.takeTokens(), envelope);
                }
            } catch (Exception ignored) {
                log.error("Assistant audit persistence failed for message {}", message);
            }
            model.takeTokens();
            deadline.cancel(false);
            active.remove(requestId);
            AssistantTask.clear();
            busy.remove(request.getConversationId());
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
        }
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
}
