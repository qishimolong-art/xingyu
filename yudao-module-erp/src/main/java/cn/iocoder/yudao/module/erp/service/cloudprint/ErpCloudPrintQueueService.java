package cn.iocoder.yudao.module.erp.service.cloudprint;

import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;

public interface ErpCloudPrintQueueService {

    void dispatchDeviceAsync(Long deviceId);

    int dispatchPendingTasks();

    void pauseDevice(Long deviceId, Long taskId, String reason);

    boolean clearPauseIfTask(Long deviceId, Long taskId);

    ErpCloudPrintTaskDO resumeQueue(Long deviceId, String failedTaskAction);

}
