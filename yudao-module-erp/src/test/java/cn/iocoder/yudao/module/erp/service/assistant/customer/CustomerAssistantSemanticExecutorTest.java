package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.module.erp.service.assistant.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerAssistantSemanticExecutorTest {
    private CustomerAssistantSemanticExecutor executor;
    private CustomerAssistantQueryService queries;
    private final CustomerAssistantPrincipal principal=new CustomerAssistantPrincipal(1L,2L,3L,"客户",1,true,true,Collections.singleton(4L));
    @BeforeEach void setUp(){executor=new CustomerAssistantSemanticExecutor();queries=mock(CustomerAssistantQueryService.class);
        ReflectionTestUtils.setField(executor,"queries",queries);ReflectionTestUtils.setField(executor,"tools",new CustomerAssistantToolRegistry(new AssistantProperties()));}
    private AssistantUnderstandingResult.Subtask task(String id,String tool){AssistantUnderstandingResult.Subtask task=new AssistantUnderstandingResult.Subtask();task.setId(id);task.setTitle("查询"+id);task.setQuestion("查询库存");task.setCapability(tool);
        AssistantSemanticRequest.EntityMention e=new AssistantSemanticRequest.EntityMention();e.setEntityType("PRODUCT");e.setKeyword("路通源刹车片");e.setRole("subject");task.getEntities().add(e);return task;}
    private AssistantUnderstandingResult result(AssistantUnderstandingResult.Subtask... tasks){AssistantUnderstandingResult r=new AssistantUnderstandingResult();r.setQueryMode(AssistantSemanticRequest.QueryMode.DATA);r.setTasks(Arrays.asList(tasks));return r;}
    private Map<String,Object> queryResult(){Map<String,Object> r=new LinkedHashMap<>();r.put("status","SUCCESS");r.put("_productId",7L);r.put("rows",Collections.emptyList());return r;}
    @Test void onlyCustomerExecutorReceivesExtractedProduct(){when(queries.stock(eq(principal),anyMap())).thenReturn(queryResult());
        Map<String,Object> r=executor.execute(result(task("t1",CustomerAssistantToolRegistry.STOCK)),Collections.emptyMap(),principal,null,null);
        verify(queries).stock(eq(principal),argThat(args->"路通源刹车片".equals(args.get("keyword"))&&!args.containsKey("productId")));
        assertEquals("SUCCESS",r.get("status"));assertFalse(r.containsKey("_productId"));}
    @Test void employeeKnowledgeAndFinanceCannotRunThroughCustomerChannel(){
        Map<String,Object> r=executor.execute(result(task("t1",AssistantCompanyKnowledge.TOOL_NAME),task("t2","query_verified_metric")),Collections.emptyMap(),principal,null,null);
        assertEquals("TASKS_INCOMPLETE",r.get("status"));verifyNoInteractions(queries);}
    @Test void blankClarificationDoesNotBlockCustomerButRealMissingInfoDoes(){
        when(queries.stock(eq(principal),anyMap())).thenAnswer(i->queryResult());
        for(String blank:Arrays.asList(null,""," \t\n")) {
            AssistantUnderstandingResult.Subtask t=task("t1",CustomerAssistantToolRegistry.STOCK);t.setClarification(blank);
            assertEquals("SUCCESS",executor.execute(result(t),Collections.emptyMap(),principal,null,null).get("status"));
        }
        verify(queries,times(3)).stock(eq(principal),anyMap());clearInvocations(queries);
        AssistantUnderstandingResult.Subtask t=task("t1",CustomerAssistantToolRegistry.STOCK);t.setClarification("请选择具体商品");
        Map<String,Object> r=executor.execute(result(t),Collections.emptyMap(),principal,null,null);
        assertEquals("CLARIFY",r.get("status"));assertEquals("请选择具体商品",r.get("clarification"));verifyNoInteractions(queries);
        t.setCapability("unsupported");
        assertEquals("CAPABILITY_FORBIDDEN",executor.execute(result(t),Collections.emptyMap(),principal,null,null).get("status"));
    }
    @Test void priceFailureDoesNotDiscardStock(){when(queries.price(eq(principal),anyMap())).thenThrow(new AssistantFailure("PRICE_HIDDEN","价格不可见"));when(queries.stock(eq(principal),anyMap())).thenReturn(queryResult());
        Map<String,Object> r=executor.execute(result(task("t1",CustomerAssistantToolRegistry.PRICE),task("t2",CustomerAssistantToolRegistry.STOCK)),Collections.emptyMap(),principal,null,null);
        assertEquals("PARTIAL_SUCCESS",r.get("status"));assertEquals(2,((List<?>)r.get("tasks")).size());}
    @Test void candidateSelectionMustComeFromOwnedSavedCandidates(){
        Map<String,Object> state=new LinkedHashMap<>();state.put("pendingCandidates",Collections.singletonList(Collections.singletonMap("id",7L)));
        Map<String,Object> previous=Collections.singletonMap("states",Collections.singletonMap("t1",state));
        Map<String,Object> invalid=executor.execute(result(task("t1",CustomerAssistantToolRegistry.STOCK)),previous,principal,"task:t1:productId",99L);
        assertEquals("INVALID_CHOICE",invalid.get("status"));verifyNoInteractions(queries);
        when(queries.stock(eq(principal),anyMap())).thenReturn(queryResult());
        executor.execute(result(task("t1",CustomerAssistantToolRegistry.STOCK)),previous,principal,"task:t1:productId",7L);
        verify(queries).stock(eq(principal),argThat(args->Long.valueOf(7L).equals(args.get("productId"))));}
    @Test void candidateRoundTripDoesNotRequireAnotherUnderstandingCall(){
        Map<String,Object> candidate=new LinkedHashMap<>();candidate.put("id",7L);candidate.put("label","路通源刹车片");
        when(queries.stock(eq(principal),anyMap())).thenThrow(new CustomerAssistantQueryService.ProductChoice(AssistantClarification.entityChoice("请选择",Collections.singletonList(candidate))));
        Map<String,Object> r=executor.execute(result(task("t1",CustomerAssistantToolRegistry.STOCK)),Collections.emptyMap(),principal,null,null);
        Map<String,Object> previous=(Map<String,Object>)r.get("_execution");assertNotNull(executor.savedUnderstanding(previous));assertEquals("CLARIFY",r.get("status"));
        reset(queries);when(queries.stock(eq(principal),anyMap())).thenReturn(queryResult());
        assertEquals("SUCCESS",executor.execute(executor.savedUnderstanding(previous),previous,principal,"task:t1:productId",7L).get("status"));}
}
