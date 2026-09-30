package cn.iocoder.yudao.module.erp.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintCallbackService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ErpCloudPrintCallbackRetryJob implements JobHandler {

    @Resource
    private ErpCloudPrintCallbackService callbackService;

    @Override
    @TenantIgnore
    public String execute(String param) {
        int count = callbackService.retryPendingCallbacks();
        return "云打印回调本次处理数量：" + count;
    }

}
