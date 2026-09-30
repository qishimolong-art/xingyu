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
    private AssistantProperties properties;

    @BeforeEach
    void setUp() {
        orchestrator=new AssistantOrchestrator();model=mock(AssistantModelClient.class);
        queries=mock(AssistantQueryService.class);semantic=mock(AssistantSemanticQueryService.class);entities=mock(AssistantEntityResolver.class);
        masterData=mock(AssistantMasterDataQueryService.class);
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
        ReflectionTestUtils.setField(orchestrator,"semanticKnowledge",new AssistantSemanticKnowledge());
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
        assertFalse(outcome.getEnvelope().getLegacyPlan().isIncludeProductPrices());
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

        for(String question:Arrays.asList("库存中有多少个配件","有库存的配件数量")) {
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
        verify(queries,times(2)).execute(any());
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
        periods.put("本周","THIS_WEEK");periods.put("上周","LAST_WEEK");periods.put("本月","THIS_MONTH");
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
    void spacedProductStockQuestionKeepsWholeKeywordForAuthorizedResolution() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        when(entities.resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚 CF 4L"))
                .thenReturn(Arrays.asList(entity(4509L,"美孚黑霸王CF-4 15W-40 4L 6*4L","PRODUCT"),
                        entity(12548L,"美孚黑霸王CF-4 20W-50 4L 6x4L","PRODUCT")));

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("美孚 CF 4L 库存",null,Collections.emptyList()));

        assertEquals("CHOICE_REQUIRED",clarification.getResponse().get("status"));
        verify(entities).resolveType(AssistantEntityResolver.EntityType.PRODUCT,"美孚 CF 4L");
        verify(model,never()).parse(anyString(),any(),anyList());verify(queries,never()).execute(any());
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
    void highFrequencyBareProductAliasClarifiesProductCandidatesInsteadOfPurpose() {
        properties.getEntitySearch().setEnabled(true);when(entities.enabled()).thenReturn(true);
        Map<String,Object> a=new LinkedHashMap<>();a.put("id",1L);a.put("name","美孚黑霸王长效X40");a.put("entityType","PRODUCT");
        Map<String,Object> b=new LinkedHashMap<>();b.put("id",2L);b.put("name","美孚黑霸王傲超X40");b.put("entityType","PRODUCT");
        when(entities.resolveAcrossTypes("X40")).thenReturn(Arrays.asList(a,b));

        AssistantOrchestrationClarification clarification=assertThrows(AssistantOrchestrationClarification.class,
                ()->orchestrator.execute("X40",null,Collections.emptyList()));

        assertEquals("匹配到多个有权限的业务对象，请选择",clarification.getResponse().get("clarification"));
        verify(entities).resolveAcrossTypes("X40");
        verify(queries,never()).execute(any());
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
        Map<String,Object> proposal=new LinkedHashMap<>();proposal.put("sql","SELECT COUNT(q.id) document_count FROM purchase_orders q WHERE q.order_time>=:p1");
        proposal.put("parameters",Collections.singletonMap("p1","2026-01-01 00:00:00"));
        when(model.semanticSql(anyString(),anyList(),isNull())).thenReturn(proposal);
        when(semantic.execute(anyString(),anyMap())).thenReturn(semanticResult());
        AssistantOrchestrator.Outcome outcome=orchestrator.execute("今年有多少个采购订单",null,Collections.emptyList());
        assertEquals(AssistantExecutionEnvelope.RouteType.TEXT_TO_SQL,outcome.getEnvelope().getRouteType());
        assertEquals("execute_semantic_query",outcome.getEnvelope().getToolName());
        verify(model).semanticSql(anyString(),anyList(),isNull());
        verify(semantic).execute(eq((String)proposal.get("sql")),eq(Collections.singletonMap("p1","2026-01-01 00:00:00")));
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
}
