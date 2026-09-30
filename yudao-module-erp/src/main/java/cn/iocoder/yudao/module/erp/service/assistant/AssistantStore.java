package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import javax.annotation.Resource;
import java.util.*;

@Repository
public class AssistantStore {
    @Resource private JdbcTemplate jdbc;
    @Resource private AssistantProperties properties;
    @Resource private AssistantOrchestrator orchestrator;
    @Resource private AssistantExecutionCodec codec;
    private final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper();
    private volatile long auditSchemaCheckedAt;
    private volatile boolean auditSchemaReady;
    private Long tenant() { return TenantContextHolder.getRequiredTenantId(); }
    private Long user() { Long uid=SecurityFrameworkUtils.getLoginUserId(); if(uid==null) throw new AssistantFailure("FORBIDDEN","请先登录"); return uid; }
    public void enabled() { if(!properties.isEnabled()) throw new AssistantFailure("DISABLED","智能问数尚未开放"); }
    public void owner(String id) {
        enabled();
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM erp_assistant_conversation WHERE id=? AND tenant_id=? AND user_id=? AND deleted=0",Integer.class,id,tenant(),user());
        if(n==null || n!=1) throw new AssistantFailure("NOT_FOUND","会话不存在或不可访问");
    }
    public String create() {
        enabled(); String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_conversation(id,tenant_id,user_id,title) VALUES(?,?,?,?)",id,tenant(),user(),"新会话"); return id;
    }
    public List<Map<String,Object>> list(int page) {
        enabled(); if(page<1 || page>10000) throw new IllegalArgumentException("分页无效");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,title,update_time FROM erp_assistant_conversation WHERE tenant_id=? AND user_id=? AND deleted=0 ORDER BY update_time DESC,id LIMIT 20 OFFSET ?",tenant(),user(),(page-1)*20);
        // Titles can contain customer names; do not expose them after permission changes.
        for(Map<String,Object> row:rows) row.put("title","问数会话");
        return rows;
    }
    public List<Map<String,Object>> messages(String id,int page) {
        owner(id); if(page<1 || page>10000) throw new IllegalArgumentException("分页无效");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,question,status,create_time,plan_json FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND user_id=? AND deleted=0 ORDER BY create_time DESC,id DESC LIMIT 20 OFFSET ?",id,tenant(),user(),(page-1)*20);
        for(Map<String,Object> row:rows) {
            Object plan=row.remove("plan_json");
            boolean visible=visible(plan);
            row.put("canRerun",visible && Arrays.asList("SUCCESS","EMPTY").contains(row.get("status")));
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
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT status,plan_json FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND user_id=? AND deleted=0 ORDER BY create_time DESC,id DESC LIMIT 1",id,tenant(),user());
        return previousContext(rows);
    }

    String previousContext(List<Map<String,Object>> rows) {
        if(rows==null || rows.isEmpty()) return null;
        Map<String,Object> latest=rows.get(0);String status=String.valueOf(latest.get("status"));
        Object rawPlan=latest.get("plan_json");String plan=rawPlan==null?null:String.valueOf(rawPlan);
        if(plan==null || plan.trim().isEmpty()) return null;
        if("SUCCESS".equals(status) || "EMPTY".equals(status)) return plan;
        return "CLARIFY".equals(status) && usefulClarification(plan)?plan:null;
    }

    private boolean usefulClarification(String plan) {
        try {
            com.fasterxml.jackson.databind.JsonNode root=json.readTree(plan);
            com.fasterxml.jackson.databind.JsonNode arguments=root.path("arguments");
            if(arguments.path("resolvedEntity").isObject() || arguments.path("pendingEntities").size()>0) return true;
            com.fasterxml.jackson.databind.JsonNode legacy=root.has("legacyPlan")?root.path("legacyPlan"):root;
            return legacy.path("pendingField").isTextual() && legacy.path("pendingIds").size()>0;
        } catch(Exception ignored) {return false;}
    }
    public String begin(String id,String question) {
        owner(id); String message=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_message(id,conversation_id,tenant_id,user_id,question,status) VALUES(?,?,?,?,?,'PROCESSING')",message,id,tenant(),user(),question);
        jdbc.update("UPDATE erp_assistant_conversation SET title=?,update_time=NOW(6) WHERE id=? AND tenant_id=? AND user_id=? AND deleted=0",question.substring(0,Math.min(60,question.length())),id,tenant(),user()); return message;
    }
    @Transactional
    public void finish(String message,String plan,String metric,String version,String status,long elapsed,int tokens) {
        finish(message,plan,metric,version,status,elapsed,tokens,null);
    }
    @Transactional
    public void finish(String message,String plan,String metric,String version,String status,long elapsed,int tokens,AssistantExecutionEnvelope envelope) {
        jdbc.update("UPDATE erp_assistant_message SET plan_json=?,status=? WHERE id=? AND tenant_id=? AND user_id=? AND deleted=0",plan,status,message,tenant(),user());
        if(!hybridAuditSchemaReady()) {
            jdbc.update("INSERT INTO erp_assistant_audit(id,message_id,tenant_id,user_id,metric,knowledge_version,status,condition_summary,elapsed_ms,tokens) VALUES(?,?,?,?,?,?,?,?,?,?)",
                    UUID.randomUUID().toString(),message,tenant(),user(),metric,version,status,auditSummary(plan),elapsed,tokens);
            return;
        }
        Map<String,Object> trace=envelope==null?Collections.emptyMap():envelope.getResultMetadata();Object raw=trace.get("queryTrace");
        Map<?,?> queryTrace=raw instanceof Map?(Map<?,?>)raw:Collections.emptyMap();
        jdbc.update("INSERT INTO erp_assistant_audit(id,message_id,tenant_id,user_id,metric,knowledge_version,status,condition_summary,elapsed_ms,tokens,route_type,tool_name,sql_fingerprint,query_trace,row_count) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(),message,tenant(),user(),metric,version,status,auditSummary(plan),elapsed,tokens,
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
        enabled(); List<Map<String,Object>> rows=jdbc.queryForList("SELECT m.plan_json,m.conversation_id FROM erp_assistant_message m JOIN erp_assistant_conversation c ON c.id=m.conversation_id AND c.tenant_id=m.tenant_id AND c.user_id=m.user_id AND c.deleted=0 WHERE m.id=? AND m.tenant_id=? AND m.user_id=? AND m.deleted=0 AND m.status IN ('SUCCESS','EMPTY')",message,tenant(),user());
        if(rows.size()!=1) throw new AssistantFailure("NOT_FOUND","查询记录不存在或不可访问");
        return (String)rows.get(0).get("plan_json");
    }
    public Map<String,Object> rerun(String message) {
        String plan=queryPlan(message);
        if(!visible(plan)) throw new AssistantFailure("FORBIDDEN","原查询条件已不可访问，请重新提问");
        return jdbc.queryForMap("SELECT conversation_id,question,plan_json FROM erp_assistant_message WHERE id=? AND tenant_id=? AND user_id=? AND deleted=0",message,tenant(),user());
    }
    public Map<String,Object> audit(String message) {
        if(!hybridAuditSchemaReady()) throw new AssistantFailure("MIGRATION_REQUIRED","查询审计扩展尚未完成数据库迁移");
        String plan=queryPlan(message);if(!visible(plan)) throw new AssistantFailure("FORBIDDEN","该查询记录已不可访问");
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT route_type,tool_name,sql_fingerprint,query_trace,row_count,status,elapsed_ms,tokens,create_time FROM erp_assistant_audit WHERE message_id=? AND tenant_id=? AND deleted=0 ORDER BY create_time DESC LIMIT 1",message,tenant());
        if(rows.isEmpty()) throw new AssistantFailure("NOT_FOUND","查询审计不存在");return rows.get(0);
    }
    @Transactional
    public void delete(String id) {
        owner(id);
        jdbc.update("UPDATE erp_assistant_message SET deleted=1,question='',plan_json=NULL WHERE conversation_id=? AND tenant_id=? AND user_id=? AND deleted=0",id,tenant(),user());
        jdbc.update("UPDATE erp_assistant_conversation SET deleted=1,title='' WHERE id=? AND tenant_id=? AND user_id=? AND deleted=0",id,tenant(),user());
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
