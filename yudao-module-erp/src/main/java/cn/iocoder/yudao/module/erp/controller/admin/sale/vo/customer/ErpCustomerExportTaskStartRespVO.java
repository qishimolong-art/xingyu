package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 客户异步导出任务创建 Response VO")
public class ErpCustomerExportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
