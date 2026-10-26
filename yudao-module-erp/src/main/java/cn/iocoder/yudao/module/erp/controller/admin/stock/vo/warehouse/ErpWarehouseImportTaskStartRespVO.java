package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 仓库异步导入任务创建 Response VO")
public class ErpWarehouseImportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
