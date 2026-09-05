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
     * @return 授权地址
     */
    String getAuthorizeUrl(String redirectUri, String state);

    /**
     * 使用企业微信授权码获得成员手机号。
     *
     * @param code 授权码
     * @return 成员手机号
     */
    String getUserMobileByCode(String code);

}
