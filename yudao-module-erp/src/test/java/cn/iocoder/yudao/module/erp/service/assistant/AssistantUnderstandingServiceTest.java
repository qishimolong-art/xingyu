package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantUnderstandingServiceTest {
    private AssistantUnderstandingService service;
    private AssistantModelClient model;
    private final List<Map<String,Object>> tools = new AssistantToolRegistry().definitions();
    @BeforeEach void setUp() {
        service = new AssistantUnderstandingService(); model = mock(AssistantModelClient.class);
        ReflectionTestUtils.setField(service,"model",model);
        ReflectionTestUtils.setField(service,"properties",new AssistantProperties());
    }
    private String response(String task) { return "{\"schemaVersion\":1,\"queryMode\":\"DATA\",\"tasks\":["+task+"]}"; }
    private String task(String entity, String conditions) {
        return "{\"id\":\"t1\",\"title\":\"查询\",\"capability\":\"query_product_stock\",\"question\":\"查询库存\",\"entities\":[{\"entityType\":\"PRODUCT\",\"keyword\":\""+entity+"\",\"role\":\"subject\"}],\"conditions\":"+conditions+"}";
    }
    @Test void naturalLanguageKeepsOnlyTheProductVerbatim() throws Exception {
        for (String question:Arrays.asList("路通源刹车片 有吗","请问路通源刹车片还有货吗？","路通源刹车片，有没有？")) {
            AssistantUnderstandingResult r=service.decode(response(task("路通源刹车片","{}")),question,Collections.emptyMap(),tools);
            assertEquals("路通源刹车片",r.getTasks().get(0).entity("PRODUCT"));
        }
    }
    @Test void rejectsWholeQuestionAndInventedOrReformattedNames() {
        for(String name:Arrays.asList("路通源刹车片 有吗","另一个产品","路通源 刹车片"))
            assertThrows(Exception.class,()->service.decode(response(task(name,"{}")),"路通源刹车片 有吗",Collections.emptyMap(),tools));
    }
    @Test void blankClarificationFromModelIsNotAMissingRequirement() throws Exception {
        for (String value : Arrays.asList("null", "\"\"", "\"  \\t\\n\"")) {
            String payload = task("lty999", "{}").replace("\"id\":\"t1\"", "\"id\":\"t1\",\"clarification\":" + value);
            AssistantUnderstandingResult.Subtask recognized = service.decode(response(payload), "lty999 有吗", Collections.emptyMap(), tools).getTasks().get(0);
            assertNull(recognized.getClarification());
            assertFalse(recognized.needsClarification());
            assertEquals("lty999", recognized.entity("PRODUCT"));
        }
    }
    @Test void rejectsUnknownFieldsIdsSqlAndInvalidConditions() {
        String good=response(task("刹车片","{}"));
        for(String bad:Arrays.asList(good.replace("\"schemaVersion\":1","\"schemaVersion\":1,\"userId\":99"),
                good.replace("\"conditions\":{}","\"conditions\":{\"productId\":1}"),
                good.replace("\"conditions\":{}","\"conditions\":{\"sql\":\"SELECT 1\"}"),
                good.replace("\"conditions\":{}","\"conditions\":{\"limit\":999}"),
                good.replace("\"conditions\":{}","\"conditions\":{\"stockMode\":\"OTHER\"}")))
            assertThrows(Exception.class,()->service.decode(bad,"刹车片有吗",Collections.emptyMap(),tools));
    }
    @Test void onlyAnExplicitFollowupCanUsePreviousEntity() throws Exception {
        Map<String,Object> previous=Collections.singletonMap("product","刹车片");
        String follow=response(task("刹车片","{}").replace("\"id\":\"t1\"","\"id\":\"t1\",\"contextMode\":\"INHERIT\""));
        assertEquals("刹车片",service.decode(follow,"它还有货吗",previous,tools).getTasks().get(0).entity("PRODUCT"));
        assertThrows(Exception.class,()->service.decode(response(task("刹车片","{}")),"本月销售额",previous,tools));
    }
    @Test void rejectsMissingDependenciesAndTaskBudgetOverflow() {
        String base=task("刹车片","{}");
        assertThrows(Exception.class,()->service.decode(response(base.replace("\"id\":\"t1\"","\"id\":\"t1\",\"dependsOn\":[\"t2\"]")),"刹车片有吗",Collections.emptyMap(),tools));
        StringJoiner tasks=new StringJoiner(",");for(int i=1;i<=6;i++)tasks.add(base.replace("\"t1\"","\"t"+i+"\""));
        assertThrows(Exception.class,()->service.decode(response(tasks.toString()),"刹车片有吗",Collections.emptyMap(),tools));
    }
    @Test void allRegisteredIntentCapabilitiesCanBeRepresented() throws Exception {
        for(String capability:new AssistantToolRegistry().names()) {
            if("execute_semantic_query".equals(capability))continue;
            String value=response(task("刹车片","{}").replace("query_product_stock",capability));
            assertEquals(capability,service.decode(value,"刹车片",Collections.emptyMap(),tools).getTasks().get(0).getCapability());
        }
    }
    @Test void modelFailureIsExplicitAndNeverUsesLegacyModelRoutes() throws Exception {
        when(model.understand(anyString(),anyMap())).thenThrow(new AssistantFailure("MODEL_UNAVAILABLE","offline"));
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.understand("刹车片有吗",Collections.emptyMap(),tools,false));
        assertEquals("SEMANTIC_UNAVAILABLE",failure.getCode());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
        verify(model,never()).parse(anyString(),any(),anyList());
    }
    @Test void malformedModelResponseStopsBeforeExecution() throws Exception {
        when(model.understand(anyString(),anyMap())).thenReturn("not json");
        assertEquals("SEMANTIC_INVALID",assertThrows(AssistantFailure.class,()->service.understand("刹车片有吗",Collections.emptyMap(),tools,false)).getCode());
    }
    @Test void registeredButUnopenedCapabilityBecomesAnExplicitRestrictedTask() throws Exception {
        List<Map<String,Object>> customerTools=new cn.iocoder.yudao.module.erp.service.assistant.customer.CustomerAssistantToolRegistry(new AssistantProperties()).definitions();
        String reply=response(task("刹车片","{}").replace("query_product_stock",AssistantCompanyKnowledge.TOOL_NAME));
        AssistantUnderstandingResult r=service.decode(reply,"刹车片",Collections.emptyMap(),customerTools);
        assertEquals("unsupported",r.getTasks().get(0).getCapability());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,r.getTasks().get(0).getRestrictedCapability());
        assertThrows(Exception.class,()->service.decode(reply.replace(AssistantCompanyKnowledge.TOOL_NAME,"run_unknown_sql"),"刹车片",Collections.emptyMap(),customerTools));
    }
}
