package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AssistantWeComWebSocketClientTest {

    @Test
    void buildsOfficialSubscribeFrame() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bot_id", "bot-id");
        body.put("secret", "sensitive-secret");

        String payload = ReflectionTestUtils.invokeMethod(AssistantWeComWebSocketClient.class,
                "frame", "aibot_subscribe", "req-1", body);
        JsonNode frame = JsonUtils.parseTree(payload);

        assertEquals("aibot_subscribe", frame.path("cmd").asText());
        assertEquals("req-1", frame.path("headers").path("req_id").asText());
        assertEquals("bot-id", frame.path("body").path("bot_id").asText());
        assertEquals("sensitive-secret", frame.path("body").path("secret").asText());
        assertFalse(payload.contains("password"));
    }

    @Test
    void reconnectBackoffStartsAtOneAndCapsAtThirtySeconds() {
        assertEquals(1L, AssistantWeComWebSocketClient.reconnectDelaySeconds(1));
        assertEquals(2L, AssistantWeComWebSocketClient.reconnectDelaySeconds(2));
        assertEquals(4L, AssistantWeComWebSocketClient.reconnectDelaySeconds(3));
        assertEquals(30L, AssistantWeComWebSocketClient.reconnectDelaySeconds(6));
        assertEquals(30L, AssistantWeComWebSocketClient.reconnectDelaySeconds(20));
    }

    @Test
    void botSecretIsExcludedFromConfigurationString() {
        WeComProperties.AiBotProperties properties = new WeComProperties.AiBotProperties();
        properties.setBotId("bot-id");
        properties.setSecret("must-not-appear");

        assertFalse(properties.toString().contains("must-not-appear"));
    }
}
