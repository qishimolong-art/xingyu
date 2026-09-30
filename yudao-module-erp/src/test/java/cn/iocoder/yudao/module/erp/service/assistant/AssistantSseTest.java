package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.assistant.AssistantController;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** Exercises the real async HTTP controller; authentication filters are tested separately at deployment. */
class AssistantSseTest {
    AssistantController controller;
    AssistantQueryService query;
    AssistantModelClient model;
    AssistantStore store;
    AssistantOrchestrator orchestrator;
    AssistantExecutionService execution;
    MockMvc mvc;
    @BeforeEach void setup() {
        LoginUser user=new LoginUser();user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        controller=new AssistantController();query=mock(AssistantQueryService.class);model=mock(AssistantModelClient.class);store=mock(AssistantStore.class);
        orchestrator=mock(AssistantOrchestrator.class);
        AssistantKnowledge knowledge=mock(AssistantKnowledge.class);
        AssistantProperties properties=new AssistantProperties();properties.setEnabled(true);properties.setApiKey("test-only");
        AssistantReadOnly reader=mock(AssistantReadOnly.class);when(reader.configured()).thenReturn(true);
        ReflectionTestUtils.setField(controller,"store",store);ReflectionTestUtils.setField(controller,"queries",query);
        ReflectionTestUtils.setField(controller,"model",model);ReflectionTestUtils.setField(controller,"knowledge",knowledge);
        ReflectionTestUtils.setField(controller,"orchestrator",orchestrator);ReflectionTestUtils.setField(controller,"codec",new AssistantExecutionCodec());
        ReflectionTestUtils.setField(controller,"semanticCatalog",mock(AssistantSemanticCatalog.class));
        ReflectionTestUtils.setField(controller,"semanticKnowledge",new AssistantSemanticKnowledge());
        ReflectionTestUtils.setField(controller,"properties",properties);ReflectionTestUtils.setField(controller,"reader",reader);
        execution=new AssistantExecutionService();
        ReflectionTestUtils.setField(execution,"store",store);ReflectionTestUtils.setField(execution,"queries",query);
        ReflectionTestUtils.setField(execution,"model",model);ReflectionTestUtils.setField(execution,"knowledge",knowledge);
        ReflectionTestUtils.setField(execution,"orchestrator",orchestrator);ReflectionTestUtils.setField(execution,"codec",new AssistantExecutionCodec());
        ReflectionTestUtils.setField(controller,"executionService",execution);
        when(store.begin(anyString(),anyString())).thenReturn("m1");when(knowledge.version()).thenReturn("test-version");
        when(knowledge.catalog()).thenReturn(Collections.emptyList());when(knowledge.retrieve(anyString(),any(),anyList())).thenReturn(Collections.emptyList());
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.RECEIPT);plan.setPeriod("THIS_WEEK");
        when(model.parse(anyString(),any(),anyList())).thenReturn(plan);
        when(query.execute(any())).thenAnswer(call->{
            assertEquals(101L,SecurityFrameworkUtils.getLoginUserId());assertEquals(1L,TenantContextHolder.getTenantId());
            Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("summary",Collections.singletonList(Collections.singletonMap("amount",98)));return result;
        });
        when(model.explain(anyMap())).thenReturn("汇总已完成");
        mvc=MockMvcBuilders.standaloneSetup(controller).build();
    }
    @AfterEach void clear() {execution.shutdown();SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    String ask() throws Exception {
        MvcResult initial=mvc.perform(post("/erp/assistant/send").contentType("application/json").content("{\"conversationId\":\"c1\",\"question\":\"本周收款\"}")).andReturn();
        initial.getAsyncResult(5000);
        return mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test void streamsExactResultsAndPersistsAuditWithWorkerIdentity() throws Exception {
        String response=ask();assertTrue(response.contains("event:result"));assertTrue(response.contains("\"amount\":98"));assertTrue(response.contains("event:done"));
        verify(store).finish(eq("m1"),anyString(),eq("RECEIPT"),eq("test-version"),eq("SUCCESS"),anyLong(),anyInt(),any(AssistantExecutionEnvelope.class));
    }
    @Test void forbiddenNeverBecomesZeroOrSuccessfulResult() throws Exception {
        when(query.execute(any())).thenThrow(new AssistantFailure("FORBIDDEN","没有查询权限"));
        String response=ask();assertTrue(response.contains("FORBIDDEN"));assertFalse(response.contains("event:result"));verify(model,never()).explain(anyMap());
    }
    @Test void modelFailureDoesNotExecuteAnyBusinessQuery() throws Exception {
        when(model.parse(anyString(),any(),anyList())).thenThrow(new AssistantFailure("MODEL_INVALID","无效条件"));
        String response=ask();assertTrue(response.contains("MODEL_INVALID"));assertFalse(response.contains("event:result"));verify(query,never()).execute(any());
    }
    @Test void readinessExplainsMissingReadAccount() throws Exception {
        AssistantProperties properties=new AssistantProperties();properties.setEnabled(true);
        ReflectionTestUtils.setField(controller,"properties",properties);ReflectionTestUtils.setField(controller,"reader",mock(AssistantReadOnly.class));
        String response=mvc.perform(get("/erp/assistant/readiness")).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("READ_ONLY_NOT_CONFIGURED"));assertFalse(response.contains("password"));
    }
    @Test void readinessRetainsTheReasonForAnInaccessiblePublishedMetric() {
        AssistantProperties properties=new AssistantProperties();properties.setEnabled(true);properties.setApiKey("test-only");
        AssistantReadOnly reader=mock(AssistantReadOnly.class);when(reader.configured()).thenReturn(true);
        ReflectionTestUtils.setField(controller,"properties",properties);ReflectionTestUtils.setField(controller,"reader",reader);
        AssistantKnowledge knowledge=(AssistantKnowledge)ReflectionTestUtils.getField(controller,"knowledge");
        Map<String,Object> sale=new HashMap<>();sale.put("id","SALE");sale.put("title","销售");sale.put("published",true);
        when(knowledge.catalog()).thenReturn(Collections.singletonList(sale));
        doThrow(new AssistantFailure("FIELD_FORBIDDEN","该统计涉及当前不可见字段")).when(query).authorize(AssistantPlan.Metric.SALE);
        Map<String,Object> state=controller.readiness().getData();assertEquals("FORBIDDEN",state.get("status"));
        Map<?,?> metric=(Map<?,?>)((List<?>)state.get("metrics")).get(0);
        assertEquals(false,metric.get("available"));assertEquals("FIELD_FORBIDDEN",metric.get("reason"));
        assertEquals("该统计涉及当前不可见字段",metric.get("reasonText"));
        assertTrue(controller.catalog().getData().isEmpty(),"Unauthorized definitions must still be excluded from model input");
    }
    @Test void rerunUsesSelectedSavedPlanAndNeverCallsParser() throws Exception {
        Map<String,Object> saved=new HashMap<>();saved.put("conversation_id","c1");saved.put("question","换成上周");saved.put("plan_json","{\"metric\":\"RECEIPT\",\"period\":\"LAST_WEEK\"}");
        when(store.rerun("old")).thenReturn(saved);
        MvcResult initial=mvc.perform(post("/erp/assistant/messages/old/rerun")).andReturn();initial.getAsyncResult(5000);
        String response=mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("event:result"));verify(model,never()).parse(anyString(),any(),anyList());
        verify(query).execute(argThat(plan->"LAST_WEEK".equals(plan.getPeriod()) && plan.getMetric()==AssistantPlan.Metric.RECEIPT));
        verify(store,never()).previous(anyString());
    }
    @Test void forgedClarificationCannotRunQuery() throws Exception {
        when(store.previous("c1")).thenReturn("{\"metric\":\"STOCK\",\"period\":\"CURRENT\",\"pendingField\":\"warehouse\",\"pendingIds\":[1]}");
        MvcResult initial=mvc.perform(post("/erp/assistant/send").contentType("application/json").content("{\"conversationId\":\"c1\",\"question\":\"选择仓库\",\"choiceField\":\"warehouse\",\"choiceId\":999}")).andReturn();initial.getAsyncResult(5000);
        String response=mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("INVALID_CHOICE"));verify(query,never()).execute(any());
    }
    @Test void oldAmbiguousSkuHistoryCannotReplayAnUnconfirmedDefault() throws Exception {
        Map<String,Object> saved=new HashMap<>();saved.put("conversation_id","c1");saved.put("question","蛟龙港仓有多少个SKU");saved.put("plan_json","{\"metric\":\"STOCK_SKU\",\"period\":\"CURRENT\",\"stockMode\":\"ALL\"}");
        when(store.rerun("old")).thenReturn(saved);
        MvcResult initial=mvc.perform(post("/erp/assistant/messages/old/rerun")).andReturn();initial.getAsyncResult(5000);
        String response=mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("event:clarification"));assertFalse(response.contains("event:result"));
        verify(query,never()).execute(any());verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test void completeProductStockQuestionCannotInheritOrExecuteSkuCountPlan() throws Exception {
        String question="卫斯卡空调滤清器WSC56208/WSC20856在各个仓库中的库存情况";
        when(store.previous("c1")).thenReturn("{\"metric\":\"STOCK_SKU\",\"group\":\"WAREHOUSE\",\"period\":\"CURRENT\",\"stockMode\":\"POSITIVE\"}");
        AssistantPlan wrong=new AssistantPlan();wrong.setMetric(AssistantPlan.Metric.STOCK_SKU);
        wrong.setGroup(AssistantPlan.Group.WAREHOUSE);wrong.setPeriod("CURRENT");wrong.setStockMode("POSITIVE");
        wrong.setProduct("卫斯卡空调滤清器WSC56208/WSC20856");
        when(model.parse(eq(question),isNull(),anyList())).thenReturn(wrong);
        MvcResult initial=mvc.perform(post("/erp/assistant/send").contentType("application/json")
                .content("{\"conversationId\":\"c1\",\"question\":\""+question+"\"}" )).andReturn();
        initial.getAsyncResult(5000);mvc.perform(asyncDispatch(initial)).andReturn();
        verify(query).execute(argThat(plan->plan.getMetric()==AssistantPlan.Metric.STOCK
                && plan.getGroup()==AssistantPlan.Group.WAREHOUSE && "ALL".equals(plan.getStockMode())
                && "卫斯卡空调滤清器WSC56208/WSC20856".equals(plan.getProduct())));
    }

    @Test void rerunCorrectsLegacySkuPlanForAnExplicitStockQuantityQuestion() throws Exception {
        String question="卫斯卡空调滤清器WSC56208/WSC20856在各个仓库中的库存情况";
        Map<String,Object> saved=new HashMap<>();saved.put("conversation_id","c1");saved.put("question",question);
        saved.put("plan_json","{\"metric\":\"STOCK_SKU\",\"group\":\"WAREHOUSE\",\"period\":\"CURRENT\",\"product\":\"卫斯卡空调滤清器WSC56208/WSC20856\",\"stockMode\":\"POSITIVE\"}");
        when(store.rerun("old-stock")).thenReturn(saved);
        MvcResult initial=mvc.perform(post("/erp/assistant/messages/old-stock/rerun")).andReturn();initial.getAsyncResult(5000);
        mvc.perform(asyncDispatch(initial)).andReturn();
        verify(query).execute(argThat(plan->plan.getMetric()==AssistantPlan.Metric.STOCK
                && plan.getGroup()==AssistantPlan.Group.WAREHOUSE && "ALL".equals(plan.getStockMode())));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test void rerunCorrectsLegacyProductFilterForAStockRankingQuestion() throws Exception {
        String question="哪个产品的库存是最多的";
        Map<String,Object> saved=new HashMap<>();saved.put("conversation_id","c1");saved.put("question",question);
        saved.put("plan_json","{\"metric\":\"STOCK\",\"period\":\"CURRENT\",\"product\":\"哪个产品的库存是最多的\",\"limit\":10}");
        when(store.rerun("old-ranking")).thenReturn(saved);
        MvcResult initial=mvc.perform(post("/erp/assistant/messages/old-ranking/rerun")).andReturn();initial.getAsyncResult(5000);
        mvc.perform(asyncDispatch(initial)).andReturn();
        verify(query).execute(argThat(plan->plan.getMetric()==AssistantPlan.Metric.STOCK
                && plan.getGroup()==AssistantPlan.Group.PRODUCT && plan.getRankOrder()==AssistantPlan.RankOrder.DESC
                && plan.getLimit()==1 && plan.getProduct()==null));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test void orchestrationFailurePersistsItsActualEnvelopeForAudit() throws Exception {
        when(store.hybridAuditSchemaReady()).thenReturn(true);when(orchestrator.enabledForCurrentUser()).thenReturn(true);
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();envelope.setQuestion("哪个产品库存最多");
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC);envelope.setToolName("query_verified_metric");
        envelope.getResultMetadata().put("queryTrace",new LinkedHashMap<String,Object>(){{put("stage","QUERY");put("ranking",true);}});
        when(orchestrator.execute(anyString(),any(),anyList())).thenThrow(new AssistantFailure("QUERY_FAILED","查询失败").attachEnvelope(envelope));
        MvcResult initial=mvc.perform(post("/erp/assistant/send").contentType("application/json")
                .content("{\"conversationId\":\"c1\",\"question\":\"哪个产品库存最多\"}")).andReturn();
        initial.getAsyncResult(5000);String response=mvc.perform(asyncDispatch(initial)).andReturn().getResponse().getContentAsString();
        assertTrue(response.contains("QUERY_FAILED"));
        verify(store).finish(eq("m1"),eq(new AssistantExecutionCodec().write(envelope)),isNull(),eq("test-version"),eq("QUERY_FAILED"),anyLong(),anyInt(),same(envelope));
    }
}
