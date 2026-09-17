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

    default String getUserMobileByCode(String code) {
        return getUserMobileByCode(code, null);
    }

}
