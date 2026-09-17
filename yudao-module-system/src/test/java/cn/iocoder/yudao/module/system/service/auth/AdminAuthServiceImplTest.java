package cn.iocoder.yudao.module.system.service.auth;

import cn.hutool.core.util.ReflectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.*;
import cn.iocoder.yudao.module.system.dal.dataobject.oauth2.OAuth2AccessTokenDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.enums.logger.LoginLogTypeEnum;
import cn.iocoder.yudao.module.system.enums.logger.LoginResultEnum;
import cn.iocoder.yudao.module.system.enums.sms.SmsSceneEnum;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import cn.iocoder.yudao.module.system.service.logger.LoginLogService;
import cn.iocoder.yudao.module.system.service.member.MemberService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.social.SocialUserService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.wecom.WeComClientService;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import javax.validation.Validation;
import javax.validation.Validator;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(AdminAuthServiceImpl.class)
public class AdminAuthServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AdminAuthServiceImpl authService;

    @MockBean
    private AdminUserService userService;
    @MockBean
    private CaptchaService captchaService;
    @MockBean
    private LoginLogService loginLogService;
    @MockBean
    private SocialUserService socialUserService;
    @MockBean
    private SmsCodeApi smsCodeApi;
    @MockBean
    private OAuth2TokenService oauth2TokenService;
    @MockBean
    private MemberService memberService;
    @MockBean
    private Validator validator;
    @MockBean
    private WeComClientService weComClientService;
    @MockBean
    private WeComProperties weComProperties;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;
    @MockBean
    private TenantProperties tenantProperties;
    @MockBean
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    public void setUp() {
        authService.setCaptchaEnable(true);
        // 注入一个 Validator 对象
        ReflectUtil.setFieldValue(authService, "validator",
                Validation.buildDefaultValidatorFactory().getValidator());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(weComProperties.getStateTimeout()).thenReturn(Duration.ofMinutes(5));
        when(tenantProperties.getEnable()).thenReturn(true);
        TenantContextHolder.clear();
    }

    @Test
    public void testAuthenticate_success() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        // 调用
        AdminUserDO loginUser = authService.authenticate(username, password);
        // 校验
        assertPojoEquals(user, loginUser);
    }

    @Test
    public void testAuthenticate_userNotFound() {
        // 准备参数
        String username = randomString();
        String password = randomString();

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.BAD_CREDENTIALS.getResult())
                        && o.getUserId() == null)
        );
    }

    @Test
    public void testAuthenticate_badCredentials() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_BAD_CREDENTIALS);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.BAD_CREDENTIALS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testAuthenticate_userDisabled() {
        // 准备参数
        String username = randomString();
        String password = randomString();
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setUsername(username)
                .setPassword(password).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(userService.getUserByUsername(eq(username))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq(password), eq(user.getPassword()))).thenReturn(true);

        // 调用, 并断言异常
        assertServiceException(() -> authService.authenticate(username, password),
                AUTH_LOGIN_USER_DISABLED);
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.USER_DISABLED.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testLogin_success() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class, o ->
                o.setUsername("test_username").setPassword("test_password")
                        .setSocialType(randomEle(SocialTypeEnum.values()).getType()));

        // mock 验证码正确
        authService.setCaptchaEnable(false);
        // mock user 数据
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setUsername("test_username")
                .setPassword("test_password").setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserByUsername(eq("test_username"))).thenReturn(user);
        // mock password 匹配
        when(userService.isPasswordMatch(eq("test_password"), eq(user.getPassword()))).thenReturn(true);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并校验
        AuthLoginRespVO loginRespVO = authService.login(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
        verify(socialUserService).bindSocialUser(eq(new SocialUserBindReqDTO(
                user.getId(), UserTypeEnum.ADMIN.getValue(),
                reqVO.getSocialType(), reqVO.getSocialCode(), reqVO.getSocialState())));
    }

    @Test
    public void testSendSmsCode() {
        // 准备参数
        String mobile = randomString();
        Integer scene = SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene();
        AuthSmsSendReqVO reqVO = new AuthSmsSendReqVO(mobile, scene);
        // mock 方法（用户信息）
        AdminUserDO user = randomPojo(AdminUserDO.class);
        when(userService.getUserByMobile(eq(mobile))).thenReturn(user);

        // 调用
        authService.sendSmsCode(reqVO);
        // 断言
        verify(smsCodeApi).sendSmsCode(argThat(sendReqDTO -> {
            assertEquals(mobile, sendReqDTO.getMobile());
            assertEquals(scene, sendReqDTO.getScene());
            return true;
        }));
    }

    @Test
    public void testSmsLogin_success() {
        // 准备参数
        String mobile = randomString();
        String code = randomString();
        AuthSmsLoginReqVO reqVO = new AuthSmsLoginReqVO(mobile, code);
        // mock 方法（验证码）
        doNothing().when(smsCodeApi).useSmsCode((argThat(smsCodeUseReqDTO -> {
            assertEquals(mobile, smsCodeUseReqDTO.getMobile());
            assertEquals(code, smsCodeUseReqDTO.getCode());
            assertEquals(SmsSceneEnum.ADMIN_MEMBER_LOGIN.getScene(), smsCodeUseReqDTO.getScene());
            return true;
        })));
        // mock 方法（用户信息）
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L));
        when(userService.getUserByMobile(eq(mobile))).thenReturn(user);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.smsLogin(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        // 断言调用
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_MOBILE.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testGetWeComAuthorizeUrl_success() {
        // 准备参数
        String redirectUri = "https://example.com/auth/wecom-login?tenantId=1&redirect=/";
        String authorizeUrl = randomString();
        TenantContextHolder.setTenantId(1L);
        when(weComClientService.getAuthorizeUrl(eq(redirectUri), anyString(), isNull())).thenReturn(authorizeUrl);

        // 调用，并断言
        assertEquals(authorizeUrl, authService.getWeComAuthorizeUrl(redirectUri));
        verify(valueOperations).set(argThat(key -> key.startsWith("wecom_auth_state:")),
                argThat(value -> value.contains("\"tenantId\":1") && value.contains(redirectUri)),
                eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testGetWeComAuthorizeUrl_clientKey() {
        // 准备参数
        String redirectUri = "https://example.com/auth/wecom-login?tenantId=1&workbench=sale-pick";
        String authorizeUrl = randomString();
        TenantContextHolder.setTenantId(1L);
        when(weComClientService.getAuthorizeUrl(eq(redirectUri), anyString(), eq("sale-pick"))).thenReturn(authorizeUrl);

        // 调用，并断言
        assertEquals(authorizeUrl, authService.getWeComAuthorizeUrl(redirectUri, "sale-pick"));
        verify(valueOperations).set(argThat(key -> key.startsWith("wecom_auth_state:")),
                argThat(value -> value.contains("\"tenantId\":1") && value.contains(redirectUri)
                        && value.contains("\"clientKey\":\"sale-pick\"")),
                eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void testGetWeComAuthorizeUrl_tenantEmpty() {
        // 准备参数
        String redirectUri = "https://example.com/auth/wecom-login?redirect=/";

        // 调用，并断言异常
        assertServiceException(() -> authService.getWeComAuthorizeUrl(redirectUri),
                AUTH_WECOM_API_ERROR, "租户编号不能为空");
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
        verify(weComClientService, never()).getAuthorizeUrl(anyString(), anyString(), any());
    }

    @Test
    public void testWeComSilentLogin_success() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");
        when(weComClientService.getUserMobileByCode(eq(code), isNull())).thenReturn(mobile);
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setMobile(mobile)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserListByMobile(eq(mobile))).thenAnswer(invocation -> {
            assertEquals(1L, TenantContextHolder.getTenantId());
            return Collections.singletonList(user);
        });
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state));
        assertPojoEquals(accessTokenDO, loginRespVO);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(weComClientService).getUserMobileByCode(eq(code), isNull());
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_SOCIAL.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testWeComSilentLogin_clientKey() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\","
                        + "\"clientKey\":\"sale-pick\"}");
        when(weComClientService.getUserMobileByCode(eq(code), eq("sale-pick"))).thenReturn(mobile);
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setMobile(mobile)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUserListByMobile(eq(mobile))).thenReturn(Collections.singletonList(user));
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state));
        assertPojoEquals(accessTokenDO, loginRespVO);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(weComClientService).getUserMobileByCode(eq(code), eq("sale-pick"));
    }

    @Test
    public void testWeComSilentLogin_tenantMismatch() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        TenantContextHolder.setTenantId(2L);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state)),
                AUTH_WECOM_STATE_INVALID);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(weComClientService, never()).getUserMobileByCode(anyString(), any());
    }

    @Test
    public void testWeComSilentLogin_stateInvalid() {
        // 准备参数
        String state = randomString();
        when(valueOperations.get(eq(String.format(RedisKeyConstants.WECOM_AUTH_STATE, state)))).thenReturn(null);

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(randomString(), state)),
                AUTH_WECOM_STATE_INVALID);
        verify(weComClientService, never()).getUserMobileByCode(anyString(), any());
    }

    @Test
    public void testWeComSilentLogin_userDisabled() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");
        when(weComClientService.getUserMobileByCode(eq(code), isNull())).thenReturn(mobile);
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(1L).setMobile(mobile)
                .setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(userService.getUserListByMobile(eq(mobile))).thenReturn(Collections.singletonList(user));

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state)),
                AUTH_LOGIN_USER_DISABLED);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_SOCIAL.getType())
                        && o.getResult().equals(LoginResultEnum.USER_DISABLED.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testWeComSilentLogin_mobileEmpty() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");
        when(weComClientService.getUserMobileByCode(eq(code), isNull())).thenReturn("");

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state)),
                AUTH_WECOM_MOBILE_EMPTY);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void testWeComSilentLogin_userNotExists() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");
        when(weComClientService.getUserMobileByCode(eq(code), isNull())).thenReturn(mobile);
        when(userService.getUserListByMobile(eq(mobile))).thenReturn(Collections.emptyList());

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state)),
                AUTH_MOBILE_NOT_EXISTS);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void testWeComSilentLogin_mobileDuplicate() {
        // 准备参数
        String code = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String stateKey = String.format(RedisKeyConstants.WECOM_AUTH_STATE, state);
        when(valueOperations.get(eq(stateKey)))
                .thenReturn("{\"tenantId\":1,\"redirectUri\":\"https://example.com/auth/wecom-login\"}");
        when(weComClientService.getUserMobileByCode(eq(code), isNull())).thenReturn(mobile);
        when(userService.getUserListByMobile(eq(mobile))).thenReturn(Arrays.asList(
                randomPojo(AdminUserDO.class, o -> o.setId(1L).setMobile(mobile)),
                randomPojo(AdminUserDO.class, o -> o.setId(2L).setMobile(mobile))));

        // 调用，并断言异常
        assertServiceException(() -> authService.weComSilentLogin(new AuthWeComLoginReqVO(code, state)),
                AUTH_WECOM_MOBILE_DUPLICATE);
        verify(stringRedisTemplate).delete(eq(stateKey));
        verify(oauth2TokenService, never()).createAccessToken(anyLong(), anyInt(), anyString(), any());
    }

    @Test
    public void testSocialLogin_success() {
        // 准备参数
        AuthSocialLoginReqVO reqVO = randomPojo(AuthSocialLoginReqVO.class);
        // mock 方法（绑定的用户编号）
        Long userId = 1L;
        when(socialUserService.getSocialUserByCode(eq(UserTypeEnum.ADMIN.getValue()), eq(reqVO.getType()),
                eq(reqVO.getCode()), eq(reqVO.getState()))).thenReturn(new SocialUserRespDTO(randomString(), randomString(), randomString(), userId));
        // mock（用户）
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> o.setId(userId));
        when(userService.getUser(eq(userId))).thenReturn(user);
        // mock 缓存登录用户到 Redis
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.createAccessToken(eq(1L), eq(UserTypeEnum.ADMIN.getValue()), eq("default"), isNull()))
                .thenReturn(accessTokenDO);

        // 调用，并断言
        AuthLoginRespVO loginRespVO = authService.socialLogin(reqVO);
        assertPojoEquals(accessTokenDO, loginRespVO);
        // 断言调用
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_SOCIAL.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult())
                        && o.getUserId().equals(user.getId()))
        );
    }

    @Test
    public void testValidateCaptcha_successWithEnable() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证通过
        when(captchaService.verification(argThat(captchaVO -> {
            assertEquals(reqVO.getCaptchaVerification(), captchaVO.getCaptchaVerification());
            return true;
        }))).thenReturn(ResponseModel.success());

        // 调用，无需断言
        authService.validateCaptcha(reqVO);
    }

    @Test
    public void testValidateCaptcha_successWithDisable() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证码关闭
        authService.setCaptchaEnable(false);

        // 调用，无需断言
        authService.validateCaptcha(reqVO);
    }

    @Test
    public void testCaptcha_fail() {
        // 准备参数
        AuthLoginReqVO reqVO = randomPojo(AuthLoginReqVO.class);

        // mock 验证通过
        when(captchaService.verification(argThat(captchaVO -> {
            assertEquals(reqVO.getCaptchaVerification(), captchaVO.getCaptchaVerification());
            return true;
        }))).thenReturn(ResponseModel.errorMsg("就是不对"));

        // 调用, 并断言异常
        assertServiceException(() -> authService.validateCaptcha(reqVO), AUTH_LOGIN_CAPTCHA_CODE_ERROR, "就是不对");
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGIN_USERNAME.getType())
                        && o.getResult().equals(LoginResultEnum.CAPTCHA_CODE_ERROR.getResult()))
        );
    }

    @Test
    public void testRefreshToken() {
        // 准备参数
        String refreshToken = randomString();
        // mock 方法
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class);
        when(oauth2TokenService.refreshAccessToken(eq(refreshToken), eq("default")))
                .thenReturn(accessTokenDO);

        // 调用
        AuthLoginRespVO loginRespVO = authService.refreshToken(refreshToken);
        // 断言
        assertPojoEquals(accessTokenDO, loginRespVO);
    }

    @Test
    public void testLogout_success() {
        // 准备参数
        String token = randomString();
        // mock
        OAuth2AccessTokenDO accessTokenDO = randomPojo(OAuth2AccessTokenDO.class, o -> o.setUserId(1L)
                .setUserType(UserTypeEnum.ADMIN.getValue()));
        when(oauth2TokenService.removeAccessToken(eq(token))).thenReturn(accessTokenDO);

        // 调用
        authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        // 校验调用参数
        verify(loginLogService).createLoginLog(
                argThat(o -> o.getLogType().equals(LoginLogTypeEnum.LOGOUT_SELF.getType())
                        && o.getResult().equals(LoginResultEnum.SUCCESS.getResult()))
        );
        // 调用，并校验

    }

    @Test
    public void testLogout_fail() {
        // 准备参数
        String token = randomString();

        // 调用
        authService.logout(token, LoginLogTypeEnum.LOGOUT_SELF.getType());
        // 校验调用参数
        verify(loginLogService, never()).createLoginLog(any());
    }

}
