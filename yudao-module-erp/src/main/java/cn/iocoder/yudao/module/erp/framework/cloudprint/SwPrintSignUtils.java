package cn.iocoder.yudao.module.erp.framework.cloudprint;

import cn.hutool.crypto.digest.DigestUtil;

public final class SwPrintSignUtils {

    private SwPrintSignUtils() {
    }

    public static String sign(String username, String secret, long times) {
        String raw = "secret=" + secret + "&times=" + times + "&username=" + username;
        return DigestUtil.md5Hex(raw).toUpperCase();
    }

}
