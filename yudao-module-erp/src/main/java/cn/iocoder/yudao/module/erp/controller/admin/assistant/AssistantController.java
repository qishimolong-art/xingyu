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
    @Resource private AssistantExecutionService executionService;

    @Data public static class Ask {
        @NotBlank @Size(max=36) private String conversationId;
        @lombok.ToString.Exclude @NotBlank @Size(max=2000) private String question;
        private String choiceField;
        private Long choiceId;
    }
    @GetMapping("/catalog") public CommonResult<List<Map<String,Object>>> catalog() {
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> item:knowledge.catalog()) {
            try { queries.authorize(AssistantPlan.Metric.valueOf((String)item.get("id")));result.add(item); }
            catch(AssistantFailure ignored) { /* Inaccessible metrics are not suggested. */ }
        }
        return success(result);
    }
    @GetMapping("/readiness") public CommonResult<Map<String,Object>> readiness() {
        Map<String,Object> result=new LinkedHashMap<>();
        String state="READY", text="请选择已开放的指标提问";
        if(!properties.isEnabled()) {state="DISABLED";text="智能问数尚未开放";}
        else if(!reader.configured()) {state="READ_ONLY_NOT_CONFIGURED";text="正式业务只读账号尚未配置";}
        else if(properties.getApiKey()==null || properties.getApiKey().trim().isEmpty()) {state="MODEL_NOT_CONFIGURED";text="模型服务尚未配置";}
        else try {reader.probe();} catch(AssistantFailure e) {state=e.getCode();text=e.getMessage();}
        List<Map<String,Object>> metrics=new ArrayList<>();
        int authorized=0,publishedCount=0;
        for(Map<String,Object> item:knowledge.catalog()) {
            Map<String,Object> metric=new LinkedHashMap<>();metric.put("id",item.get("id"));metric.put("title",item.get("title"));
            boolean published=Boolean.TRUE.equals(item.get("published"));
            String reason=published?state:"UNPUBLISHED",reasonText=published?text:"该指标尚未完成核验发布";
            boolean permitted=false;
            try {queries.authorize(AssistantPlan.Metric.valueOf((String)item.get("id")));permitted=true;authorized++;if(published)publishedCount++;}
            catch(AssistantFailure denied) {reason=denied.getCode();reasonText=denied.getMessage();}
            metric.put("available","READY".equals(state) && published && permitted);
            metric.put("reason",reason);metric.put("reasonText",reasonText);metrics.add(metric);
        }
        if("READY".equals(state) && authorized==0) {state="FORBIDDEN";text="没有可查询的业务指标权限";}
        else if("READY".equals(state) && publishedCount==0) {state="UNPUBLISHED";text="业务指标尚未完成核验发布";}
        result.put("status",state);result.put("text",text);result.put("metrics",metrics);result.put("knowledgeVersion",knowledge.version());
        boolean auditReady=store.hybridAuditSchemaReady();
        result.put("orchestrationMode",properties.getOrchestration().getMode());result.put("newOrchestration",orchestrator.enabledForCurrentUser() && auditReady);
        result.put("hybridAuditReady",auditReady);
        result.put("tools",auditReady?orchestrator.availableTools():Collections.emptyList());result.put("textToSql",auditReady && properties.getOrchestration().isTextToSqlEnabled());
        result.put("semanticVersion",semanticCatalog.version());result.put("datasets",semanticCatalog.publishedNames());
        result.put("semanticKnowledgeVersion",semanticKnowledge.version());
        result.put("semanticIntents",semanticKnowledge.openedIntents());
        result.put("unverifiedCapabilities",semanticKnowledge.unverifiedCapabilities());
        Map<String,Object> entitySearch=new LinkedHashMap<>();boolean entityEnabled=entityResolver!=null && entityResolver.enabled();entitySearch.put("enabled",entityEnabled);
        entitySearch.put("types",entityEnabled?entityResolver.availableTypes():Collections.emptyList());entitySearch.put("maxCandidates",Math.max(1,Math.min(20,properties.getEntitySearch().getMaxCandidates())));
        result.put("entitySearch",entitySearch);return success(result);
    }
    @PostMapping("/requests/{id}/cancel") public CommonResult<Boolean> cancel(@PathVariable String id) {
        return success(executionService.cancel(id));
    }
    @PostMapping(value="/messages/{messageId}/rerun",produces="text/event-stream")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
    public SseEmitter rerun(@PathVariable String messageId) throws IOException {
        Map<String,Object> saved=store.rerun(messageId);
        Ask ask=new Ask();ask.setConversationId((String)saved.get("conversation_id"));ask.setQuestion((String)saved.get("question"));
        AssistantExecutionEnvelope envelope=codec.read((String)saved.get("plan_json"),(String)saved.get("question"));
        if(envelope.getLegacyPlan()!=null) {AssistantPlan plan=envelope.getLegacyPlan();plan.setPendingField(null);plan.setPendingIds(new ArrayList<>());plan.setClarification(null);plan.setChoices(new ArrayList<>());}
        return start(ask,envelope);
    }
    @PostMapping("/conversation") public CommonResult<String> create() { return success(store.create()); }
    @GetMapping("/conversations") public CommonResult<List<Map<String,Object>>> list(@RequestParam(defaultValue="1") int page) { return success(store.list(page)); }
    @GetMapping("/messages") public CommonResult<List<Map<String,Object>>> history(@RequestParam String conversationId,@RequestParam(defaultValue="1") int page) { return success(store.messages(conversationId,page)); }
    @DeleteMapping("/conversation") public CommonResult<Boolean> delete(@RequestParam String id) {
        if(executionService.isConversationBusy(id)) throw new AssistantFailure("BUSY","请先停止当前查询"); store.delete(id);return success(true);
    }
    @GetMapping("/details") public CommonResult<Map<String,Object>> details(@RequestParam String messageId,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize) throws IOException {
        return success(orchestrator.details(codec.read(store.queryPlan(messageId),null),page,pageSize));
    }
    @GetMapping("/messages/{messageId}/audit")
    @PreAuthorize("@ss.hasPermission('erp:assistant:query') && @ss.hasPermission('erp:assistant:audit')")
    public CommonResult<Map<String,Object>> audit(@PathVariable String messageId) {return success(store.audit(messageId));}
    @PostMapping(value="/send",produces="text/event-stream")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
    public SseEmitter send(@Valid @RequestBody Ask ask) { return start(ask,null); }
    private SseEmitter start(Ask ask,AssistantExecutionEnvelope savedEnvelope) {
        SseEmitter emitter=new SseEmitter(60_000L);
        AssistantExecutionService.Request request=new AssistantExecutionService.Request();
        request.setConversationId(ask.getConversationId());request.setQuestion(ask.getQuestion());
        request.setChoiceField(ask.getChoiceField());request.setChoiceId(ask.getChoiceId());request.setSavedEnvelope(savedEnvelope);
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
