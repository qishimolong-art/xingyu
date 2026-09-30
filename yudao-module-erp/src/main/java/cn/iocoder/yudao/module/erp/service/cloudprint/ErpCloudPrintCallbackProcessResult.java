package cn.iocoder.yudao.module.erp.service.cloudprint;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErpCloudPrintCallbackProcessResult {

    public enum Type {
        SUCCESS,
        IGNORED,
        RETRY
    }

    private final Type type;
    private final boolean matched;
    private final String message;

    public static ErpCloudPrintCallbackProcessResult success(boolean matched, String message) {
        return new ErpCloudPrintCallbackProcessResult(Type.SUCCESS, matched, message);
    }

    public static ErpCloudPrintCallbackProcessResult ignored(boolean matched, String message) {
        return new ErpCloudPrintCallbackProcessResult(Type.IGNORED, matched, message);
    }

    public static ErpCloudPrintCallbackProcessResult retry(String message) {
        return new ErpCloudPrintCallbackProcessResult(Type.RETRY, false, message);
    }

}
