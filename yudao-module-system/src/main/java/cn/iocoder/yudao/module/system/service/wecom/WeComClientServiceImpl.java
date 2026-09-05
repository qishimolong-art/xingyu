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

    private static final String ACCESS_TOKEN_KEY = "wecom_access_token:%s:%s";

    @Resource
    private WeComProperties properties;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String getAuthorizeUrl(String redirectUri, String state) {
        validateConfig();
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("appid", properties.getCorpId())
                .queryParam("agentid", properties.getAgentId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "snsapi_privateinfo")
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString() + "#wechat_redirect";
    }

    @Override
    public String getUserMobileByCode(String code) {
        validateConfig();
        String accessToken = getAccessToken();
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
        return user.getMobile();
    }

    private String getAccessToken() {
        String cacheKey = formatAccessTokenKey();
        String accessToken = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StrUtil.isNotBlank(accessToken)) {
            return accessToken;
        }

        WeComAccessTokenRespDTO response = getForObject(UriComponentsBuilder.fromHttpUrl(GET_TOKEN_URL)
                .queryParam("corpid", properties.getCorpId())
                .queryParam("corpsecret", properties.getSecret())
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

    private String formatAccessTokenKey() {
        return String.format(ACCESS_TOKEN_KEY, properties.getCorpId(), properties.getAgentId());
    }

    private void validateConfig() {
        if (!properties.isEnabled()) {
            throw exception(AUTH_WECOM_DISABLED);
        }
        if (StrUtil.hasBlank(properties.getCorpId(), properties.getAgentId(), properties.getSecret())) {
            throw exception(AUTH_WECOM_CONFIG_ERROR);
        }
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

}
