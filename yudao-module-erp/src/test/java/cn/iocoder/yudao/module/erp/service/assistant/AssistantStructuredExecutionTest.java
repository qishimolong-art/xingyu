package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantStructuredExecutionTest {
    private AssistantOrchestrator executor;
    private AssistantQueryService queries;
    private AssistantModelClient model;
    private AssistantEntityResolver entities;
    private AssistantSemanticQueryService semantic;
    private AssistantCompanyKnowledge knowledge;
    @BeforeEach void setUp() {
        executor=new AssistantOrchestrator(); queries=mock(AssistantQueryService.class);model=mock(AssistantModelClient.class);
        entities=mock(AssistantEntityResolver.class);semantic=mock(AssistantSemanticQueryService.class);knowledge=mock(AssistantCompanyKnowledge.class);
        ReflectionTestUtils.setField(executor,"properties",new AssistantProperties());
        ReflectionTestUtils.setField(executor,"queries",queries);ReflectionTestUtils.setField(executor,"model",model);
        ReflectionTestUtils.setField(executor,"entities",entities);ReflectionTestUtils.setField(executor,"semantic",semantic);
        ReflectionTestUtils.setField(executor,"companyKnowledge",knowledge);ReflectionTestUtils.setField(executor,"registry",new AssistantToolRegistry());
        when(entities.enabled()).thenReturn(true);
        when(queries.execute(any())).thenAnswer(i->result());when(semantic.execute(anyString(),anyMap())).thenAnswer(i->result());
    }
    private Map<String,Object> result(){Map<String,Object> r=new LinkedHashMap<>();r.put("status","SUCCESS");r.put("rows",Collections.emptyList());r.put("summary",Collections.emptyList());return r;}
    private AssistantUnderstandingResult.Subtask task(String id,String capability){AssistantUnderstandingResult.Subtask t=new AssistantUnderstandingResult.Subtask();t.setId(id);t.setTitle("查询"+id);t.setQuestion("本轮完整问题");t.setCapability(capability);return t;}
    private AssistantUnderstandingResult understanding(AssistantUnderstandingResult.Subtask... tasks){AssistantUnderstandingResult r=new AssistantUnderstandingResult();r.setQueryMode(AssistantSemanticRequest.QueryMode.DATA);r.setTasks(Arrays.asList(tasks));return r;}
    private void entity(AssistantUnderstandingResult.Subtask task,String type,String name,long id){
        AssistantSemanticRequest.EntityMention e=new AssistantSemanticRequest.EntityMention();e.setEntityType(type);e.setKeyword(name);e.setRole("subject");task.getEntities().add(e);
        Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);row.put("name",name);row.put("entityType",type);
        when(entities.resolveType(AssistantEntityResolver.EntityType.valueOf(type),name)).thenReturn(Collections.singletonList(row));
    }
    @Test void availabilityUsesExtractedNameWithoutReparsingQuestion(){
        AssistantUnderstandingResult.Subtask task=task("t1","query_product_stock");entity(task,"PRODUCT","路通源刹车片",7);
        AssistantOrchestrator.Outcome outcome=executor.executeUnderstood("路通源刹车片 有吗",understanding(task),null,Collections.emptyList());
        ArgumentCaptor<AssistantPlan> plan=ArgumentCaptor.forClass(AssistantPlan.class);verify(queries).execute(plan.capture());
        assertEquals("路通源刹车片",plan.getValue().getProduct());assertEquals(7L,plan.getValue().getProductId());
        assertTrue(plan.getValue().isProductResultTable());assertNotNull(outcome.getEnvelope().getUnderstanding());verifyNoInteractions(model);
    }
    @Test void multipleDataAndKnowledgeTasksKeepIndependentResults(){
        AssistantUnderstandingResult.Subtask a=task("t1",AssistantCompanyKnowledge.TOOL_NAME),b=task("t2","query_verified_metric"),c=task("t3","unsupported");
        b.getConditions().setMetric(AssistantPlan.Metric.SALE);b.getConditions().setPeriod("THIS_MONTH");
        when(knowledge.available()).thenReturn(true);when(knowledge.answer(anyString(),eq(model))).thenAnswer(i->result());
        Map<String,Object> result=executor.executeUnderstood("介绍公司并查销售和未开放功能",understanding(a,b,c),null,Collections.emptyList()).getResult();
        assertEquals("PARTIAL_SUCCESS",result.get("status"));assertEquals(3,((List<?>)result.get("tasks")).size());verify(queries,times(1)).execute(any());verifyNoInteractions(model);
    }
    @Test void savedBlankClarificationDoesNotBlockStockOrHistoryRerun(){
        for(String blank:Arrays.asList(null,"","  \t\n")) {
            AssistantUnderstandingResult.Subtask task=task("t1","query_product_stock");
            task.setClarification(blank);entity(task,"PRODUCT","lty999",7);
            AssistantOrchestrator.Outcome outcome=executor.executeUnderstood("lty999 有吗",understanding(task),null,Collections.emptyList());
            assertEquals("SUCCESS",outcome.getResult().get("status"));
            assertEquals("SUCCESS",executor.rerun(new AssistantExecutionCodec().read(new AssistantExecutionCodec().write(outcome.getEnvelope()),null),Collections.emptyList()).get("status"));
        }
        verify(queries,times(6)).execute(any());verifyNoInteractions(model);
    }
    @Test void genuineMissingFieldsAndClarificationsStillStopBeforeQuery(){
        for(int mode=0;mode<3;mode++) {
            AssistantUnderstandingResult.Subtask task=task("t1","query_product_stock");
            task.setClarification("");
            if(mode==0) task.setMissingFields(Collections.singletonList("entity:PRODUCT"));
            if(mode==1) task.setClarification("请提供具体产品");
            if(mode==2) task.setCapability("clarify_intent");
            AssistantOrchestrationClarification failure=assertThrows(AssistantOrchestrationClarification.class,()->executor.executeUnderstood("它有吗",understanding(task),null,Collections.emptyList()));
            assertFalse(String.valueOf(failure.getResponse().get("clarification")).trim().isEmpty());
        }
        verifyNoInteractions(queries,entities);
    }
    @Test void dependentTaskDoesNotRunWhenItsPrerequisiteFailed(){
        AssistantUnderstandingResult.Subtask a=task("t1","unsupported"),b=task("t2","query_verified_metric");b.setDependsOn(Collections.singletonList("t1"));
        Map<String,Object> result=executor.executeUnderstood("组合问题",understanding(a,b),null,Collections.emptyList()).getResult();
        assertEquals("TASKS_INCOMPLETE",result.get("status"));verifyNoInteractions(queries);
    }
    @Test void documentQueryUsesBoundStructuredTimeAndEntityFilters(){
        AssistantUnderstandingResult.Subtask task=task("t1","query_purchase_documents");
        task.getConditions().setPeriod("THIS_MONTH");task.getConditions().setAggregation("COUNT");entity(task,"SUPPLIER","测试供应商",9);
        executor.executeUnderstood("供应商订单有几张",understanding(task),null,Collections.emptyList());
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);ArgumentCaptor<Map> params=ArgumentCaptor.forClass(Map.class);verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("supplier_id=:"));assertTrue(sql.getValue().contains("order_time>=:from"));assertTrue(params.getValue().containsValue(9L));verifyNoInteractions(model);
    }
    @Test void unsupportedGroupCannotSilentlyBroadenTheQuery(){
        AssistantUnderstandingResult.Subtask task=task("t1","query_purchase_documents");task.getConditions().setGroup("PRODUCT");
        assertThrows(AssistantFailure.class,()->executor.executeUnderstood("按产品采购订单",understanding(task),null,Collections.emptyList()));verifyNoInteractions(semantic);
    }
    @Test void structuredHistoryRoundTripsWithoutNewModelCalls(){
        AssistantUnderstandingResult.Subtask task=task("t1","query_verified_metric");task.getConditions().setMetric(AssistantPlan.Metric.RECEIPT);task.getConditions().setPeriod("LAST_WEEK");
        AssistantExecutionEnvelope envelope=executor.executeUnderstood("上周收款",understanding(task),null,Collections.emptyList()).getEnvelope();
        AssistantExecutionCodec codec=new AssistantExecutionCodec();AssistantExecutionEnvelope saved=codec.read(codec.write(envelope),null);
        executor.rerun(saved,Collections.emptyList());verify(queries,times(2)).execute(any());verifyNoInteractions(model);
    }
    @Test void rerunKeepsSelectedIdAndRevalidatesItWithoutResolvingTheNameAgain(){
        AssistantUnderstandingResult.Subtask task=task("t1","query_product_stock");entity(task,"PRODUCT","路通源刹车片",7);
        AssistantExecutionEnvelope envelope=executor.executeUnderstood("路通源刹车片 有吗",understanding(task),null,Collections.emptyList()).getEnvelope();
        executor.rerun(envelope,Collections.emptyList());
        verify(entities,times(1)).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源刹车片");
        verify(entities).validate(AssistantEntityResolver.EntityType.PRODUCT,7L);verify(queries,times(2)).execute(argThat(p->Long.valueOf(7L).equals(p.getProductId())));verifyNoInteractions(model);
    }
}
