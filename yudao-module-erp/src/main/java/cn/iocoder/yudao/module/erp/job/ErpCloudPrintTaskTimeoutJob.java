package cn.iocoder.yudao.module.erp.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.module.erp.dal.dataobject.cloudprint.ErpCloudPrintTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.cloudprint.ErpCloudPrintTaskMapper;
import cn.iocoder.yudao.module.erp.enums.cloudprint.ErpCloudPrintConstants;
import cn.iocoder.yudao.module.erp.framework.cloudprint.config.SwPrintProperties;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class ErpCloudPrintTaskTimeoutJob implements JobHandler {

    @Resource
    private ErpCloudPrintTaskMapper taskMapper;
    @Resource
    private SwPrintProperties properties;

    @Override
    public String execute(String param) {
        int timeoutMinutes = properties.getSubmitTimeoutMinutes() == null ? 10 : properties.getSubmitTimeoutMinutes();
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<ErpCloudPrintTaskDO> tasks = taskMapper.selectListByStatuses(Arrays.asList(
                ErpCloudPrintConstants.STATUS_SUBMITTED, ErpCloudPrintConstants.STATUS_UNKNOWN));
        int count = 0;
        for (ErpCloudPrintTaskDO task : tasks) {
            if (task.getSubmitTime() == null || !task.getSubmitTime().isBefore(deadline)) {
                continue;
            }
            taskMapper.updateById(new ErpCloudPrintTaskDO()
                    .setId(task.getId())
                    .setStatus(ErpCloudPrintConstants.STATUS_TIMEOUT)
                    .setErrorMsg("超时未收到打印结果回调"));
            count++;
        }
        return "云打印超时任务处理：" + count;
    }

}
