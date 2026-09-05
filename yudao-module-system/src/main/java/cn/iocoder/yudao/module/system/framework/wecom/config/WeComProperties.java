package cn.iocoder.yudao.module.system.framework.wecom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

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

}
