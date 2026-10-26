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
        List<String> combined = AssistantSemanticKnowledge.productKeywordVariants("经典表现 530 4L");
        assertTrue(combined.contains("经典表现 530 4L"));
        assertTrue(combined.contains("经典表现 5W-30 4L"));
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("经典表现 530 4升")
                .contains("经典表现 5W-30 4升"));
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("225 55 18").contains("225/55R18"));
        assertTrue(AssistantSemanticKnowledge.productKeywordVariants("2255518").contains("225/55R18"));
    }

    @Test
    void productUnitVariantsRequireNumericUnitsAndCoverConfiguredAliases() {
        assertUnitAliases("4升", "4升", "4L");
        assertUnitAliases("4 L", "4 L", "4升", "4L");
        assertUnitAliases("500毫升", "500毫升", "500mL");
        assertUnitAliases("500ML", "500毫升", "500mL");
        assertUnitAliases("20千克", "20千克", "20公斤", "20kg");
        assertUnitAliases("20公斤", "20千克", "20公斤", "20kg");
        assertUnitAliases("20KG", "20千克", "20公斤", "20kg");
        assertUnitAliases("500克", "500克", "500g");
        assertUnitAliases("500G", "500克", "500g");
        assertUnitAliases("30度", "30摄氏度", "30度", "30℃", "30°C");
        assertUnitAliases("30摄氏度", "30摄氏度", "30度", "30℃", "30°C");
        assertUnitAliases("30℃", "30摄氏度", "30度", "30℃", "30°C");
        assertUnitAliases("30°C", "30摄氏度", "30度", "30℃", "30°C");
        assertUnitAliases("4.5升", "4.5升", "4.5L");
        assertUnitAliases("-30℃", "-30摄氏度", "-30度", "-30℃", "-30°C");
        assertUnitAliases("零下30度", "零下30摄氏度", "零下30度", "零下30℃", "零下30°C");
        assertUnitAliases("20公斤A款", "20kgA款");
    }

    @Test
    void productUnitVariantsDoNotRewriteOrdinaryKeywordsOrViscosity() {
        for (String keyword : Arrays.asList("abl", "型号L", "QTF60", "530", "5W-30", "4LUBE", "20kgA")) {
            assertEquals(Collections.singletonList(keyword), AssistantSemanticKnowledge.productUnitVariants(keyword));
        }
        assertEquals("经典表现 530 4升",
                AssistantSemanticKnowledge.compactProductUnitSpacing("经典表现 530 4 升"));
        assertEquals("abl", AssistantSemanticKnowledge.compactProductUnitSpacing("abl"));
        assertEquals(Arrays.asList("美孚", "530", "1升"),
                AssistantSemanticKnowledge.productNameQueryTokens("美孚/530 1 升"));
        assertEquals(Arrays.asList("k40", "18升"),
                AssistantSemanticKnowledge.productNameQueryTokens("k40　18 升"));
    }

    @Test
    void originalAndConvertedProductUnitsStayAtomicInOrderedPatterns() {
        assertEquals("18升",
                AssistantSemanticKnowledge.productUnitAwareOrderedPattern("18升", "18升"));
        assertEquals("18L",
                AssistantSemanticKnowledge.productUnitAwareOrderedPattern("18升", "18L"));
        assertEquals("20kg",
                AssistantSemanticKnowledge.productUnitAwareOrderedPattern("20千克", "20kg"));
        assertEquals("18L",
                AssistantSemanticKnowledge.productUnitAwareOrderedPattern("18L", "18L"));
        assertEquals("18升",
                AssistantSemanticKnowledge.productUnitAwareOrderedPattern("18L", "18升"));
        assertEquals(Arrays.asList("A", "18L", "款"),
                AssistantSemanticKnowledge.productUnitAwareOrderedFragments("A18升款", "A18L款"));
    }

    private static void assertUnitAliases(String keyword, String... expected) {
        List<String> variants = AssistantSemanticKnowledge.productUnitVariants(keyword);
        for (String value : expected) assertTrue(variants.contains(value), keyword + " missing " + value + ": " + variants);
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
