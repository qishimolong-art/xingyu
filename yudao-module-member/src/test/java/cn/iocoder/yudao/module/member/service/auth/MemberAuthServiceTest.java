package cn.iocoder.yudao.module.member.service.auth;

import cn.iocoder.yudao.framework.common.biz.system.oauth2.OAuth2TokenCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenCreateReqDTO;
import cn.iocoder.yudao.framework.common.biz.system.oauth2.dto.OAuth2AccessTokenRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.TerminalEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.redis.config.YudaoRedisAutoConfiguration;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbAndRedisUnitTest;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthLoginRespVO;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthWeixinMiniAppLoginReqVO;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthWeixinMiniAppSilentLoginReqVO;
import cn.iocoder.yudao.module.member.controller.app.auth.vo.AppAuthWeixinMiniAppSilentLoginRespVO;
import cn.iocoder.yudao.module.member.dal.dataobject.user.MemberUserDO;
import cn.iocoder.yudao.module.member.dal.mysql.user.MemberUserMapper;
import cn.iocoder.yudao.module.member.service.user.MemberUserService;
import cn.iocoder.yudao.module.system.api.logger.LoginLogApi;
import cn.iocoder.yudao.module.system.api.sms.SmsCodeApi;
import cn.iocoder.yudao.module.system.api.social.SocialClientApi;
import cn.iocoder.yudao.module.system.api.social.SocialUserApi;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserBindReqDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialUserRespDTO;
import cn.iocoder.yudao.module.system.api.social.dto.SocialWxPhoneNumberInfoRespDTO;
import cn.iocoder.yudao.module.system.enums.oauth2.OAuth2ClientConstants;
import cn.iocoder.yudao.module.system.enums.social.SocialTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomString;
import static cn.iocoder.yudao.module.member.enums.ErrorCodeConstants.AUTH_LOGIN_USER_DISABLED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// TODO @芋艿：单测的 review，等逻辑都达成一致后
/**
 * {@link MemberAuthService} 的单元测试类
 *
 * @author 宋天
 */
@Import({MemberAuthServiceImpl.class, YudaoRedisAutoConfiguration.class})
public class MemberAuthServiceTest extends BaseDbAndRedisUnitTest {

    // TODO @芋艿：登录相关的单测，待补全

    @Resource
    private MemberAuthServiceImpl authService;

    @MockBean
    private MemberUserService userService;
    @MockBean
    private SmsCodeApi smsCodeApi;
    @MockBean
    private LoginLogApi loginLogApi;
    @MockBean
    private OAuth2TokenCommonApi oauth2TokenApi;
    @MockBean
    private SocialUserApi socialUserApi;
    @MockBean
    private SocialClientApi socialClientApi;
    @MockBean
    private PasswordEncoder passwordEncoder;

    @Resource
    private MemberUserMapper memberUserMapper;

    // TODO 芋艿：后续重构这个单测
//    @Test
//    public void testUpdatePassword_success(){
//        // 准备参数
//        MemberUserDO userDO = randomUserDO();
//        memberUserMapper.insert(userDO);
//
//        // 新密码
//        String newPassword = randomString();
//
//        // 请求实体
//        AppMemberUserUpdatePasswordReqVO reqVO = AppMemberUserUpdatePasswordReqVO.builder()
//                .oldPassword(userDO.getPassword())
//                .password(newPassword)
//                .build();
//
//        // 测试桩
//        // 这两个相等是为了返回ture这个结果
//        when(passwordEncoder.matches(reqVO.getOldPassword(),reqVO.getOldPassword())).thenReturn(true);
//        when(passwordEncoder.encode(newPassword)).thenReturn(newPassword);
//
//        // 更新用户密码
//        authService.updatePassword(userDO.getId(), reqVO);
//        assertEquals(memberUserMapper.selectById(userDO.getId()).getPassword(),newPassword);
//    }

    @Test
    public void testWeixinMiniAppSilentLogin_boundUser_success() {
        // 准备参数
        String loginCode = randomString();
        String state = randomString();
        Long userId = randomLongId();
        String openid = randomString();
        AppAuthWeixinMiniAppSilentLoginReqVO reqVO = AppAuthWeixinMiniAppSilentLoginReqVO.builder()
                .loginCode(loginCode).state(state).build();

        // mock
        when(socialUserApi.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq(loginCode), eq(state)))
                .thenReturn(new SocialUserRespDTO(openid, randomString(), randomString(), userId));
        MemberUserDO user = randomUserDO(o -> o.setId(userId).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.getUser(eq(userId))).thenReturn(user);
        OAuth2AccessTokenRespDTO token = randomAccessToken(userId);
        when(oauth2TokenApi.createAccessToken(eq(new OAuth2AccessTokenCreateReqDTO()
                .setUserId(userId).setUserType(UserTypeEnum.MEMBER.getValue())
                .setClientId(OAuth2ClientConstants.CLIENT_ID_DEFAULT)))).thenReturn(token);

        // 调用
        AppAuthWeixinMiniAppSilentLoginRespVO result = authService.weixinMiniAppSilentLogin(reqVO);

        // 断言
        assertFalse(result.getNeedPhoneAuth());
        assertEquals(userId, result.getUserId());
        assertEquals(token.getAccessToken(), result.getAccessToken());
        assertEquals(token.getRefreshToken(), result.getRefreshToken());
        assertEquals(token.getExpiresTime(), result.getExpiresTime());
        assertEquals(openid, result.getOpenid());
        verify(userService).updateUserLogin(eq(userId), isNull());
    }

    @Test
    public void testWeixinMiniAppSilentLogin_unbound_needPhoneAuth() {
        // 准备参数
        String loginCode = randomString();
        String state = randomString();
        AppAuthWeixinMiniAppSilentLoginReqVO reqVO = AppAuthWeixinMiniAppSilentLoginReqVO.builder()
                .loginCode(loginCode).state(state).build();

        // mock
        when(socialUserApi.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq(loginCode), eq(state)))
                .thenReturn(new SocialUserRespDTO(randomString(), randomString(), randomString(), null));

        // 调用
        AppAuthWeixinMiniAppSilentLoginRespVO result = authService.weixinMiniAppSilentLogin(reqVO);

        // 断言
        assertTrue(result.getNeedPhoneAuth());
        assertNull(result.getAccessToken());
        verify(userService, never()).createUser(anyString(), anyString(), anyString(), anyInt());
        verify(userService, never()).createUserIfAbsent(anyString(), anyString(), anyInt());
        verify(socialUserApi, never()).bindSocialUser(any());
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    public void testWeixinMiniAppSilentLogin_userDisabled() {
        // 准备参数
        String loginCode = randomString();
        String state = randomString();
        Long userId = randomLongId();
        AppAuthWeixinMiniAppSilentLoginReqVO reqVO = AppAuthWeixinMiniAppSilentLoginReqVO.builder()
                .loginCode(loginCode).state(state).build();

        // mock
        when(socialUserApi.getSocialUserByCode(eq(UserTypeEnum.MEMBER.getValue()),
                eq(SocialTypeEnum.WECHAT_MINI_PROGRAM.getType()), eq(loginCode), eq(state)))
                .thenReturn(new SocialUserRespDTO(randomString(), randomString(), randomString(), userId));
        MemberUserDO user = randomUserDO(o -> o.setId(userId).setStatus(CommonStatusEnum.DISABLE.getStatus()));
        when(userService.getUser(eq(userId))).thenReturn(user);

        // 调用，并断言
        assertServiceException(() -> authService.weixinMiniAppSilentLogin(reqVO), AUTH_LOGIN_USER_DISABLED);
        verify(oauth2TokenApi, never()).createAccessToken(any());
    }

    @Test
    public void testWeixinMiniAppLogin_success() {
        // 准备参数
        String phoneCode = randomString();
        String loginCode = randomString();
        String state = randomString();
        String mobile = "13800138000";
        String openid = randomString();
        AppAuthWeixinMiniAppLoginReqVO reqVO = AppAuthWeixinMiniAppLoginReqVO.builder()
                .phoneCode(phoneCode).loginCode(loginCode).state(state).build();

        // mock
        SocialWxPhoneNumberInfoRespDTO phoneNumberInfo = new SocialWxPhoneNumberInfoRespDTO();
        phoneNumberInfo.setPhoneNumber(mobile);
        phoneNumberInfo.setPurePhoneNumber(mobile);
        when(socialClientApi.getWxMaPhoneNumberInfo(eq(UserTypeEnum.MEMBER.getValue()), eq(phoneCode)))
                .thenReturn(phoneNumberInfo);
        Long userId = randomLongId();
        MemberUserDO user = randomUserDO(o -> o.setId(userId).setMobile(mobile)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(userService.createUserIfAbsent(eq(mobile), isNull(),
                eq(TerminalEnum.WECHAT_MINI_PROGRAM.getTerminal()))).thenReturn(user);
        when(socialUserApi.bindSocialUser(eq(new SocialUserBindReqDTO(userId, UserTypeEnum.MEMBER.getValue(),
                SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), loginCode, state)))).thenReturn(openid);
        OAuth2AccessTokenRespDTO token = randomAccessToken(userId);
        when(oauth2TokenApi.createAccessToken(eq(new OAuth2AccessTokenCreateReqDTO()
                .setUserId(userId).setUserType(UserTypeEnum.MEMBER.getValue())
                .setClientId(OAuth2ClientConstants.CLIENT_ID_DEFAULT)))).thenReturn(token);

        // 调用
        AppAuthLoginRespVO result = authService.weixinMiniAppLogin(reqVO);

        // 断言
        assertEquals(userId, result.getUserId());
        assertEquals(token.getAccessToken(), result.getAccessToken());
        assertEquals(token.getRefreshToken(), result.getRefreshToken());
        assertEquals(openid, result.getOpenid());
        verify(socialUserApi).bindSocialUser(eq(new SocialUserBindReqDTO(userId, UserTypeEnum.MEMBER.getValue(),
                SocialTypeEnum.WECHAT_MINI_PROGRAM.getType(), loginCode, state)));
    }

    // TODO 芋艿：后续重构这个单测
//    @Test
//    public void testResetPassword_success(){
//        // 准备参数
//        MemberUserDO userDO = randomUserDO();
//        memberUserMapper.insert(userDO);
//
//        // 随机密码
//        String password = randomNumbers(11);
//        // 随机验证码
//        String code = randomNumbers(4);
//
//        // mock
//        when(passwordEncoder.encode(password)).thenReturn(password);
//
//        // 更新用户密码
//        AppMemberUserResetPasswordReqVO reqVO = new AppMemberUserResetPasswordReqVO();
//        reqVO.setMobile(userDO.getMobile());
//        reqVO.setPassword(password);
//        reqVO.setCode(code);
//
//        authService.resetPassword(reqVO);
//        assertEquals(memberUserMapper.selectById(userDO.getId()).getPassword(),password);
//    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static MemberUserDO randomUserDO(Consumer<MemberUserDO>... consumers) {
        Consumer<MemberUserDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setPassword(randomString());
        };
        return randomPojo(MemberUserDO.class, ArrayUtils.append(consumer, consumers));
    }

    private static OAuth2AccessTokenRespDTO randomAccessToken(Long userId) {
        OAuth2AccessTokenRespDTO token = randomPojo(OAuth2AccessTokenRespDTO.class, o -> {
            o.setUserId(userId);
            o.setUserType(UserTypeEnum.MEMBER.getValue());
            o.setExpiresTime(LocalDateTime.now().plusHours(1));
        });
        return token;
    }


}
