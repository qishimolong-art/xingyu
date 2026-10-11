package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssistantMixedQueryTest {

    @Test
    void carriesProductSubjectIntoRealtimeStockQuestion() {
        AssistantMixedQuery.Split split = AssistantMixedQuery.split("介绍美孚黑霸王并查当前库存");

        assertNotNull(split);
        assertEquals("介绍美孚黑霸王", split.getKnowledgeQuestion());
        assertEquals("查询美孚黑霸王当前库存", split.getDataQuestion());
        assertEquals("美孚黑霸王", split.getSharedSubject());
    }

    @Test
    void doesNotCarryCompanySubjectIntoSalesMetricQuestion() {
        AssistantMixedQuery.Split split = AssistantMixedQuery.split("兴宇公司主营什么，同时查本月销售额");

        assertNotNull(split);
        assertEquals("兴宇公司主营什么", split.getKnowledgeQuestion());
        assertEquals("查本月销售额", split.getDataQuestion());
        assertNull(split.getSharedSubject());
    }

    @Test
    void ordinaryKnowledgeOrDataQuestionIsNotMixed() {
        assertFalse(AssistantMixedQuery.matches("介绍美孚黑霸王"));
        assertFalse(AssistantMixedQuery.matches("查美孚黑霸王当前库存"));
    }

    @Test
    void separatesKnowledgeMethodFromContextDependentDataExplanation() {
        AssistantMixedQuery.Split split = AssistantMixedQuery.split(
                "结合公司销售话术解释为什么本月这个产品销量下降");

        assertNotNull(split);
        assertEquals("请说明公司销售话术", split.getKnowledgeQuestion());
        assertEquals("为什么本月这个产品销量下降", split.getDataQuestion());
    }

    @Test
    void separatesCompanyChannelKnowledgeFromRealtimeSalesMetric() {
        AssistantMixedQuery.Split split = AssistantMixedQuery.split("介绍公司的渠道模式并查询本月销售额");

        assertNotNull(split);
        assertEquals("介绍公司的渠道模式", split.getKnowledgeQuestion());
        assertEquals("查询本月销售额", split.getDataQuestion());
        assertNull(split.getSharedSubject());
    }
}
