package cn.iocoder.yudao.module.erp.enums.cloudprint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ErpCloudPrintConstants {

    private ErpCloudPrintConstants() {
    }

    public static final String BIZ_TYPE_SALE_OUT = "SALE_OUT";
    public static final String MODULE_KEY_SALE_OUT = "sale_out";

    public static final int DEV_TYPE_THERMAL_80 = 1;
    public static final int DEV_TYPE_LABEL = 2;
    public static final int DEV_TYPE_A4_BOX = 3;
    public static final int DEV_TYPE_DOT_MATRIX = 4;

    public static final int CONTENT_TYPE_PDF = 7;
    public static final int CONTENT_TYPE_HTML = 9;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_SUBMITTED = 1;
    public static final int STATUS_SUCCESS = 2;
    public static final int STATUS_FAILED = 3;
    public static final int STATUS_SUBMIT_FAILED = 4;
    public static final int STATUS_UNKNOWN = 5;
    public static final int STATUS_TIMEOUT = 6;
    public static final int STATUS_CANCELED = 7;

    public static final Set<Integer> RUNNING_STATUSES =
            new HashSet<>(Arrays.asList(STATUS_PENDING, STATUS_SUBMITTED, STATUS_UNKNOWN));
    public static final Set<Integer> FINAL_STATUSES =
            new HashSet<>(Arrays.asList(STATUS_SUCCESS, STATUS_FAILED, STATUS_SUBMIT_FAILED, STATUS_CANCELED));

}
