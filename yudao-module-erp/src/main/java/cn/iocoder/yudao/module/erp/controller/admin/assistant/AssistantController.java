package cn.iocoder.yudao.module.erp.controller.admin.assistant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.assistant.*;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.io.IOException;
import java.util.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@lombok.extern.slf4j.Slf4j
@RequestMapping("/erp/assistant")
@PreAuthorize("@ss.hasPermission('erp:assistant:query')")
public class AssistantController {
    @Resource private AssistantStore store;
    @Resource private AssistantQueryService queries;
    @Resource private AssistantModelClient model;
    @Resource private AssistantKnowledge knowledge;
    @Resource private AssistantProperties properties;
    @Resource private AssistantReadOnly reader;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantExecutionCodec codec;
    @Resource private AssistantSemanticCatalog semanticCatalog;
    @Resource private AssistantEntityResolver entityResolver;
    @Resource private AssistantSemanticKnowledge semanticKnowledge;
    @Resource private AssistantCompanyKnowledge companyKnowledge;
    @Resource private AssistantExecutionService executionService;
    @Resource private AssistantEngineRouter engineRouter;
    @Resource private AssistantCapabilityRegistry capabilityRegistry;
    @Resource private AssistantReadinessService readinessService;

    @Data public static class Ask {
        @NotBlank @Size(max=36) private String conversationId;
        @lombok.ToString.Exclude @NotBlank @Size(max=2000) private String question;
        private String choiceField;
        private Long choiceId;
        @Size(max=32) private String entry;
        @Size(max=500) private String currentPage;
    }
    @GetMapping("/catalog") public CommonResult<List<Map<String,Object>>> catalog() {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> item:knowledge.catalog()) {
            try { queries.authorize(AssistantPlan.Metric.valueOf((String)item.get("id")));result.add(item); }
            catch(AssistantFailure ignored) { /* Inaccessible metrics are not suggested. */ }
        }
        return success(result);
    }
    @GetMapping("/readiness") public CommonResult<Map<String,Object>> readiness(
            @RequestParam(defaultValue="false") boolean refresh) {
        return success(readinessService.get(refresh));
    }

    /** Kept as a source-level compatibility helper for direct controller tests. */
    public CommonResult<Map<String,Object>> readiness() {
        return readiness(false);
    }

    @PostMapping("/requests/{id}/cancel") public CommonResult<Boolean> cancel(@PathVariable String id) {
        return success(executionService.cancel(id));
    }
    @PostMapping(value="/messages/{messageId}/rerun",produces="text/event-stream")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
    public SseEmitter rerun(@PathVariable String messageId) throws IOException {
        Map<String,Object> saved=store.rerun(messageId);
        Ask ask=new Ask();ask.setConversationId((String)saved.get("conversation_id"));ask.setQuestion((String)saved.get("question"));
        ask.setEntry("RERUN");
        AssistantExecutionEnvelope envelope=codec.read((String)saved.get("plan_json"),(String)saved.get("question"));
        if(envelope.getLegacyPlan()!=null) {AssistantPlan plan=envelope.getLegacyPlan();plan.setPendingField(null);plan.setPendingIds(new ArrayList<>());plan.setClarification(null);plan.setChoices(new ArrayList<>());}
        return start(ask,envelope);
    }
    @PostMapping("/conversation") public CommonResult<String> create() { return success(store.create()); }
    @GetMapping("/conversations") public CommonResult<List<Map<String,Object>>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize) { return success(store.list(page,pageSize)); }
    @GetMapping("/messages") public CommonResult<List<Map<String,Object>>> history(@RequestParam String conversationId,@RequestParam(defaultValue="1") int page) { return success(store.messages(conversationId,page)); }
    @DeleteMapping("/conversation") public CommonResult<Boolean> delete(@RequestParam String id) {
        if(executionService.isConversationBusy(id)) throw new AssistantFailure("BUSY","请先停止当前查询"); store.delete(id);return success(true);
    }
    @GetMapping("/details") public CommonResult<Map<String,Object>> details(@RequestParam String messageId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize,@RequestParam(required=false) String taskId) throws IOException {
        return success(orchestrator.details(codec.read(store.queryPlan(messageId),null),page,pageSize,taskId));
    }
    @GetMapping("/messages/{messageId}/audit")
    @PreAuthorize("@ss.hasPermission('erp:assistant:query') && @ss.hasPermission('erp:assistant:audit')")
    public CommonResult<Map<String,Object>> audit(@PathVariable String messageId) {return success(store.audit(messageId));}
    @GetMapping("/rollout-summary")
    @PreAuthorize("@ss.hasPermission('erp:assistant:query') && @ss.hasPermission('erp:assistant:audit')")
    public CommonResult<Map<String,Object>> rolloutSummary(@RequestParam(defaultValue="24") @Min(1) @Max(720) int hours) {
        Map<String,Object> result=store.rolloutSummary(hours);
        result.put("configuration",engineRouter==null?Collections.emptyMap():engineRouter.readiness());
        return success(result);
    }
    @PostMapping(value="/send",produces="text/event-stream")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
    public SseEmitter send(@Valid @RequestBody Ask ask) { return start(ask,null); }
    private SseEmitter start(Ask ask,AssistantExecutionEnvelope savedEnvelope) {
        SseEmitter emitter=new SseEmitter(60_000L);
        AssistantExecutionService.Request request=new AssistantExecutionService.Request();
        request.setConversationId(ask.getConversationId());request.setQuestion(ask.getQuestion());
        request.setChoiceField(ask.getChoiceField());request.setChoiceId(ask.getChoiceId());request.setSavedEnvelope(savedEnvelope);
        request.setEntry(ask.getEntry()==null?"WEB_PAGE":ask.getEntry());request.setCurrentPage(ask.getCurrentPage());
        AssistantExecutionService.Handle handle=executionService.execute(request,new AssistantExecutionService.EventSink() {
            @Override public void emit(String event,Object data) throws IOException {emitter.send(SseEmitter.event().name(event).data(data));}
            @Override public void complete() {emitter.complete();}
        });
        emitter.onCompletion(()->handle.stop(false));
        emitter.onTimeout(()->handle.stop(true));
        emitter.onError(e->handle.stop(false));
        return emitter;
    }
    @ExceptionHandler(AssistantFailure.class)
    public CommonResult<Object> failure(AssistantFailure failure) { return CommonResult.error(1_030_990_001,failure.getMessage()); }
}
