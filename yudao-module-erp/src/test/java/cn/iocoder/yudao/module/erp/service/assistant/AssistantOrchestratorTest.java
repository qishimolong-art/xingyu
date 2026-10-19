package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantOrchestratorTest {
    private AssistantOrchestrator orchestrator;
    private AssistantModelClient model;
    private AssistantQueryService queries;
    private AssistantSemanticQueryService semantic;
    private AssistantEntityResolver entities;
    private AssistantMasterDataQueryService masterData;
    private AssistantCompanyKnowledge companyKnowledge;
    private AssistantStage5BusinessQuery stage5;
    private AssistantProperties properties;

    @BeforeEach
    void setUp() {
        orchestrator=new AssistantOrchestrator();model=mock(AssistantModelClient.class);
        queries=mock(AssistantQueryService.class);semantic=mock(AssistantSemanticQueryService.class);entities=mock(AssistantEntityResolver.class);
        masterData=mock(AssistantMasterDataQueryService.class);
        companyKnowledge=mock(AssistantCompanyKnowledge.class);
        stage5=mock(AssistantStage5BusinessQuery.class);
        properties=new AssistantProperties();properties.getOrchestration().setPublishedDatasets(Collections.singletonList("purchase_orders"));
        AssistantSemanticCatalog catalog=new AssistantSemanticCatalog();ReflectionTestUtils.setField(catalog,"properties",properties);
        ReflectionTestUtils.setField(orchestrator,"properties",properties);
        ReflectionTestUtils.setField(orchestrator,"model",model);
        AssistantKnowledge knowledge=mock(AssistantKnowledge.class);when(knowledge.retrieve(anyString(),any(),anyList())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(orchestrator,"knowledge",knowledge);
        ReflectionTestUtils.setField(orchestrator,"queries",queries);
        ReflectionTestUtils.setField(orchestrator,"registry",new AssistantToolRegistry());
        ReflectionTestUtils.setField(orchestrator,"semanticCatalog",catalog);
        ReflectionTestUtils.setField(orchestrator,"semantic",semantic);
        ReflectionTestUtils.setField(orchestrator,"entities",entities);
        ReflectionTestUtils.setField(orchestrator,"masterData",masterData);
        ReflectionTestUtils.setField(orchestrator,"companyKnowledge",companyKnowledge);
        ReflectionTestUtils.setField(orchestrator,"stage5",stage5);
        ReflectionTestUtils.setField(orchestrator,"semanticKnowledge",new AssistantSemanticKnowledge());
    }

    @Test
    void stage5RepresentativeQuestionsUseControlledBusinessTools() {
        Map<String,String> cases=new LinkedHashMap<>();
        cases.put("列出启用的商品分类",AssistantStage5BusinessQuery.MASTER_TOOL);
        cases.put("列出订货公司和费用项目",AssistantStage5BusinessQuery.MASTER_TOOL);
        cases.put("列出所有部门",AssistantStage5BusinessQuery.ORGANIZATION_TOOL);
        cases.put("查询当前启用部门",AssistantStage5BusinessQuery.ORGANIZATION_TOOL);
        cases.put("列出用户与部门关系",AssistantStage5BusinessQuery.ORGANIZATION_TOOL);
        cases.put("张三属于哪个部门",AssistantStage5BusinessQuery.ORGANIZATION_TOOL);
        cases.put("本月调拨出库有多少张",AssistantStage5BusinessQuery.STOCK_TOOL);
        cases.put("没有调拨移仓权限时查询调拨台账",AssistantStage5BusinessQuery.STOCK_TOOL);
        cases.put("本月配件价格调整历史",AssistantStage5BusinessQuery.STOCK_TOOL);
        cases.put("本月待拣货任务",AssistantStage5BusinessQuery.SALE_TOOL);
        cases.put("本月采购入库和退货分别有多少张",AssistantStage5BusinessQuery.PURCHASE_TOOL);
        cases.put("分别查询配件价格调整和采购调价业务单据",AssistantStage5BusinessQuery.PURCHASE_TOOL);
        cases.put("本月应收调账记录",AssistantStage5BusinessQuery.FINANCE_TOOL);
        for(Map.Entry<String,String> item:cases.entrySet())
            assertEquals(item.getValue(),orchestrator.previewCapability(item.getKey()),item.getKey());
        assertEquals("query_verified_metric",orchestrator.previewCapability("本月销售额"));
        assertEquals("query_sale_documents",orchestrator.previewCapability("本月销售订单有多少张"));
        assertEquals("model_routed",orchestrator.previewCapability("总账科目余额是多少"));
    }

    @Test
    void generalizedPreviewSeparatesInventoryOperationsAndCompanyFacts() {
        when(companyKnowledge.available()).thenReturn(true);
        for(String question:Arrays.asList("有库存的配件有多少种","多少种配件现在有库存","产品库存排名前10"))
            assertEquals("query_verified_metric",orchestrator.previewCapability(question),question);
        for(String question:Arrays.asList("哪些配件有库存","列出有货产品","有库存的商品清单"))
            assertEquals("query_inventory_details",orchestrator.previewCapability(question),question);
        for(String question:Arrays.asList("公司成立了多久","兴宇什么时候创立的","兴宇主营业务是什么"))
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,orchestrator.previewCapability(question),question);
        assertEquals("query_product_stock",orchestrator.previewCapability("黑霸王当前库存"));
    }

    @Test
    void productStockKeepsFirstTenFuzzyMatchesInsteadOfRequestingAChoice() {
        List<Map<String,Object>> matches=new ArrayList<>();
        for(long id=1;id<=12;id++) {Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);
            row.put("name","黑霸王"+id);row.put("entityType","PRODUCT");matches.add(row);}
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"黑霸王")).thenReturn(matches);
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();envelope.setToolName("query_product_stock");

        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(orchestrator,"resolveEntityOrClarify",
                envelope,"PRODUCT","黑霸王","QUERY","PRODUCT",true));

        assertEquals(10,((List<?>)envelope.getArguments().get("resolvedProductCandidates")).size());
        assertEquals(true,envelope.getArguments().get("productMatchTruncated"));
        assertEquals(1L,((Map<?,?>)envelope.getArguments().get("resolvedEntity")).get("id"));
    }

    @Test
    void bareProductNameKeepsFirstTenFuzzyMatchesAndDefaultsToPriceAndStock() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        List<Map<String,Object>> matches=new ArrayList<>();
        for(long id=1;id<=21;id++) matches.add(entity(id,"卫斯卡空气滤清器"+id,"PRODUCT"));
        when(entities.resolveAcrossTypes("卫斯卡空气滤清器")).thenReturn(matches);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("卫斯卡空气滤清器",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("query_product_stock",outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertTrue(plan.isProductResultTable());assertTrue(plan.isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.ALL,plan.getPriceQueryMode());
        assertEquals(10,plan.getProductIds().size());assertEquals(Long.valueOf(1L),plan.getProductIds().get(0));
        assertEquals(Long.valueOf(10L),plan.getProductIds().get(9));assertTrue(plan.isProductMatchTruncated());
        assertNull(plan.getProduct());assertNull(plan.getProductId());
        verify(entities).resolveAcrossTypes("卫斯卡空气滤清器");verify(queries).execute(any());
    }

    @Test
    void bareCrossTypeMatchesStillRequireAChoice() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveAcrossTypes("兴宇"))
                .thenReturn(Arrays.asList(entity(1L,"兴宇产品","PRODUCT"),entity(2L,"兴宇客户","CUSTOMER")));

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("兴宇",null,Collections.emptyList()));

        assertEquals("CHOICE_REQUIRED",clarification.getResponse().get("status"));
        assertEquals(2,((List<?>)clarification.getResponse().get("candidates")).size());
        verify(queries,never()).execute(any());
    }

    @Test
    void stockOnlyProductQuestionLoadsVisiblePricesWithoutBecomingAnExplicitPriceQuery() {
        Map<String,Object> arguments=new LinkedHashMap<>();arguments.put("product","黑霸王");

        AssistantPlan plan=ReflectionTestUtils.invokeMethod(orchestrator,"productStockPlan","黑霸王库存",arguments);

        assertNotNull(plan);assertTrue(plan.isProductResultTable());assertTrue(plan.isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.NONE,plan.getPriceQueryMode());
    }

    @Test
    void companyPartnerRelationshipQuestionsUseKnowledgeBeforeBrandProductLookup() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=knowledgeResult("四川兴宇是美孚授权经销商。[S1]");
        result.put("basis",Collections.singletonList("[S1]《四川兴宇与美孚的合作关系》"));
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        List<String> questions=Arrays.asList(
                "我们和美孚是什么关系",
                "咱们和美孚到底是什么合作关系",
                "我们公司怎么和美孚合作",
                "美孚和兴宇是什么关系",
                "兴宇是美孚的代理商还是经销商");
        for(String question:questions) {
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,orchestrator.previewCapability(question),question);
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
            assertEquals("COMPANY_KNOWLEDGE",outcome.getEnvelope().getSemanticResult().get("intentId"),question);
        }

        verify(companyKnowledge,times(questions.size())).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void realtimeBrandQueriesKeepTheirControlledBusinessRoutes() {
        when(companyKnowledge.available()).thenReturn(true);

        assertEquals("query_product_stock",orchestrator.previewCapability("美孚DOT4价格"));
        assertEquals("query_product_stock",orchestrator.previewCapability("美孚黑霸王库存"));
        assertEquals("query_verified_metric",orchestrator.previewCapability("美孚9月份销量"));
        assertEquals("query_verified_metric",orchestrator.previewCapability("成都客户本月欠款"));
        assertEquals("query_purchase_documents",orchestrator.previewCapability("某供应商采购订单"));
    }

    @Test
    void filteredProductArchiveCountKeepsKeywordThroughRoutingAndExecution() {
        String question="ERP中有多少个美孚产品";
        AssistantMasterDataQueryService.Query parsed=new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.PRODUCT,
                AssistantMasterDataQueryService.StatusMode.ENABLED,"美孚");
        when(masterData.parse(question)).thenReturn(parsed);
        when(masterData.parseArguments(anyMap())).thenAnswer(invocation -> {
            Map<String,Object> arguments=invocation.getArgument(0);
            return new AssistantMasterDataQueryService.Query(
                    AssistantMasterDataQueryService.ArchiveType.valueOf(String.valueOf(arguments.get("archiveType"))),
                    AssistantMasterDataQueryService.StatusMode.valueOf(String.valueOf(arguments.get("statusMode"))),
                    String.valueOf(arguments.get("keyword")));
        });
        when(masterData.execute(any())).thenReturn(semanticResult());

        assertEquals(AssistantMasterDataQueryService.TOOL_NAME,orchestrator.previewCapability(question));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());

        assertEquals(AssistantMasterDataQueryService.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("美孚",outcome.getEnvelope().getArguments().get("keyword"));
        verify(masterData).execute(argThat(query -> "美孚".equals(query.getProductKeyword())));
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
    }

    @Test
    void stage5ExecutionDelegatesWithoutModelGeneratedSql() {
        Map<String,Object> result=semanticResult();result.put("queryType","CONTROLLED_BUSINESS_DATASET");
        when(stage5.execute(eq(AssistantStage5BusinessQuery.PURCHASE_TOOL),anyString(),any())).thenReturn(result);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("本月采购入库和退货分别有多少张",null,Collections.emptyList());

        assertEquals(AssistantStage5BusinessQuery.PURCHASE_TOOL,outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        verify(stage5).execute(eq(AssistantStage5BusinessQuery.PURCHASE_TOOL),eq("本月采购入库和退货分别有多少张"),same(outcome.getEnvelope()));
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
        verify(model,never()).semanticSql(anyString(),anyList(),any());
    }

    @Test
    void ambiguousModelRouteClarifiesInsteadOfGuessingABusinessTool() {
        AssistantToolCall selected=new AssistantToolCall();selected.setId("clarify-1");
        selected.setName(AssistantToolRegistry.CLARIFY_INTENT_TOOL);
        Map<String,Object> arguments=new LinkedHashMap<>();arguments.put("question","帮我看看这个情况");
        arguments.put("choice1","查询实时业务数据");arguments.put("choice2","查询公司知识资料");
        selected.setArguments(arguments);
        when(model.chooseTool(eq("帮我看看这个情况"),argThat(Map::isEmpty),anyList())).thenReturn(selected);

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("帮我看看这个情况",null,Collections.emptyList()));

        assertEquals("AMBIGUOUS_INTENT",clarification.getResponse().get("reasonCode"));
        assertEquals(Arrays.asList("查询实时业务数据","查询公司知识资料"),clarification.getResponse().get("choices"));
        verify(queries,never()).execute(any());
        verify(entities,never()).resolveType(any(),anyString());
    }

    @Test
    void productStockCapabilityRequiresAConcreteProductInsteadOfSearchingTheWholeSentence() {
        for(String question:Arrays.asList("有没有货","配件库存是多少","查一下产品价格")) {
            AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                    ()->orchestrator.execute(question,null,Collections.emptyList()),question);
            assertEquals("MISSING_PRODUCT",clarification.getResponse().get("reasonCode"),question);
            assertTrue(((List<?>)clarification.getResponse().get("missingFields")).contains("entity:PRODUCT"),question);
        }
        verify(entities,never()).resolveType(eq(AssistantEntityResolver.EntityType.PRODUCT),anyString());
        verify(queries,never()).execute(any());
    }

    @Test
    void companyIntroductionUsesSharedKnowledgeToolWithoutDatabaseQuery() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","公司知识回答");
        when(companyKnowledge.answer("请介绍一下兴宇公司",model)).thenReturn(result);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("请介绍一下兴宇公司",null,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("公司知识回答",outcome.getResult().get("answerText"));
        verify(companyKnowledge).answer("请介绍一下兴宇公司",model);
        verifyNoInteractions(queries,semantic,entities);
    }

    @Test
    void namedProductCertificationQuestionUsesKnowledgeInsteadOfEntityLookup() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","经典表现认证信息。[S1]");
        when(companyKnowledge.answer("经典表现有什么认证",model)).thenReturn(result);

        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                orchestrator.previewCapability("经典表现有什么认证"));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "经典表现有什么认证",null,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals("经典表现认证信息。[S1]",outcome.getResult().get("answerText"));
        verify(companyKnowledge).answer("经典表现有什么认证",model);
        verifyNoInteractions(queries,semantic,entities);
    }

    @Test
    void companyOrganizationQuestionsUseKnowledgeWithoutProductOrOrganizationDataQuery() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","公司组织架构回答。[S1]");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList(
                "公司的组织架构",
                "公司组织结构是什么",
                "兴宇的公司架构",
                "有哪些部门",
                "兴宇有哪些部门和负责人",
                "公司的部门负责人分别是谁",
                "谁是总经办的负责人",
                "市场部负责人是谁",
                "项目部由谁负责",
                "谁负责物流部")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
        }

        verify(companyKnowledge,times(10)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void companyLeadershipQuestionsUseKnowledgeBeforeBareEntityResolution() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","截至2026年原文版本，资料记录的董事长为熊进战。[S1]");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList("公司的老板是谁", "公司的一把手是谁", "董事长是谁",
                "兴宇的掌舵人是谁", "公司最高负责人是哪位", "谁负责整个公司",
                "总经理是谁", "法人是谁", "法定代表人是谁", "创始人是谁")) {
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,orchestrator.previewCapability(question),question);
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals("COMPANY_KNOWLEDGE",outcome.getEnvelope().getSemanticResult().get("intentId"),question);
            assertTrue(outcome.getEnvelope().getToolCalls().stream().noneMatch(call ->
                    "resolve_business_entity".equals(call.get("name"))),question);
        }

        verify(companyKnowledge,times(10)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void companyLeadershipPersonContextSupportsReviewedPronounFollowUp() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> leadership=new LinkedHashMap<>();leadership.put("status","SUCCESS");
        leadership.put("queryType","KNOWLEDGE");leadership.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        leadership.put("summary",Collections.emptyList());leadership.put("rows",Collections.emptyList());
        leadership.put("answerText","截至2026年原文版本，资料记录的董事长为熊进战。[S1]");
        leadership.put(AssistantCompanyKnowledge.INTERNAL_ORGANIZATION_CONTEXT,
                organizationContext("公司","熊进战"));
        when(companyKnowledge.answer(eq("公司的老板是谁"),same(model))).thenReturn(leadership);

        Map<String,Object> personWork=new LinkedHashMap<>();personWork.put("status","SUCCESS");
        personWork.put("queryType","KNOWLEDGE");personWork.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        personWork.put("summary",Collections.emptyList());personWork.put("rows",Collections.emptyList());
        personWork.put("answerText","熊进战兼管项目部、经销商业务部。[S1]");
        String context="公司的老板是谁；已确认负责人：熊进战；追问：他还负责什么";
        when(companyKnowledge.answer(eq("他还负责什么"),eq(context),
                eq(AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK),same(model))).thenReturn(personWork);

        AssistantOrchestrator.Outcome first=orchestrator.execute("公司的老板是谁",null,Collections.emptyList());
        assertEquals(Collections.singletonList("熊进战"),
                ((Map<?,?>)first.getEnvelope().getArguments().get(
                        AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT)).get("people"));
        assertFalse(first.getResult().containsKey(AssistantCompanyKnowledge.INTERNAL_ORGANIZATION_CONTEXT));

        AssistantOrchestrator.Outcome second=orchestrator.execute("他还负责什么",first.getEnvelope(),Collections.emptyList());
        assertEquals("熊进战兼管项目部、经销商业务部。[S1]",second.getResult().get("answerText"));
        assertEquals(context,second.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals(AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK,
                AssistantKnowledgeContext.organizationIntent("他还负责什么",first.getEnvelope()));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void specificOrganizationDutyQuestionsUseKnowledgeWithoutEntityResolution() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","现有资料未详细提供部门职责。[S1]");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList("财务部的主要职责", "市场部职责是什么", "物流部负责什么",
                "项目部主要做什么", "总经办有哪些职责")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
            assertEquals("现有资料未详细提供部门职责。[S1]",outcome.getResult().get("answerText"),question);
        }

        verify(companyKnowledge,times(5)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void unifiedOrganizationQuestionsUseKnowledgeBeforeProductOrEntityResolution() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","组织知识回答。[S1]");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList("公司各部门之间如何协作", "这些部门的负责人分别是谁",
                "谁负责传媒工作", "传媒工作由谁负责", "汪凯虎的工作", "汪凯虎负责什么",
                "组织架构经历过哪些变化", "经销商业务部如何划分区域")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
            assertEquals(Collections.emptyList(),outcome.getEnvelope().getSemanticResult().get("expectedEntityTypes"),question);
        }

        verify(companyKnowledge,times(8)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationPeopleQuestionsUseKnowledgeBeforeBusinessObjectResolution() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","现有资料仅明确记录总经办汪凯虎。[S1]");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList("总经办有哪些人", "总经办有那些人", "总经办都有谁",
                "总经办的人员组成", "传媒运营部有哪些人", "财务部成员有哪些")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
        }

        verify(companyKnowledge,times(6)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void completePeopleQuestionOverridesAccumulatedCollaborationIntent() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","现有资料仅明确记录总经办汪凯虎。[S1]");
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);
        AssistantExecutionEnvelope previous=knowledgePrevious("这些部门的负责人");
        previous.getArguments().put(AssistantKnowledgeContext.CONTEXT_ARGUMENT,
                "公司各部门之间如何协作；追问：这些部门的负责人");

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "总经办有哪些人",previous,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals("公司各部门之间如何协作；追问：这些部门的负责人；追问：总经办有哪些人",
                outcome.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        verify(companyKnowledge).answer("总经办有哪些人",
                "公司各部门之间如何协作；追问：这些部门的负责人；追问：总经办有哪些人",
                AssistantKnowledgeContext.OrganizationIntent.PEOPLE,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationPronounAndCollaborationFollowUpsKeepKnowledgeContext() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);

        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");
        AssistantOrchestrator.Outcome leaders=orchestrator.execute(
                "这些部门的负责人分别是谁",previous,Collections.emptyList());
        AssistantOrchestrator.Outcome collaboration=orchestrator.execute(
                "它们之间如何协作",previous,Collections.emptyList());

        assertEquals("公司的组织架构；追问：这些部门的负责人分别是谁",
                leaders.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals("公司的组织架构；追问：它们之间如何协作",
                collaboration.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationKnowledgeFollowUpKeepsTopicAndNeverFallsBackToEntityResolution() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","公司人员组成回答。[S1]");
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);
        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "具体的人员组成",previous,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("公司的组织架构；追问：具体的人员组成",
                outcome.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals(Collections.emptyList(),outcome.getEnvelope().getSemanticResult().get("expectedEntityTypes"));
        verify(companyKnowledge).answer("具体的人员组成","公司的组织架构；追问：具体的人员组成",
                AssistantKnowledgeContext.OrganizationIntent.PEOPLE,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationDepartmentFollowUpKeepsKnowledgeContext() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","公司部门回答。[S1]");
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);
        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "有哪些部门",previous,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("公司的组织架构；追问：有哪些部门",
                outcome.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        verify(companyKnowledge).answer("有哪些部门","公司的组织架构；追问：有哪些部门",
                AssistantKnowledgeContext.OrganizationIntent.STRUCTURE,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationResponsibleFollowUpKeepsKnowledgeContext() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","总经办负责人回答。[S1]");
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);
        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "那总经办负责人是谁",previous,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("公司的组织架构；追问：那总经办负责人是谁",
                outcome.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals(Collections.emptyList(),outcome.getEnvelope().getSemanticResult().get("expectedEntityTypes"));
        verify(companyKnowledge).answer("那总经办负责人是谁","公司的组织架构；追问：那总经办负责人是谁",
                AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationLeaderAndSingularPronounCompleteThreeTurnKnowledgeChain() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> peopleResult=knowledgeResult("传媒运营部人员回答。[S1]");
        Map<String,Object> leaderResult=knowledgeResult("传媒运营部负责人为王宇。[S1]");
        Map<String,Object> organizationContext=organizationContext("传媒运营部","王宇");
        leaderResult.put(AssistantCompanyKnowledge.INTERNAL_ORGANIZATION_CONTEXT,organizationContext);
        Map<String,Object> workResult=knowledgeResult(
                "现有资料明确王宇为传媒运营部负责人，未提供其他明确工作分工。[S1]");
        when(companyKnowledge.answer("传媒运营部有哪些人",model)).thenReturn(peopleResult);
        when(companyKnowledge.answer(eq("谁是负责人"),anyString(),
                eq(AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY),same(model)))
                .thenReturn(leaderResult);
        when(companyKnowledge.answer(eq("他还负责哪些工作"),anyString(),
                eq(AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK),same(model)))
                .thenReturn(workResult);

        AssistantOrchestrator.Outcome first=orchestrator.execute(
                "传媒运营部有哪些人",null,Collections.emptyList());
        AssistantOrchestrator.Outcome second=orchestrator.execute(
                "谁是负责人",first.getEnvelope(),Collections.emptyList());
        AssistantOrchestrator.Outcome third=orchestrator.execute(
                "他还负责哪些工作",second.getEnvelope(),Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,second.getEnvelope().getRouteType());
        assertEquals(organizationContext,second.getEnvelope().getArguments()
                .get(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT));
        assertFalse(second.getResult().containsKey(AssistantCompanyKnowledge.INTERNAL_ORGANIZATION_CONTEXT));
        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,third.getEnvelope().getRouteType());
        assertEquals("传媒运营部有哪些人；追问：谁是负责人；已确认负责人：王宇；追问：他还负责哪些工作",
                third.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals(organizationContext,third.getEnvelope().getArguments()
                .get(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT));
        verify(companyKnowledge).answer("谁是负责人","传媒运营部有哪些人；追问：谁是负责人",
                AssistantKnowledgeContext.OrganizationIntent.RESPONSIBILITY,model);
        verify(companyKnowledge).answer("他还负责哪些工作",
                "传媒运营部有哪些人；追问：谁是负责人；已确认负责人：王宇；追问：他还负责哪些工作",
                AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void singularPersonPronounClarifiesWhenLeaderIsMissingOrAmbiguous() {
        AssistantFailure missing=assertThrows(AssistantFailure.class,()->orchestrator.execute(
                "他还负责什么",null,Collections.emptyList()));
        assertEquals("CLARIFY",missing.getCode());
        assertTrue(missing.getMessage().contains("具体指哪位人员"));

        AssistantExecutionEnvelope previous=knowledgePrevious("后勤部负责人是谁");
        previous.getArguments().put(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT,
                organizationContext("后勤部","毛琳","冯靖雯"));
        AssistantFailure ambiguous=assertThrows(AssistantFailure.class,()->orchestrator.execute(
                "他还负责什么",previous,Collections.emptyList()));
        assertEquals("CLARIFY",ambiguous.getCode());
        assertTrue(ambiguous.getMessage().contains("毛琳、冯靖雯"));
        verifyNoInteractions(model,queries,semantic,entities,stage5);
    }

    @Test
    void realtimeEllipticalOrganizationReferencesUseCapabilityBoundary() {
        when(companyKnowledge.available()).thenReturn(true);
        AssistantExecutionEnvelope unitPrevious=knowledgePrevious("传媒运营部有哪些人");
        assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                orchestrator.previewCapability("当前谁是负责人",unitPrevious));

        AssistantExecutionEnvelope personPrevious=knowledgePrevious("谁负责");
        personPrevious.getArguments().put(AssistantKnowledgeContext.ORGANIZATION_CONTEXT_ARGUMENT,
                organizationContext("传媒运营部","王宇"));
        assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                orchestrator.previewCapability("他当前还负责什么",personPrevious));
        when(stage5.execute(eq(AssistantStage5BusinessQuery.ORGANIZATION_TOOL),eq("他当前还负责什么"),any()))
                .thenThrow(new AssistantFailure("CAPABILITY_UNVERIFIED","实时查询尚未开放"));

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->orchestrator.execute(
                "他当前还负责什么",personPrevious,Collections.emptyList()));
        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        verify(stage5).execute(eq(AssistantStage5BusinessQuery.ORGANIZATION_TOOL),eq("他当前还负责什么"),any());
        verifyNoInteractions(model,queries,semantic,entities);
    }

    @Test
    void organizationDutyFollowUpKeepsKnowledgeContext() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","现有资料未详细提供财务部职责。[S1]");
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);
        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "那财务部主要负责什么",previous,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("公司的组织架构；追问：那财务部主要负责什么",
                outcome.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        assertEquals(Collections.emptyList(),outcome.getEnvelope().getSemanticResult().get("expectedEntityTypes"));
        verify(companyKnowledge).answer("那财务部主要负责什么","公司的组织架构；追问：那财务部主要负责什么",
                AssistantKnowledgeContext.OrganizationIntent.DUTY,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void organizationKnowledgeContextSupportsAnotherEllipticalTurn() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        when(companyKnowledge.answer(anyString(),anyString(),any(AssistantKnowledgeContext.OrganizationIntent.class),same(model)))
                .thenReturn(result);

        AssistantOrchestrator.Outcome second=orchestrator.execute(
                "具体的人员组成",knowledgePrevious("公司的组织架构"),Collections.emptyList());
        AssistantOrchestrator.Outcome third=orchestrator.execute(
                "这些人分别负责什么",second.getEnvelope(),Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,third.getEnvelope().getRouteType());
        assertEquals("公司的组织架构；追问：具体的人员组成；追问：这些人分别负责什么",
                third.getEnvelope().getArguments().get(AssistantKnowledgeContext.CONTEXT_ARGUMENT));
        verify(companyKnowledge).answer("这些人分别负责什么",
                "公司的组织架构；追问：具体的人员组成；追问：这些人分别负责什么",
                AssistantKnowledgeContext.OrganizationIntent.PERSON_WORK,model);
        verifyNoInteractions(queries,semantic,entities,stage5);
    }

    @Test
    void knowledgeContextDoesNotCaptureLiveOrganizationOrProductQueries() {
        when(companyKnowledge.available()).thenReturn(true);
        AssistantExecutionEnvelope previous=knowledgePrevious("公司的组织架构");

        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                orchestrator.previewCapability("具体的人员组成",previous));
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                orchestrator.previewCapability("有哪些部门",previous));
        assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                orchestrator.previewCapability("当前有哪些部门",previous));
        assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                orchestrator.previewCapability("列出当前启用部门",previous));
        assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                orchestrator.previewCapability("部门列表",previous));
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                orchestrator.previewCapability("那总经办负责人是谁",previous));
        for(String question:Arrays.asList("当前总经办负责人是谁", "ERP中总经办负责人是谁", "系统中市场部负责人是谁"))
            assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                    orchestrator.previewCapability(question,previous),question);
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                orchestrator.previewCapability("那财务部主要负责什么",previous));
        for(String question:Arrays.asList("当前财务部职责是什么", "ERP中市场部负责什么", "系统记录的物流部工作范围"))
            assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                    orchestrator.previewCapability(question,previous),question);
        for(String question:Arrays.asList("当前传媒工作由谁负责", "现在各部门如何协作", "ERP中汪凯虎负责什么",
                "当前总经办有哪些人", "ERP中总经办有那些人"))
            assertEquals(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                    orchestrator.previewCapability(question,previous),question);
        for(String question:Arrays.asList("财务部本月付款金额", "市场部销售额", "物流部库存"))
            assertNotEquals(AssistantCompanyKnowledge.TOOL_NAME,
                    orchestrator.previewCapability(question,previous),question);
        assertEquals("query_verified_metric",orchestrator.previewCapability("汪凯虎本月销售额",previous));
        assertEquals("query_product_stock",orchestrator.previewCapability("黑霸王库存",previous));
        assertNotEquals("resolve_business_entity",orchestrator.previewCapability("具体的人员组成"));
    }

    @Test
    void prefixedCustomerRankingFollowupUsesPreviousVerifiedMetricWithoutToolRerouting() {
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("THIS_MONTH");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("本月销售额是多少",previousPlan);
        AssistantPlan modelPlan=new AssistantPlan();modelPlan.setMetric(AssistantPlan.Metric.STOCK);
        modelPlan.setPeriod("CURRENT");modelPlan.setGroup(AssistantPlan.Group.PRODUCT);
        when(model.parse(eq("那按客户排名前5名呢"),same(previousPlan),anyList())).thenReturn(modelPlan);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("那按客户排名前5名呢",previous,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());
        assertEquals("THIS_MONTH",plan.getPeriod());
        assertEquals(AssistantPlan.Group.PARTY,plan.getGroup());
        assertEquals(5,plan.getLimit());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
    }

    @Test
    void singleDayFollowupCopiesTheCompletePreviousPlanAndOnlyReplacesDate() {
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("CUSTOM");previousPlan.setStartDate("2026-09-28");previousPlan.setEndDate("2026-09-28");
        previousPlan.setGroup(AssistantPlan.Group.PARTY);previousPlan.setParty("成都客户");previousPlan.setPartyId(101L);
        previousPlan.setDepartment("销售一部");previousPlan.setDepartmentId(201L);previousPlan.setLimit(5);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("9月28号成都客户的销售情况",previousPlan);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("29号的销售情况",previous,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("CUSTOM",plan.getPeriod());assertEquals("2026-09-29",plan.getStartDate());
        assertEquals("2026-09-29",plan.getEndDate());assertEquals(AssistantPlan.Group.PARTY,plan.getGroup());
        assertEquals("成都客户",plan.getParty());assertEquals(101L,plan.getPartyId());
        assertEquals("销售一部",plan.getDepartment());assertEquals(201L,plan.getDepartmentId());assertEquals(5,plan.getLimit());
        Map<?,?> trace=(Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace");
        assertEquals("PREVIOUS_YEAR_MONTH",trace.get("calendarDateSource"));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void terseDayFollowupUsesThePreviousSingleDayWithoutClarification() {
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("CUSTOM");previousPlan.setStartDate("2026-09-28");previousPlan.setEndDate("2026-09-28");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("9月28号销售情况",previousPlan);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("29号呢",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals("2026-09-29",plan.getStartDate());assertNull(plan.getClarification());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void standaloneDayClarifiesAndLastMonthChoiceExecutesOnlyThatDay() {
        AssistantOrchestrator.Outcome clarification=orchestrator.execute("29号的销售情况",null,Collections.emptyList());
        AssistantPlan missing=clarification.getEnvelope().getLegacyPlan();
        assertEquals("请确认“29号”是哪个月",missing.getClarification());
        assertEquals(Arrays.asList("上个月","本月"),missing.getChoices());
        verify(queries,never()).execute(any());

        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantOrchestrator.Outcome selected=orchestrator.execute(
                "29号的销售情况；补充：上个月",clarification.getEnvelope(),Collections.emptyList());
        AssistantPlan plan=selected.getEnvelope().getLegacyPlan();
        assertEquals("CUSTOM",plan.getPeriod());assertEquals("2026-09-29",plan.getStartDate());
        assertEquals("2026-09-29",plan.getEndDate());
        Map<?,?> trace=(Map<?,?>)selected.getEnvelope().getResultMetadata().get("queryTrace");
        assertEquals("CLARIFICATION_SUPPLEMENT",trace.get("calendarDateSource"));
        verify(queries,times(1)).execute(any());
    }

    @Test
    void changingMetricOnDayFollowupKeepsOnlyPreviousYearMonth() {
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("CUSTOM");previousPlan.setStartDate("2026-09-28");previousPlan.setEndDate("2026-09-28");
        previousPlan.setGroup(AssistantPlan.Group.PARTY);previousPlan.setParty("旧客户");previousPlan.setPartyId(101L);
        previousPlan.setProduct("旧配件");previousPlan.setProductId(301L);previousPlan.setDepartment("旧部门");previousPlan.setDepartmentId(201L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("9月28号旧客户销售额",previousPlan);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("29号的采购额",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.PURCHASE,plan.getMetric());assertEquals("2026-09-29",plan.getStartDate());
        assertEquals(AssistantPlan.Group.NONE,plan.getGroup());assertNull(plan.getParty());assertNull(plan.getPartyId());
        assertNull(plan.getProduct());assertNull(plan.getProductId());assertNull(plan.getDepartment());assertNull(plan.getDepartmentId());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void dayFollowupRevalidatesAndInheritsPrimaryAndMultiObjectContextTogether() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> customer=entity(101L,"成都客户","CUSTOMER");
        Map<String,Object> department=entity(201L,"销售一部","DEPARTMENT");
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,101L)).thenReturn(customer);
        when(entities.validate(AssistantEntityResolver.EntityType.DEPARTMENT,201L)).thenReturn(department);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("CUSTOM");previousPlan.setStartDate("2026-09-28");previousPlan.setEndDate("2026-09-28");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("9月28号销售情况",previousPlan);
        previous.getArguments().put("resolvedEntity",customer);
        Map<String,Object> all=new LinkedHashMap<>();all.put("CUSTOMER",customer);all.put("DEPARTMENT",department);
        previous.getArguments().put("resolvedEntities",all);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("29号的销售情况",previous,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(101L,plan.getPartyId());assertEquals(201L,plan.getDepartmentId());
        Map<?,?> inherited=(Map<?,?>)outcome.getEnvelope().getArguments().get("resolvedEntities");
        assertEquals(new LinkedHashSet<>(Arrays.asList("CUSTOMER","DEPARTMENT")),inherited.keySet());
        verify(entities).validate(AssistantEntityResolver.EntityType.CUSTOMER,101L);
        verify(entities).validate(AssistantEntityResolver.EntityType.DEPARTMENT,201L);
    }

    @Test
    void futureCurrentMonthDayFailsBeforeAnyQuery() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("29号的销售情况；补充：本月",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());assertTrue(failure.getMessage().contains("尚未发生"));
        verify(queries,never()).execute(any());
    }

    @Test
    void acceptanceKnowledgeQuestionsUseSharedKnowledgeToolWithoutDatabaseQuery() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText","公司知识回答");
        when(companyKnowledge.answer(anyString(),same(model))).thenReturn(result);

        for(String question:Arrays.asList(
                "API SP 和 ILSAC GF-6A 有什么关系",
                "客户觉得价格贵应该怎么沟通",
                "新员工产品学习路径是什么")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE,outcome.getEnvelope().getRouteType(),question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,outcome.getEnvelope().getToolName(),question);
        }

        verify(companyKnowledge,times(3)).answer(anyString(),same(model));
        verifyNoInteractions(queries,semantic,entities);
    }

    @Test
    void companyKnowledgeSemanticRoutingSkipsErpAndAuditsTopicFreshnessAndSources() {
        when(companyKnowledge.available()).thenReturn(true);
        when(companyKnowledge.answer(anyString(), same(model))).thenAnswer(invocation -> {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("id", "reviewed-source-1");
            source.put("updated", "2026-09-30");
            source.put("title", "已审核公司资料");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "SUCCESS");
            result.put("queryType", "KNOWLEDGE");
            result.put("toolName", AssistantCompanyKnowledge.TOOL_NAME);
            result.put("summary", Collections.emptyList());
            result.put("rows", Collections.emptyList());
            result.put("sources", Collections.singletonList(source));
            result.put("answerText", "公司知识回答。[S1]");
            return result;
        });

        Map<String, String> cases = new LinkedHashMap<>();
        cases.put("公司跟其他同行相比哪里做的更好", "ADVANTAGE");
        cases.put("我们和美孚是什么关系", "RELATIONSHIP");
        cases.put("公司是通过哪些渠道把产品卖出去的", "CHANNEL");
        cases.put("兴宇的业务现在覆盖了哪些区域", "REGION");
        for (Map.Entry<String, String> item : cases.entrySet()) {
            AssistantOrchestrator.Outcome outcome = orchestrator.execute(item.getKey(), null, Collections.emptyList());
            assertEquals(AssistantExecutionEnvelope.RouteType.KNOWLEDGE, outcome.getEnvelope().getRouteType(), item.getKey());
            assertEquals(item.getValue(), outcome.getEnvelope().getSemanticResult().get("knowledgeTopic"), item.getKey());
            Map<?, ?> trace = (Map<?, ?>) outcome.getEnvelope().getResultMetadata().get("queryTrace");
            assertEquals(Boolean.FALSE, trace.get("sqlGenerated"), item.getKey());
            assertEquals(Boolean.TRUE, trace.get("entityResolutionSkipped"), item.getKey());
            assertEquals(Collections.singletonList("reviewed-source-1"), trace.get("knowledgeSourceIds"), item.getKey());
            if ("REGION".equals(item.getValue())) {
                assertEquals("LATEST_REVIEWED", outcome.getResult().get("freshness"));
                assertTrue(String.valueOf(outcome.getResult().get("freshnessNotice")).contains("2026-09-30"));
                assertTrue(String.valueOf(outcome.getResult().get("freshnessNotice")).contains("不代表实时"));
            }
        }

        verifyNoInteractions(queries, semantic, entities, stage5);
    }

    @Test
    void mixedKnowledgeAndRealtimeQuestionReturnsKnowledgeAndAuthorizedDataTogether() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> knowledgeResult=new LinkedHashMap<>();knowledgeResult.put("status","SUCCESS");
        knowledgeResult.put("queryType","KNOWLEDGE");knowledgeResult.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        knowledgeResult.put("summary",Collections.emptyList());knowledgeResult.put("rows",Collections.emptyList());
        knowledgeResult.put("answerText","黑霸王是商用车润滑产品系列。[S1]");
        knowledgeResult.put("basis",Collections.singletonList("[S1]《美孚黑霸王产品总览》"));
        knowledgeResult.put("sources",Collections.singletonList(Collections.singletonMap("citation","S1")));
        when(companyKnowledge.answer("介绍美孚黑霸王",model)).thenReturn(knowledgeResult);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "介绍美孚黑霸王并查当前库存",null,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.MIXED,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantMixedQuery.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("SUCCESS",outcome.getResult().get("status"));
        assertEquals("MIXED",outcome.getResult().get("queryType"));
        assertEquals("query_product_stock",outcome.getResult().get("dataToolName"));
        assertEquals("黑霸王是商用车润滑产品系列。[S1]",outcome.getResult().get("answerText"));
        assertEquals("美孚黑霸王",outcome.getEnvelope().getLegacyPlan().getProduct());
        verify(companyKnowledge).answer("介绍美孚黑霸王",model);
        verify(queries).execute(any());
    }

    @Test
    void mixedQuestionReturnsPartialSuccessWhenKnowledgeBranchFails() {
        when(companyKnowledge.available()).thenReturn(true);
        when(companyKnowledge.answer("介绍美孚黑霸王",model))
                .thenThrow(new AssistantFailure("MODEL_INVALID","知识回答暂时不可用"));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "介绍美孚黑霸王并查当前库存",null,Collections.emptyList());

        assertEquals("PARTIAL_SUCCESS",outcome.getResult().get("status"));
        assertTrue(String.valueOf(outcome.getResult().get("warnings")).contains("知识回答暂时不可用"));
        assertNotNull(outcome.getEnvelope().getLegacyPlan());
        verify(queries).execute(any());
    }

    @Test
    void mixedQuestionKeepsKnowledgeAnswerWhenRealtimePermissionIsDenied() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> knowledgeResult=new LinkedHashMap<>();knowledgeResult.put("status","SUCCESS");
        knowledgeResult.put("answerText","产品知识回答。[S1]");knowledgeResult.put("basis",Collections.singletonList("[S1]《产品知识》"));
        knowledgeResult.put("sources",Collections.singletonList(Collections.singletonMap("citation","S1")));
        when(companyKnowledge.answer("介绍美孚黑霸王",model)).thenReturn(knowledgeResult);
        when(queries.execute(any())).thenThrow(new AssistantFailure("NO_PERMISSION","当前账号没有库存查询权限"));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "介绍美孚黑霸王并查当前库存",null,Collections.emptyList());

        assertEquals("PARTIAL_SUCCESS",outcome.getResult().get("status"));
        assertEquals("产品知识回答。[S1]",outcome.getResult().get("answerText"));
        assertEquals("FAILED",outcome.getResult().get("dataStatus"));
        assertTrue(String.valueOf(outcome.getResult().get("warnings")).contains("没有库存查询权限"));
    }

    @Test
    void mixedQuestionWithUnverifiedKnowledgeIsExplicitlyPartial() {
        when(companyKnowledge.available()).thenReturn(true);
        Map<String,Object> knowledgeResult=new LinkedHashMap<>();knowledgeResult.put("status","UNVERIFIED_KNOWLEDGE");
        knowledgeResult.put("answerText","该认证关系仍待核实。[S1]");knowledgeResult.put("verificationStatus","UNVERIFIED");
        knowledgeResult.put("warnings",Collections.singletonList("部分依据仍处于待核实状态"));
        when(companyKnowledge.answer("说明每日保护的卖点",model)).thenReturn(knowledgeResult);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "说明每日保护的卖点并查询现在有没有货",null,Collections.emptyList());

        assertEquals("PARTIAL_SUCCESS",outcome.getResult().get("status"));
        assertEquals("UNVERIFIED_KNOWLEDGE",outcome.getResult().get("knowledgeStatus"));
        assertTrue(String.valueOf(outcome.getResult().get("warnings")).contains("待核实"));
    }

    @Test
    void masterDataCountUsesDedicatedToolWithoutModelOrEntityResolution() {
        String question="总共有多少个客户";
        AssistantMasterDataQueryService.Query query=new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.CUSTOMER,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        when(masterData.parse(question)).thenReturn(query);
        when(masterData.parseArguments(anyMap())).thenReturn(query);
        Map<String,Object> row=new LinkedHashMap<>();row.put("label","启用客户");row.put("amount",204L);row.put("unit","个");
        Map<String,Object> result=new LinkedHashMap<>();result.put("summary",Collections.singletonList(row));
        result.put("rows",Collections.emptyList());result.put("status","SUCCESS");
        result.put("queryType","BUSINESS_TOOL");result.put("toolName",AssistantMasterDataQueryService.TOOL_NAME);
        when(masterData.execute(query)).thenReturn(result);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());

        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantMasterDataQueryService.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("CUSTOMER",outcome.getEnvelope().getArguments().get("archiveType"));
        assertEquals("ENABLED",outcome.getEnvelope().getArguments().get("statusMode"));
        assertEquals("MASTER_DATA_STATS",outcome.getEnvelope().getSemanticResult().get("intentId"));
        verify(masterData).execute(query);
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
        verify(model,never()).parse(anyString(),any(),anyList());
        verifyNoInteractions(entities);
    }

    @Test
    void employeeCountUsesDedicatedMasterDataToolWithoutModel() {
        String question="有多少个员工";
        AssistantMasterDataQueryService.Query query=new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.EMPLOYEE,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        when(masterData.parse(question)).thenReturn(query);
        when(masterData.parseArguments(anyMap())).thenReturn(query);
        Map<String,Object> result=new LinkedHashMap<>();result.put("summary",Collections.emptyList());
        result.put("rows",Collections.emptyList());result.put("status","SUCCESS");
        result.put("answerText","按 ERP 员工账号口径，当前租户内共有 42 个启用员工档案。");
        result.put("queryType","BUSINESS_TOOL");result.put("toolName",AssistantMasterDataQueryService.TOOL_NAME);
        when(masterData.execute(query)).thenReturn(result);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());

        assertEquals(AssistantMasterDataQueryService.TOOL_NAME,outcome.getEnvelope().getToolName());
        assertEquals("EMPLOYEE",outcome.getEnvelope().getArguments().get("archiveType"));
        assertEquals("ENABLED",outcome.getEnvelope().getArguments().get("statusMode"));
        assertEquals("按 ERP 员工账号口径，当前租户内共有 42 个启用员工档案。",outcome.getResult().get("answerText"));
        verify(masterData).execute(query);
        verifyNoInteractions(model,entities,queries,semantic);
    }

    @Test
    void receiptQuestionAfterProductArchiveCountStartsIndependentReceiptQuery() {
        String countQuestion="总共有多少个配件";
        AssistantMasterDataQueryService.Query query=new AssistantMasterDataQueryService.Query(
                AssistantMasterDataQueryService.ArchiveType.PRODUCT,
                AssistantMasterDataQueryService.StatusMode.ENABLED);
        when(masterData.parse(countQuestion)).thenReturn(query);
        when(masterData.parseArguments(anyMap())).thenReturn(query);
        Map<String,Object> countResult=new LinkedHashMap<>();countResult.put("summary",Collections.emptyList());
        countResult.put("rows",Collections.emptyList());countResult.put("status","SUCCESS");
        countResult.put("queryType","BUSINESS_TOOL");countResult.put("toolName",AssistantMasterDataQueryService.TOOL_NAME);
        when(masterData.execute(query)).thenReturn(countResult);
        when(entities.enabled()).thenReturn(true);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantExecutionEnvelope previous=orchestrator.execute(countQuestion,null,Collections.emptyList()).getEnvelope();
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("本周收了多少钱",previous,Collections.emptyList());

        assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
        assertEquals(AssistantPlan.Metric.RECEIPT,outcome.getEnvelope().getLegacyPlan().getMetric());
        assertEquals("THIS_WEEK",outcome.getEnvelope().getLegacyPlan().getPeriod());
        assertEquals("RECEIPT",outcome.getEnvelope().getSemanticResult().get("intentId"));
        assertEquals(Collections.emptyList(),outcome.getEnvelope().getSemanticResult().get("expectedEntityTypes"));
        assertNull(outcome.getEnvelope().getArguments().get("resolvedEntity"));
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
        verify(model,never()).parse(anyString(),any(),anyList());
        verify(entities,never()).resolveAcrossTypes(anyString());
        verify(entities,never()).resolveType(eq(AssistantEntityResolver.EntityType.PRODUCT),anyString());
    }

    @Test
    void unverifiedBusinessCapabilityStopsBeforeModelAndDatabaseQuery() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("客户欠款账龄是多少",null,Collections.emptyList()));

        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        assertNotNull(failure.getEnvelope());
        assertEquals("CAPABILITY_UNVERIFIED",failure.getEnvelope().getSemanticResult().get("outcomeStatus"));
        verifyNoInteractions(model,queries,entities);
    }

    @Test
    void explicitProductWarehouseStockUsesDedicatedToolAndQuantityMetric() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "卫斯卡空调滤清器WSC56208/WSC20856在各个仓库中的库存情况",null,Collections.emptyList());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals("query_product_stock",outcome.getEnvelope().getToolName());
        assertEquals(AssistantPlan.Metric.STOCK,outcome.getEnvelope().getLegacyPlan().getMetric());
        assertEquals("ALL",outcome.getEnvelope().getLegacyPlan().getStockMode());
        assertEquals("卫斯卡空调滤清器WSC56208/WSC20856",outcome.getEnvelope().getLegacyPlan().getProduct());
        assertTrue(outcome.getEnvelope().getLegacyPlan().isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.NONE,outcome.getEnvelope().getLegacyPlan().getPriceQueryMode());
        assertEquals("BUSINESS_TOOL",outcome.getResult().get("queryType"));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void productPriceQuestionUsesDedicatedBusinessToolAndDoesNotParseAsMetric() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("每日保护全合成530股份价",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("query_product_stock",outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantPlan.Metric.STOCK,plan.getMetric());
        assertEquals("每日保护全合成530",plan.getProduct());
        assertTrue(plan.isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.SPECIFIC,plan.getPriceQueryMode());
        assertEquals("sharePrice",plan.getPriceField());
        assertEquals("产品价格/库存查询",outcome.getResult().get("title"));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void productCodeWithLetterPrefixUsesDedicatedBusinessToolDeterministically() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveAcrossTypes("C402011661"))
                .thenReturn(Collections.singletonList(entity(402L,"测试编码配件","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("C402011661",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("query_product_stock",outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals(AssistantPlan.Metric.STOCK,plan.getMetric());
        assertEquals("测试编码配件",plan.getProduct());
        assertTrue(plan.isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.ALL,plan.getPriceQueryMode());
        verify(entities).resolveAcrossTypes("C402011661");
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void genericProductPriceQuestionRequestsAllPriceFields() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("美孚DOT4 价格",null,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals("美孚DOT4",plan.getProduct());
        assertTrue(plan.isIncludeProductPrices());
        assertEquals(AssistantPlan.PriceQueryMode.ALL,plan.getPriceQueryMode());
        assertNull(plan.getPriceField());
    }

    @Test
    void everySupportedSpecificPriceUsesServerOwnedFieldIntent() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        Map<String,String> cases=new LinkedHashMap<>();cases.put("采购价","purchasePrice");cases.put("销售价","salePrice");
        cases.put("最低价","minPrice");cases.put("参考价","referencePrice");cases.put("零售价","retailPrice");
        cases.put("批发价","wholesalePrice");cases.put("股份价","sharePrice");cases.put("最后采购价","lastPurchasePrice");

        for(Map.Entry<String,String> entry:cases.entrySet()) {
            AssistantPlan plan=orchestrator.execute("美孚DOT4 "+entry.getKey(),null,Collections.emptyList()).getEnvelope().getLegacyPlan();
            assertEquals(AssistantPlan.PriceQueryMode.SPECIFIC,plan.getPriceQueryMode(),entry.getKey());
            assertEquals(entry.getValue(),plan.getPriceField(),entry.getKey());
            assertEquals("美孚DOT4",plan.getProduct(),entry.getKey());
        }
    }

    @Test
    void barePriceQuestionAsksForProductName() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("价格",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());
        assertEquals("请说明要查询的产品名称、编码或型号",failure.getMessage());
        verify(model,never()).parse(anyString(),any(),anyList());
        verify(queries,never()).execute(any());
    }

    @Test
    void genericProductStockRankingUsesVerifiedMetricWithoutEntityLookup() {
        AssistantPlan guessed=new AssistantPlan();guessed.setMetric(AssistantPlan.Metric.STOCK_SKU);guessed.setPeriod("CURRENT");
        guessed.setProduct("哪个产品的库存是最多的");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(guessed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("哪个产品的库存是最多的",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC,outcome.getEnvelope().getRouteType());
        assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
        assertEquals(AssistantPlan.Metric.STOCK,plan.getMetric());assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        assertEquals(AssistantPlan.RankOrder.DESC,plan.getRankOrder());assertEquals(1,plan.getLimit());assertNull(plan.getProduct());
        verify(entities,never()).resolveType(any(),anyString());
        assertEquals(true,((Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace")).get("ranking"));
        assertEquals("RANKING",((Map<?,?>)outcome.getResult().get("presentation")).get("mode"));
        assertEquals(AssistantPlan.PresentationMode.RANKING,plan.getPresentationMode());
    }

    @Test
    void genericStockProductCountUsesPositiveSkuMetricWithoutEntityLookup() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        List<String> questions=Arrays.asList("库存中有多少个配件","有库存的配件数量",
                "有库存的配件有多少种","多少种配件现在有库存","配件有库存的品种数");
        for(String question:questions) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();

            assertEquals(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC,outcome.getEnvelope().getRouteType());
            assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
            assertEquals(AssistantPlan.Metric.STOCK_SKU,plan.getMetric());
            assertEquals("POSITIVE",plan.getStockMode());
            assertEquals(AssistantPlan.Group.NONE,plan.getGroup());
            assertNull(plan.getProduct());
            assertNull(outcome.getClarification());
        }
        verify(model,never()).parse(anyString(),any(),anyList());
        verify(entities,never()).resolveType(eq(AssistantEntityResolver.EntityType.PRODUCT),anyString());
        verify(queries,times(questions.size())).execute(any());
    }

    @Test
    void completeRankingDoesNotInheritPreviousProduct() {
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        previousPlan.setProduct("旧产品");previousPlan.setProductId(88L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧产品库存",previousPlan);
        AssistantPlan guessed=new AssistantPlan();guessed.setMetric(AssistantPlan.Metric.STOCK);guessed.setPeriod("CURRENT");guessed.setProduct("旧产品");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(guessed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("哪个产品库存最多",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertNull(plan.getProduct());assertNull(plan.getProductId());assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        verify(model).parse(eq("哪个产品库存最多"),isNull(),anyList());
    }

    @Test
    void productStockRankingTopResultBecomesFollowupProductContext() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"理塘成都进口汽修","PRODUCT"));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);
        previousPlan.setPeriod("CURRENT");previousPlan.setProduct("旧产品");previousPlan.setProductId(88L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("145988",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(88L,"卫斯卡空调滤清器WSC56208/WSC20856","PRODUCT"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.STOCK);parsed.setPeriod("CURRENT");
        when(model.parse(eq("哪个产品库存最多"),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->rankedStockResult(invocation.getArgument(0),501L,"理塘成都进口汽修"));
        AssistantOrchestrator.Outcome ranking=orchestrator.execute("哪个产品库存最多",previous,Collections.emptyList());

        Map<?,?> resolved=(Map<?,?>)ranking.getEnvelope().getArguments().get("resolvedEntity");
        assertEquals(501L,resolved.get("id"));
        assertEquals("理塘成都进口汽修",resolved.get("name"));

        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        orchestrator.execute("这个产品的出入库情况",ranking.getEnvelope(),Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));
        assertEquals(501L,params.getValue().get("p1"));
    }

    @Test
    void productFollowupWithWarehouseAddsBothStockMovementFilters() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"理塘成都进口汽修","PRODUCT"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.STOCK);parsed.setPeriod("CURRENT");
        when(model.parse(eq("哪个产品库存最多"),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->rankedStockResult(invocation.getArgument(0),501L,"理塘成都进口汽修"));
        AssistantOrchestrator.Outcome ranking=orchestrator.execute("哪个产品库存最多",null,Collections.emptyList());
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("这个产品在大塘仓的出入库情况",ranking.getEnvelope(),Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));
        assertTrue(sql.getValue().contains("q.warehouse_id=:p2"));
        assertEquals(501L,params.getValue().get("p1"));
        assertEquals(66L,params.getValue().get("p2"));
    }

    @Test
    void productFollowupCanReplaceWarehouseByNaturalEllipsis() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧产品库存",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(501L,"理塘成都进口汽修","PRODUCT"));
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"理塘成都进口汽修","PRODUCT"));
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("这个产品换成大塘仓看出入库",previous,Collections.emptyList());

        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(contains("q.product_id=:p1 AND q.warehouse_id=:p2"),params.capture());
        assertEquals(501L,params.getValue().get("p1"));assertEquals(66L,params.getValue().get("p2"));
    }

    @Test
    void directProductCodeWithWarehouseAddsBothStockMovementFilters() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"145988"))
                .thenReturn(Collections.singletonList(entity(99L,"测试配件","PRODUCT")));
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("产品编码145988在大塘仓的出入库情况",null,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));
        assertTrue(sql.getValue().contains("q.warehouse_id=:p2"));
        assertEquals(99L,params.getValue().get("p1"));
        assertEquals(66L,params.getValue().get("p2"));
    }

    @Test
    void naturalProductNameStockMovementIsResolvedAndNeverFallsBackToAllProducts() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24"))
                .thenReturn(Collections.singletonList(entity(701L,"路通源化油器清洗剂450ml*24","PRODUCT")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute(
                "路通源化油器清洗剂450ml*24 的出入库情况",null,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));
        assertEquals(701L,params.getValue().get("p1"));
        List<Map<String,Object>> mentions=(List<Map<String,Object>>)outcome.getEnvelope().getSemanticResult().get("entityMentions");
        assertEquals("RESOLVED",mentions.get(0).get("status"));
        assertEquals(701L,mentions.get(0).get("resolvedId"));
    }

    @Test
    void naturalProductAndWarehouseStockMovementUsesBothResolvedIds() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24"))
                .thenReturn(Collections.singletonList(entity(701L,"路通源化油器清洗剂450ml*24","PRODUCT")));
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("路通源化油器清洗剂450ml*24在大塘仓的出入库情况",null,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));
        assertTrue(sql.getValue().contains("q.warehouse_id=:p2"));
        assertEquals(701L,params.getValue().get("p1"));assertEquals(66L,params.getValue().get("p2"));
    }

    @Test
    void warehouseFirstNaturalStockQuestionKeepsBothObjectFilters() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24"))
                .thenReturn(Collections.singletonList(entity(701L,"路通源化油器清洗剂450ml*24","PRODUCT")));
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("大塘仓里，路通源化油器清洗剂450ml*24的库存是多少",null,Collections.emptyList())
                .getEnvelope().getLegacyPlan();

        assertEquals(701L,plan.getProductId());assertEquals(66L,plan.getWarehouseId());
        assertEquals("路通源化油器清洗剂450ml*24",plan.getProduct());assertEquals("大塘仓",plan.getWarehouse());
    }

    @Test
    void missingNaturalProductStopsBeforeStockMovementQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"不存在的产品ABC-404"))
                .thenReturn(Collections.emptyList());

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("不存在的产品ABC-404的出入库情况",null,Collections.emptyList()));

        assertEquals("ENTITY_NOT_FOUND",failure.getCode());
        assertTrue(failure.getMessage().contains("产品"));
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void missingProductWithWarehouseStopsWithoutDroppingProductCondition() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"不存在的产品ABC-404"))
                .thenReturn(Collections.emptyList());

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("不存在的产品ABC-404在大塘仓的出入库情况",null,Collections.emptyList()));

        assertEquals("ENTITY_NOT_FOUND",failure.getCode());
        verify(entities,never()).resolveType(eq(AssistantEntityResolver.EntityType.WAREHOUSE),anyString());
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void completeNewNaturalProductQuestionDoesNotInheritOldProduct() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧产品库存",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(501L,"旧产品","PRODUCT"));
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24"))
                .thenReturn(Collections.singletonList(entity(701L,"路通源化油器清洗剂450ml*24","PRODUCT")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("路通源化油器清洗剂450ml*24的出入库情况",previous,Collections.emptyList());

        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(anyString(),params.capture());assertEquals(701L,params.getValue().get("p1"));
        verify(entities,never()).validate(AssistantEntityResolver.EntityType.PRODUCT,501L);
    }

    @Test
    void contextProductWithoutPreviousResolvedObjectStopsInsteadOfQueryingAllMovements() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("这个产品的出入库情况",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());assertTrue(failure.getMessage().contains("产品"));
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void missingCustomerStopsBeforeReceiptQueryAndCannotReturnAllCustomers() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"不存在"))
                .thenReturn(Collections.emptyList());

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("不存在的客户本月收款情况",null,Collections.emptyList()));

        assertEquals("ENTITY_NOT_FOUND",failure.getCode());
        assertTrue(failure.getMessage().contains("客户"));
        verify(model,never()).parse(anyString(),any(),anyList());
        verify(queries,never()).execute(any());
    }

    @Test
    void placeholderCustomerWithoutNameStopsBeforeReceiptQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("某客户本月收款情况",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());assertTrue(failure.getMessage().contains("客户"));
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries,never()).execute(any());
    }

    @Test
    void naturalCustomerReceiptUsesResolvedCustomerAndMarksConfirmedEmptyAsNoData() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源"))
                .thenReturn(Collections.singletonList(entity(91L,"成都路通源","CUSTOMER")));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("THIS_MONTH");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->{
            Map<String,Object> value=legacyResult(invocation.getArgument(0));value.put("status","EMPTY");return value;
        });

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("客户成都路通源本月收款情况",null,Collections.emptyList());

        assertEquals(91L,outcome.getEnvelope().getLegacyPlan().getPartyId());
        assertEquals("NO_DATA",outcome.getResult().get("semanticStatus"));
        assertTrue(String.valueOf(outcome.getResult().get("emptyText")).contains("已确认查询对象"));
    }

    @Test
    void confirmedProductBareSalesFollowupQueriesYearToDateAndReturnsNoData() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> product=entity(501L,"美孚 DOT4 PLUS","PRODUCT");
        product.put("keyword","dot4 plus");
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"美孚 DOT4 PLUS","PRODUCT"));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("dot4 plus 价格",previousPlan);
        previous.getArguments().put("resolvedEntity",product);
        when(queries.execute(any())).thenAnswer(invocation->{
            Map<String,Object> value=legacyResult(invocation.getArgument(0));value.put("status","EMPTY");return value;
        });

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("销量",previous,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());
        assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        assertEquals("CUSTOM",plan.getPeriod());assertEquals(LocalDate.now().withDayOfYear(1).toString(),plan.getStartDate());
        assertEquals(LocalDate.now().toString(),plan.getEndDate());assertEquals(501L,plan.getProductId());
        assertEquals("NO_DATA",outcome.getResult().get("semanticStatus"));
        assertTrue(String.valueOf(outcome.getResult().get("emptyText")).contains("没有业务记录"));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void bareChineseNameStartsANewObjectAndNeverFallsBackToOldProduct() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);previousPlan.setPeriod("THIS_MONTH");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("黑霸王的销量",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(759L,"美孚黑霸王CF-4 20W-50 18L","PRODUCT"));
        AssistantToolCall selected=new AssistantToolCall();selected.setId("t-new");selected.setName("resolve_business_entity");
        selected.setArguments(Collections.singletonMap("keyword","诚远"));
        when(model.chooseTool(eq("诚远"),argThat(Map::isEmpty),anyList())).thenReturn(selected);
        when(entities.resolveAcrossTypes("诚远")).thenReturn(Collections.singletonList(entity(3968L,"诚远235/60R18 CRN88","PRODUCT")));

        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("诚远",previous,Collections.emptyList());

        assertEquals(3968L,outcome.getEnvelope().getLegacyPlan().getProductId());
        assertTrue(outcome.getEnvelope().getLegacyPlan().isIncludeProductPrices());
        verify(entities,never()).validate(AssistantEntityResolver.EntityType.PRODUCT,759L);
        verify(queries).execute(any());
    }

    @Test
    void explicitNewProductSalesUsesCurrentObjectInsteadOfOldContext() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);previousPlan.setPeriod("THIS_MONTH");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("黑霸王的销量",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(759L,"美孚黑霸王CF-4 20W-50 18L","PRODUCT"));
        when(entities.resolveAcrossTypes("诚远2356018"))
                .thenReturn(Collections.singletonList(entity(3968L,"诚远235/60R18 CRN88","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("诚远2356018的销量",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        assertEquals(3968L,plan.getProductId());assertEquals("诚远235/60R18 CRN88",plan.getProduct());
        verify(entities,never()).validate(AssistantEntityResolver.EntityType.PRODUCT,759L);
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void unlabeledSalesSubjectIsResolvedAcrossBusinessObjectTypes() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveAcrossTypes("理县武杰"))
                .thenReturn(Collections.singletonList(entity(701L,"理县武杰汽修厂","CUSTOMER")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("理县武杰销量",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals(AssistantPlan.Group.PARTY,plan.getGroup());
        assertEquals(701L,plan.getPartyId());assertEquals("CUSTOM",plan.getPeriod());
        assertEquals(Collections.singletonList("CUSTOMER"),outcome.getEnvelope().getSemanticResult().get("resolvedEntityTypes"));
        verify(entities).resolveAcrossTypes("理县武杰");
    }

    @Test
    void bareSalesWithoutConfirmedObjectClarifiesInsteadOfQueryingAllProducts() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("销量",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());assertTrue(failure.getMessage().contains("哪个产品"));
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries,never()).execute(any());
    }

    @Test
    void explicitProductReferenceCannotReachPastAContextBarrier() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("这个产品的销量",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());assertTrue(failure.getMessage().contains("产品"));
        verify(queries,never()).execute(any());
    }

    @Test
    void bareSalesKeepsAnUnresolvedCandidateChoiceInsteadOfUsingOlderContext() {
        AssistantExecutionEnvelope previous=new AssistantExecutionEnvelope();
        Map<String,Object> candidate=entity(3968L,"诚远235/60R18 CRN88","PRODUCT");candidate.put("field","entity:PRODUCT");
        previous.getArguments().put("pendingEntities",Collections.singletonList(candidate));
        previous.getArguments().put("pendingMode","BARE");

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("销量",previous,Collections.emptyList()));

        assertEquals("CHOICE_REQUIRED",clarification.getResponse().get("status"));
        assertEquals(1,((List<?>)clarification.getResponse().get("candidates")).size());
        verify(queries,never()).execute(any());
    }

    @Test
    void repeatedConfirmedProductNameSalesFollowupDoesNotFailModelParsing() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> product=entity(501L,"美孚 DOT4 PLUS","PRODUCT");
        product.put("keyword","dot4 plus");
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"美孚 DOT4 PLUS","PRODUCT"));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("dot4 plus 价格",previousPlan);
        previous.getArguments().put("resolvedEntity",product);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("dot4 plus的销量",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        assertEquals("CUSTOM",plan.getPeriod());assertEquals(LocalDate.now().withDayOfYear(1).toString(),plan.getStartDate());
        assertEquals(LocalDate.now().toString(),plan.getEndDate());assertEquals(501L,plan.getProductId());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void confirmedProductSalesWithCustomMonthKeepsModelParsedPeriod() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> product=entity(501L,"美孚 DOT4 PLUS","PRODUCT");
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"美孚 DOT4 PLUS","PRODUCT"));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("dot4 plus 价格",previousPlan);
        previous.getArguments().put("resolvedEntity",product);
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.SALE);
        parsed.setGroup(AssistantPlan.Group.PRODUCT);parsed.setPeriod("CUSTOM");
        parsed.setStartDate("2026-08-01");parsed.setEndDate("2026-08-31");
        when(model.parse(anyString(),any(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("这个产品8月份销量",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals("CUSTOM",plan.getPeriod());assertEquals("2026-08-01",plan.getStartDate());
        assertEquals(501L,plan.getProductId());verify(model).parse(anyString(),any(),anyList());
    }

    @Test
    void emptyMetricWithoutEntityAlsoGetsClearNoDataText() {
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.SALE);parsed.setPeriod("THIS_MONTH");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->{
            Map<String,Object> value=legacyResult(invocation.getArgument(0));value.put("status","EMPTY");return value;
        });

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("本月销售额",null,Collections.emptyList());

        assertEquals("NO_DATA",outcome.getResult().get("semanticStatus"));
        assertEquals("在指定条件和当前权限范围内没有查询到相关数据",outcome.getResult().get("emptyText"));
    }

    @Test
    void commonMetricAndPeriodQuestionsAreDeterministicAndBypassModel() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        Map<String,Object[]> cases=new LinkedHashMap<>();
        cases.put("本月销售额",new Object[]{AssistantPlan.Metric.SALE,"THIS_MONTH",AssistantPlan.Group.NONE});
        cases.put("本月的销售额",new Object[]{AssistantPlan.Metric.SALE,"THIS_MONTH",AssistantPlan.Group.NONE});
        cases.put("这个月的销售额",new Object[]{AssistantPlan.Metric.SALE,"THIS_MONTH",AssistantPlan.Group.NONE});
        cases.put("本周收款趋势",new Object[]{AssistantPlan.Metric.RECEIPT,"THIS_WEEK",AssistantPlan.Group.DAY});
        Map<String,String> periods=new LinkedHashMap<>();periods.put("今天","TODAY");periods.put("昨天","YESTERDAY");
        periods.put("本周","THIS_WEEK");periods.put("这个礼拜","THIS_WEEK");
        periods.put("上周","LAST_WEEK");periods.put("上个星期","LAST_WEEK");periods.put("上一星期","LAST_WEEK");
        periods.put("上个礼拜","LAST_WEEK");periods.put("本月","THIS_MONTH");
        periods.put("上月","LAST_MONTH");periods.put("本年","THIS_YEAR");periods.put("去年","LAST_YEAR");
        Map<String,AssistantPlan.Metric> metrics=new LinkedHashMap<>();metrics.put("销售额",AssistantPlan.Metric.SALE);
        metrics.put("采购金额",AssistantPlan.Metric.PURCHASE);metrics.put("收款",AssistantPlan.Metric.RECEIPT);
        metrics.put("付款",AssistantPlan.Metric.PAYMENT);
        periods.forEach((periodText,period)->metrics.forEach((metricText,metric)->
                cases.put(periodText+metricText,new Object[]{metric,period,AssistantPlan.Group.NONE})));
        for(Map.Entry<String,Object[]> entry:cases.entrySet()) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(entry.getKey(),null,Collections.emptyList());
            AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
            assertEquals(entry.getValue()[0],plan.getMetric(),entry.getKey());
            assertEquals(entry.getValue()[1],plan.getPeriod(),entry.getKey());
            assertEquals(entry.getValue()[2],plan.getGroup(),entry.getKey());
            Map<?,?> trace=(Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace");
            assertEquals("DETERMINISTIC",trace.get("planSource"),entry.getKey());
        }
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void rollingMetricQuestionIsDeterministicAndCannotUseModelDates() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("最近7天的销售额",null,Collections.emptyList());
        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        LocalDate today=LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE);

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals("CUSTOM",plan.getPeriod());
        assertEquals(today.minusDays(6).toString(),plan.getStartDate());assertEquals(today.toString(),plan.getEndDate());
        Map<?,?> trace=(Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace");
        assertEquals("DETERMINISTIC",trace.get("planSource"));
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void rollingSalesSituationIsNotMisreadAsBusinessEntity() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("最近14填的销售情况",null,Collections.emptyList());
        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        LocalDate today=LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE);

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals("CUSTOM",plan.getPeriod());
        assertEquals(today.minusDays(13).toString(),plan.getStartDate());assertEquals(today.toString(),plan.getEndDate());
        assertEquals("DETERMINISTIC",((Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace")).get("planSource"));
        verify(entities,never()).resolveAcrossTypes(anyString());verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void previousWeekSalesAliasesAreNotMisreadAsBusinessEntities() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        for(String question:Arrays.asList("上个星期的销售情况","上星期销售了多少","上一星期的销售额","上个礼拜的销量")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
            assertEquals(AssistantPlan.Metric.SALE,plan.getMetric(),question);assertEquals("LAST_WEEK",plan.getPeriod(),question);
            assertEquals(AssistantPlan.Group.NONE,plan.getGroup(),question);assertNull(plan.getProduct(),question);
        }
        verify(entities,never()).resolveAcrossTypes(anyString());verify(entities,never()).resolveType(any(),anyString());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void previousWeekPurchaseReceiptAndPaymentDoNotCreateFalseParties() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        Map<String,AssistantPlan.Metric> cases=new LinkedHashMap<>();
        cases.put("上个星期的采购额",AssistantPlan.Metric.PURCHASE);
        cases.put("上个星期收了多少钱",AssistantPlan.Metric.RECEIPT);
        cases.put("上个星期的付款情况",AssistantPlan.Metric.PAYMENT);
        for(Map.Entry<String,AssistantPlan.Metric> entry:cases.entrySet()) {
            AssistantPlan plan=orchestrator.execute(entry.getKey(),null,Collections.emptyList()).getEnvelope().getLegacyPlan();
            assertEquals(entry.getValue(),plan.getMetric(),entry.getKey());assertEquals("LAST_WEEK",plan.getPeriod(),entry.getKey());
            assertNull(plan.getParty(),entry.getKey());assertNull(plan.getPartyId(),entry.getKey());
        }
        verify(entities,never()).resolveAcrossTypes(anyString());verify(entities,never()).resolveType(any(),anyString());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void namedProductBeforeRollingPeriodRemainsEntityKeyword() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveAcrossTypes("经典表现")).thenReturn(Collections.singletonList(entity(88L,"经典表现","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("经典表现最近14天的销售情况",null,Collections.emptyList())
                .getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals(88L,plan.getProductId());
        assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        verify(entities).resolveAcrossTypes("经典表现");verify(entities,never()).resolveAcrossTypes("最近14天");
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void namedProductBeforePreviousWeekAliasRemainsEntityKeyword() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveAcrossTypes("黑霸王")).thenReturn(Collections.singletonList(entity(89L,"黑霸王","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("黑霸王上个星期的销售情况",null,Collections.emptyList())
                .getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.SALE,plan.getMetric());assertEquals("LAST_WEEK",plan.getPeriod());
        assertEquals(89L,plan.getProductId());assertEquals(AssistantPlan.Group.PRODUCT,plan.getGroup());
        verify(entities).resolveAcrossTypes("黑霸王");verify(entities,never()).resolveAcrossTypes("上个星期");
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void invalidRollingPeriodClarifiesWithoutCallingModelOrQuery() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("本月最近7天的销售额",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries,never()).execute(any());
    }

    @Test
    void periodOnlyFollowupKeepsPreviousMetricAndFiltersWithoutCallingModel() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("THIS_MONTH");previousPlan.setGroup(AssistantPlan.Group.PARTY);
        previousPlan.setParty("测试客户");previousPlan.setPartyId(91L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("本月销售额",previousPlan);

        AssistantPlan changed=orchestrator.execute("换成去年",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.SALE,changed.getMetric());assertEquals("LAST_YEAR",changed.getPeriod());
        assertEquals(AssistantPlan.Group.PARTY,changed.getGroup());assertEquals(91L,changed.getPartyId());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void rollingPeriodFollowupKeepsPreviousMetricEntityAndFilters() {
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.SALE);
        previousPlan.setPeriod("THIS_MONTH");previousPlan.setGroup(AssistantPlan.Group.PARTY);
        previousPlan.setParty("测试客户");previousPlan.setPartyId(91L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("本月销售额",previousPlan);

        AssistantPlan changed=orchestrator.execute("换成最近7天",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();
        LocalDate today=LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE);

        assertEquals(AssistantPlan.Metric.SALE,changed.getMetric());assertEquals("CUSTOM",changed.getPeriod());
        assertEquals(today.minusDays(6).toString(),changed.getStartDate());assertEquals(today.toString(),changed.getEndDate());
        assertEquals(AssistantPlan.Group.PARTY,changed.getGroup());assertEquals(91L,changed.getPartyId());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void deterministicCustomerMentionWinsBeforeEquivalentModelMention() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("THIS_MONTH");
        parsed.setParty("成都路通源");parsed.setEntityMentions(Collections.singletonList(modelMention("CUSTOMER","成都路通源","CUSTOMER_FILTER")));
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源"))
                .thenReturn(Collections.singletonList(entity(91L,"成都路通源","CUSTOMER")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("成都路通源这个月收款",null,Collections.emptyList());

        assertEquals(91L,outcome.getEnvelope().getLegacyPlan().getPartyId());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源");
        List<Map<String,Object>> mentions=(List<Map<String,Object>>)outcome.getEnvelope().getSemanticResult().get("entityMentions");
        assertTrue(mentions.stream().anyMatch(item->"NATURAL_NAME".equals(item.get("extractionMethod")) && "RESOLVED".equals(item.get("status"))));
    }

    @Test
    void modelCannotInventBusinessObjectAbsentFromOriginalQuestion() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("CUSTOM");
        parsed.setStartDate("2026-08-01");parsed.setEndDate("2026-08-31");
        parsed.setParty("模型猜测客户");parsed.setEntityMentions(Collections.singletonList(modelMention("CUSTOMER","模型猜测客户","CUSTOMER_FILTER")));
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("查询收款，日期为8月",null,Collections.emptyList()));

        assertEquals("CLARIFY",failure.getCode());verify(entities,never()).resolveType(any(),anyString());verify(queries,never()).execute(any());
    }

    @Test
    void deterministicEntityWinsAndModelConflictIsAudited() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源"))
                .thenReturn(Collections.singletonList(entity(91L,"成都路通源","CUSTOMER")));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("CUSTOM");
        parsed.setStartDate("2026-08-01");parsed.setEndDate("2026-08-31");
        parsed.setParty("路通源");parsed.setEntityMentions(Collections.singletonList(modelMention("CUSTOMER","路通源","CUSTOMER_FILTER")));
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("客户成都路通源收款，日期为8月",null,Collections.emptyList());

        assertEquals(91L,outcome.getEnvelope().getLegacyPlan().getPartyId());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都路通源");
        verify(entities,never()).resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"路通源");
        List<Map<String,Object>> conflicts=(List<Map<String,Object>>)outcome.getEnvelope().getSemanticResult().get("conflicts");
        assertEquals("SERVER_EXPLICIT_WINS",conflicts.get(0).get("decision"));
    }

    @Test
    void warehouseStockRankingResolvesOnlyWarehouseAndKeepsProductAsDimension() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"甘孜仓"))
                .thenReturn(Collections.singletonList(entity(67L,"甘孜仓","WAREHOUSE")));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.STOCK);parsed.setPeriod("CURRENT");
        parsed.setGroup(AssistantPlan.Group.PRODUCT);parsed.setLimit(1);
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("甘孜仓中哪个产品库存最多",null,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(67L,plan.getWarehouseId());assertNull(plan.getProductId());
        verify(entities,never()).resolveType(eq(AssistantEntityResolver.EntityType.PRODUCT),anyString());
    }

    @Test
    void recognizedSupplierProductCombinationIsBlockedWhenDetailDatasetIsUnpublished() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantToolCall selected=new AssistantToolCall();selected.setId("t1");selected.setName("query_verified_metric");
        selected.setArguments(Collections.singletonMap("question","供应商A卖给我们多少个产品B"));
        when(model.chooseTool(anyString(),anyMap(),anyList())).thenReturn(selected);
        when(entities.resolveType(AssistantEntityResolver.EntityType.SUPPLIER,"A"))
                .thenReturn(Collections.singletonList(entity(81L,"供应商A","SUPPLIER")));
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"B"))
                .thenReturn(Collections.singletonList(entity(82L,"产品B","PRODUCT")));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.PURCHASE);parsed.setPeriod("THIS_MONTH");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("供应商A卖给我们多少个产品B",null,Collections.emptyList()));

        assertEquals("DATASET_UNPUBLISHED",failure.getCode());
        assertTrue(failure.getMessage().contains("产品"));
        assertTrue(failure.getMessage().contains("未执行删减条件"));
        verify(queries,never()).execute(any());
    }

    @Test
    void ambiguousWarehouseInProductFollowupClarifiesBeforeStockMovementQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,501L))
                .thenReturn(entity(501L,"理塘成都进口汽修","PRODUCT"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.STOCK);parsed.setPeriod("CURRENT");
        when(model.parse(eq("哪个产品库存最多"),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->rankedStockResult(invocation.getArgument(0),501L,"理塘成都进口汽修"));
        AssistantOrchestrator.Outcome ranking=orchestrator.execute("哪个产品库存最多",null,Collections.emptyList());
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Arrays.asList(entity(66L,"大塘仓","WAREHOUSE"),entity(67L,"大塘仓库二","WAREHOUSE")));
        clearInvocations(semantic);

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("这个产品在大塘仓的出入库情况",ranking.getEnvelope(),Collections.emptyList()));

        assertEquals("匹配到多个有权限的业务对象，请选择",clarification.getResponse().get("clarification"));
        assertEquals("WAREHOUSE",clarification.getEnvelope().getArguments().get("pendingEntitySlot"));
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void selectedNaturalProductCandidateResumesWithWarehouseFilter() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> first=entity(701L,"路通源化油器清洗剂450ml*24 A","PRODUCT");
        Map<String,Object> selected=entity(702L,"路通源化油器清洗剂450ml*24 B","PRODUCT");
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"路通源化油器清洗剂450ml*24"))
                .thenReturn(Arrays.asList(first,selected));
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,702L))
                .thenReturn(new LinkedHashMap<>(selected));
        when(entities.resolveType(AssistantEntityResolver.EntityType.WAREHOUSE,"大塘仓"))
                .thenReturn(Collections.singletonList(entity(66L,"大塘仓","WAREHOUSE")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("路通源化油器清洗剂450ml*24在大塘仓的出入库情况",null,Collections.emptyList()));

        orchestrator.resumeChoice(clarification.getEnvelope(),"entity:PRODUCT",702L,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.product_id=:p1"));assertTrue(sql.getValue().contains("q.warehouse_id=:p2"));
        assertEquals(702L,params.getValue().get("p1"));assertEquals(66L,params.getValue().get("p2"));
    }

    @Test
    void explicitProductCodeIsResolvedToAuthorizedIdBeforeStockQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> product=new LinkedHashMap<>();product.put("id",99L);product.put("name","测试配件");
        product.put("entityType","PRODUCT");product.put("matchType","CODE");
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"145988"))
                .thenReturn(Collections.singletonList(product));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("产品编码145988在各仓库的库存",null,Collections.emptyList());

        assertEquals(99L,outcome.getEnvelope().getLegacyPlan().getProductId());
        assertEquals("测试配件",outcome.getEnvelope().getLegacyPlan().getProduct());
        assertEquals(AssistantPlan.Group.WAREHOUSE,outcome.getEnvelope().getLegacyPlan().getGroup());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"145988");
        verify(entities).preparePlan(any(AssistantPlan.class));
    }

    @Test
    void bareUniqueProductUsesDefaultPriceAndStockCapability() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantToolCall selected=new AssistantToolCall();selected.setId("t1");selected.setName("resolve_business_entity");
        selected.setArguments(new LinkedHashMap<String,Object>(){{put("keyword","145988");}});
        when(model.chooseTool(anyString(),anyMap(),anyList())).thenReturn(selected);
        Map<String,Object> product=new LinkedHashMap<>();product.put("id",99L);product.put("name","测试配件");product.put("entityType","PRODUCT");
        when(entities.resolveAcrossTypes("145988")).thenReturn(Collections.singletonList(product));

        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("145988",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.STOCK,plan.getMetric());assertEquals(99L,plan.getProductId());
        assertTrue(plan.isIncludeProductPrices());assertEquals(AssistantPlan.PriceQueryMode.ALL,plan.getPriceQueryMode());
        verify(entities).resolveAcrossTypes("145988");
    }

    @Test
    void selectedProductPurposeChoiceCarriesResolvedEntityIntoStockQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantToolCall selectedTool=new AssistantToolCall();selectedTool.setId("t1");selectedTool.setName("resolve_business_entity");
        selectedTool.setArguments(new LinkedHashMap<String,Object>(){{put("keyword","145988");}});
        when(model.chooseTool(anyString(),anyMap(),anyList())).thenReturn(selectedTool);
        Map<String,Object> product=entity(99L,"测试配件","PRODUCT");
        when(entities.resolveAcrossTypes("145988")).thenReturn(Collections.singletonList(product));
        when(entities.validate(AssistantEntityResolver.EntityType.PRODUCT,99L)).thenReturn(entity(99L,"测试配件","PRODUCT"));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantExecutionEnvelope clarification=new AssistantExecutionEnvelope();clarification.setQuestion("145988");
        clarification.getArguments().put("resolvedEntity",product);
        AssistantOrchestrator.Outcome outcome=orchestrator.resumeChoice(clarification,"resolved:PRODUCT",99L,
                "这个产品；补充：查询价格/当前库存",Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.STOCK,plan.getMetric());
        assertEquals(99L,plan.getProductId());assertEquals("测试配件",plan.getProduct());
        assertTrue(plan.isIncludeProductPrices());
        verify(entities).validate(AssistantEntityResolver.EntityType.PRODUCT,99L);
    }

    @Test
    void selectedCustomerPurposeChoiceCarriesResolvedEntityIntoReceiptQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantToolCall selectedTool=new AssistantToolCall();selectedTool.setId("t1");selectedTool.setName("resolve_business_entity");
        selectedTool.setArguments(new LinkedHashMap<String,Object>(){{put("keyword","成都路通源");}});
        when(model.chooseTool(anyString(),anyMap(),anyList())).thenReturn(selectedTool);
        Map<String,Object> customer=entity(91L,"成都路通源","CUSTOMER");
        when(entities.resolveAcrossTypes("成都路通源")).thenReturn(Collections.singletonList(customer));
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,91L)).thenReturn(entity(91L,"成都路通源","CUSTOMER"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("THIS_MONTH");
        when(model.parse(eq("这个客户；补充：查询本月收款"),any(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantExecutionEnvelope clarification=new AssistantExecutionEnvelope();clarification.setQuestion("成都路通源");
        clarification.getArguments().put("resolvedEntity",customer);
        AssistantOrchestrator.Outcome outcome=orchestrator.resumeChoice(clarification,"resolved:CUSTOMER",91L,
                "这个客户；补充：查询本月收款",Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.RECEIPT,plan.getMetric());
        assertEquals(91L,plan.getPartyId());assertEquals("成都路通源",plan.getParty());
        verify(entities).validate(AssistantEntityResolver.EntityType.CUSTOMER,91L);
    }

    @Test
    void resolvedChoiceRejectsIdNotPresentInPreviousClarification() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantExecutionEnvelope previous=new AssistantExecutionEnvelope();
        previous.setQuestion("145988");previous.setToolName("resolve_business_entity");
        previous.setRouteType(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL);
        previous.getArguments().put("resolvedEntity",entity(99L,"测试配件","PRODUCT"));

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.resumeChoice(previous,"resolved:PRODUCT",100L,"这个产品；补充：查询价格/当前库存",Collections.emptyList()));

        assertEquals("INVALID_CHOICE",failure.getCode());
        verify(entities,never()).validate(any(),anyLong());
    }

    @Test
    void verifiedMetricAlwaysKeepsVerifiedRoute() {
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.SALE);parsed.setPeriod("THIS_MONTH");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("本月销售总金额是多少",null,Collections.emptyList());
        assertEquals(AssistantExecutionEnvelope.RouteType.VERIFIED_METRIC,outcome.getEnvelope().getRouteType());
        assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void receivableDebtQuestionStillUsesVerifiedMetricRoute() {
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIVABLE);parsed.setPeriod("CURRENT");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("哪个客户当前欠款最多",null,Collections.emptyList());

        assertEquals("query_verified_metric",outcome.getEnvelope().getToolName());
        assertEquals(AssistantPlan.Metric.RECEIVABLE,outcome.getEnvelope().getLegacyPlan().getMetric());
    }

    @Test
    void completeReceivableRankingDoesNotInheritPreviousProductEntity() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.STOCK);
        previousPlan.setPeriod("CURRENT");previousPlan.setProduct("旧产品");previousPlan.setProductId(2030L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("C402011661",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(2030L,"旧产品","PRODUCT"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIVABLE);parsed.setPeriod("CURRENT");
        parsed.setProduct("旧产品");parsed.setProductId(2030L);
        when(model.parse(eq("哪个客户欠钱最多"),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("哪个客户欠钱最多",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.RECEIVABLE,plan.getMetric());
        assertEquals(AssistantPlan.Group.PARTY,plan.getGroup());
        assertEquals("CURRENT",plan.getPeriod());
        assertNull(plan.getProduct());assertNull(plan.getProductId());
        assertNull(plan.getParty());assertNull(plan.getPartyId());
        verify(entities,never()).preparePlan(argThat(value->value.getProductId()!=null || value.getPartyId()!=null));
    }

    @Test
    void unlabelledFullCustomerDebtResolvesBeforeModelAndUsesReceivableMetric() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"理塘众鑫进口汽修"))
                .thenReturn(Collections.singletonList(entity(382L,"理塘众鑫进口汽修","CUSTOMER")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("理塘众鑫进口汽修  欠款",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals(AssistantPlan.Metric.RECEIVABLE,plan.getMetric());
        assertEquals("CURRENT",plan.getPeriod());assertEquals(382L,plan.getPartyId());
        assertEquals("理塘众鑫进口汽修",plan.getParty());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"理塘众鑫进口汽修");
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void partialCustomerDebtRequiresChoiceInsteadOfSelectingFirstMatch() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"理塘众鑫"))
                .thenReturn(Arrays.asList(entity(382L,"理塘众鑫进口汽修","CUSTOMER"),
                        entity(394L,"理塘众鑫进口汽修1","CUSTOMER")));

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("理塘众鑫 欠款",null,Collections.emptyList()));

        assertEquals("CHOICE_REQUIRED",clarification.getResponse().get("status"));
        assertEquals(2,((List<?>)clarification.getResponse().get("candidates")).size());
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries,never()).execute(any());
    }

    @Test
    void spacedProductStockQuestionKeepsWholeKeywordAndReturnsAuthorizedMatches() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚 CF 4L"))
                .thenReturn(Arrays.asList(entity(4509L,"美孚黑霸王CF-4 15W-40 4L 6*4L","PRODUCT"),
                        entity(12548L,"美孚黑霸王CF-4 20W-50 4L 6x4L","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("美孚 CF 4L 库存",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertTrue(plan.isProductResultTable());
        assertEquals(Arrays.asList(4509L,12548L),plan.getProductIds());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚 CF 4L");
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries).execute(any());
    }

    @Test
    void explicitProductBrandUsesDedicatedResolverScope() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚",
                AssistantEntityResolver.ProductMatchScope.BRAND))
                .thenReturn(Arrays.asList(entity(4509L,"美孚黑霸王CF-4 15W-40 4L 6*4L","PRODUCT"),
                        entity(12548L,"美孚黑霸王CF-4 20W-50 4L 6x4L","PRODUCT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("品牌为美孚的产品库存",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertTrue(plan.isProductResultTable());
        assertEquals(Arrays.asList(4509L,12548L),plan.getProductIds());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚",
                AssistantEntityResolver.ProductMatchScope.BRAND);
        verify(entities,never()).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚");
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries).execute(any());
    }

    @Test
    void completeReceivableRankingDoesNotInheritPreviousCustomerEntity() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.RECEIVABLE);
        previousPlan.setPeriod("CURRENT");previousPlan.setParty("旧客户");previousPlan.setPartyId(182L);
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧客户欠款",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(182L,"旧客户","CUSTOMER"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIVABLE);
        parsed.setPeriod("CURRENT");parsed.setParty("旧客户");parsed.setPartyId(182L);
        when(model.parse(eq("哪个客户当前欠款最多"),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("哪个客户当前欠款最多",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(AssistantPlan.Metric.RECEIVABLE,plan.getMetric());
        assertEquals(AssistantPlan.Group.PARTY,plan.getGroup());
        assertNull(plan.getParty());assertNull(plan.getPartyId());
        assertNull(plan.getProduct());assertNull(plan.getProductId());
    }

    @Test
    void explicitCustomerReferenceCanStillInheritSelectedCustomer() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,182L))
                .thenReturn(entity(182L,"旧客户","CUSTOMER"));
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.RECEIVABLE);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧客户欠款",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(182L,"旧客户","CUSTOMER"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIVABLE);parsed.setPeriod("CURRENT");
        when(model.parse(eq("这个客户欠款多少"),same(previousPlan),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantPlan plan=orchestrator.execute("这个客户欠款多少",previous,Collections.emptyList()).getEnvelope().getLegacyPlan();

        assertEquals(182L,plan.getPartyId());
        assertEquals("旧客户",plan.getParty());
    }

    @Test
    void inheritedCustomerIsRejectedAfterPermissionRevocation() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan previousPlan=new AssistantPlan();previousPlan.setMetric(AssistantPlan.Metric.RECEIVABLE);previousPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope previous=AssistantExecutionEnvelope.metric("旧客户欠款",previousPlan);
        previous.getArguments().put("resolvedEntity",entity(182L,"旧客户","CUSTOMER"));
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,182L))
                .thenThrow(new AssistantFailure("FORBIDDEN","该候选对象已不可访问，请重新查询"));

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("这个客户欠款多少",previous,Collections.emptyList()));

        assertEquals("NO_PERMISSION",failure.getCode());
        verify(queries,never()).execute(any());verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void historicalEllipticalRerunRevalidatesAndKeepsResolvedCustomer() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan oldPlan=new AssistantPlan();oldPlan.setMetric(AssistantPlan.Metric.RECEIVABLE);oldPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope old=AssistantExecutionEnvelope.metric("这个客户欠款多少",oldPlan);
        old.getArguments().put("resolvedEntity",entity(182L,"旧客户","CUSTOMER"));
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,182L)).thenReturn(entity(182L,"旧客户","CUSTOMER"));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIVABLE);parsed.setPeriod("CURRENT");
        when(model.parse(anyString(),same(oldPlan),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        Map<String,Object> result=orchestrator.rerun(old,Collections.emptyList());

        AssistantPlan executed=(AssistantPlan)result.get("plan");assertEquals(182L,executed.getPartyId());
        verify(entities).validate(AssistantEntityResolver.EntityType.CUSTOMER,182L);
    }

    @Test
    void historicalRerunStopsWhenResolvedObjectPermissionWasRevoked() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        AssistantPlan oldPlan=new AssistantPlan();oldPlan.setMetric(AssistantPlan.Metric.RECEIVABLE);oldPlan.setPeriod("CURRENT");
        AssistantExecutionEnvelope old=AssistantExecutionEnvelope.metric("这个客户欠款多少",oldPlan);
        old.getArguments().put("resolvedEntity",entity(182L,"旧客户","CUSTOMER"));
        when(entities.validate(AssistantEntityResolver.EntityType.CUSTOMER,182L))
                .thenThrow(new AssistantFailure("FORBIDDEN","该候选对象已不可访问，请重新查询"));

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->orchestrator.rerun(old,Collections.emptyList()));

        assertEquals("NO_PERMISSION",failure.getCode());verify(queries,never()).execute(any());
    }

    @Test
    void productNamePriceQuestionResolvesCandidatesBeforeQueryWhenEnabled() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> product=new LinkedHashMap<>();product.put("id",77L);product.put("name","每日保护全合成");
        product.put("entityType","PRODUCT");product.put("matchType","NAME");
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"每日保护全合成"))
                .thenReturn(Collections.singletonList(product));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("每日保护全合成多少钱",null,Collections.emptyList());

        assertEquals(77L,outcome.getEnvelope().getLegacyPlan().getProductId());
        assertEquals("每日保护全合成",outcome.getEnvelope().getLegacyPlan().getProduct());
        verify(entities).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"每日保护全合成");
    }

    @Test
    void highFrequencyBareProductAliasQueriesEveryMatchWithinTheLimit() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> a=new LinkedHashMap<>();a.put("id",1L);a.put("name","美孚黑霸王长效X40");a.put("entityType","PRODUCT");
        Map<String,Object> b=new LinkedHashMap<>();b.put("id",2L);b.put("name","美孚黑霸王傲超X40");b.put("entityType","PRODUCT");
        when(entities.resolveAcrossTypes("X40")).thenReturn(Arrays.asList(a,b));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("X40",null,Collections.emptyList());

        AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
        assertEquals("query_product_stock",outcome.getEnvelope().getToolName());
        assertEquals(Arrays.asList(1L,2L),plan.getProductIds());assertFalse(plan.isProductMatchTruncated());
        verify(entities).resolveAcrossTypes("X40");
        verify(queries).execute(any());
    }

    @Test
    void purchaseDocumentQuestionUsesServerOwnedTemplateWithinBudget() {
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("rows",Collections.singletonList(Collections.singletonMap("document_count",3)));
        result.put("summary",Collections.emptyList());result.put("queryTrace",Collections.singletonMap("fingerprint","abc"));
        when(semantic.execute(anyString(),anyMap())).thenReturn(result);
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("今天做了几个采购订单",null,Collections.emptyList());
        assertEquals("query_purchase_documents",outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals(3,outcome.getEnvelope().getToolCalls().size());
        verify(semantic).validateDatasetAccess("purchase_orders");
        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().startsWith("SELECT COUNT(q.id) document_count FROM purchase_orders q WHERE q.order_time>=:p1 AND q.order_time<:p2"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("p1","p2")),params.getValue().keySet());
        verify(model,never()).semanticSql(anyString(),anyList(),any());
    }

    @Test
    void dedicatedPurchaseToolUsesModelSupplementOnlyForUnlabelledConcreteSupplier() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(model.extractEntityMentions(eq("四川供应链公司的采购订单"),eq("query_purchase_documents"),anyCollection()))
                .thenReturn(Collections.singletonList(modelMention("SUPPLIER","四川供应链公司","PURCHASE_SUPPLIER")));
        when(entities.resolveType(AssistantEntityResolver.EntityType.SUPPLIER,"四川供应链公司"))
                .thenReturn(Collections.singletonList(entity(81L,"四川供应链公司","SUPPLIER")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("四川供应链公司的采购订单",null,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.supplier_id=:p1"));assertEquals(81L,params.getValue().get("p1"));
        verify(model,never()).semanticSql(anyString(),anyList(),any());
    }

    @Test
    void genericPurchaseDocumentQuestionDoesNotAddASecondModelCall() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("今天做了几个采购订单",null,Collections.emptyList());

        verify(model,never()).extractEntityMentions(anyString(),anyString(),anyCollection());
    }

    @Test
    void saleDocumentCountUsesServerOwnedTemplate() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("今天做了几个销售订单",null,Collections.emptyList());
        assertEquals("query_sale_documents",outcome.getEnvelope().getToolName());
        verify(semantic).execute(startsWith("SELECT COUNT(q.id) document_count FROM sale_orders q WHERE q.order_time>=:p1"),anyMap());
        verify(model,never()).semanticSql(anyString(),anyList(),any());
    }

    @Test
    void saleOrderProductAggregationUsesCustomerProductRelationDataset() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("按产品统计本月销售订单金额",null,Collections.emptyList());

        assertEquals("query_sale_documents",outcome.getEnvelope().getToolName());
        verify(semantic).validateDatasetAccess("sale_order_items");
        verify(semantic).execute(contains("SELECT q.product_name,SUM(q.line_amount) total_amount FROM sale_order_items q WHERE q.order_time>=:p1"),anyMap());
        verify(model,never()).semanticSql(anyString(),anyList(),any());
    }

    @Test
    void saleOrderNumberUsesExactServerOwnedCustomerLookup() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        orchestrator.execute("销售订单号XS20260929001是哪个客户",null,Collections.emptyList());

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("q.no=:p1"));
        assertEquals("XS20260929001",params.getValue().get("p1"));
    }

    @Test
    void namedCustomerSaleOrdersUseResolvedCustomerRelation() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.CUSTOMER,"成都车友"))
                .thenReturn(Collections.singletonList(entity(81L,"成都车友","CUSTOMER")));
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("成都车友客户有哪些销售订单",null,Collections.emptyList());

        assertEquals("query_sale_documents",outcome.getEnvelope().getToolName());
        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("FROM sale_orders q WHERE q.customer_id=:p1"));
        assertEquals(81L,params.getValue().get("p1"));
        assertEquals(Collections.singletonList("已审核销售订单；订单金额不等于已审核销售出库扣除退货后的销售金额"),outcome.getResult().get("basis"));
    }

    @Test
    void negativeInventoryDetailsUseCurrentInventoryDataset() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("查询负库存明细",null,Collections.emptyList());

        assertEquals("query_inventory_details",outcome.getEnvelope().getToolName());
        assertEquals("当前库存明细查询",outcome.getResult().get("title"));
        assertEquals(Collections.singletonList("当前账面库存数量；不包含库存金额、成本或历史时点库存"),outcome.getResult().get("basis"));
        verify(semantic).validateDatasetAccess("inventory_current");
        verify(semantic).execute(contains("FROM inventory_current q WHERE q.quantity<0 ORDER BY q.product_name,q.warehouse_name LIMIT 50"),eq(Collections.emptyMap()));
    }

    @Test
    void inventoryAmountIsRejectedInsteadOfReturningQuantity() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("当前库存金额是多少",null,Collections.emptyList()));

        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        assertTrue(failure.getMessage().contains("成本版本"));
        verifyNoInteractions(queries,entities);
    }

    @Test
    void unfulfilledPurchaseUsesRemainingQuantityTemplate() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        orchestrator.execute("哪些采购订单还没有到货",null,Collections.emptyList());
        verify(semantic).execute(contains("q.remaining_count>0 ORDER BY q.order_time DESC LIMIT 50"),eq(Collections.emptyMap()));
    }

    @Test
    void recentStockMovementsAreLimitedToTwentyRows() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        orchestrator.execute("最近的库存流水情况",null,Collections.emptyList());
        verify(semantic).execute(contains("ORDER BY q.biz_date DESC LIMIT 20"),eq(Collections.emptyMap()));
    }

    @Test
    void genericInboundExistenceUsesControlledInboundTypesAndCurrentWeek() {
        Map<String,Object> grouped=semanticResult();
        grouped.put("rows",Arrays.asList(row("biz_type",70,"record_count",3L),row("biz_type",30,"record_count",2L)));
        when(semantic.execute(anyString(),anyMap())).thenReturn(grouped);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("这个星期是否有入库记录",null,Collections.emptyList());

        assertEquals("query_stock_movements",outcome.getEnvelope().getToolName());
        assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType());
        assertEquals(5L,outcome.getResult().get("total"));
        assertTrue(String.valueOf(outcome.getResult().get("answerText")).contains("本周在当前权限范围内有入库记录，共5条"));
        @SuppressWarnings("unchecked")
        List<Map<String,Object>> summary=(List<Map<String,Object>>)outcome.getResult().get("summary");
        assertEquals(3,summary.size());
        assertEquals(Arrays.asList("全部库存入库","调拨入库","采购入库"),summary.stream()
                .map(row->String.valueOf(row.get("label"))).collect(java.util.stream.Collectors.toList()));
        assertEquals(Arrays.asList(5L,2L,3L),summary.stream().map(row->row.get("amount"))
                .collect(java.util.stream.Collectors.toList()));
        assertTrue(summary.stream().allMatch(row->"条".equals(row.get("unit"))));
        assertTrue(summary.stream().allMatch(row->Objects.equals(row.get("label"),row.get("businessObject"))));
        assertTrue(summary.stream().allMatch(row->Objects.equals(row.get("amount"),row.get("record_count"))));
        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic).execute(sql.capture(),params.capture());
        assertTrue(sql.getValue().contains("COUNT(q.id) record_count"));
        assertTrue(sql.getValue().contains("q.biz_date>=:p1 AND q.biz_date<:p2"));
        assertTrue(sql.getValue().contains("q.biz_type IN (:p3,:p4,:p5,:p6,:p7,:p8)"));
        assertEquals(new LinkedHashSet<>(Arrays.asList(10,30,34,40,60,70)),
                new LinkedHashSet<>(params.getValue().values()).stream().filter(Integer.class::isInstance)
                        .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)));
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
    }

    @Test
    void genericInboundDetailsReturnLatestTwentyWithReadableTypes() {
        Map<String,Object> grouped=semanticResult();grouped.put("rows",Collections.singletonList(row("biz_type",10,"record_count",1L)));
        Map<String,Object> details=semanticResult();details.put("rows",Collections.singletonList(row("biz_type",10,"biz_no","QTRK-1")));
        details.put("columns",new ArrayList<>());
        when(semantic.execute(contains("COUNT(q.id) record_count"),anyMap())).thenReturn(grouped);
        when(semantic.execute(contains("ORDER BY q.biz_date DESC LIMIT 20"),anyMap())).thenReturn(details);

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("这一星期的入库记录",null,Collections.emptyList());

        List<Map<String,Object>> rows=(List<Map<String,Object>>)outcome.getResult().get("rows");
        assertEquals("其它入库",rows.get(0).get("biz_type_name"));
        assertTrue(String.valueOf(outcome.getEnvelope().getArguments().get("detailSql")).contains("ORDER BY q.biz_date DESC"));
        assertFalse(String.valueOf(outcome.getEnvelope().getArguments().get("detailSql")).contains("LIMIT 20"));
        verify(semantic,times(2)).execute(anyString(),anyMap());
    }

    @Test
    void previousWeekInboundAliasesUseLastNaturalWeekWithoutEntityResolution() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        for(String question:Arrays.asList("上个星期的入库情况","上星期是否有入库记录","上一星期的入库流水")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            assertEquals("query_stock_movements",outcome.getEnvelope().getToolName(),question);
            assertEquals(AssistantExecutionEnvelope.RouteType.BUSINESS_TOOL,outcome.getEnvelope().getRouteType(),question);
        }
        org.mockito.ArgumentCaptor<Map<String,Object>> params=org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(semantic,times(3)).execute(anyString(),params.capture());
        LocalDate monday=LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE)
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        for(Map<String,Object> value:params.getAllValues()) {
            assertEquals(monday.minusWeeks(1)+" 00:00:00",String.valueOf(value.get("p1")));
            assertEquals(monday+" 00:00:00",String.valueOf(value.get("p2")));
        }
        verify(entities,never()).resolveAcrossTypes(anyString());verify(entities,never()).resolveType(any(),anyString());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
    }

    @Test
    void genericInboundExistenceReturnsClearEmptyAnswer() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("这一个月有没有入库",null,Collections.emptyList());

        assertEquals("EMPTY",outcome.getResult().get("status"));
        assertEquals(0L,outcome.getResult().get("total"));
        assertEquals("本月在当前权限范围内没有入库记录。",outcome.getResult().get("answerText"));
        assertEquals(Collections.emptyList(),outcome.getResult().get("summary"));
        verify(semantic,times(1)).execute(anyString(),anyMap());
        verify(model,never()).chooseTool(anyString(),anyMap(),anyList());
    }

    @Test
    void explicitInboundDocumentTypesKeepTheirExistingStage5Routes() {
        assertEquals(AssistantStage5BusinessQuery.PURCHASE_TOOL,orchestrator.previewCapability("本周采购入库记录"));
        assertEquals(AssistantStage5BusinessQuery.STOCK_TOOL,orchestrator.previewCapability("本周调拨入库记录"));
        assertEquals(AssistantStage5BusinessQuery.STOCK_TOOL,orchestrator.previewCapability("本周其他入库记录"));
        assertEquals("query_stock_movements",orchestrator.previewCapability("本周出入库记录"));
    }

    @Test
    void stockMovementDetailsUseServerOwnedUnboundedPageSql() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        when(semantic.serverOwnedDetails(anyString(),anyMap(),isNull(),eq(2),eq(20))).thenReturn(new LinkedHashMap<String,Object>(){{
            put("total",25);put("list",Collections.emptyList());put("columns",Collections.emptyList());
        }});

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("最近的库存流水情况",null,Collections.emptyList());
        orchestrator.details(outcome.getEnvelope(),2,20);

        org.mockito.ArgumentCaptor<String> sql=org.mockito.ArgumentCaptor.forClass(String.class);
        verify(semantic).serverOwnedDetails(sql.capture(),eq(Collections.emptyMap()),isNull(),eq(2),eq(20));
        assertTrue(sql.getValue().contains("ORDER BY q.biz_date DESC"));
        assertFalse(sql.getValue().contains("LIMIT 20"));
    }

    @Test
    void purchaseAmountBySupplierUsesGroupedServerTemplate() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        orchestrator.execute("按供应商统计本月采购订单金额",null,Collections.emptyList());
        verify(semantic).execute(contains("SELECT q.supplier_name,SUM(q.total_price) total_amount FROM purchase_orders q WHERE q.order_time>=:p1 AND q.order_time<:p2 GROUP BY q.supplier_name ORDER BY total_amount DESC LIMIT 10"),anyMap());
    }

    @Test
    void productDistributionDoesNotReturnIncompleteOrderResult() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("按产品统计本月采购订单金额",null,Collections.emptyList()));
        assertEquals("DATASET_UNPUBLISHED",failure.getCode());
        verify(semantic,never()).execute(anyString(),anyMap());
    }

    @Test
    void averageOrderAmountUsesScalarTemplate() {
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        orchestrator.execute("本月采购订单平均金额",null,Collections.emptyList());
        verify(semantic).execute(startsWith("SELECT AVG(q.total_price) average_amount FROM purchase_orders q WHERE"),anyMap());
    }

    @Test
    void customPeriodFallsBackToControlledTextToSqlOnlyWhenEnabled() {
        properties.getOrchestration().setTextToSqlEnabled(true);
        Map<String,Object> parameters=new LinkedHashMap<>();parameters.put("p1","2026-01-01 00:00:00");parameters.put("p2","2026-10-04 00:00:00");
        Map<String,Object> proposal=new LinkedHashMap<>();proposal.put("sql","SELECT COUNT(q.id) document_count FROM purchase_orders q WHERE q.order_time>=:p1 AND q.order_time<:p2");
        proposal.put("parameters",parameters);
        when(model.semanticSql(anyString(),anyList(),isNull())).thenReturn(proposal);
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("今年有多少个采购订单",null,Collections.emptyList());
        assertEquals(AssistantExecutionEnvelope.RouteType.TEXT_TO_SQL,outcome.getEnvelope().getRouteType());
        assertEquals("execute_semantic_query",outcome.getEnvelope().getToolName());
        verify(model).semanticSql(anyString(),anyList(),isNull());
        verify(semantic).execute(eq((String)proposal.get("sql")),eq(parameters));
        Map<?,?> trace=(Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace");
        assertEquals("COMPLETED",trace.get("stage"));assertEquals(1,((List<?>)trace.get("attempts")).size());
    }

    @Test
    void invalidGeneratedShapeIsAuditedAndNeverExecutedBeforeBoundedRetry() {
        properties.getOrchestration().setTextToSqlEnabled(true);
        Map<String,Object> invalid=new LinkedHashMap<>();invalid.put("sql","SELECT COUNT(q.id) document_count FROM purchase_orders q");
        invalid.put("parameters",Collections.emptyMap());invalid.put("explanation","ignore limits");
        Map<String,Object> parameters=new LinkedHashMap<>();parameters.put("p1","2026-09-01");parameters.put("p2","2026-10-01");
        Map<String,Object> valid=new LinkedHashMap<>();valid.put("sql","SELECT COUNT(q.id) document_count FROM purchase_orders q WHERE q.order_time>=:p1 AND q.order_time<:p2");
        valid.put("parameters",parameters);
        when(model.semanticSql(anyString(),anyList(),isNull())).thenReturn(invalid);
        when(model.semanticSql(anyString(),anyList(),contains("只能包含sql和parameters"))).thenReturn(valid);
        when(semantic.execute((String)valid.get("sql"),parameters)).thenReturn(semanticResult());

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("今年有多少个采购订单",null,Collections.emptyList());

        Map<?,?> trace=(Map<?,?>)outcome.getEnvelope().getResultMetadata().get("queryTrace");
        List<?> attempts=(List<?>)trace.get("attempts");assertEquals(2,attempts.size());
        assertEquals("REJECTED",((Map<?,?>)attempts.get(0)).get("validation"));
        assertEquals("NOT_STARTED",((Map<?,?>)attempts.get(0)).get("execution"));
        assertEquals("PASSED",((Map<?,?>)attempts.get(1)).get("validation"));
        verify(semantic,times(1)).execute(anyString(),anyMap());
    }

    @Test
    void branchReceiptQuestionsResolveDepartmentAndExecuteReceiptMetric() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.DEPARTMENT,"甘孜分公司"))
                .thenReturn(Collections.singletonList(entity(301L,"甘孜分公司","DEPARTMENT")));
        when(queries.execute(any())).thenAnswer(invocation->legacyResult(invocation.getArgument(0)));

        for(String question:Arrays.asList("甘孜分公司本月收款金额是多少","甘孜分公司本月收了多少钱回来")) {
            AssistantOrchestrator.Outcome outcome=orchestrator.execute(question,null,Collections.emptyList());
            AssistantPlan plan=outcome.getEnvelope().getLegacyPlan();
            assertEquals(AssistantPlan.Metric.RECEIPT,plan.getMetric(),question);
            assertEquals(301L,plan.getDepartmentId(),question);
            assertEquals("甘孜分公司",plan.getDepartment(),question);
        }
        verify(queries,times(2)).execute(any());
        verify(model,never()).parse(anyString(),any(),anyList());
    }

    @Test
    void resolvedBranchWithoutReceiptDataReturnsNoData() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.DEPARTMENT,"甘孜分公司"))
                .thenReturn(Collections.singletonList(entity(301L,"甘孜分公司","DEPARTMENT")));
        AssistantPlan parsed=new AssistantPlan();parsed.setMetric(AssistantPlan.Metric.RECEIPT);parsed.setPeriod("THIS_MONTH");
        when(model.parse(anyString(),isNull(),anyList())).thenReturn(parsed);
        when(queries.execute(any())).thenAnswer(invocation->{
            Map<String,Object> result=legacyResult(invocation.getArgument(0));result.put("status","EMPTY");return result;
        });

        AssistantOrchestrator.Outcome outcome=orchestrator.execute("甘孜分公司本月收款金额是多少",null,Collections.emptyList());

        assertEquals("NO_DATA",outcome.getResult().get("semanticStatus"));
        assertEquals("NO_DATA",outcome.getEnvelope().getSemanticResult().get("status"));
    }

    @Test
    void unavailableBranchStopsAsDepartmentNotFoundBeforeQuery() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.DEPARTMENT,"甘孜分公司"))
                .thenReturn(Collections.emptyList());

        AssistantFailure failure=assertThrows(AssistantFailure.class,
                ()->orchestrator.execute("甘孜分公司本月收了多少钱回来",null,Collections.emptyList()));

        assertEquals("ENTITY_NOT_FOUND",failure.getCode());assertTrue(failure.getMessage().contains("部门"));
        verify(model,never()).parse(anyString(),any(),anyList());
        verify(queries,never()).execute(any());
    }

    private static Map<String,Object> semanticResult() {
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("rows",Collections.emptyList());
        result.put("summary",Collections.emptyList());result.put("queryTrace",Collections.singletonMap("fingerprint","abc"));return result;
    }

    private static Map<String,Object> row(Object... values) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i+1<values.length;i+=2) result.put(String.valueOf(values[i]),values[i+1]);
        return result;
    }

    private static Map<String,Object> legacyResult(AssistantPlan plan) {
        Map<String,Object> knowledge=new LinkedHashMap<>();knowledge.put("title","测试指标");
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");result.put("plan",plan);
        result.put("knowledge",knowledge);result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());return result;
    }

    private static Map<String,Object> rankedStockResult(AssistantPlan plan,Long groupId,String label) {
        Map<String,Object> result=legacyResult(plan);
        Map<String,Object> row=new LinkedHashMap<>();row.put("groupId",groupId);row.put("label",label);
        row.put("amount",100);row.put("unit","件");row.put("unitRank",1);
        result.put("rows",Collections.singletonList(row));
        return result;
    }

    private static Map<String,Object> entity(Long id,String name,String type) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("id",id);result.put("name",name);result.put("entityType",type);return result;
    }

    private static AssistantPlan.ModelEntityMention modelMention(String type,String keyword,String role) {
        AssistantPlan.ModelEntityMention result=new AssistantPlan.ModelEntityMention();result.setEntityType(type);result.setKeyword(keyword);result.setRole(role);return result;
    }

    private static AssistantExecutionEnvelope knowledgePrevious(String question) {
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        envelope.setQuestion(question);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.KNOWLEDGE);
        envelope.setToolName(AssistantCompanyKnowledge.TOOL_NAME);
        AssistantSemanticRequest request=new AssistantSemanticRequest();request.setRequestId("previous-knowledge");
        envelope.setSemanticRequest(request);
        return envelope;
    }

    private static Map<String,Object> knowledgeResult(String answer) {
        Map<String,Object> result=new LinkedHashMap<>();result.put("status","SUCCESS");
        result.put("queryType","KNOWLEDGE");result.put("toolName",AssistantCompanyKnowledge.TOOL_NAME);
        result.put("summary",Collections.emptyList());result.put("rows",Collections.emptyList());
        result.put("answerText",answer);return result;
    }

    private static Map<String,Object> organizationContext(String unit,String... people) {
        Map<String,Object> context=new LinkedHashMap<>();context.put("unit",unit);
        context.put("people",Arrays.asList(people));context.put("role","负责人");
        context.put("sourceIds",Collections.singletonList("reviewed-source"));return context;
    }
}
