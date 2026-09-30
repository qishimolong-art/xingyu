package cn.iocoder.yudao.module.erp.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintQueueService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 云打印本地持久化队列兜底调度。
 */
@Component
public class ErpCloudPrintQueueDispatchJob implements JobHandler {

    @Resource
    private ErpCloudPrintQueueService queueService;

    @Override
    @TenantJob
    public String execute(String param) {
        int count = queueService.dispatchPendingTasks();
        return "云打印队列本次提交任务数：" + count;
    }

}
