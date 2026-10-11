package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssistantCompanyKnowledgeIntentTest {

    @Test
    void recognizesCompanyThemesByMeaningInsteadOfSingleKeywords() {
        Map<String, AssistantCompanyKnowledgeIntent.Topic> cases = new LinkedHashMap<>();
        cases.put("公司主要经营哪些业务", AssistantCompanyKnowledgeIntent.Topic.PROFILE);
        cases.put("公司成立多久了？发展历程是怎样的", AssistantCompanyKnowledgeIntent.Topic.HISTORY);
        cases.put("公司跟其他同行相比哪里做的更好", AssistantCompanyKnowledgeIntent.Topic.ADVANTAGE);
        cases.put("我们和美孚是什么关系", AssistantCompanyKnowledgeIntent.Topic.RELATIONSHIP);
        cases.put("公司目前代理或经营哪些品牌", AssistantCompanyKnowledgeIntent.Topic.BRAND);
        cases.put("公司的主要产品有哪些", AssistantCompanyKnowledgeIntent.Topic.PRODUCT);
        cases.put("公司是通过哪些渠道把产品卖出去的", AssistantCompanyKnowledgeIntent.Topic.CHANNEL);
        cases.put("兴宇的业务现在覆盖了哪些区域", AssistantCompanyKnowledgeIntent.Topic.REGION);
        cases.put("公司有哪些部门", AssistantCompanyKnowledgeIntent.Topic.ORGANIZATION);
        cases.put("财务部主要负责哪些工作", AssistantCompanyKnowledgeIntent.Topic.RESPONSIBILITY);
        cases.put("总经办有哪些人", AssistantCompanyKnowledgeIntent.Topic.PEOPLE);
        cases.put("公司主要服务哪些类型的客户", AssistantCompanyKnowledgeIntent.Topic.CUSTOMER);
        cases.put("新员工应该先了解哪些公司制度和业务知识", AssistantCompanyKnowledgeIntent.Topic.TRAINING);
        cases.put("向客户介绍公司时应该重点讲哪些内容", AssistantCompanyKnowledgeIntent.Topic.METHOD);
        cases.put("API SP 和 ILSAC GF-6A 有什么关系", AssistantCompanyKnowledgeIntent.Topic.PRODUCT);
        cases.put("客户觉得价格贵应该怎么沟通", AssistantCompanyKnowledgeIntent.Topic.METHOD);
        cases.put("新员工产品学习路径是什么", AssistantCompanyKnowledgeIntent.Topic.TRAINING);
        cases.put("市场部职责是什么", AssistantCompanyKnowledgeIntent.Topic.RESPONSIBILITY);
        cases.put("物流部负责什么", AssistantCompanyKnowledgeIntent.Topic.RESPONSIBILITY);
        cases.put("项目部主要做什么", AssistantCompanyKnowledgeIntent.Topic.RESPONSIBILITY);
        cases.put("总经办有哪些职责", AssistantCompanyKnowledgeIntent.Topic.RESPONSIBILITY);

        cases.forEach((question, topic) -> {
            AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
            assertTrue(analysis.isKnowledge(), question);
            assertFalse(analysis.isExplicitRealtime(), question);
            assertEquals(topic, analysis.getTopic(), question);
            assertTrue(analysis.getConfidence() >= 0.95D, question);
        });
    }

    @Test
    void currentCompanyFactsUseLatestReviewedSourcesButLiveOrganizationUsesErp() {
        for (String question : Arrays.asList("兴宇的业务现在覆盖了哪些区域", "公司目前通过哪些渠道销售产品")) {
            AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
            assertTrue(analysis.isKnowledge(), question);
            assertEquals(AssistantCompanyKnowledgeIntent.Freshness.LATEST_REVIEWED, analysis.getFreshness(), question);
        }
        for (String question : Arrays.asList("当前有哪些部门", "现在市场部负责人是谁")) {
            AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
            assertFalse(analysis.isKnowledge(), question);
            assertTrue(analysis.isExplicitRealtime(), question);
            assertEquals(AssistantCompanyKnowledgeIntent.Freshness.REALTIME_REQUIRED, analysis.getFreshness(), question);
        }
    }

    @Test
    void explicitMetricsDocumentsInventoryAndPricesRemainOutsideKnowledge() {
        for (String question : Arrays.asList(
                "本月销售额", "各地区销量", "按部门采购额", "客户欠款",
                "美孚产品当前库存", "美孚DOT4价格", "查询销售订单", "查看采购订单",
                "按渠道统计本月销售额")) {
            AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(question);
            assertFalse(analysis.isKnowledge(), question);
            assertTrue(analysis.isExplicitRealtime(), question);
        }
    }

    @Test
    void relationshipNormalizesCompanyAliasesAndCapturesCounterparty() {
        AssistantCompanyKnowledgeIntent.Analysis analysis = AssistantCompanyKnowledgeIntent.analyze(
                "咱们和美孚到底是什么合作关系");

        assertEquals("四川兴宇和美孚到底是什么合作关系", analysis.getNormalizedQuestion());
        assertEquals(Arrays.asList("美孚"), analysis.getEntities());
        assertEquals("COMPANY_PARTNER_RELATIONSHIP", analysis.getReason());
    }
}
