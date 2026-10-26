package cn.iocoder.yudao.module.system.service.wecom;

/**
 * 企业微信客户端 Service。
 */
public interface WeComClientService {

    /**
     * 获得企业微信网页授权地址。
     *
     * @param redirectUri 回调地址
     * @param state state
     * @param clientKey 企业微信应用配置 key
     * @return 授权地址
     */
    String getAuthorizeUrl(String redirectUri, String state, String clientKey);

    default String getAuthorizeUrl(String redirectUri, String state) {
        return getAuthorizeUrl(redirectUri, state, null);
    }

    /**
     * 使用企业微信授权码获得成员手机号。
     *
     * @param code 授权码
     * @param clientKey 企业微信应用配置 key
     * @return 成员手机号
     */
    String getUserMobileByCode(String code, String clientKey);

    /**
     * 使用企业微信授权码获得成员 UserId 和手机号。
     */
    WeComUserIdentity getUserIdentityByCode(String code, String clientKey);

    default WeComUserIdentity getUserIdentityByCode(String code) {
        return getUserIdentityByCode(code, null);
    }

    default String getUserMobileByCode(String code) {
        return getUserMobileByCode(code, null);
    }

    /**
     * 根据成员手机号查询企业微信 UserID。
     *
     * @param mobile 手机号
     * @param clientKey 企业微信应用配置 key
     * @return 企业微信 UserID
     */
    String getUserIdByMobile(String mobile, String clientKey);

    /**
     * 发送企业微信应用文本卡片消息。
     *
     * @param userId 企业微信 UserID
     * @param title 卡片标题
     * @param description 卡片内容
     * @param url 点击跳转地址
     * @param buttonText 按钮文案
     * @param clientKey 企业微信应用配置 key
     */
    void sendTextCardMessage(String userId, String title, String description, String url,
                             String buttonText, String clientKey);

}
