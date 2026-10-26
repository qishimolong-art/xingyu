package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantUnderstandingEntryTest {
    private AssistantExecutionService service;
    private AssistantUnderstandingService understanding;
    private AssistantV2Engine engine;
    private AssistantOrchestrator orchestrator;
    private AssistantModelClient model;
    private AssistantStore store;
    private AssistantProperties properties;
    @BeforeEach void setUp(){
        LoginUser user=new LoginUser();user.setId(1L);TenantContextHolder.setTenantId(1L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        service=new AssistantExecutionService();understanding=mock(AssistantUnderstandingService.class);engine=mock(AssistantV2Engine.class);
        orchestrator=mock(AssistantOrchestrator.class);model=mock(AssistantModelClient.class);store=mock(AssistantStore.class);
        properties=new AssistantProperties();properties.getV2().setMode("V2");
        AssistantToolRegistry tools=new AssistantToolRegistry();AssistantCapabilityRegistry registry=new AssistantCapabilityRegistry();ReflectionTestUtils.setField(registry,"tools",tools);
        AssistantEngineRouter router=new AssistantEngineRouter();ReflectionTestUtils.setField(router,"properties",properties);ReflectionTestUtils.setField(router,"registry",registry);
        ReflectionTestUtils.setField(router,"orchestrator",orchestrator);ReflectionTestUtils.setField(router,"semanticAnalyzer",new AssistantSemanticAnalyzer());
        Object[][] dependencies={{"understandingService",understanding},{"v2Engine",engine},{"orchestrator",orchestrator},{"model",model},{"store",store},{"properties",properties},
                {"toolRegistry",tools},{"engineRouter",router},{"codec",new AssistantExecutionCodec()},{"semanticAnalyzer",new AssistantSemanticAnalyzer()}};
        for(Object[] dependency:dependencies)ReflectionTestUtils.setField(service,(String)dependency[0],dependency[1]);
        AssistantKnowledge knowledge=mock(AssistantKnowledge.class);when(knowledge.catalog()).thenReturn(Collections.emptyList());when(knowledge.version()).thenReturn("v1");ReflectionTestUtils.setField(service,"knowledge",knowledge);
        ReflectionTestUtils.setField(service,"companyKnowledge",mock(AssistantCompanyKnowledge.class));ReflectionTestUtils.setField(service,"queries",mock(AssistantQueryService.class));
        when(store.begin(anyString(),anyString())).thenReturn("m1");when(store.hybridAuditSchemaReady()).thenReturn(true);
        when(orchestrator.availableTools()).thenReturn(Arrays.asList("query_product_stock","query_verified_metric"));
        when(understanding.context(any())).thenReturn(Collections.emptyMap());
    }
    @AfterEach void clear(){service.shutdown();TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    private AssistantUnderstandingResult understood(){AssistantUnderstandingResult r=new AssistantUnderstandingResult();r.setQueryMode(AssistantSemanticRequest.QueryMode.DATA);
        AssistantUnderstandingResult.Subtask t=new AssistantUnderstandingResult.Subtask();t.setId("t1");t.setCapability("query_product_stock");t.setQuestion("查询刹车片库存");t.setTitle("库存");r.setTasks(Collections.singletonList(t));return r;}
    private AssistantExecutionService.Request request(String entry){AssistantExecutionService.Request r=new AssistantExecutionService.Request();r.setConversationId("c-"+entry);r.setQuestion("刹车片有吗");r.setEntry(entry);return r;}
    @Test void allEmployeeEntrancesEmitProgressBeforeExactlyOneUnderstanding()throws Exception {
        for(String entry:Arrays.asList("WEB_PAGE","WEB_DRAWER","WECOM")){
            Sink sink=new Sink();AssistantUnderstandingResult recognized=understood();
            when(understanding.understand(anyString(),anyMap(),anyList(),eq(false))).thenAnswer(i->{assertTrue(sink.events.contains("progress"));return recognized;});
            doAnswer(i->{assertSame(recognized,((AssistantSemanticRequest)i.getArgument(0)).getUnderstanding());
                AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();envelope.setUnderstanding(recognized);envelope.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);envelope.setToolName("query_product_stock");
                Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("rows",Collections.emptyList());result.put("summary",Collections.emptyList());result.put("answerText","完成");return AssistantOrchestrator.Outcome.result(envelope,result);}).when(engine).execute(any(),anyString(),any(),anyList());
            service.execute(request(entry),sink);assertTrue(sink.done.await(5,TimeUnit.SECONDS));assertTrue(sink.events.contains("result"));
        }
        verify(understanding,times(3)).understand(anyString(),anyMap(),anyList(),eq(false));verify(model,never()).parse(anyString(),any(),anyList());
    }
    @Test void understandingFailureNeverExecutesOrFallsBack()throws Exception {
        when(understanding.understand(anyString(),anyMap(),anyList(),eq(false))).thenThrow(new AssistantFailure("SEMANTIC_INVALID","请重试"));
        Sink sink=new Sink();service.execute(request("WEB_PAGE"),sink);assertTrue(sink.done.await(5,TimeUnit.SECONDS));assertTrue(sink.events.contains("error"));
        verifyNoInteractions(engine);verify(orchestrator,never()).execute(anyString(),any(),anyList());verify(model,never()).parse(anyString(),any(),anyList());
    }
    @Test void structuredExecutionFailureNeverRetriesLegacyEngine()throws Exception {
        when(understanding.understand(anyString(),anyMap(),anyList(),eq(false))).thenReturn(understood());
        when(engine.execute(any(),anyString(),any(),anyList())).thenThrow(new AssistantFailure("QUERY_FAILED","查询失败"));
        Sink sink=new Sink();service.execute(request("WEB_PAGE"),sink);assertTrue(sink.done.await(5,TimeUnit.SECONDS));
        verify(orchestrator,never()).execute(anyString(),any(),anyList());verify(model,never()).parse(anyString(),any(),anyList());
    }
    @Test void savedQueryReusesStructuredConditionsWithoutCallingModel()throws Exception {
        AssistantExecutionEnvelope saved=new AssistantExecutionEnvelope();saved.setUnderstanding(understood());saved.setQuestion("刹车片有吗");saved.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        AssistantExecutionService.Request request=request("RERUN");request.setSavedEnvelope(saved);
        when(orchestrator.executeUnderstood(anyString(),any(),any(),anyList())).thenAnswer(i->{Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("answerText","完成");return AssistantOrchestrator.Outcome.result(saved,result);});
        Sink sink=new Sink();service.execute(request,sink);assertTrue(sink.done.await(5,TimeUnit.SECONDS));assertTrue(sink.events.contains("result"));
        verify(understanding,never()).understand(anyString(),anyMap(),anyList(),anyBoolean());verify(orchestrator).validateAccess(saved);
    }
    static class Sink implements AssistantExecutionService.EventSink {final List<String> events=new CopyOnWriteArrayList<>();final CountDownLatch done=new CountDownLatch(1);
        public void emit(String event,Object data){events.add(event);}public void complete(){done.countDown();}}
}
