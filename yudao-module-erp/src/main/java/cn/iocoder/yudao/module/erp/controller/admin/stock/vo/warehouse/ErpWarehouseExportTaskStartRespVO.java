package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 仓库异步导出任务创建 Response VO")
public class ErpWarehouseExportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
