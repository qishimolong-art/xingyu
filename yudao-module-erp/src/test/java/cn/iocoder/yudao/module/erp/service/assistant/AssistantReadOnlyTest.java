package cn.iocoder.yudao.module.erp.service.assistant;

import com.alibaba.druid.pool.DruidDataSource;
import org.apache.ibatis.session.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AssistantReadOnlyTest {
    @AfterEach void clear() {AssistantTask.clear();}
    @Test void absentReadAccountNeverOpensPrimaryConnection() {
        AssistantReadOnly reader=new AssistantReadOnly();SqlSessionFactory factory=mock(SqlSessionFactory.class);
        ReflectionTestUtils.setField(reader,"properties",new AssistantProperties());ReflectionTestUtils.setField(reader,"sqlSessionFactory",factory);
        assertFalse(reader.configured());
        assertEquals("READ_ONLY_NOT_CONFIGURED",assertThrows(AssistantFailure.class,()->reader.snapshot(()->true)).getCode());
        verifyNoInteractions(factory);
    }
    @Test void explicitConnectionRetainsTenantMapperAndSnapshot() throws Exception {
        AssistantReadOnly reader=new AssistantReadOnly();AssistantProperties p=new AssistantProperties();
        p.getReadOnly().setUrl("jdbc:mysql://127.0.0.1/test");p.getReadOnly().setUsername("reader");p.getReadOnly().setPassword("test-only");
        DruidDataSource ds=mock(DruidDataSource.class);com.alibaba.druid.pool.DruidPooledConnection conn=mock(com.alibaba.druid.pool.DruidPooledConnection.class);
        when(ds.getConnection()).thenReturn(conn);SqlSessionFactory factory=mock(SqlSessionFactory.class);SqlSession session=mock(SqlSession.class);
        when(factory.openSession(any(Connection.class))).thenReturn(session);
        ReflectionTestUtils.setField(reader,"properties",p);ReflectionTestUtils.setField(reader,"datasource",ds);ReflectionTestUtils.setField(reader,"sqlSessionFactory",factory);
        ReflectionTestUtils.setField(reader,"readFactory",factory);
        Statement statement=mock(Statement.class);ResultSet grants=mock(ResultSet.class);
        when(conn.createStatement()).thenReturn(statement);when(statement.executeQuery("SHOW GRANTS")).thenReturn(grants);
        when(grants.next()).thenReturn(true,false);when(grants.getString(1)).thenReturn("GRANT SELECT ON `test`.* TO 'reader'@'127.0.0.1'");when(conn.getCatalog()).thenReturn("test");
        assertEquals("ok",reader.snapshot(()->reader.snapshot(()->"ok")));
        verify(conn).setReadOnly(true);verify(conn).setAutoCommit(false);verify(conn).setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        verify(factory,times(1)).openSession(any(Connection.class));verify(factory,never()).openSession();verify(conn,never()).commit();
    }
    @Test void cancellationReachesModelAndDatabaseAndStopsFurtherWork() throws Exception {
        AssistantTask task=new AssistantTask();okhttp3.Call call=mock(okhttp3.Call.class);Statement statement=mock(Statement.class);
        task.call(call);task.statement(statement);task.cancel();
        verify(call).cancel();verify(statement).cancel();assertEquals("CANCELLED",assertThrows(AssistantFailure.class,task::check).getCode());
    }
    @Test void deadlineIsDifferentFromUserCancellation() {
        AssistantTask task=new AssistantTask();task.timeout();
        assertEquals("TIMEOUT",assertThrows(AssistantFailure.class,task::check).getCode());
    }
    @Test void revokedHistoryDoesNotReturnQuestionTextOrReplayPermission() {
        AssistantStore store=new AssistantStore();org.springframework.jdbc.core.JdbcTemplate jdbc=mock(org.springframework.jdbc.core.JdbcTemplate.class);
        AssistantOrchestrator orchestrator=mock(AssistantOrchestrator.class);AssistantProperties p=new AssistantProperties();p.setEnabled(true);
        ReflectionTestUtils.setField(store,"jdbc",jdbc);ReflectionTestUtils.setField(store,"orchestrator",orchestrator);
        ReflectionTestUtils.setField(store,"codec",new AssistantExecutionCodec());ReflectionTestUtils.setField(store,"properties",p);
        cn.iocoder.yudao.framework.security.core.LoginUser user=new cn.iocoder.yudao.framework.security.core.LoginUser();user.setId(101L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        when(jdbc.queryForObject(anyString(),eq(Integer.class),any(),any(),any())).thenReturn(1);
        Map<String,Object> row=new HashMap<>();row.put("question","秘密客户余额");row.put("status","SUCCESS");row.put("plan_json","{\"metric\":\"RECEIVABLE\"}");
        when(jdbc.queryForList(anyString(),eq("c1"),eq(1L),eq(101L),eq(0))).thenReturn(Collections.singletonList(row));
        doThrow(new AssistantFailure("FORBIDDEN","撤权")).when(orchestrator).validateAccess(any());
        try {Map<String,Object> result=store.messages("c1",1).get(0);assertFalse(result.get("question").toString().contains("秘密客户"));assertEquals(false,result.get("canRerun"));assertFalse(result.containsKey("plan_json"));}
        finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    }
    @Test void conversationListUsesTheFirstCurrentlyVisibleQuestionAsItsSafeTitle() {
        AssistantStore store=new AssistantStore();org.springframework.jdbc.core.JdbcTemplate jdbc=mock(org.springframework.jdbc.core.JdbcTemplate.class);
        AssistantOrchestrator orchestrator=mock(AssistantOrchestrator.class);AssistantExecutionCodec codec=mock(AssistantExecutionCodec.class);
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.system.api.permission.PermissionApi permissions=mock(cn.iocoder.yudao.module.system.api.permission.PermissionApi.class);
        AssistantExecutionEnvelope visibleEnvelope=mock(AssistantExecutionEnvelope.class),revokedEnvelope=mock(AssistantExecutionEnvelope.class),knowledgeEnvelope=mock(AssistantExecutionEnvelope.class);
        AssistantProperties p=new AssistantProperties();p.setEnabled(true);
        ReflectionTestUtils.setField(store,"jdbc",jdbc);ReflectionTestUtils.setField(store,"orchestrator",orchestrator);
        ReflectionTestUtils.setField(store,"codec",codec);ReflectionTestUtils.setField(store,"properties",p);
        ReflectionTestUtils.setField(store,"reader",reader);ReflectionTestUtils.setField(store,"permissions",permissions);
        doAnswer(invocation->((java.util.function.Supplier<?>)invocation.getArgument(0)).get()).when(reader).snapshot(any());
        doAnswer(invocation->((java.util.function.Supplier<?>)invocation.getArgument(0)).get()).when(permissions).executeInReadSnapshot(any());
        cn.iocoder.yudao.framework.security.core.LoginUser user=new cn.iocoder.yudao.framework.security.core.LoginUser();user.setId(101L);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.setTenantId(1L);
        Map<String,Object> fresh=new LinkedHashMap<>();fresh.put("id","c-new");fresh.put("title","旧标题");
        Map<String,Object> visible=new LinkedHashMap<>();visible.put("id","c-visible");visible.put("title","后续问题");
        Map<String,Object> revoked=new LinkedHashMap<>();revoked.put("id","c-revoked");revoked.put("title","秘密客户余额");
        Map<String,Object> knowledge=new LinkedHashMap<>();knowledge.put("id","c-knowledge");knowledge.put("title","公司知识");
        Map<String,Object> invalid=new LinkedHashMap<>();invalid.put("id","c-invalid");invalid.put("title","无效计划");
        when(jdbc.queryForList(startsWith("SELECT id,title,update_time"),eq(1L),eq(101L),eq(10),eq(0))).thenReturn(Arrays.asList(fresh,visible,revoked,knowledge,invalid));
        Map<String,Object> visibleMessage=new LinkedHashMap<>();visibleMessage.put("conversation_id","c-visible");visibleMessage.put("question","本月销售情况");visibleMessage.put("status","SUCCESS");visibleMessage.put("plan_json","visible-plan");
        Map<String,Object> revokedMessage=new LinkedHashMap<>();revokedMessage.put("conversation_id","c-revoked");revokedMessage.put("question","秘密客户余额");revokedMessage.put("status","SUCCESS");revokedMessage.put("plan_json","revoked-plan");
        Map<String,Object> knowledgeMessage=new LinkedHashMap<>();knowledgeMessage.put("conversation_id","c-knowledge");knowledgeMessage.put("question","公司有哪些部门");knowledgeMessage.put("status","SUCCESS");knowledgeMessage.put("plan_json","knowledge-plan");
        Map<String,Object> invalidMessage=new LinkedHashMap<>();invalidMessage.put("conversation_id","c-invalid");invalidMessage.put("question","不应显示的问题");invalidMessage.put("status","SUCCESS");invalidMessage.put("plan_json","invalid-plan");
        when(jdbc.queryForList(startsWith("SELECT m.conversation_id,m.question,m.status,m.plan_json"),eq("c-new"),eq("c-visible"),eq("c-revoked"),eq("c-knowledge"),eq("c-invalid"),eq(1L),eq(101L))).thenReturn(Arrays.asList(visibleMessage,revokedMessage,knowledgeMessage,invalidMessage));
        when(codec.read("visible-plan",null)).thenReturn(visibleEnvelope);when(codec.read("revoked-plan",null)).thenReturn(revokedEnvelope);
        when(codec.read("knowledge-plan",null)).thenReturn(knowledgeEnvelope);when(codec.read("invalid-plan",null)).thenThrow(new IllegalArgumentException("无效计划"));
        when(visibleEnvelope.getLegacyPlan()).thenReturn(new AssistantPlan());when(revokedEnvelope.getLegacyPlan()).thenReturn(new AssistantPlan());
        doThrow(new AssistantFailure("FORBIDDEN","撤权")).when(orchestrator).validateAccess(revokedEnvelope);
        try {
            List<Map<String,Object>> rows=store.list(1,10);
            assertEquals("新会话",rows.get(0).get("title"));assertEquals("本月销售情况",rows.get(1).get("title"));assertEquals("历史会话",rows.get(2).get("title"));
            assertEquals("公司有哪些部门",rows.get(3).get("title"));assertEquals("历史会话",rows.get(4).get("title"));
            Map<String,Object> failedMessage=new LinkedHashMap<>();failedMessage.put("question","失败问题原文");failedMessage.put("status","FAILED");failedMessage.put("plan_json","visible-plan");
            assertEquals("历史会话",store.conversationTitle(failedMessage));
            assertThrows(IllegalArgumentException.class,()->store.list(1,0));assertThrows(IllegalArgumentException.class,()->store.list(1,51));
            verify(jdbc).queryForList(contains("earlier.create_time=m.create_time AND earlier.id<m.id"),eq("c-new"),eq("c-visible"),eq("c-revoked"),eq("c-knowledge"),eq("c-invalid"),eq(1L),eq(101L));
            verify(permissions).executeInReadSnapshot(any());verify(reader).snapshot(any());
        } finally {org.springframework.security.core.context.SecurityContextHolder.clearContext();cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder.clear();}
    }
}
