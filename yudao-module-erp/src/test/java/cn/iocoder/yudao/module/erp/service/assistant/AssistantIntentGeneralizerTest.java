package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class AssistantIntentGeneralizerTest {

    @Test
    void stockVarietyCountIsIndependentOfWordOrderAndColloquialWording() {
        for (String question : Arrays.asList(
                "有库存的配件有多少种",
                "多少种配件现在有库存",
                "现在库存里一共有几个产品",
                "库存产品总共有多少个",
                "有货的 SKU 数量是多少",
                "配件有库存的品种数")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Domain.INVENTORY, decision.getDomain(), question);
            assertEquals(AssistantIntentGeneralizer.Operation.COUNT_DISTINCT, decision.getOperation(), question);
            assertEquals("query_verified_metric", decision.getCapabilityId(), question);
            assertEquals("STOCK_SKU", decision.getMetric(), question);
            assertEquals("COUNT_DISTINCT", decision.getAggregation(), question);
            assertEquals("NONE", decision.getEntityRequirement(), question);
            assertTrue(decision.getConfidence() >= 0.85D, question);
        }
    }

    @Test
    void inventoryListAndRankingStayDistinctFromVarietyCount() {
        for (String question : Arrays.asList("哪些配件有库存", "列出有货产品", "有库存的商品清单")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Operation.LIST, decision.getOperation(), question);
            assertEquals("query_inventory_details", decision.getCapabilityId(), question);
            assertEquals("STOCK", decision.getMetric(), question);
        }
        for (String question : Arrays.asList("库存最多的配件", "产品库存排名前10")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Operation.RANK, decision.getOperation(), question);
            assertEquals("query_verified_metric", decision.getCapabilityId(), question);
            assertEquals("STOCK", decision.getMetric(), question);
        }
    }

    @Test
    void companyFactsRouteToKnowledgeButRealtimeBusinessDoesNot() {
        for (String question : Arrays.asList(
                "公司成立了多久",
                "兴宇什么时候创立的",
                "介绍一下公司的发展历程",
                "兴宇主营业务是什么")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Domain.COMPANY_KNOWLEDGE, decision.getDomain(), question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME, decision.getCapabilityId(), question);
        }
        AssistantIntentGeneralizer.Decision realtime=AssistantIntentGeneralizer.analyze("公司本月销售额是多少");
        assertEquals(AssistantIntentGeneralizer.Domain.SALE,realtime.getDomain());
        assertEquals("query_verified_metric",realtime.getCapabilityId());
        assertFalse(AssistantIntentGeneralizer.analyze("黑霸王当前库存").resolved());
    }

    @Test
    void companyAdvantageChannelRegionAndRelationshipOverrideWrongEntityTools() {
        for (String question : Arrays.asList(
                "公司的核心优势是什么",
                "公司跟其他同行相比哪里做的更好",
                "我们和美孚是什么关系",
                "兴宇的业务现在覆盖了哪些区域",
                "公司是通过哪些渠道把产品卖出去的")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Domain.COMPANY_KNOWLEDGE, decision.getDomain(), question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME, decision.getCapabilityId(), question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME,
                    AssistantIntentGeneralizer.reconcileCapability(question, "query_product_stock", true), question);
        }

        for (String question : Arrays.asList("美孚产品当前库存", "美孚DOT4价格", "按渠道统计本月销售额")) {
            assertNotEquals(AssistantCompanyKnowledge.TOOL_NAME,
                    AssistantIntentGeneralizer.reconcileCapability(question, "query_product_stock", true), question);
        }
    }

    @Test
    void companyPartnerRelationshipsNormalizeSelfReferencesAndIgnoreWordOrder() {
        for (String question : Arrays.asList(
                "我们和美孚是什么关系",
                "咱们和美孚到底是什么合作关系",
                "我们公司怎么和美孚合作",
                "美孚和兴宇是什么关系",
                "兴宇是美孚的代理商还是经销商",
                "本公司与道达尔合作多久")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertEquals(AssistantIntentGeneralizer.Domain.COMPANY_KNOWLEDGE, decision.getDomain(), question);
            assertEquals(AssistantIntentGeneralizer.Operation.KNOWLEDGE_ANSWER, decision.getOperation(), question);
            assertEquals(AssistantCompanyKnowledge.TOOL_NAME, decision.getCapabilityId(), question);
            assertEquals("COMPANY_PARTNER_RELATIONSHIP", decision.getReason(), question);
            assertTrue(decision.getConfidence() >= 0.95D, question);
        }
        assertEquals("美孚", AssistantIntentGeneralizer.companyRelationshipCounterparty(
                "咱们和美孚到底是什么合作关系"));
        assertEquals("美孚", AssistantIntentGeneralizer.companyRelationshipCounterparty(
                "兴宇是美孚的代理商还是经销商"));
    }

    @Test
    void realtimeBrandQueriesRemainOutsideCompanyRelationshipKnowledge() {
        for (String question : Arrays.asList("美孚DOT4价格", "美孚黑霸王库存", "美孚9月份销量",
                "我们本月的美孚销量", "我们和美孚的采购订单")) {
            AssistantIntentGeneralizer.Decision decision = AssistantIntentGeneralizer.analyze(question);
            assertNotEquals("COMPANY_PARTNER_RELATIONSHIP", decision.getReason(), question);
        }
    }

    @Test
    void categorySemanticsCorrectWrongSingleEntitySelections() {
        assertEquals("query_verified_metric", AssistantIntentGeneralizer.reconcileCapability(
                "有库存的配件有多少种", "query_product_stock", true));
        assertEquals("query_inventory_details", AssistantIntentGeneralizer.reconcileCapability(
                "哪些配件有库存", "query_product_stock", true));
        assertEquals(AssistantCompanyKnowledge.TOOL_NAME, AssistantIntentGeneralizer.reconcileCapability(
                "公司成立了多久", "resolve_business_entity", true));
        assertEquals("query_product_stock", AssistantIntentGeneralizer.reconcileCapability(
                "黑霸王当前库存", "query_product_stock", true));
    }

    @Test
    void businessMetricsAndDocumentsUseMeaningInsteadOfFixedWordOrder() {
        assertMetric("本月卖出去的货一共多少钱",AssistantIntentGeneralizer.Domain.SALE,"SALE");
        assertMetric("最近成交了多少金额",AssistantIntentGeneralizer.Domain.SALE,"SALE");
        assertMetric("这月进货花了多少钱",AssistantIntentGeneralizer.Domain.PURCHASE,"PURCHASE");
        assertMetric("上周买进的货总量",AssistantIntentGeneralizer.Domain.PURCHASE,"PURCHASE");
        assertMetric("今天一共收回来多少钱",AssistantIntentGeneralizer.Domain.FINANCE,"RECEIPT");
        assertMetric("目前客户欠我们的金额",AssistantIntentGeneralizer.Domain.FINANCE,"RECEIVABLE");

        AssistantIntentGeneralizer.Decision saleOrder=AssistantIntentGeneralizer.analyze("未发完的销售订单有哪些");
        assertEquals(AssistantIntentGeneralizer.Operation.FULFILLMENT,saleOrder.getOperation());
        assertEquals("query_sale_fulfillment",saleOrder.getCapabilityId());
        AssistantIntentGeneralizer.Decision purchaseOrder=AssistantIntentGeneralizer.analyze("哪些采购订单还没到货");
        assertEquals(AssistantIntentGeneralizer.Operation.FULFILLMENT,purchaseOrder.getOperation());
        assertEquals("query_purchase_fulfillment",purchaseOrder.getCapabilityId());

        assertFalse(AssistantIntentGeneralizer.analyze("黑霸王销售价是多少").resolved());
        assertEquals(AssistantStage5BusinessQuery.SALE_TOOL,
                AssistantIntentGeneralizer.analyze("本月销售退货单").getCapabilityId());
        assertEquals(AssistantStage5BusinessQuery.PURCHASE_TOOL,
                AssistantIntentGeneralizer.analyze("本月采购入库单").getCapabilityId());
        assertEquals(AssistantStage5BusinessQuery.FINANCE_TOOL,
                AssistantIntentGeneralizer.analyze("本月付款单").getCapabilityId());
    }

    private static void assertMetric(String question,AssistantIntentGeneralizer.Domain domain,String metric) {
        AssistantIntentGeneralizer.Decision decision=AssistantIntentGeneralizer.analyze(question);
        assertEquals(domain,decision.getDomain(),question);
        assertEquals(AssistantIntentGeneralizer.Operation.METRIC,decision.getOperation(),question);
        assertEquals("query_verified_metric",decision.getCapabilityId(),question);
        assertEquals(metric,decision.getMetric(),question);
    }
}
