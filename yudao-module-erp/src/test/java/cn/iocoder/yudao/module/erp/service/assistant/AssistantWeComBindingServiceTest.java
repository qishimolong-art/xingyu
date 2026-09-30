package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.erp.dal.dataobject.assistant.ErpAssistantWeComBindingDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.ErpAssistantWeComBindingMapper;
import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComBindingService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import cn.iocoder.yudao.module.system.service.wecom.WeComClientService;
import cn.iocoder.yudao.module.system.service.wecom.WeComUserIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantWeComBindingServiceTest {

    @Mock private WeComClientService weComClientService;
    @Mock private AdminUserApi adminUserApi;
    @Mock private ErpAssistantWeComBindingMapper mapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> values;
    @Mock private ApplicationEventPublisher publisher;

    private AssistantWeComBindingService service;
    private WeComProperties properties;

    @BeforeEach
    void setUp() {
        properties = new WeComProperties();
        properties.setCorpId("ww-corp");
        WeComProperties.AiBotProperties bot = properties.getAiBot();
        bot.setEnabled(true); bot.setTenantId(1L); bot.setBotId("bot-1");
        bot.setBindPageUrl("http://erp.example.com/wecom-bot-bind.html");
        bot.setLoginClientKey("");
        service = new AssistantWeComBindingService();
        ReflectionTestUtils.setField(service, "properties", properties);
        ReflectionTestUtils.setField(service, "weComClientService", weComClientService);
        ReflectionTestUtils.setField(service, "adminUserApi", adminUserApi);
        ReflectionTestUtils.setField(service, "bindingMapper", mapper);
        ReflectionTestUtils.setField(service, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(service, "eventPublisher", publisher);
    }

    @Test
    void authorizeUsesDefaultWeComApplicationWithoutClientKey() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("erp:assistant:wecom:bind-ticket:t1")).thenReturn(ticket("wecom-a", "查库存"));
        when(weComClientService.getAuthorizeUrl(anyString(), anyString(), isNull())).thenReturn("oauth-url");

        String url = service.getAuthorizeUrl("t1");

        assertEquals("oauth-url", url);
        verify(weComClientService).getAuthorizeUrl(
                contains("wecom-bot-bind.html?ticket=t1&tenantId=1"), anyString(), isNull());
        verify(values).set(startsWith("erp:assistant:wecom:bind-state:"), anyString(), eq(300_000L), any());
    }

    @Test
    void authorizeNormalizesLegacyHashRouteToPublicBridgePage() {
        properties.getAiBot().setBindPageUrl("http://erp.example.com/#/auth/wecom-bot-bind");
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get("erp:assistant:wecom:bind-ticket:t1")).thenReturn(ticket("wecom-a", "查库存"));
        when(weComClientService.getAuthorizeUrl(anyString(), anyString(), isNull())).thenReturn("oauth-url");

        service.getAuthorizeUrl("t1");

        ArgumentCaptor<String> redirectUri = ArgumentCaptor.forClass(String.class);
        verify(weComClientService).getAuthorizeUrl(redirectUri.capture(), anyString(), isNull());
        assertEquals("http://erp.example.com/wecom-bot-bind.html?ticket=t1&tenantId=1", redirectUri.getValue());
        assertFalse(redirectUri.getValue().contains("#"));
    }

    @Test
    void bindCreatesUniqueEnabledUserBindingAndConsumesState() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        AdminUserRespDTO user = user(7L, CommonStatusEnum.ENABLE.getStatus());
        when(adminUserApi.getUserListByMobile("13800138000")).thenReturn(Collections.singletonList(user));

        AssistantWeComBindingService.BindResult result = service.bind("code", "state");

        assertEquals(7L, result.getUserId());
        assertFalse(result.isExisting());
        ArgumentCaptor<ErpAssistantWeComBindingDO> captor = ArgumentCaptor.forClass(ErpAssistantWeComBindingDO.class);
        verify(mapper).insert(captor.capture());
        assertEquals("wecom-a", captor.getValue().getWecomUserId());
        assertEquals("ww-corp", captor.getValue().getCorpId());
        verify(redisTemplate).execute(any(RedisScript.class), eq(Collections.singletonList("erp:assistant:wecom:bind-state:state")));
        verify(redisTemplate).execute(any(RedisScript.class), eq(Collections.singletonList("erp:assistant:wecom:bind-ticket:ticket")));
        verify(publisher).publishEvent(any(Object.class));
    }

    @Test
    void bindRejectsOAuthUserMismatch() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-b", "13800138000"));

        AssistantFailure failure = assertThrows(AssistantFailure.class, () -> service.bind("code", "state"));

        assertEquals("IDENTITY_MISMATCH", failure.getCode());
        verifyNoInteractions(adminUserApi, mapper, publisher);
    }

    @Test
    void bindRejectsWeComAccountAlreadyBoundToAnotherErpUser() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000"))
                .thenReturn(Collections.singletonList(user(7L, CommonStatusEnum.ENABLE.getStatus())));
        when(mapper.selectByBotAndWeComUser("bot-1", "wecom-a"))
                .thenReturn(new ErpAssistantWeComBindingDO().setUserId(8L));

        AssistantFailure failure = assertThrows(AssistantFailure.class, () -> service.bind("code", "state"));

        assertEquals("BINDING_CONFLICT", failure.getCode());
        verify(mapper, never()).insert(any(ErpAssistantWeComBindingDO.class));
    }

    @Test
    void bindAllowsSameErpUserToMoveToNewWeComIdentityAfterUniqueMobileMatch() {
        mockStateAndTicket("wecom-new", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-new", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000"))
                .thenReturn(Collections.singletonList(user(7L, CommonStatusEnum.ENABLE.getStatus())));
        ErpAssistantWeComBindingDO existing = new ErpAssistantWeComBindingDO()
                .setId(9L).setUserId(7L).setWecomUserId("wecom-old");
        when(mapper.selectByBotAndUser("bot-1", 7L)).thenReturn(existing);

        AssistantWeComBindingService.BindResult result = service.bind("code", "state");

        assertTrue(result.isExisting());
        assertEquals("wecom-new", existing.getWecomUserId());
        verify(mapper).updateById(existing);
    }

    @Test
    void bindRejectsMobileWithoutErpUser() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000")).thenReturn(Collections.emptyList());

        AssistantFailure failure = assertThrows(AssistantFailure.class, () -> service.bind("code", "state"));

        assertEquals("USER_NOT_FOUND", failure.getCode());
    }

    @Test
    void bindRejectsMobileWithOnlyDisabledErpUser() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000"))
                .thenReturn(Collections.singletonList(user(7L, CommonStatusEnum.DISABLE.getStatus())));

        AssistantFailure failure = assertThrows(AssistantFailure.class, () -> service.bind("code", "state"));

        assertEquals("USER_DISABLED", failure.getCode());
    }

    @Test
    void bindRejectsMultipleEnabledUsersWithSameMobile() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000")).thenReturn(Arrays.asList(
                user(7L, CommonStatusEnum.ENABLE.getStatus()), user(8L, CommonStatusEnum.ENABLE.getStatus())));

        AssistantFailure failure = assertThrows(AssistantFailure.class, () -> service.bind("code", "state"));

        assertEquals("USER_DUPLICATE", failure.getCode());
    }

    @Test
    void bindUsesSoleEnabledUserWhenSameMobileAlsoHasDisabledHistory() {
        mockStateAndTicket("wecom-a", "查库存");
        when(weComClientService.getUserIdentityByCode("code", null))
                .thenReturn(new WeComUserIdentity("wecom-a", "13800138000"));
        when(adminUserApi.getUserListByMobile("13800138000")).thenReturn(Arrays.asList(
                user(6L, CommonStatusEnum.DISABLE.getStatus()), user(7L, CommonStatusEnum.ENABLE.getStatus())));

        AssistantWeComBindingService.BindResult result = service.bind("code", "state");

        assertEquals(7L, result.getUserId());
        verify(mapper).insert(any(ErpAssistantWeComBindingDO.class));
    }

    private void mockStateAndTicket(String wecomUserId, String question) {
        when(redisTemplate.execute(any(RedisScript.class), anyList()))
                .thenReturn("{\"ticket\":\"ticket\",\"tenantId\":1}", ticket(wecomUserId, question));
    }

    private static String ticket(String wecomUserId, String question) {
        return "{\"tenantId\":1,\"botId\":\"bot-1\",\"wecomUserId\":\"" + wecomUserId
                + "\",\"question\":\"" + question + "\"}";
    }

    private static AdminUserRespDTO user(Long id, Integer status) {
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(id); user.setNickname("测试用户"); user.setStatus(status); user.setMobile("13800138000");
        return user;
    }
}
