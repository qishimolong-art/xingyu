package cn.iocoder.yudao.module.erp.service.assistant;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AssistantWecomFaqFixtureTest {
    @Test
    @SuppressWarnings("unchecked")
    void fixtureKeepsAllHighFrequencyQuestionsWithoutHistoricalAnswers() throws Exception {
        AssistantSemanticKnowledge semantic=new AssistantSemanticKnowledge();
        try(InputStream input=getClass().getResourceAsStream("/assistant/wecom_faq_route_fixture_20260928.json")) {
            assertNotNull(input);
            Map<String,Object> root=new ObjectMapper().readValue(input,Map.class);
            List<Map<String,Object>> rows=(List<Map<String,Object>>)root.get("rows");
            assertEquals(171,rows.size());
            assertEquals("每日保护",rows.get(0).get("question"));
            Set<String> routes=rows.stream().map(row->String.valueOf(row.get("expectedRoute"))).collect(Collectors.toSet());
            assertTrue(routes.contains("PRODUCT_CANDIDATE_CLARIFICATION"));
            assertTrue(routes.contains("PRODUCT_PRICE_STOCK"));
            assertTrue(routes.contains("SALE_VERIFIED_METRIC"));
            assertTrue(routes.contains("RECEIVABLE_VERIFIED_METRIC"));
            for(Map<String,Object> row:rows) {
                assertTrue(row.containsKey("question"));
                assertFalse(row.containsKey("代表回答"));
                assertFalse(row.containsKey("最近回答"));
                AssistantSemanticKnowledge.Match match=semantic.match(String.valueOf(row.get("question")));
                assertEquals(row.get("expectedIntent"),match.getIntentId(),String.valueOf(row.get("question")));
                assertEquals(row.get("expectedCapability"),match.getCapabilityId(),String.valueOf(row.get("question")));
                Collection<String> expected=(Collection<String>)row.get("expectedEntityTypes");
                assertTrue(match.getEntityTypes().containsAll(expected),String.valueOf(row.get("question"))+" => "+match.getEntityTypes());
                assertTrue(Arrays.asList("CHOICE_REQUIRED","ENTITY_NOT_FOUND","RESOLVED_OR_NO_DATA").contains(row.get("expectedOutcomeClass")));
            }
        }
    }

    @Test
    void productSpecificationVariantsAreDeterministic() {
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("每日保护全合成530").contains("每日保护全合成5W-30"));
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("225 55 18").contains("225/55R18"));
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("2255518").contains("225/55R18"));
    }

    @Test
    void riskyCapabilitiesAreRecognizedButNotOpened() {
        AssistantSemanticKnowledge semantic=new AssistantSemanticKnowledge();
        for(String question:Arrays.asList("客户欠款账龄","跨账套欠款","哪个产品毛利最高")) {
            AssistantSemanticKnowledge.Match match=semantic.match(question);
            assertEquals("CAPABILITY_UNVERIFIED",match.getIntentId());
            assertTrue(match.unverified());
        }
    }

    @Test
    void commonPeriodMetricsUseTheirPublishedSemanticCards() {
        AssistantSemanticKnowledge semantic=new AssistantSemanticKnowledge();
        assertEquals("SALE",semantic.match("本月销售额").getIntentId());
        assertEquals("PURCHASE",semantic.match("本月采购额").getIntentId());
        assertEquals("RECEIPT",semantic.match("本周收款").getIntentId());
        assertEquals("PAYMENT",semantic.match("去年付款").getIntentId());
        assertTrue(semantic.openedIntents().containsAll(Arrays.asList("SALE","PURCHASE","RECEIPT","PAYMENT")));
    }
}
