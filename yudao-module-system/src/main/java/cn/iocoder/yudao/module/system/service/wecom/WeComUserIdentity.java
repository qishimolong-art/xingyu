package cn.iocoder.yudao.module.system.service.wecom;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 企业微信网页授权成员身份。 */
@Data
@AllArgsConstructor
public class WeComUserIdentity {

    private String userId;
    private String mobile;

}
