package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantWeComMessageServiceTest {

    private AssistantWeComMessageService service;
    private AssistantWeComWebSocketClient webSocketClient;
    private AssistantWeComBindingService bindingService;
    private ValueOperations<String, String> values;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new AssistantWeComMessageService();
        webSocketClient = mock(AssistantWeComWebSocketClient.class);
        bindingService = mock(AssistantWeComBindingService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(webSocketClient.replyStream(anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(JsonUtils.parseTree("{\"errcode\":0}")));

        WeComProperties properties = new WeComProperties();
        WeComProperties.AiBotProperties bot = new WeComProperties.AiBotProperties();
        bot.setBotId("bot-1");
        bot.setTenantId(1L);
        properties.setAiBot(bot);
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "webSocketClient", webSocketClient);
        ReflectionTestUtils.setField(service, "bindingService", bindingService);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
    }

    @Test
    void groupMessageIsDeduplicatedAndNeverRunsQuery() {
        when(values.setIfAbsent(eq("erp:assistant:wecom:msg:msg-1"), eq("1"), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true, false);
        AssistantWeComInboundEvent event = event("group", "text", "查库存");

        service.onInbound(event);
        service.onInbound(event);

        verify(webSocketClient, times(1)).replyStream(eq("req-1"), anyString(),
                eq("智能问数暂仅支持与机器人单聊使用"), eq(true));
    }

    @Test
    void nonTextMessageGetsFixedUnsupportedReply() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);

        service.onInbound(event("single", "image", ""));

        verify(webSocketClient).replyStream(eq("req-1"), anyString(),
                eq("智能问数首期仅支持单聊文本消息"), eq(true));
    }

    @Test
    void unboundUserGetsPublicBridgeLinkWithoutHashRoute() {
        when(values.setIfAbsent(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS))).thenReturn(true);
        when(bindingService.getBinding("wecom-1")).thenReturn(null);
        when(bindingService.createTicket("wecom-1", "查库存")).thenReturn("ticket-1");
        when(bindingService.getBindEntryUrl("ticket-1", 1L))
                .thenReturn("http://erp.example.com/wecom-bot-bind.html?ticket=ticket-1&tenantId=1");

        service.onInbound(event("single", "text", "查库存"));

        verify(webSocketClient).replyStream(eq("req-1"), anyString(), argThat(message ->
                message.contains("http://erp.example.com/wecom-bot-bind.html?ticket=ticket-1&tenantId=1")
                        && !message.contains("/#/auth/wecom-bot-bind")), eq(true));
    }

    private static AssistantWeComInboundEvent event(String chatType, String msgType, String content) {
        String json = "{\"headers\":{\"req_id\":\"req-1\"},\"body\":{" +
                "\"msgid\":\"msg-1\",\"aibotid\":\"bot-1\",\"chattype\":\"" + chatType + "\"," +
                "\"msgtype\":\"" + msgType + "\",\"from\":{\"userid\":\"wecom-1\"}," +
                "\"text\":{\"content\":\"" + content + "\"}}}";
        return new AssistantWeComInboundEvent(JsonUtils.parseTree(json));
    }
}
