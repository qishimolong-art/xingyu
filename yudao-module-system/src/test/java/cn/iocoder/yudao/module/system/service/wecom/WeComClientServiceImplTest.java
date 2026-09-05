package cn.iocoder.yudao.module.system.service.wecom;

import cn.hutool.core.util.ReflectUtil;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.AUTH_WECOM_API_ERROR;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class WeComClientServiceImplTest extends BaseMockitoUnitTest {

    private static final String ACCESS_TOKEN_KEY = "wecom_access_token:ww123:1000002";

    private WeComClientServiceImpl weComClientService;
    private MockRestServiceServer mockServer;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setUp() {
        WeComProperties properties = new WeComProperties();
        properties.setEnabled(true);
        properties.setCorpId("ww123");
        properties.setAgentId("1000002");
        properties.setSecret("secret");
        properties.setAccessTokenAheadRefresh(Duration.ofSeconds(120));

        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        weComClientService = new WeComClientServiceImpl();
        ReflectUtil.setFieldValue(weComClientService, "properties", properties);
        ReflectUtil.setFieldValue(weComClientService, "restTemplate", restTemplate);
        ReflectUtil.setFieldValue(weComClientService, "stringRedisTemplate", stringRedisTemplate);
    }

    @Test
    public void testGetAuthorizeUrl_privateInfoScope() {
        String authorizeUrl = weComClientService.getAuthorizeUrl("https://example.com/auth/wecom-login", "state");

        assertTrue(authorizeUrl.contains("appid=ww123"));
        assertTrue(authorizeUrl.contains("agentid=1000002"));
        assertTrue(authorizeUrl.contains("scope=snsapi_privateinfo"));
        assertTrue(authorizeUrl.endsWith("#wechat_redirect"));
    }

    @Test
    public void testGetUserMobileByCode_accessTokenCacheMiss() {
        mockValueOperations();
        when(valueOperations.get(eq(ACCESS_TOKEN_KEY))).thenReturn(null);
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=ww123&corpsecret=secret"))
                .andRespond(withSuccess("{\"errcode\":0,\"access_token\":\"access-token\",\"expires_in\":7200}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=access-token&code=auth-code"))
                .andRespond(withSuccess("{\"errcode\":0,\"UserId\":\"zhangsan\",\"user_ticket\":\"ticket\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail?access_token=access-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"errcode\":0,\"userid\":\"zhangsan\",\"mobile\":\"13800138000\"}",
                        MediaType.APPLICATION_JSON));

        String mobile = weComClientService.getUserMobileByCode("auth-code");

        assertEquals("13800138000", mobile);
        verify(valueOperations).set(eq(ACCESS_TOKEN_KEY), eq("access-token"), eq(7080L), eq(TimeUnit.SECONDS));
        mockServer.verify();
    }

    @Test
    public void testGetUserMobileByCode_accessTokenCacheHit() {
        mockValueOperations();
        when(valueOperations.get(eq(ACCESS_TOKEN_KEY))).thenReturn("cached-token");
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=cached-token&code=auth-code"))
                .andRespond(withSuccess("{\"errcode\":0,\"UserId\":\"zhangsan\",\"user_ticket\":\"ticket\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail?access_token=cached-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"errcode\":0,\"userid\":\"zhangsan\",\"mobile\":\"13800138000\"}",
                        MediaType.APPLICATION_JSON));

        String mobile = weComClientService.getUserMobileByCode("auth-code");

        assertEquals("13800138000", mobile);
        verify(valueOperations, never()).set(eq(ACCESS_TOKEN_KEY), eq("cached-token"), eq(7080L), eq(TimeUnit.SECONDS));
        mockServer.verify();
    }

    @Test
    public void testGetUserMobileByCode_lowercaseUserId() {
        mockValueOperations();
        when(valueOperations.get(eq(ACCESS_TOKEN_KEY))).thenReturn("cached-token");
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=cached-token&code=auth-code"))
                .andRespond(withSuccess("{\"errcode\":0,\"userid\":\"zhangsan\",\"user_ticket\":\"ticket\"}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail?access_token=cached-token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"errcode\":0,\"userid\":\"zhangsan\",\"mobile\":\"13800138000\"}",
                        MediaType.APPLICATION_JSON));

        String mobile = weComClientService.getUserMobileByCode("auth-code");

        assertEquals("13800138000", mobile);
        mockServer.verify();
    }

    @Test
    public void testGetUserMobileByCode_userTicketEmpty() {
        mockValueOperations();
        when(valueOperations.get(eq(ACCESS_TOKEN_KEY))).thenReturn("cached-token");
        mockServer.expect(requestTo("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=cached-token&code=auth-code"))
                .andRespond(withSuccess("{\"errcode\":0,\"UserId\":\"zhangsan\"}", MediaType.APPLICATION_JSON));

        assertServiceException(() -> weComClientService.getUserMobileByCode("auth-code"),
                AUTH_WECOM_API_ERROR, "企业微信未返回 user_ticket，请确认授权 scope 为 snsapi_privateinfo");
        mockServer.verify();
    }

    private void mockValueOperations() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

}
