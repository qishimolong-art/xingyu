package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import javax.annotation.Resource;
import java.util.*;

@Repository
public class AssistantStore {
    private static final String ADMIN_OWNER = "ADMIN";
    private static final Set<String> VISIBLE_HISTORY_STATUSES = new HashSet<>(Arrays.asList(
            "SUCCESS", "EMPTY", "PARTIAL_SUCCESS", "KNOWLEDGE_NOT_FOUND", "UNVERIFIED_KNOWLEDGE", "CLARIFY", "TASKS_INCOMPLETE"));
    @Resource private JdbcTemplate jdbc;
    @Resource private AssistantProperties properties;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantExecutionCodec codec;
    @Resource private AssistantReadOnly reader;
    @Resource private PermissionApi permissions;
    private final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper();
    private volatile long auditSchemaCheckedAt;
    private volatile boolean auditSchemaReady;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private Long user() { Long uid=SecurityFrameworkUtils.getLoginUserId(); if(uid==null) throw new AssistantFailure("FORBIDDEN","请先登录"); return uid; }
    public void enabled() { if(!properties.isEnabled()) throw new AssistantFailure("DISABLED","智能问数尚未开放"); }
    public void owner(String id) {
        enabled();
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM erp_assistant_conversation WHERE id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",Integer.class,id,tenant(),user(),ADMIN_OWNER);
        if(n==null || n!=1) throw new AssistantFailure("NOT_FOUND","会话不存在或不可访问");
    }
    public String create() {
        enabled(); String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_conversation(id,tenant_id,user_id,owner_type,customer_id,entry,title) VALUES(?,?,?,?,?,?,?)",id,tenant(),user(),ADMIN_OWNER,null,"ADMIN_WEB","新会话"); return id;
    }
    public List<Map<String,Object>> list(int page) { return list(page,20); }
    public List<Map<String,Object>> list(int page,int pageSize) {
        enabled(); if(page<1 || page>10000 || pageSize<1 || pageSize>50) throw new IllegalArgumentException("分页无效");
        Long tenantId=tenant(),userId=user();
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,title,update_time FROM erp_assistant_conversation WHERE tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0 ORDER BY update_time DESC,id LIMIT ? OFFSET ?",tenantId,userId,ADMIN_OWNER,pageSize,(page-1)*pageSize);
        if(rows.isEmpty()) return rows;
        List<String> ids=new ArrayList<>();for(Map<String,Object> row:rows) ids.add(String.valueOf(row.get("id")));
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        List<Object> args=new ArrayList<>(ids);args.add(tenantId);args.add(userId);args.add(ADMIN_OWNER);
        List<Map<String,Object>> messages=jdbc.queryForList(
                "SELECT m.conversation_id,m.question,m.status,m.plan_json FROM erp_assistant_message m WHERE m.conversation_id IN ("+placeholders+") AND m.tenant_id=? AND m.user_id=? AND m.owner_type=? AND m.customer_id IS NULL AND m.deleted=0 AND NOT EXISTS (SELECT 1 FROM erp_assistant_message earlier WHERE earlier.conversation_id=m.conversation_id AND earlier.tenant_id=m.tenant_id AND earlier.user_id=m.user_id AND earlier.owner_type=m.owner_type AND earlier.customer_id IS NULL AND earlier.deleted=0 AND (earlier.create_time<m.create_time OR (earlier.create_time=m.create_time AND earlier.id<m.id)))",
                args.toArray());
        Map<String,Map<String,Object>> firstByConversation=new HashMap<>();
        for(Map<String,Object> message:messages) firstByConversation.put(String.valueOf(message.get("conversation_id")),message);
        Map<String,Boolean> titleAccess=titleAccess(messages);
        for(Map<String,Object> row:rows) {
            String id=String.valueOf(row.get("id"));
            row.put("title",conversationTitle(firstByConversation.get(id),Boolean.TRUE.equals(titleAccess.get(id))));
        }
        return rows;
    }
    String conversationTitle(Map<String,Object> message) {
        if(message==null) return "新会话";
        Object rawPlan=message.get("plan_json");
        boolean eligible=rawPlan!=null && VISIBLE_HISTORY_STATUSES.contains(String.valueOf(message.get("status")));
        return conversationTitle(message,eligible && visible(rawPlan));
    }
    private String conversationTitle(Map<String,Object> message,boolean currentlyVisible) {
        if(message==null) return "新会话";
        String status=String.valueOf(message.get("status"));Object rawPlan=message.get("plan_json");
        if(!VISIBLE_HISTORY_STATUSES.contains(status) || rawPlan==null || !currentlyVisible) return "历史会话";
        String question=String.valueOf(message.get("question")).trim();if(question.isEmpty() || "null".equals(question)) return "历史会话";
        int length=question.codePointCount(0,question.length());
        int end=question.offsetByCodePoints(0,Math.min(60,length));return question.substring(0,end);
    }
    private Map<String,Boolean> titleAccess(List<Map<String,Object>> messages) {
        Map<String,AssistantExecutionEnvelope> envelopes=new LinkedHashMap<>();
        Map<String,Boolean> result=new LinkedHashMap<>();
        for(Map<String,Object> message:messages) {
            String id=String.valueOf(message.get("conversation_id"));
            result.put(id,false);
            Object raw=message.get("plan_json");
            if(raw==null || !VISIBLE_HISTORY_STATUSES.contains(String.valueOf(message.get("status")))) continue;
            try {AssistantExecutionEnvelope envelope=codec.read(String.valueOf(raw),null);if(envelope!=null) envelopes.put(id,envelope);}
            catch(Exception ignored) { /* Invalid persisted plans must not reveal the original question. */ }
        }
        if(envelopes.isEmpty()) return result;
        return permissions.executeInReadSnapshot(()->validateTitleAccess(envelopes,result));
    }
    private Map<String,Boolean> validateTitleAccess(Map<String,AssistantExecutionEnvelope> envelopes,Map<String,Boolean> result) {
        List<Map.Entry<String,AssistantExecutionEnvelope>> legacy=new ArrayList<>();
        for(Map.Entry<String,AssistantExecutionEnvelope> entry:envelopes.entrySet()) {
            if(entry.getValue().getLegacyPlan()!=null) legacy.add(entry);
            else validateTitleAccess(entry,result);
        }
        if(!legacy.isEmpty()) try {
            reader.snapshot(()->{for(Map.Entry<String,AssistantExecutionEnvelope> entry:legacy) validateTitleAccess(entry,result);return true;});
        } catch(Exception ignored) { /* Legacy titles remain hidden when the read-only snapshot is unavailable. */ }
        return result;
    }
    private void validateTitleAccess(Map.Entry<String,AssistantExecutionEnvelope> entry,Map<String,Boolean> result) {
        try {orchestrator.validateAccess(entry.getValue());result.put(entry.getKey(),true);}
        catch(Exception ignored) { /* Each inaccessible title fails closed without failing the page. */ }
    }
    public List<Map<String,Object>> messages(String id,int page) {
        owner(id); if(page<1 || page>10000) throw new IllegalArgumentException("分页无效");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,question,status,create_time,plan_json FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0 ORDER BY create_time DESC,id DESC LIMIT 20 OFFSET ?",id,tenant(),user(),ADMIN_OWNER,(page-1)*20);
        for(Map<String,Object> row:rows) {
            Object plan=row.remove("plan_json");
            boolean visible=visible(plan);
            row.put("canRerun",visible && Arrays.asList("SUCCESS","EMPTY","PARTIAL_SUCCESS",
                    "KNOWLEDGE_NOT_FOUND","UNVERIFIED_KNOWLEDGE").contains(row.get("status")));
            if(!visible) row.put("question","此历史问题暂无可访问的查询条件，请重新提问");
        }
        return rows;
    }
    private boolean visible(Object plan) {
        try { if(plan==null) return false; orchestrator.validateAccess(codec.read(plan.toString(),null));return true; }
        catch(Exception e) {return false;}
    }
    public String previous(String id) {
        owner(id);
        // Only the immediately preceding message may contribute context.  Looking for the
        // newest successful row skips over a failed/new-object question and revives stale
        // entities from several turns earlier.
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT status,plan_json FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0 ORDER BY create_time DESC,id DESC LIMIT 1",id,tenant(),user(),ADMIN_OWNER);
        return previousContext(rows);
    }

    String previousContext(List<Map<String,Object>> rows) {
        if(rows==null || rows.isEmpty()) return null;
        Map<String,Object> latest=rows.get(0);String status=String.valueOf(latest.get("status"));
        Object rawPlan=latest.get("plan_json");String plan=rawPlan==null?null:String.valueOf(rawPlan);
        if(plan==null || plan.trim().isEmpty()) return null;
        if("SUCCESS".equals(status) || "EMPTY".equals(status)) return plan;
          if (Arrays.asList("PARTIAL_SUCCESS","TASKS_INCOMPLETE","UNVERIFIED_KNOWLEDGE","KNOWLEDGE_NOT_FOUND").contains(status)) {
              try {
                  com.fasterxml.jackson.databind.JsonNode root=json.readTree(plan);
                  if (root.path("understanding").isObject() && ("PARTIAL_SUCCESS".equals(status) || usefulClarification(plan))) return plan;
              } catch (Exception ignored) { return null; }
          }
        return "CLARIFY".equals(status) && usefulClarification(plan)?plan:null;
    }

    private boolean usefulClarification(String plan) {
        try {
            com.fasterxml.jackson.databind.JsonNode root=json.readTree(plan);
            com.fasterxml.jackson.databind.JsonNode arguments=root.path("arguments");
              for (com.fasterxml.jackson.databind.JsonNode child : root.path("subtasks")) {
                  com.fasterxml.jackson.databind.JsonNode childArguments=child.path("arguments");
                  if (childArguments.path("pendingEntities").size()>0) return true;
              }
            if(arguments.path("resolvedEntity").isObject() || arguments.path("pendingEntities").size()>0) return true;
            com.fasterxml.jackson.databind.JsonNode legacy=root.has("legacyPlan")?root.path("legacyPlan"):root;
            return legacy.path("pendingField").isTextual() && legacy.path("pendingIds").size()>0;
        } catch(Exception ignored) {return false;}
    }
    public String begin(String id,String question) {
        owner(id); String message=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_message(id,conversation_id,tenant_id,user_id,owner_type,customer_id,entry,question,status) VALUES(?,?,?,?,?,?,?,?,?)",message,id,tenant(),user(),ADMIN_OWNER,null,"ADMIN",question,"PROCESSING");
        String title=question.substring(0,question.offsetByCodePoints(0,Math.min(60,question.codePointCount(0,question.length()))));
        jdbc.update("UPDATE erp_assistant_conversation SET title=CASE WHEN title IS NULL OR title='' OR title='新会话' THEN ? ELSE title END,update_time=NOW(6) WHERE id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",title,id,tenant(),user(),ADMIN_OWNER); return message;
    }
    @Transactional
    public void finish(String message,String plan,String metric,String version,String status,long elapsed,int tokens) {
        finish(message,plan,metric,version,status,elapsed,tokens,null);
    }
    @Transactional
    public void finish(String message,String plan,String metric,String version,String status,long elapsed,int tokens,AssistantExecutionEnvelope envelope) {
        jdbc.update("UPDATE erp_assistant_message SET plan_json=?,status=? WHERE id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",plan,status,message,tenant(),user(),ADMIN_OWNER);
        if(!hybridAuditSchemaReady()) {
            jdbc.update("INSERT INTO erp_assistant_audit(id,message_id,tenant_id,user_id,owner_type,customer_id,entry,metric,knowledge_version,status,condition_summary,elapsed_ms,tokens) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID().toString(),message,tenant(),user(),ADMIN_OWNER,null,"ADMIN",metric,version,status,auditSummary(plan),elapsed,tokens);
            return;
        }
        Map<String,Object> trace=envelope==null?Collections.emptyMap():envelope.getResultMetadata();Object raw=trace.get("queryTrace");
        Map<?,?> queryTrace=raw instanceof Map?(Map<?,?>)raw:Collections.emptyMap();
        jdbc.update("INSERT INTO erp_assistant_audit(id,message_id,tenant_id,user_id,owner_type,customer_id,entry,metric,knowledge_version,status,condition_summary,elapsed_ms,tokens,route_type,tool_name,sql_fingerprint,query_trace,row_count) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(),message,tenant(),user(),ADMIN_OWNER,null,"ADMIN",metric,version,status,auditSummary(plan),elapsed,tokens,
                envelope==null?"VERIFIED_METRIC":String.valueOf(envelope.getRouteType()),envelope==null?"query_verified_metric":envelope.getToolName(),
                queryTrace.get("fingerprint"),safeTrace(queryTrace),number(queryTrace.get("rowCount")));
    }
    public boolean hybridAuditSchemaReady() {
        long now=System.currentTimeMillis();
        if(now-auditSchemaCheckedAt<30_000) return auditSchemaReady;
        try {
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_assistant_audit' AND column_name IN ('route_type','tool_name','sql_fingerprint','query_trace','row_count')",Integer.class);
            auditSchemaReady=count!=null && count==5;
        } catch(Exception ignored) {auditSchemaReady=false;}
        auditSchemaCheckedAt=now;return auditSchemaReady;
    }
    private Integer number(Object value){return value instanceof Number?((Number)value).intValue():null;}
    private String safeTrace(Map<?,?> value){try{return value.isEmpty()?null:json.writeValueAsString(value);}catch(Exception e){return "{\"validation\":\"INVALID_TRACE\"}";}}
    String auditSummary(String plan) {
        if(plan==null) return null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root=json.readTree(plan);
            com.fasterxml.jackson.databind.JsonNode input=root.hasNonNull("legacyPlan")?root.get("legacyPlan"):root;
            Map<String,Object> safe=new LinkedHashMap<>();
            if(root.hasNonNull("routeType")) safe.put("routeType",root.get("routeType"));
            if(root.hasNonNull("toolName")) safe.put("toolName",root.get("toolName"));
            for(String key:Arrays.asList("metric","group","period","startDate","endDate","stockMode","limit","rankOrder"))
                if(input.hasNonNull(key)) safe.put(key,input.get(key));
            for(String key:Arrays.asList("warehouse","party","product","department"))
                safe.put(key+"Filtered",input.hasNonNull(key) || input.hasNonNull(key+"Id"));
            return json.writeValueAsString(safe);
        } catch(Exception ignored) { return "{\"invalidPlan\":true}"; }
    }
    public String queryPlan(String message) {
        enabled(); List<Map<String,Object>> rows=jdbc.queryForList("SELECT m.plan_json,m.conversation_id FROM erp_assistant_message m JOIN erp_assistant_conversation c ON c.id=m.conversation_id AND c.tenant_id=m.tenant_id AND c.user_id=m.user_id AND c.owner_type=m.owner_type AND c.customer_id IS NULL AND c.deleted=0 WHERE m.id=? AND m.tenant_id=? AND m.user_id=? AND m.owner_type=? AND m.customer_id IS NULL AND m.deleted=0 AND m.status IN ('SUCCESS','EMPTY','PARTIAL_SUCCESS','KNOWLEDGE_NOT_FOUND','UNVERIFIED_KNOWLEDGE')",message,tenant(),user(),ADMIN_OWNER);
        if(rows.size()!=1) throw new AssistantFailure("NOT_FOUND","查询记录不存在或不可访问");
        return (String)rows.get(0).get("plan_json");
    }
    public Map<String,Object> rerun(String message) {
        String plan=queryPlan(message);
        if(!visible(plan)) throw new AssistantFailure("FORBIDDEN","原查询条件已不可访问，请重新提问");
        return jdbc.queryForMap("SELECT conversation_id,question,plan_json FROM erp_assistant_message WHERE id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",message,tenant(),user(),ADMIN_OWNER);
    }
    public Map<String,Object> audit(String message) {
        if(!hybridAuditSchemaReady()) throw new AssistantFailure("MIGRATION_REQUIRED","查询审计扩展尚未完成数据库迁移");
        String plan=queryPlan(message);if(!visible(plan)) throw new AssistantFailure("FORBIDDEN","该查询记录已不可访问");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT route_type,tool_name,sql_fingerprint,query_trace,row_count,status,elapsed_ms,tokens,create_time FROM erp_assistant_audit WHERE message_id=? AND tenant_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0 ORDER BY create_time DESC LIMIT 1",message,tenant(),ADMIN_OWNER);
        if(rows.isEmpty()) throw new AssistantFailure("NOT_FOUND","查询审计不存在");
        Map<String,Object> result=rows.get(0);
        AssistantExecutionEnvelope envelope=codec.read(plan,null);
        if(envelope!=null) {
            result.put("engine",envelope.getEngine());
            if(envelope.getSemanticRequest()!=null) {
                Map<String,Object> request=new LinkedHashMap<>();
                request.put("source",envelope.getSemanticRequest().getSource());
                request.put("currentPage",envelope.getSemanticRequest().getCurrentPage());
                request.put("departmentId",envelope.getSemanticRequest().getDepartmentId());
                request.put("queryMode",envelope.getSemanticRequest().getQueryMode());
                request.put("capabilityId",envelope.getSemanticRequest().getCapabilityId());
                request.put("intentId",envelope.getSemanticRequest().getIntentId());
                request.put("businessObjects",envelope.getSemanticRequest().getBusinessObjects());
                request.put("metric",envelope.getSemanticRequest().getMetric());
                request.put("dimensions",envelope.getSemanticRequest().getDimensions());
                request.put("documentStatuses",envelope.getSemanticRequest().getDocumentStatuses());
                request.put("timeRange",envelope.getSemanticRequest().getTimeRange());
                request.put("sort",envelope.getSemanticRequest().getSort());
                request.put("context",envelope.getSemanticRequest().getContext());
                request.put("missingFields",envelope.getSemanticRequest().getMissingFields());
                request.put("inheritedContext",envelope.getSemanticRequest().getInheritedContext());
                result.put("semanticRequest",request);
            }
            if(envelope.getExecutionAudit()!=null) {
                result.put("engineAudit",json.convertValue(envelope.getExecutionAudit(),Map.class));
            }
        }
        return result;
    }
    public Map<String,Object> rolloutSummary(int hours) {
        enabled();
        if(hours<1 || hours>24*30) throw new IllegalArgumentException("观察窗口必须在 1 到 720 小时之间");
        List<Map<String,Object>> rows=jdbc.queryForList(
                "SELECT m.plan_json,a.status,a.elapsed_ms FROM erp_assistant_audit a " +
                        "JOIN erp_assistant_message m ON m.id=a.message_id AND m.tenant_id=a.tenant_id AND m.deleted=0 " +
                        "WHERE a.tenant_id=? AND a.owner_type='ADMIN' AND a.customer_id IS NULL AND a.deleted=0 AND a.create_time>=DATE_SUB(NOW(),INTERVAL ? HOUR) " +
                        "ORDER BY a.create_time DESC LIMIT 1000",tenant(),hours);
        Map<String,Object> result=summarizeRollout(rows);
        result.put("windowHours",hours);result.put("sampleLimit",1000);
        return result;
    }
    Map<String,Object> summarizeRollout(List<Map<String,Object>> rows) {
        Map<String,Integer> statusCounts=new TreeMap<>();Map<String,List<Long>> elapsedByEngine=new LinkedHashMap<>();
        elapsedByEngine.put("V1",new ArrayList<>());elapsedByEngine.put("V2",new ArrayList<>());
        int technicalFailures=0,automaticFallbacks=0,capabilityFallbacks=0,entryFallbacks=0;
        int shadowExecuted=0,shadowMatched=0,shadowReleaseEligible=0,permissionMismatches=0,answerMismatches=0;
        for(Map<String,Object> row:rows==null?Collections.<Map<String,Object>>emptyList():rows) {
            String status=String.valueOf(row.get("status"));statusCounts.put(status,statusCounts.getOrDefault(status,0)+1);
            if(technicalFailure(status)) technicalFailures++;
            AssistantExecutionEnvelope envelope;
            try {envelope=codec.read(row.get("plan_json")==null?null:String.valueOf(row.get("plan_json")),null);}
            catch(Exception ignored) {continue;}
            AssistantExecutionAudit audit=envelope==null?null:envelope.getExecutionAudit();
            String engine=audit!=null&&audit.getSelectedEngine()!=null?audit.getSelectedEngine():envelope.getEngine();
            if(!elapsedByEngine.containsKey(engine)) elapsedByEngine.put(engine,new ArrayList<>());
            if(row.get("elapsed_ms") instanceof Number) elapsedByEngine.get(engine).add(((Number)row.get("elapsed_ms")).longValue());
            if(audit==null) continue;
            if(audit.isAutomaticFallback()) automaticFallbacks++;
            if(audit.isCapabilityFallback()) capabilityFallbacks++;
            if(audit.isEntryFallback()) entryFallbacks++;
            Map<String,Object> shadow=audit.getShadowComparison();
            if(shadow==null || !Boolean.TRUE.equals(shadow.get("executed"))) continue;
            shadowExecuted++;
            if(Boolean.TRUE.equals(shadow.get("matched"))) shadowMatched++;
            if(Boolean.TRUE.equals(shadow.get("releaseEligible"))) shadowReleaseEligible++;
            if(Boolean.FALSE.equals(shadow.get("permissionMatched"))) permissionMismatches++;
            if(Boolean.FALSE.equals(shadow.get("answerMatched"))) answerMismatches++;
        }
        Map<String,Object> result=new LinkedHashMap<>();int samples=rows==null?0:rows.size();
        result.put("sampleCount",samples);result.put("statusCounts",statusCounts);
        result.put("technicalFailureCount",technicalFailures);result.put("technicalFailureRate",rate(technicalFailures,samples));
        result.put("automaticFallbackCount",automaticFallbacks);result.put("capabilityFallbackCount",capabilityFallbacks);
        result.put("entryFallbackCount",entryFallbacks);
        Map<String,Object> performance=new LinkedHashMap<>();
        elapsedByEngine.forEach((engine,values)->performance.put(engine,latency(values)));
        result.put("performance",performance);result.put("shadowExecutedCount",shadowExecuted);
        result.put("shadowMatchedCount",shadowMatched);result.put("shadowMatchRate",rate(shadowMatched,shadowExecuted));
        result.put("shadowReleaseEligibleCount",shadowReleaseEligible);
        result.put("permissionMismatchCount",permissionMismatches);result.put("answerMismatchCount",answerMismatches);
        return result;
    }
    private static Map<String,Object> latency(List<Long> source) {
        List<Long> values=new ArrayList<>(source);Collections.sort(values);Map<String,Object> result=new LinkedHashMap<>();
        result.put("count",values.size());
        if(values.isEmpty()) {result.put("averageMillis",null);result.put("p95Millis",null);result.put("maxMillis",null);return result;}
        long total=0L;for(Long value:values) total+=value;
        result.put("averageMillis",Math.round((double)total/values.size()));
        result.put("p95Millis",values.get(Math.max(0,(int)Math.ceil(values.size()*0.95D)-1)));
        result.put("maxMillis",values.get(values.size()-1));return result;
    }
    private static double rate(int count,int total) {return total==0?0D:Math.round(count*10000D/total)/100D;}
    private static boolean technicalFailure(String status) {
        return Arrays.asList("FAILED","QUERY_FAILED","SYSTEM_ERROR","MODEL_TIMEOUT","MIGRATION_REQUIRED",
                "CAPABILITY_UNAVAILABLE","CANCELLED","TIMEOUT").contains(status);
    }
    @Transactional
    public void delete(String id) {
        owner(id);
        jdbc.update("UPDATE erp_assistant_message SET deleted=1,question='',plan_json=NULL WHERE conversation_id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",id,tenant(),user(),ADMIN_OWNER);
        jdbc.update("UPDATE erp_assistant_conversation SET deleted=1,title='' WHERE id=? AND tenant_id=? AND user_id=? AND owner_type=? AND customer_id IS NULL AND deleted=0",id,tenant(),user(),ADMIN_OWNER);
    }
    @Scheduled(cron="0 20 3 * * ?")
    public void expire() {
        if(!properties.isEnabled()) return;
        // Bounded, retention-only cleanup of assistant tables. No business tables involved.
        int days=Math.max(1,properties.getConversationDays());
        jdbc.update("DELETE FROM erp_assistant_message WHERE create_time < DATE_SUB(NOW(),INTERVAL ? DAY) LIMIT 1000",days);
        jdbc.update("DELETE FROM erp_assistant_conversation WHERE update_time < DATE_SUB(NOW(),INTERVAL ? DAY) LIMIT 1000",days);
        jdbc.update("DELETE FROM erp_assistant_audit WHERE create_time < DATE_SUB(NOW(),INTERVAL ? DAY) LIMIT 1000",Math.max(1,properties.getAuditDays()));
    }
}
