package cn.iocoder.yudao.module.erp.service.assistant;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssistantStage4KnowledgeGoldenTest {

    private final AssistantCompanyKnowledge knowledge = new AssistantCompanyKnowledge();

    @Test
    void allStage4KnowledgeAndMixedRoutingCasesRemainStable() throws Exception {
        for (Case value : cases()) {
            switch (value.expectation) {
                case "REVIEWED":
                    assertTrue(knowledge.retrieve(value.question).stream()
                            .anyMatch(item -> "REVIEWED".equals(item.get("verificationStatus"))), value.caseId);
                    break;
                case "UNVERIFIED":
                    assertTrue(knowledge.retrieve(value.question).stream()
                            .anyMatch(item -> "UNVERIFIED".equals(item.get("verificationStatus"))), value.caseId);
                    break;
                case "MIXED":
                    assertTrue(AssistantMixedQuery.matches(value.question), value.caseId);
                    assertNotNull(AssistantMixedQuery.split(value.question), value.caseId);
                    break;
                case "NOT_MIXED":
                    assertFalse(AssistantMixedQuery.matches(value.question), value.caseId);
                    break;
                case "NO_EVIDENCE":
                    assertTrue(knowledge.retrieve(value.question).isEmpty(), value.caseId);
                    break;
                default:
                    fail("未知阶段 4 期望: " + value.expectation);
            }
        }
    }

    @Test
    void everyPublishedReferenceCarriesTraceabilityAndDraftBodiesStayExcluded() {
        for (Map<String, Object> item : knowledge.retrieve("ILSAC GF-6A 澎湃 5W-40")) {
            assertNotNull(item.get("id"));
            assertNotNull(item.get("path"));
            assertNotNull(item.get("verificationStatus"));
            if ("REVIEWED".equals(item.get("verificationStatus"))) {
                assertEquals(64, String.valueOf(item.get("contentHash")).length());
            } else {
                assertFalse(String.valueOf(item.get("text")).contains("draft body"));
            }
        }
    }

    private static List<Case> cases() throws Exception {
        InputStream input = AssistantStage4KnowledgeGoldenTest.class.getClassLoader()
                .getResourceAsStream("assistant/assistant_stage4_knowledge_cases.psv");
        assertNotNull(input);
        List<Case> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] values = line.split("\\|", -1);
                assertEquals(3, values.length, line);
                result.add(new Case(values[0], values[1], values[2]));
            }
        }
        assertEquals(25, result.size());
        return result;
    }

    private static class Case {
        final String caseId;
        final String expectation;
        final String question;

        Case(String caseId, String expectation, String question) {
            this.caseId = caseId;
            this.expectation = expectation;
            this.question = question;
        }
    }
}
