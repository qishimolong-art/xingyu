package cn.iocoder.yudao.module.erp.enums.cloudprint;

public final class ErpCloudPrintCallbackConstants {

    private ErpCloudPrintCallbackConstants() {
    }

    public static final int PROCESS_STATUS_PENDING = 0;
    public static final int PROCESS_STATUS_PROCESSING = 1;
    public static final int PROCESS_STATUS_SUCCESS = 2;
    public static final int PROCESS_STATUS_IGNORED = 3;
    public static final int PROCESS_STATUS_RETRY = 4;

    public static final int MAX_BODY_BYTES = 64 * 1024;
    public static final int RETRY_BATCH_SIZE = 100;
    public static final int PROCESS_LEASE_MINUTES = 5;
    public static final int TASK_WAIT_MINUTES = 10;

}
