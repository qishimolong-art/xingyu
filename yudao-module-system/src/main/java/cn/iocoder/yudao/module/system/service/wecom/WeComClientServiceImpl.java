package cn.iocoder.yudao.module.system.service.wecom;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.framework.wecom.config.WeComProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;

/**
 * 企业微信客户端 Service 实现类。
 */
@Service
@Slf4j
public class WeComClientServiceImpl implements WeComClientService {

    private static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    private static final String GET_TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken";
    private static final String GET_USER_INFO_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo";
    private static final String GET_USER_DETAIL_URL = "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail";
    private static final String GET_USER_ID_BY_MOBILE_URL = "https://qyapi.weixin.qq.com/cgi-bin/user/getuserid";
    private static final String SEND_MESSAGE_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send";

    private static final String ACCESS_TOKEN_KEY = "wecom_access_token:%s:%s";

    @Resource
    private WeComProperties properties;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String getAuthorizeUrl(String redirectUri, String state, String clientKey) {
        WeComClientConfig config = resolveConfig(clientKey);
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("appid", config.getCorpId())
                .queryParam("agentid", config.getAgentId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_privateinfo")
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString() + "#wechat_redirect";
    }

    @Override
    public String getUserMobileByCode(String code, String clientKey) {
        return getUserIdentityByCode(code, clientKey).getMobile();
    }

    @Override
    public WeComUserIdentity getUserIdentityByCode(String code, String clientKey) {
        WeComClientConfig config = resolveConfig(clientKey);
        String accessToken = getAccessToken(config);
        WeComUserInfoRespDTO userInfo = getForObject(UriComponentsBuilder.fromHttpUrl(GET_USER_INFO_URL)
                .queryParam("access_token", accessToken)
                .queryParam("code", code)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri(), WeComUserInfoRespDTO.class, "获取访问用户身份");
        if (StrUtil.isBlank(userInfo.getUserId())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信未返回 UserId，请确认当前用户属于该企业并在应用可见范围内");
        }
        if (StrUtil.isBlank(userInfo.getUserTicket())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信未返回 user_ticket，请确认授权 scope 为 snsapi_privateinfo");
        }

        WeComUserDetailRespDTO user = postForObject(UriComponentsBuilder.fromHttpUrl(GET_USER_DETAIL_URL)
                .queryParam("access_token", accessToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri(), new WeComUserDetailReqDTO(userInfo.getUserTicket()), WeComUserDetailRespDTO.class,
                "读取成员敏感信息");
        if (StrUtil.isBlank(user.getMobile())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信未返回成员手机号");
        }
        if (StrUtil.isNotBlank(user.getUserId()) && !StrUtil.equals(userInfo.getUserId(), user.getUserId())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信成员身份不一致");
        }
        return new WeComUserIdentity(userInfo.getUserId(), user.getMobile());
    }

    @Override
    public String getUserIdByMobile(String mobile, String clientKey) {
        if (StrUtil.isBlank(mobile)) {
            throw exception(AUTH_WECOM_API_ERROR, "查询企业微信 UserID 的手机号不能为空");
        }
        WeComClientConfig config = resolveConfig(clientKey);
        String accessToken = getAccessToken(config);
        WeComUserIdByMobileRespDTO response = postForObject(UriComponentsBuilder
                        .fromHttpUrl(GET_USER_ID_BY_MOBILE_URL)
                        .queryParam("access_token", accessToken)
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUri(),
                new WeComUserIdByMobileReqDTO(StrUtil.trim(mobile)),
                WeComUserIdByMobileRespDTO.class, "根据手机号查询企业微信成员");
        if (StrUtil.isBlank(response.getUserId())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信未返回成员 UserID");
        }
        return response.getUserId();
    }

    @Override
    public void sendTextCardMessage(String userId, String title, String description, String url,
                                    String buttonText, String clientKey) {
        if (StrUtil.hasBlank(userId, title, description, url)) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信文本卡片消息参数不完整");
        }
        WeComClientConfig config = resolveConfig(clientKey);
        long agentId;
        try {
            agentId = Long.parseLong(config.getAgentId());
        } catch (NumberFormatException exception) {
            throw cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil
                    .exception(AUTH_WECOM_CONFIG_ERROR);
        }
        String accessToken = getAccessToken(config);
        WeComTextCardMessageReqDTO request = new WeComTextCardMessageReqDTO(
                userId, agentId, new WeComTextCardDTO(title, description, url,
                StrUtil.blankToDefault(buttonText, "查看详情")));
        WeComSendMessageRespDTO response = postForObject(UriComponentsBuilder.fromHttpUrl(SEND_MESSAGE_URL)
                        .queryParam("access_token", accessToken)
                        .build()
                        .encode(StandardCharsets.UTF_8)
                        .toUri(), request, WeComSendMessageRespDTO.class, "发送企业微信应用消息");
        if (StrUtil.isNotBlank(response.getInvalidUser())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信应用消息收件人无效");
        }
    }

    private String getAccessToken(WeComClientConfig config) {
        String cacheKey = formatAccessTokenKey(config);
        String accessToken = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        WeComAccessTokenRespDTO response = getForObject(UriComponentsBuilder.fromHttpUrl(GET_TOKEN_URL)
                .queryParam("corpid", config.getCorpId())
                .queryParam("corpsecret", config.getSecret())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri(), WeComAccessTokenRespDTO.class, "获取 access_token");
        if (StrUtil.isBlank(response.getAccessToken())) {
            throw exception(AUTH_WECOM_API_ERROR, "企业微信未返回 access_token");
        }

        long expiresIn = response.getExpiresIn() == null ? 7200L : response.getExpiresIn();
        long aheadSeconds = properties.getAccessTokenAheadRefresh() == null ? 120L
                : properties.getAccessTokenAheadRefresh().getSeconds();
        long cacheSeconds = Math.max(60L, expiresIn - aheadSeconds);
        stringRedisTemplate.opsForValue().set(cacheKey, response.getAccessToken(), cacheSeconds, TimeUnit.SECONDS);
        return response.getAccessToken();
    }

    private String formatAccessTokenKey(WeComClientConfig config) {
        return String.format(ACCESS_TOKEN_KEY, config.getCorpId(), config.getAgentId());
    }

    private WeComClientConfig resolveConfig(String clientKey) {
        if (!properties.isEnabled()) {
            throw exception(AUTH_WECOM_DISABLED);
        }
        String normalizedClientKey = StrUtil.trim(clientKey);
        WeComProperties.ClientProperties clientProperties = null;
        if (StrUtil.isNotBlank(normalizedClientKey)) {
            clientProperties = properties.getClients() == null ? null : properties.getClients().get(normalizedClientKey);
            if (clientProperties == null) {
                throw exception(AUTH_WECOM_CONFIG_ERROR);
            }
        }
        String corpId = clientProperties == null || StrUtil.isBlank(clientProperties.getCorpId())
                ? properties.getCorpId() : clientProperties.getCorpId();
        String agentId = clientProperties == null || StrUtil.isBlank(clientProperties.getAgentId())
                ? properties.getAgentId() : clientProperties.getAgentId();
        String secret = clientProperties == null || StrUtil.isBlank(clientProperties.getSecret())
                ? properties.getSecret() : clientProperties.getSecret();
        if (StrUtil.hasBlank(corpId, agentId, secret)) {
            throw exception(AUTH_WECOM_CONFIG_ERROR);
        }
        return new WeComClientConfig(corpId, agentId, secret);
    }

    private <T extends WeComBaseRespDTO> T getForObject(URI uri, Class<T> responseType, String action) {
        T response;
        try {
            response = restTemplate.getForObject(uri, responseType);
        } catch (Exception ex) {
            log.warn("[getForObject][action({}) uri({}) 调用失败]", action, uri, ex);
            throw exception(AUTH_WECOM_API_ERROR, action + "失败");
        }
        if (response == null) {
            throw exception(AUTH_WECOM_API_ERROR, action + "无响应");
        }
        if (response.getErrCode() != null && response.getErrCode() != 0) {
            throw exception(AUTH_WECOM_API_ERROR,
                    action + "失败：" + response.getErrCode() + " " + StrUtil.nullToDefault(response.getErrMsg(), ""));
        }
        return response;
    }

    private <T extends WeComBaseRespDTO> T postForObject(URI uri, Object request, Class<T> responseType,
                                                         String action) {
        T response;
        try {
            response = restTemplate.postForObject(uri, request, responseType);
        } catch (Exception ex) {
            log.warn("[postForObject][action({}) uri({}) 调用失败]", action, uri, ex);
            throw exception(AUTH_WECOM_API_ERROR, action + "失败");
        }
        if (response == null) {
            throw exception(AUTH_WECOM_API_ERROR, action + "无响应");
        }
        if (response.getErrCode() != null && response.getErrCode() != 0) {
            throw exception(AUTH_WECOM_API_ERROR,
                    action + "失败：" + response.getErrCode() + " " + StrUtil.nullToDefault(response.getErrMsg(), ""));
        }
        return response;
    }

    @Data
    @NoArgsConstructor
    private static class WeComBaseRespDTO {

        @JsonProperty("errcode")
        private Integer errCode;

        @JsonProperty("errmsg")
        private String errMsg;

    }

    @Data
    @NoArgsConstructor
    private static class WeComAccessTokenRespDTO extends WeComBaseRespDTO {

        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expires_in")
        private Integer expiresIn;

    }

    @Data
    @NoArgsConstructor
    private static class WeComUserInfoRespDTO extends WeComBaseRespDTO {

        @JsonProperty("UserId")
        @JsonAlias("userid")
        private String userId;

        @JsonProperty("OpenId")
        @JsonAlias("openid")
        private String openId;

        @JsonProperty("user_ticket")
        private String userTicket;

    }

    @Data
    @NoArgsConstructor
    private static class WeComUserDetailReqDTO {

        @JsonProperty("user_ticket")
        private String userTicket;

        public WeComUserDetailReqDTO(String userTicket) {
            this.userTicket = userTicket;
        }

    }

    @Data
    @NoArgsConstructor
    private static class WeComUserDetailRespDTO extends WeComBaseRespDTO {

        @JsonProperty("userid")
        private String userId;

        @JsonProperty("mobile")
        private String mobile;

    }

    @Data
    @NoArgsConstructor
    private static class WeComUserIdByMobileReqDTO {

        private String mobile;

        public WeComUserIdByMobileReqDTO(String mobile) {
            this.mobile = mobile;
        }

    }

    @Data
    @NoArgsConstructor
    private static class WeComUserIdByMobileRespDTO extends WeComBaseRespDTO {

        @JsonProperty("userid")
        private String userId;

    }

    @Data
    @NoArgsConstructor
    private static class WeComTextCardMessageReqDTO {

        @JsonProperty("touser")
        private String toUser;

        @JsonProperty("msgtype")
        private String messageType;

        @JsonProperty("agentid")
        private Long agentId;

        @JsonProperty("textcard")
        private WeComTextCardDTO textCard;

        public WeComTextCardMessageReqDTO(String toUser, Long agentId, WeComTextCardDTO textCard) {
            this.toUser = toUser;
            this.messageType = "textcard";
            this.agentId = agentId;
            this.textCard = textCard;
        }

    }

    @Data
    @NoArgsConstructor
    private static class WeComTextCardDTO {

        private String title;
        private String description;
        private String url;

        @JsonProperty("btntxt")
        private String buttonText;

        public WeComTextCardDTO(String title, String description, String url, String buttonText) {
            this.title = title;
            this.description = description;
            this.url = url;
            this.buttonText = buttonText;
        }

    }

    @Data
    @NoArgsConstructor
    private static class WeComSendMessageRespDTO extends WeComBaseRespDTO {

        @JsonProperty("invaliduser")
        private String invalidUser;

        @JsonProperty("msgid")
        private String messageId;

    }

    @Data
    private static class WeComClientConfig {

        private final String corpId;

        private final String agentId;

        private final String secret;

    }

}
