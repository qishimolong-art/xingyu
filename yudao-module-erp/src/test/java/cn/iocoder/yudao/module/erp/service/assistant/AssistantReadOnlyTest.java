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
}
