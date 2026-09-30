package cn.iocoder.yudao.module.system.framework.wecom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 企业微信 H5 免登配置。
 */
@Component
@ConfigurationProperties(prefix = "yudao.wecom")
@Data
public class WeComProperties {

    /**
     * 是否开启企业微信 H5 免登。
     */
    private Boolean enabled = false;

    /**
     * 企业 ID。
     */
    private String corpId;

    /**
     * 应用 AgentId。
     */
    private String agentId;

    /**
     * 应用 Secret。
     */
    private String secret;

    /**
     * 多企业微信应用配置，key 由前端入口传入，例如 sale-pick、sale-delivery。
     */
    private Map<String, ClientProperties> clients = new HashMap<>();

    /**
     * 企业微信智能机器人 API 模式配置。
     */
    private AiBotProperties aiBot = new AiBotProperties();

    /**
     * OAuth state 有效期。
     */
    private Duration stateTimeout = Duration.ofMinutes(5);

    /**
     * access_token 提前刷新时间，避免临界点失效。
     */
    private Duration accessTokenAheadRefresh = Duration.ofSeconds(120);

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    @Data
    public static class ClientProperties {

        /**
         * 企业 ID。为空时使用外层 corpId。
         */
        private String corpId;

        /**
         * 应用 AgentId。
         */
        private String agentId;

        /**
         * 应用 Secret。
         */
        private String secret;

    }

    @Data
    public static class AiBotProperties {

        private Boolean enabled = false;
        private String botId;
        @lombok.ToString.Exclude
        private String secret;
        private Long tenantId;
        private String loginClientKey;
        private String bindPageUrl;
        private String websocketUrl = "wss://openws.work.weixin.qq.com";
        private Duration heartbeatInterval = Duration.ofSeconds(30);
        private Duration ticketTimeout = Duration.ofMinutes(5);
        private Duration choiceTimeout = Duration.ofMinutes(10);

        public boolean isEnabled() {
            return Boolean.TRUE.equals(enabled);
        }

    }

}
