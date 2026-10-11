package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssistantSemanticAnalyzerTest {

    private final AssistantSemanticAnalyzer analyzer = new AssistantSemanticAnalyzer();

    @Test
    void normalizesDomainOperationAndAggregationBeforeExecution() {
        AssistantSemanticRequest request = request("query_verified_metric", "有库存的配件有多少种");

        analyzer.analyze(request, null);

        assertEquals(4, request.getSchemaVersion());
        assertEquals("INVENTORY", request.getDomain());
        assertEquals("COUNT_DISTINCT", request.getOperation());
        assertEquals("COUNT_DISTINCT", request.getAggregation());
        assertEquals("NONE", request.getEntityRequirement());
        assertEquals("STOCK_SKU", request.getMetric());
        assertTrue(request.getEntityMentions().isEmpty());
        assertTrue(request.getRoutingConfidence() >= 0.85D);
    }

    @Test
    void normalizesCompanyPartnerRelationshipBeforeCapabilityExecution() {
        AssistantSemanticRequest request=request(AssistantCompanyKnowledge.TOOL_NAME,
                "咱们和美孚到底是什么合作关系");

        analyzer.analyze(request,null);

        assertEquals("COMPANY_KNOWLEDGE",request.getDomain());
        assertEquals("KNOWLEDGE_ANSWER",request.getOperation());
        assertEquals("COMPANY_PARTNER_RELATIONSHIP",request.getRoutingReason());
        assertEquals("NONE",request.getEntityRequirement());
        assertTrue(request.getRoutingConfidence()>=0.95D);
    }

    @Test
    void recordsUnifiedCompanyKnowledgeFrameWithoutBusinessEntities() {
        AssistantSemanticRequest request = request(AssistantCompanyKnowledge.TOOL_NAME,
                "兴宇的业务现在覆盖了哪些区域");

        analyzer.analyze(request, null);

        assertEquals("COMPANY_KNOWLEDGE", request.getDomain());
        assertEquals("REGION", request.getKnowledgeIntent().get("topic"));
        assertEquals("SCOPE", request.getKnowledgeIntent().get("operation"));
        assertEquals("LATEST_REVIEWED", request.getKnowledgeIntent().get("freshness"));
        assertEquals(Boolean.FALSE, request.getKnowledgeIntent().get("explicitRealtime"));
        assertTrue(request.getEntityMentions().isEmpty());
    }

    @Test
    void extractsIntentObjectMetricDimensionTimeStatusAndSortIntoOneRequest() {
        AssistantSemanticRequest request = request("query_sale_documents",
                "查询2026年9月已审核销售订单，按客户排名前5");

        analyzer.analyze(request, null);

        assertEquals("DOCUMENT_QUERY", request.getIntentId());
        assertEquals("SALE", request.getMetric());
        assertTrue(request.getBusinessObjects().contains("CUSTOMER"));
        assertTrue(request.getDimensions().contains("CUSTOMER"));
        assertEquals("CUSTOM", request.getTimeRange().getPeriod());
        assertEquals("2026-09-01", request.getTimeRange().getStartDate());
        assertEquals("2026-09-30", request.getTimeRange().getEndDate());
        assertTrue(request.getDocumentStatuses().contains("APPROVED"));
        assertEquals("DESC", request.getSort().getDirection());
        assertEquals(5, request.getSort().getLimit());
        assertEquals("NEW_TOPIC", request.getContext().getMode());
    }

    @Test
    void periodOnlyFollowUpExplicitlyInheritsMetricAndEntityButReplacesTime() {
        AssistantExecutionEnvelope previous = previous("CUSTOMER", AssistantPlan.Metric.SALE, "THIS_MONTH");
        AssistantSemanticRequest request = request("query_verified_metric", "上月呢");

        analyzer.analyze(request, previous);

        assertEquals("INHERIT", request.getContext().getMode());
        assertEquals("SALE", request.getMetric());
        assertEquals("LAST_MONTH", request.getTimeRange().getPeriod());
        assertEquals("EXPLICIT", request.getTimeRange().getSource());
        assertEquals(java.util.Collections.singletonList("CUSTOMER"), request.getInheritedContext().get("entityTypes"));
        assertTrue(request.getContext().getInheritedFields().stream()
                .anyMatch(field -> "metric".equals(field.getField()) && "PREVIOUS_PLAN".equals(field.getSource())));
        assertFalse(request.getContext().getInheritedFields().stream()
                .anyMatch(field -> "timeRange".equals(field.getField())));
    }

    @Test
    void rollingPeriodIsServerNormalizedBeforeV2Execution() {
        AssistantSemanticRequest request=request("query_verified_metric","最近7天的销售额");

        analyzer.analyze(request,null);

        java.time.LocalDate today=java.time.LocalDate.now(AssistantRelativePeriod.BUSINESS_ZONE);
        assertEquals("CUSTOM",request.getTimeRange().getPeriod());
        assertEquals(today.minusDays(6).toString(),request.getTimeRange().getStartDate());
        assertEquals(today.toString(),request.getTimeRange().getEndDate());
        assertEquals("EXPLICIT",request.getTimeRange().getSource());
    }

    @Test
    void singleDayFollowupUsesTheSameServerDateAsV1AndAuditsItsSource() {
        AssistantExecutionEnvelope previous=previous("CUSTOMER",AssistantPlan.Metric.SALE,"CUSTOM");
        previous.getLegacyPlan().setStartDate("2026-09-28");previous.getLegacyPlan().setEndDate("2026-09-28");
        AssistantSemanticRequest request=request("query_verified_metric","29号的销售情况");

        analyzer.analyze(request,previous);

        assertEquals("INHERIT",request.getContext().getMode());assertEquals("SALE",request.getMetric());
        assertEquals("CUSTOM",request.getTimeRange().getPeriod());
        assertEquals("2026-09-29",request.getTimeRange().getStartDate());
        assertEquals("2026-09-29",request.getTimeRange().getEndDate());
        assertEquals("PREVIOUS_YEAR_MONTH",request.getTimeRange().getSource());
        assertEquals(java.util.Collections.singletonList("CUSTOMER"),request.getInheritedContext().get("entityTypes"));
    }

    @Test
    void standaloneDayProducesMonthClarificationWithExistingButtonProtocol() {
        AssistantSemanticRequest request=request("query_verified_metric","29号的销售情况");

        analyzer.analyze(request,null);
        Map<String,Object> clarification=analyzer.contextClarification(request);

        assertEquals("MISSING_CONTEXT",request.getContext().getMode());
        assertEquals("NO_PREVIOUS_DATE",request.getContext().getReason());
        assertTrue(request.getMissingFields().contains("calendarMonth"));
        assertEquals("MISSING_CALENDAR_MONTH",clarification.get("reasonCode"));
        assertEquals(java.util.Arrays.asList("上个月","本月"),clarification.get("choices"));
    }

    @Test
    void dayMetricSwitchInheritsDateButNotEntitiesOrDimensions() {
        AssistantExecutionEnvelope previous=previous("CUSTOMER",AssistantPlan.Metric.SALE,"CUSTOM");
        previous.getLegacyPlan().setStartDate("2026-09-28");previous.getLegacyPlan().setEndDate("2026-09-28");
        previous.getLegacyPlan().setGroup(AssistantPlan.Group.PARTY);
        AssistantSemanticRequest request=request("query_verified_metric","29号的采购额");

        analyzer.analyze(request,previous);

        assertEquals("INHERIT",request.getContext().getMode());
        assertEquals("DATE_YEAR_MONTH_FOLLOW_UP",request.getContext().getReason());
        assertEquals("PURCHASE",request.getMetric());assertEquals("2026-09-29",request.getTimeRange().getStartDate());
        assertFalse(request.getInheritedContext().containsKey("entityTypes"));
        assertFalse(request.getBusinessObjects().contains("CUSTOMER"));assertTrue(request.getDimensions().isEmpty());
    }

    @Test
    void completeNewTopicDoesNotCarryCustomerIntoInventoryQuestion() {
        AssistantExecutionEnvelope previous = previous("CUSTOMER", AssistantPlan.Metric.SALE, "THIS_MONTH");
        AssistantSemanticRequest request = request("query_product_stock", "那当前库存呢");

        analyzer.analyze(request, previous);

        assertEquals("NEW_TOPIC", request.getContext().getMode());
        assertEquals("STOCK", request.getMetric());
        assertTrue(request.getInheritedContext().isEmpty());
        assertNull(analyzer.executionContext(request, previous));
    }

    @Test
    void prefixedRankingFollowUpInheritsMetricAndPeriod() {
        AssistantExecutionEnvelope previous = previous(null, AssistantPlan.Metric.SALE, "THIS_MONTH");
        AssistantSemanticRequest request = request("query_verified_metric", "那按客户排名前5名呢");

        analyzer.analyze(request, previous);

        assertEquals("INHERIT", request.getContext().getMode());
        assertEquals("SALE", request.getMetric());
        assertEquals("THIS_MONTH", request.getTimeRange().getPeriod());
        assertEquals("INHERITED", request.getTimeRange().getSource());
        assertTrue(request.getBusinessObjects().contains("CUSTOMER"));
        assertTrue(request.getDimensions().contains("CUSTOMER"));
        assertEquals(5, request.getSort().getLimit());
    }

    @Test
    void typedReferenceCannotReuseAnEntityOfAnotherType() {
        AssistantExecutionEnvelope previous = previous("CUSTOMER", AssistantPlan.Metric.SALE, "THIS_MONTH");
        AssistantSemanticRequest request = request("query_verified_metric", "这个供应商本月采购是多少");

        analyzer.analyze(request, previous);

        assertEquals("MISSING_CONTEXT", request.getContext().getMode());
        assertEquals("ENTITY_REFERENCE_TYPE_MISMATCH", request.getContext().getReason());
        assertTrue(request.getMissingFields().contains("entity"));
        assertTrue(analyzer.needsContextClarification(request));
    }

    @Test
    void ellipticalFollowUpWithoutPreviousTurnReturnsStructuredMissingContext() {
        AssistantSemanticRequest request = request("query_verified_metric", "销量呢");

        analyzer.analyze(request, null);
        Map<String, Object> clarification = analyzer.contextClarification(request);

        assertEquals("MISSING_CONTEXT", request.getContext().getMode());
        assertEquals("CLARIFY", clarification.get("status"));
        assertEquals("MISSING_CONTEXT", clarification.get("reasonCode"));
        assertTrue(((java.util.List<?>) clarification.get("missingFields")).contains("context"));
        assertTrue(clarification.containsKey("choices"));
        assertTrue(clarification.containsKey("candidates"));
    }

    @Test
    void organizationKnowledgeFollowUpInheritsKnowledgeTopicWithoutBusinessEntity() {
        AssistantExecutionEnvelope previous = knowledgePrevious("公司的组织架构");
        AssistantSemanticRequest request = request(AssistantCompanyKnowledge.TOOL_NAME, "具体的人员组成");

        analyzer.analyze(request, previous);

        assertEquals("INHERIT", request.getContext().getMode());
        assertEquals("KNOWLEDGE_FOLLOW_UP", request.getContext().getReason());
        assertEquals("previous-knowledge", request.getContext().getSourceRequestId());
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME, request.getInheritedContext().get("capabilityId"));
        assertEquals("KNOWLEDGE", request.getInheritedContext().get("routeType"));
        assertTrue(request.getContext().getInheritedFields().stream()
                .anyMatch(field -> "knowledgeTopic".equals(field.getField())
                        && "PREVIOUS_KNOWLEDGE".equals(field.getSource())));
        assertTrue(request.getEntityMentions().isEmpty());
    }

    @Test
    void liveOrganizationQuestionStartsNewTopicAfterKnowledgeAnswer() {
        AssistantSemanticRequest request = request(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                "列出当前启用部门");

        analyzer.analyze(request, knowledgePrevious("公司的组织架构"));

        assertEquals("NEW_TOPIC", request.getContext().getMode());
        assertTrue(request.getInheritedContext().isEmpty());
    }

    @Test
    void completeCalendarQuestionsNeverRequirePreviousEntityContext() {
        Map<String,String> cases=new LinkedHashMap<>();
        cases.put("这个星期是否有入库记录","THIS_WEEK");
        cases.put("这一星期有没有入库","THIS_WEEK");
        cases.put("本星期有入库吗","THIS_WEEK");
        cases.put("这一个月是否有入库记录","THIS_MONTH");
        for(Map.Entry<String,String> item:cases.entrySet()) {
            AssistantSemanticRequest withoutPrevious=request("query_stock_movements",item.getKey());
            analyzer.analyze(withoutPrevious,null);
            assertEquals("NEW_TOPIC",withoutPrevious.getContext().getMode(),item.getKey());
            assertEquals("COMPLETE_CURRENT_QUESTION",withoutPrevious.getContext().getReason(),item.getKey());
            assertFalse(analyzer.needsContextClarification(withoutPrevious),item.getKey());

            AssistantSemanticRequest request=request("query_stock_movements",item.getKey());
            analyzer.analyze(request,previous("CUSTOMER",AssistantPlan.Metric.SALE,"THIS_MONTH"));
            assertEquals("NEW_TOPIC",request.getContext().getMode(),item.getKey());
            assertEquals("COMPLETE_CURRENT_QUESTION",request.getContext().getReason(),item.getKey());
            assertEquals(item.getValue(),request.getTimeRange().getPeriod(),item.getKey());
            assertFalse(analyzer.needsContextClarification(request),item.getKey());
        }
    }

    @Test
    void concreteProductPronounStillRequiresEntityContext() {
        AssistantSemanticRequest request=request("query_stock_movements","这个产品本周有入库记录吗");
        analyzer.analyze(request,null);
        assertEquals("MISSING_CONTEXT",request.getContext().getMode());
        assertEquals("NO_PREVIOUS_TURN",request.getContext().getReason());
        assertTrue(request.getMissingFields().contains("entity"));
    }

    private static AssistantSemanticRequest request(String capability, String question) {
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setCapabilityId(capability);
        request.setQuestion(question);
        return request;
    }

    private static AssistantExecutionEnvelope previous(String type, AssistantPlan.Metric metric, String period) {
        AssistantPlan plan = new AssistantPlan();
        plan.setMetric(metric);
        plan.setPeriod(period);
        AssistantExecutionEnvelope envelope = AssistantExecutionEnvelope.metric("上一轮", plan);
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setRequestId("previous-request");
        envelope.setSemanticRequest(request);
        Map<String, Object> entity = new LinkedHashMap<>();
        if (type != null) {
            entity.put("id", 10L);
            entity.put("name", "上一轮对象");
            entity.put("entityType", type);
            envelope.getArguments().put("resolvedEntity", entity);
            Map<String, Object> all = new LinkedHashMap<>();
            all.put(type, entity);
            envelope.getArguments().put("resolvedEntities", all);
        }
        return envelope;
    }

    private static AssistantExecutionEnvelope knowledgePrevious(String question) {
        AssistantExecutionEnvelope envelope = new AssistantExecutionEnvelope();
        envelope.setQuestion(question);
        envelope.setRouteType(AssistantExecutionEnvelope.RouteType.KNOWLEDGE);
        envelope.setToolName(AssistantCompanyKnowledge.TOOL_NAME);
        AssistantSemanticRequest request = new AssistantSemanticRequest();
        request.setRequestId("previous-knowledge");
        envelope.setSemanticRequest(request);
        return envelope;
    }
}
