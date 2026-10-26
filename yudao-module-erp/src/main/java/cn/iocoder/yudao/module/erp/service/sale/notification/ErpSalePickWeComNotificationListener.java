package cn.iocoder.yudao.module.erp.service.sale.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import javax.annotation.Resource;

/**
 * 拣货任务创建后的企业微信通知监听器。
 */
@Component
@Slf4j
public class ErpSalePickWeComNotificationListener {

    @Resource
    private ErpSalePickWeComNotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTaskCreated(ErpSalePickTaskCreatedEvent event) {
        try {
            notificationService.sendTaskCreatedNotification(event.getTenantId(), event.getTaskId());
        } catch (Exception exception) {
            log.error("[onTaskCreated][tenantId({}) taskId({}) 企微拣货通知处理失败]",
                    event.getTenantId(), event.getTaskId(), exception);
        }
    }

}
