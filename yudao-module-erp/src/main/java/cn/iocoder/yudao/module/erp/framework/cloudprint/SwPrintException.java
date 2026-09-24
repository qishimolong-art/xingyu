package cn.iocoder.yudao.module.erp.framework.cloudprint;

import lombok.Getter;

@Getter
public class SwPrintException extends RuntimeException {

    private final Integer code;
    private final boolean timeout;

    public SwPrintException(Integer code, String message) {
        this(code, message, false, null);
    }

    public SwPrintException(Integer code, String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.timeout = timeout;
    }

}
