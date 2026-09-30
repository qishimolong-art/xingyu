package cn.iocoder.yudao.module.erp.service.cloudprint;

public interface ErpCloudPrintCallbackService {

    void receiveCallback(String pathToken, String rawBody);

    int retryPendingCallbacks();

}
