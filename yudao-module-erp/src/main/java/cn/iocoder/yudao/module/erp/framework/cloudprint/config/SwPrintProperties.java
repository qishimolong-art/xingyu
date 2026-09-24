package cn.iocoder.yudao.module.erp.framework.cloudprint.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.sw-print")
public class SwPrintProperties {

    private Boolean enabled = false;
    private String baseUrl = "https://open.sw-aiot.com";
    private String username;
    private String secret;
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);
    private String callbackPathToken;
    private Integer submitTimeoutMinutes = 10;
    private Boolean checkDeviceOnline = true;

}
