package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 采购订单异步任务启动 Response VO")
public class ErpPurchaseOrderTaskStartRespVO {
    private Long taskId;
    private Boolean reused;
}
