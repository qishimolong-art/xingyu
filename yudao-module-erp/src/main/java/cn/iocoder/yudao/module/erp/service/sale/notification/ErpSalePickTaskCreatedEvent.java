package cn.iocoder.yudao.module.erp.service.sale.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 拣货任务创建事件。
 */
@Data
@AllArgsConstructor
public class ErpSalePickTaskCreatedEvent {

    private Long tenantId;
    private Long taskId;

}
