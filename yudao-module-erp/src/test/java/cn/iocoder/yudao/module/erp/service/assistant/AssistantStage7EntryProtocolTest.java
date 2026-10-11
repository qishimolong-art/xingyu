package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComResultFormatter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AssistantStage7EntryProtocolTest {

    @Test
    void successfulResultPublishesStableTableSourceAndDetailMetadata() {
        AssistantExecutionEnvelope envelope = AssistantExecutionEnvelope.metric("本月销售额", new AssistantPlan());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");
        result.put("summary", Collections.singletonList(Collections.singletonMap("amount", 100)));
        result.put("rows", Collections.singletonList(Collections.singletonMap("no", "XS001")));
        result.put("columns", Collections.singletonList(column("no", "单据编号")));
        result.put("basis", Collections.singletonList("已审核销售单"));

        Map<String, Object> payload = AssistantEntryProtocol.result(result, envelope, "message-1");
        Map<?, ?> response = (Map<?, ?>) payload.get("response");
        Map<?, ?> table = (Map<?, ?>) response.get("table");
        Map<?, ?> source = (Map<?, ?>) response.get("source");
        Map<?, ?> detail = (Map<?, ?>) response.get("detail");

        assertEquals(2, response.get("schemaVersion"));
        assertEquals("RESULT", response.get("type"));
        assertEquals("SUCCESS", response.get("status"));
        assertEquals(true, table.get("visible"));
        assertEquals(1, table.get("summaryRows"));
        assertEquals(1, table.get("detailRows"));
        assertEquals(true, source.get("available"));
        assertEquals(true, detail.get("available"));
        assertEquals("message-1", detail.get("messageId"));
    }

    @Test
    void backendOwnsDetailAvailabilityInsteadOfLettingEntriesGuess() {
        AssistantExecutionEnvelope stage5 = new AssistantExecutionEnvelope();
        stage5.setToolName(AssistantStage5BusinessQuery.SALE_TOOL);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "SUCCESS");

        Map<String, Object> payload = AssistantEntryProtocol.result(result, stage5, "message-2");
        Map<?, ?> response = (Map<?, ?>) payload.get("response");
        Map<?, ?> detail = (Map<?, ?>) response.get("detail");

        assertEquals(false, detail.get("available"));
        assertEquals("DETAIL_NOT_REGISTERED", detail.get("reason"));
    }

    @Test
    void clarificationAndErrorUseTheSameTerminalProtocol() {
        Map<String, Object> clarification = AssistantClarification.missing("MISSING_METRIC", "请选择查询内容",
                Collections.singletonList("metric"), Arrays.asList("库存", "销量"));
        Map<String, Object> clarificationPayload = AssistantEntryProtocol.clarification(clarification);
        Map<String, Object> errorPayload = AssistantEntryProtocol.error("FORBIDDEN", "没有查询权限");

        assertEquals("CLARIFICATION", response(clarificationPayload).get("type"));
        assertEquals("请选择查询内容", response(clarificationPayload).get("message"));
        assertEquals(Arrays.asList("库存", "销量"), clarificationPayload.get("choices"));
        assertEquals("ERROR", response(errorPayload).get("type"));
        assertEquals("FORBIDDEN", response(errorPayload).get("status"));
        assertEquals("没有查询权限", response(errorPayload).get("message"));
    }

    @Test
    void weComKeepsCitationsAndTextClarificationOptions() {
        AssistantWeComResultFormatter formatter = new AssistantWeComResultFormatter();
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("citation", "S1"); source.put("title", "产品知识"); source.put("section", "黑霸王");
        source.put("verificationStatus", "UNVERIFIED");
        Map<String, Object> knowledge = new LinkedHashMap<>();
        knowledge.put("status", "UNVERIFIED_KNOWLEDGE");
        knowledge.put("toolName", AssistantCompanyKnowledge.TOOL_NAME);
        knowledge.put("answerText", "产品说明 [S1]");
        knowledge.put("sources", Collections.singletonList(source));
        knowledge.put("warnings", Collections.singletonList("部分内容待核实"));
        String markdown = formatter.result(AssistantEntryProtocol.result(knowledge, null, "m3"), null);

        Map<String, Object> selected = new LinkedHashMap<>();
        selected.put("entityType", "PRODUCT"); selected.put("field", "resolved:PRODUCT"); selected.put("id", 8L);
        Map<String, Object> clarification = AssistantClarification.intentChoice("请选择查询内容", selected,
                Arrays.asList("库存", "销量"));
        AssistantWeComResultFormatter.ChoiceResult choices = formatter.clarification(clarification, "黑霸王");
        JsonNode saved = JsonUtils.parseTree(choices.toJson());

        assertTrue(markdown.contains("[S1]《产品知识》·黑霸王（待核实）"));
        assertTrue(markdown.contains("部分内容待核实"));
        assertTrue(choices.getContent().contains("1. 库存"));
        assertTrue(choices.getContent().contains("2. 销量"));
        assertEquals("resolved:PRODUCT", saved.path("options").get(0).path("field").asText());
        assertEquals(8L, saved.path("options").get(0).path("id").asLong());
        assertEquals("这个产品；补充：库存", saved.path("options").get(0).path("question").asText());
    }

    private static Map<String, Object> response(Map<String, Object> value) {
        return (Map<String, Object>) value.get("response");
    }

    private static Map<String, Object> column(String key, String title) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", key); result.put("title", title); return result;
    }
}
