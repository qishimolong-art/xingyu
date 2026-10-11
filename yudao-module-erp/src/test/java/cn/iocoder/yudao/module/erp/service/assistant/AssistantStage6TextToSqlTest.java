package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Stage 6 acceptance checks for the optional controlled Text-to-SQL sandbox. */
class AssistantStage6TextToSqlTest {
    private AssistantProperties properties;
    private AssistantSemanticCatalog catalog;
    private AssistantSemanticSqlGuard guard;

    @BeforeEach
    void setUp() {
        properties=new AssistantProperties();
        properties.getOrchestration().setPublishedDatasets(Arrays.asList(
                "purchase_orders","sale_orders","sale_order_items","inventory_current","stock_movements"));
        catalog=new AssistantSemanticCatalog();ReflectionTestUtils.setField(catalog,"properties",properties);
        guard=new AssistantSemanticSqlGuard();ReflectionTestUtils.setField(guard,"catalog",catalog);
        LoginUser user=new LoginUser();user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();SecurityContextHolder.clearContext();AssistantTask.clear();
    }

    @Test
    void catalogRegistersOnlyLogicalFieldsRelationsOperationsAndCostLimits() {
        AssistantSemanticCatalog.Dataset purchase=catalog.requirePublished("purchase_orders");
        assertEquals(new LinkedHashSet<>(Arrays.asList("PROJECT","FILTER","AGGREGATE","GROUP","SORT")),
                purchase.getAllowedOperations());
        assertTrue(purchase.getRelationships().contains("采购订单—供应商"));
        assertTrue(purchase.isRequiresSelectiveFilter());
        assertEquals(Collections.singleton("order_time"),purchase.getTimeColumns());
        assertEquals(731,purchase.getMaxTimeRangeDays());assertEquals(200,purchase.getMaxRows());
        assertFalse(purchase.getColumns().keySet().stream().anyMatch(name->name.startsWith("erp_")));
        assertEquals(64,catalog.packageChecksum().length());assertEquals(16,catalog.version().length());
    }

    @Test
    void highVolumeDatasetsRequireBoundedSelectiveFilters() {
        AssistantSemanticCatalog.Dataset purchase=catalog.requirePublished("purchase_orders");
        assertCostRejected("SELECT p.id FROM purchase_orders p",Collections.emptyMap(),purchase);
        assertCostRejected("SELECT p.id FROM purchase_orders p WHERE p.order_time>=:p1",
                Collections.singletonMap("p1","2026-01-01"),purchase);
        Map<String,Object> tooLong=new LinkedHashMap<>();tooLong.put("p1","2024-01-01");tooLong.put("p2","2026-10-03");
        assertCostRejected("SELECT p.id FROM purchase_orders p WHERE p.order_time>=:p1 AND p.order_time<:p2",tooLong,purchase);
        Map<String,Object> bypass=new LinkedHashMap<>();bypass.put("p1","2026-10-01");bypass.put("p2","2026-10-03");
        assertCostRejected("SELECT p.id FROM purchase_orders p WHERE p.order_time>=:p1 OR p.order_time<:p2",bypass,purchase);

        Map<String,Object> bounded=new LinkedHashMap<>();bounded.put("p1","2026-09-01");bounded.put("p2","2026-10-01");
        AssistantSemanticSqlGuard.Validated checked=guard.validate(
                "SELECT p.id FROM purchase_orders p WHERE p.order_time>=:p1 AND p.order_time<:p2");
        assertDoesNotThrow(()->guard.enforceCost(checked,purchase,bounded,false));
        AssistantSemanticSqlGuard.Validated exact=guard.validate("SELECT p.id FROM purchase_orders p WHERE p.no=:p1");
        assertDoesNotThrow(()->guard.enforceCost(exact,purchase,Collections.singletonMap("p1","CGDD001"),false));
        AssistantSemanticSqlGuard.Validated current=guard.validate("SELECT q.product_id,q.quantity FROM inventory_current q");
        assertDoesNotThrow(()->guard.enforceCost(current,catalog.requirePublished("inventory_current"),Collections.emptyMap(),false));
    }

    @Test
    void unsafeVariantsAndQueriesBeyondDatasetLimitsAreRejected() {
        assertRejected("SELECT p.id FROM purchase_orders p FOR UPDATE");
        assertRejected("SELECT SLEEP(1) wait_value FROM purchase_orders p");
        assertRejected("SELECT p.id FROM purchase_orders p LIMIT 10 OFFSET 1001");
        assertRejected("SELECT p.id,p.no,p.status,p.supplier_id,p.supplier_name,p.dept_id,p.dept_name,p.order_time,"
                +"p.total_count,p.total_price,p.in_count,p.return_count,p.remaining_count FROM purchase_orders p");
        assertRejected("SELECT p.status,p.supplier_id,p.dept_id,DATE(p.order_time),COUNT(*) document_count FROM purchase_orders p "
                +"GROUP BY p.status,p.supplier_id,p.dept_id,DATE(p.order_time)");
    }

    @Test
    void validationFailureNeverReachesReadOnlyExecutor() {
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantSemanticQueryService service=service(reader,mock(AssistantToolPermissionService.class));

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->service.execute("SELECT p.id FROM purchase_orders p",Collections.emptyMap()));

        assertEquals("SQL_REJECTED",failure.getCode());verifyNoInteractions(reader);
    }

    @Test
    void successfulExecutionUsesDatasetTimeoutAndProducesSafeCompleteTrace() {
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permission=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{Map<String,Object> values=invocation.getArgument(0);values.put(invocation.getArgument(1)+"All",true);return null;})
                .when(permission).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        when(reader.query(anyString(),anyList(),eq(200),eq(10)))
                .thenReturn(Collections.singletonList(Collections.singletonMap("document_count",5L)));
        AssistantSemanticQueryService service=service(reader,permission);
        Map<String,Object> parameters=new LinkedHashMap<>();parameters.put("p1","2026-09-01");parameters.put("p2","2026-10-01");

        Map<String,Object> result=service.execute("SELECT COUNT(p.id) document_count FROM purchase_orders p "
                +"WHERE p.order_time>=:p1 AND p.order_time<:p2",parameters);

        Map<?,?> trace=(Map<?,?>)result.get("queryTrace");
        assertEquals("PASSED",trace.get("validation"));assertEquals("COMPLETED",trace.get("execution"));
        assertEquals(5L,((Map<?,?>)((List<?>)result.get("rows")).get(0)).get("document_count"));
        assertEquals(10,trace.get("queryTimeoutSeconds"));assertEquals(200,trace.get("maximumRows"));
        assertEquals(64,String.valueOf(trace.get("fingerprint")).length());assertFalse(trace.containsKey("sql"));
        assertTrue(((Collection<?>)trace.get("operations")).contains("AGGREGATE"));
    }

    @Test
    void readOnlyExecutorAppliesTheDatasetTimeoutToJdbcStatement() throws Exception {
        AssistantProperties configured=new AssistantProperties();configured.getReadOnly().setUrl("jdbc:mysql://127.0.0.1/test");
        configured.getReadOnly().setUsername("reader");configured.getReadOnly().setPassword("test-only");
        DruidDataSource datasource=mock(DruidDataSource.class);DruidPooledConnection connection=mock(DruidPooledConnection.class);
        Statement grantStatement=mock(Statement.class);ResultSet grants=mock(ResultSet.class);
        PreparedStatement query=mock(PreparedStatement.class);ResultSet rows=mock(ResultSet.class);
        when(datasource.getConnection()).thenReturn(connection);when(connection.getCatalog()).thenReturn("test");
        when(connection.createStatement()).thenReturn(grantStatement);when(grantStatement.executeQuery("SHOW GRANTS")).thenReturn(grants);
        when(grants.next()).thenReturn(true,false);when(grants.getString(1)).thenReturn("GRANT SELECT ON `test`.* TO 'reader'@'127.0.0.1'");
        when(connection.prepareStatement(anyString())).thenReturn(query);when(query.executeQuery()).thenReturn(rows);when(rows.next()).thenReturn(false);
        AssistantSemanticReadOnly reader=new AssistantSemanticReadOnly();ReflectionTestUtils.setField(reader,"properties",configured);
        ReflectionTestUtils.setField(reader,"datasource",datasource);

        assertTrue(reader.query("SELECT 1",Collections.emptyList(),20,4).isEmpty());

        verify(query).setQueryTimeout(4);verify(query).setMaxRows(21);verify(connection).setReadOnly(true);
    }

    private AssistantSemanticQueryService service(AssistantSemanticReadOnly reader,AssistantToolPermissionService permission) {
        AssistantSemanticQueryService service=new AssistantSemanticQueryService();
        ReflectionTestUtils.setField(service,"catalog",catalog);ReflectionTestUtils.setField(service,"guard",guard);
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"permissions",permission);return service;
    }

    private void assertCostRejected(String sql,Map<String,Object> parameters,AssistantSemanticCatalog.Dataset dataset) {
        AssistantSemanticSqlGuard.Validated checked=guard.validate(sql);
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->guard.enforceCost(checked,dataset,parameters,false));
        assertEquals("SQL_REJECTED",failure.getCode());
    }

    private void assertRejected(String sql) {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->guard.validate(sql));
        assertEquals("SQL_REJECTED",failure.getCode());
    }
}
