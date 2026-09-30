package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantSemanticQueryServiceTest {
    private AssistantSemanticQueryService service;
    private AssistantToolPermissionService permissions;
    private AssistantSemanticReadOnly reader;
    private AssistantProperties properties;

    @BeforeEach
    void setUp() {
        properties=new AssistantProperties();
        properties.getOrchestration().setPublishedDatasets(Arrays.asList("purchase_orders","stock_movements"));
        AssistantSemanticCatalog catalog=new AssistantSemanticCatalog();
        ReflectionTestUtils.setField(catalog,"properties",properties);
        AssistantSemanticSqlGuard guard=new AssistantSemanticSqlGuard();
        ReflectionTestUtils.setField(guard,"catalog",catalog);
        permissions=mock(AssistantToolPermissionService.class);
        reader=mock(AssistantSemanticReadOnly.class);
        service=new AssistantSemanticQueryService();
        ReflectionTestUtils.setField(service,"catalog",catalog);
        ReflectionTestUtils.setField(service,"guard",guard);
        ReflectionTestUtils.setField(service,"permissions",permissions);
        ReflectionTestUtils.setField(service,"reader",reader);
        LoginUser user=new LoginUser();user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clear() {
        TenantContextHolder.clear();SecurityContextHolder.clearContext();
    }

    @Test
    void modelSchemaContainsOnlyPermissionVisibleFields() {
        when(permissions.visibleFields(eq("erp:purchase-order:query"),eq("erp_purchase_order"),anyMap()))
                .thenReturn(Collections.singletonMap("id","订单标识"));
        List<Map<String,Object>> schema=service.schema(Collections.singletonList("purchase_orders"));
        assertEquals(Collections.singletonMap("id","订单标识"),schema.get(0).get("columns"));
    }

    @Test
    void accessCheckUsesOnlyColumnsReferencedByValidatedSql() {
        service.validateAccess("SELECT p.supplier_name,COUNT(*) document_count FROM purchase_orders p GROUP BY p.supplier_name");
        verify(permissions).authorize("erp:purchase-order:query","erp_purchase_order",Collections.singleton("supplier_name"));
        verify(permissions).authorize("erp:purchase-order:query","erp_purchase_order",Collections.emptySet());
    }

    @Test
    void semanticDetailsUseDatabaseCountAndPageQueries() {
        when(reader.query(startsWith("SELECT COUNT(*) total FROM"),anyList(),eq(1)))
                .thenReturn(Collections.singletonList(Collections.singletonMap("total",31)));
        when(reader.query(startsWith("SELECT * FROM"),anyList(),eq(20)))
                .thenReturn(Collections.singletonList(new LinkedHashMap<String,Object>(){{put("no","CGDD001");put("order_time","2026-09-28 10:00:00");}}));

        Map<String,Object> result=service.details("SELECT p.no,p.order_time FROM purchase_orders p ORDER BY p.order_time DESC",Collections.emptyMap(),null,2,20);

        assertEquals(31,result.get("total"));
        assertEquals("CGDD001",((List<Map<String,Object>>)result.get("list")).get(0).get("no"));
        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<List<Object>> parameters=org.mockito.ArgumentCaptor.forClass(List.class);
        verify(reader).query(sql.capture(),parameters.capture(),eq(20));
        assertTrue(sql.getValue().startsWith("SELECT * FROM ("));
        assertTrue(sql.getValue().endsWith(" LIMIT ? OFFSET ?"));
        assertEquals(Arrays.asList(20,20),parameters.getValue().subList(parameters.getValue().size()-2,parameters.getValue().size()));
    }

    @Test
    void serverOwnedStockMovementDetailsAreNotCappedByDefaultSemanticLimit() {
        doAnswer(invocation -> {((Map<String,Object>)invocation.getArgument(0)).put("stockAll",Boolean.TRUE);return null;})
                .when(permissions).stockScope(anyMap());
        when(reader.query(startsWith("SELECT COUNT(*) total FROM"),anyList(),eq(1)))
                .thenReturn(Collections.singletonList(Collections.singletonMap("total",250)));
        when(reader.query(startsWith("SELECT * FROM"),anyList(),eq(20)))
                .thenReturn(Collections.singletonList(new LinkedHashMap<String,Object>(){{put("product_name","路通源化油器清洗剂");put("biz_type",50);put("biz_date","2026-09-21 16:56:10");}}));

        Map<String,Object> result=service.serverOwnedDetails(
                "SELECT q.product_name,q.biz_type,q.biz_date FROM stock_movements q ORDER BY q.biz_date DESC",
                Collections.emptyMap(),null,1,20);

        assertEquals(250,result.get("total"));
        List<Map<String,Object>> columns=(List<Map<String,Object>>)result.get("columns");
        assertEquals("产品名称",columns.get(0).get("title"));
        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(reader).query(sql.capture(),anyList(),eq(1));
        assertFalse(sql.getValue().contains("LIMIT 200"));
    }

    @Test
    void textToSqlInjectsEveryResolvedProductAndWarehouseId() {
        doAnswer(invocation -> {((Map<String,Object>)invocation.getArgument(0)).put("stockAll",Boolean.TRUE);return null;})
                .when(permissions).stockScope(anyMap());
        when(reader.query(anyString(),anyList(),eq(200))).thenReturn(Collections.emptyList());
        Map<String,Object> product=new LinkedHashMap<>();product.put("entityType","PRODUCT");product.put("id",701L);
        Map<String,Object> warehouse=new LinkedHashMap<>();warehouse.put("entityType","WAREHOUSE");warehouse.put("id",66L);
        Map<String,Object> constraint=new LinkedHashMap<>();constraint.put("entities",Arrays.asList(product,warehouse));

        service.execute("SELECT q.product_name,q.warehouse_name FROM stock_movements q",Collections.emptyMap(),constraint);

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<List<Object>> parameters=org.mockito.ArgumentCaptor.forClass(List.class);
        verify(reader).query(sql.capture(),parameters.capture(),eq(200));
        assertTrue(sql.getValue().contains("r.product_id=?"));
        assertTrue(sql.getValue().contains("r.warehouse_id=?"));
        assertTrue(parameters.getValue().contains(701L));assertTrue(parameters.getValue().contains(66L));
    }

    @Test
    void saleOrderItemsSourceKeepsCustomerProductAndPermissionScopes() {
        properties.getOrchestration().setPublishedDatasets(Collections.singletonList("sale_order_items"));
        doAnswer(invocation -> {
            Map<String,Object> values=invocation.getArgument(0);
            String prefix=invocation.getArgument(1);
            values.put(prefix+"All",Boolean.TRUE);
            return null;
        }).when(permissions).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        when(reader.query(anyString(),anyList(),eq(200))).thenReturn(Collections.emptyList());

        service.execute("SELECT q.customer_name,q.product_name,SUM(q.line_amount) total_amount FROM sale_order_items q GROUP BY q.customer_name,q.product_name",
                Collections.emptyMap());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(reader).query(sql.capture(),anyList(),eq(200));
        assertTrue(sql.getValue().contains("FROM erp_sale_order_items i JOIN erp_sale_order o"));
        assertTrue(sql.getValue().contains("JOIN erp_customer c"));
        assertTrue(sql.getValue().contains("JOIN erp_product p"));
        assertTrue(sql.getValue().contains("o.status=20"));
        verify(permissions).authorize(eq("erp:sale-order:query"),eq("erp_sale_order"),
                argThat(fields->fields.containsAll(Arrays.asList("customer_name","product_name","line_amount"))));
    }
}
