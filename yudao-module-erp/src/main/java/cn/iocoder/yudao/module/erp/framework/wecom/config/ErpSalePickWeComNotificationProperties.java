package cn.iocoder.yudao.module.erp.framework.wecom.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ERP 拣货任务企业微信通知配置。
 */
@Component
@ConfigurationProperties(prefix = "yudao.erp.sale-pick-wecom-notification")
@Data
public class ErpSalePickWeComNotificationProperties {

    /**
     * 是否发送拣货任务企业微信通知。默认关闭，关闭时不会查询成员或调用消息接口。
     */
    private Boolean enabled = false;

    /**
     * 企业微信应用配置 key。
     */
    private String clientKey = "sale-pick";

    /**
     * 企业微信免登静态入口地址。
     */
    private String entryUrl;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

}
