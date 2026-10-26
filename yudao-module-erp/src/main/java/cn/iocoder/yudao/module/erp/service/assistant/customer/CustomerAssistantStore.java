package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

@Repository
public class CustomerAssistantStore {
    private static final String OWNER="CUSTOMER";
    @Resource private JdbcTemplate jdbc;
    private final ObjectMapper json=new ObjectMapper();
    private volatile long checkedAt;
    private volatile boolean ready;

    public void requireSchema() {
        long now=System.currentTimeMillis();
        if(now-checkedAt>30_000) {
            Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name IN ('erp_assistant_conversation','erp_assistant_message','erp_assistant_audit') AND column_name IN ('owner_type','customer_id','entry')",Integer.class);
            ready=count!=null&&count==9;checkedAt=now;
        }
        if(!ready) throw new AssistantFailure("MIGRATION_REQUIRED","客户智能问数数据库迁移尚未完成");
    }
    public String create(CustomerAssistantPrincipal p) {
        requireSchema();String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_conversation(id,tenant_id,user_id,owner_type,customer_id,entry,title) VALUES(?,?,?,?,?,?,?)",
                id,p.getTenantId(),p.getMemberUserId(),OWNER,p.getCustomerId(),"CUSTOMER_APP","新会话");return id;
    }
    public void owner(String id,CustomerAssistantPrincipal p) {
        requireSchema();Integer count=jdbc.queryForObject("SELECT COUNT(*) FROM erp_assistant_conversation WHERE id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0",Integer.class,
                id,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());
        if(count==null||count!=1) throw new AssistantFailure("NOT_FOUND","会话不存在或不可访问");
    }
    public List<Map<String,Object>> list(CustomerAssistantPrincipal p,int page,int pageSize) {
        requireSchema();validatePage(page,pageSize);
        return jdbc.queryForList("SELECT id,title,update_time FROM erp_assistant_conversation WHERE tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0 ORDER BY update_time DESC,id LIMIT ? OFFSET ?",
                p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId(),pageSize,(page-1)*pageSize);
    }
    public List<Map<String,Object>> messages(String conversationId,CustomerAssistantPrincipal p,int page) {
        owner(conversationId,p);validatePage(page,20);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT id,question,status,plan_json AS response,create_time FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0 ORDER BY create_time DESC,id DESC LIMIT 20 OFFSET ?",
                conversationId,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId(),(page-1)*20);
        for(Map<String,Object> row:rows){Object raw=row.get("response");try{Map<String,Object> response=raw==null?null:json.readValue(String.valueOf(raw),Map.class);if(response!=null)response.remove("_execution");row.put("response",response);}catch(Exception e){row.put("response",null);}}
        return rows;
    }
    public String begin(String conversationId,String question,CustomerAssistantPrincipal p) {
        owner(conversationId,p);String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO erp_assistant_message(id,conversation_id,tenant_id,user_id,owner_type,customer_id,entry,question,status) VALUES(?,?,?,?,?,?,?,?,?)",
                id,conversationId,p.getTenantId(),p.getMemberUserId(),OWNER,p.getCustomerId(),"CUSTOMER_APP",question,"PROCESSING");
        String title=question.substring(0,question.offsetByCodePoints(0,Math.min(60,question.codePointCount(0,question.length()))));
        jdbc.update("UPDATE erp_assistant_conversation SET title=CASE WHEN title='新会话' THEN ? ELSE title END,update_time=NOW(6) WHERE id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0",
                title,conversationId,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());return id;
    }

    /** 仅读取同一所有者紧邻终态；失败终态构成上下文屏障，不越过它回找旧对象。 */
    public Map<String,Object> previousExecution(String conversationId, CustomerAssistantPrincipal p) {
        owner(conversationId, p);
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT plan_json,status FROM erp_assistant_message WHERE conversation_id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0 AND status<>'PROCESSING' ORDER BY create_time DESC,id DESC LIMIT 1",
                conversationId,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());
        if (rows.isEmpty() || !Arrays.asList("SUCCESS","EMPTY","PARTIAL_SUCCESS","CLARIFY","TASKS_INCOMPLETE").contains(String.valueOf(rows.get(0).get("status")))) return Collections.emptyMap();
        try {
            Map<?,?> response = json.readValue(String.valueOf(rows.get(0).get("plan_json")), Map.class);
            Object execution = response.get("_execution");
            return execution instanceof Map ? (Map<String,Object>)execution : Collections.emptyMap();
        } catch (Exception ignored) { return Collections.emptyMap(); }
    }
    @Transactional
    public void finish(String messageId,CustomerAssistantPrincipal p,String status,Object safeResponse,long elapsed,int tokens,String capability) {
        String body=null;try{body=safeResponse==null?null:json.writeValueAsString(safeResponse);}catch(Exception ignored){}
        jdbc.update("UPDATE erp_assistant_message SET plan_json=?,status=? WHERE id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0",
                body,status,messageId,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());
        jdbc.update("INSERT INTO erp_assistant_audit(id,message_id,tenant_id,user_id,owner_type,customer_id,entry,metric,knowledge_version,status,condition_summary,elapsed_ms,tokens) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(),messageId,p.getTenantId(),p.getMemberUserId(),OWNER,p.getCustomerId(),"CUSTOMER_APP",capability,"CUSTOMER_APP_V1",status,null,elapsed,tokens);
    }
    @Transactional
    public void delete(String id,CustomerAssistantPrincipal p) {
        owner(id,p);jdbc.update("UPDATE erp_assistant_message SET deleted=1,question='',plan_json=NULL WHERE conversation_id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0",id,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());
        jdbc.update("UPDATE erp_assistant_conversation SET deleted=1,title='' WHERE id=? AND tenant_id=? AND owner_type=? AND user_id=? AND customer_id=? AND deleted=0",id,p.getTenantId(),OWNER,p.getMemberUserId(),p.getCustomerId());
    }
    private void validatePage(int page,int size){if(page<1||page>10000||size<1||size>50)throw new IllegalArgumentException("分页无效");}
}
