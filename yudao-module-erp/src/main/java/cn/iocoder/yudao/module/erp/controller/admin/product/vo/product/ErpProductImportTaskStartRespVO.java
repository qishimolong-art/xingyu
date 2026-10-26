package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 配件异步导入任务创建 Response VO")
public class ErpProductImportTaskStartRespVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long taskId;

    @Schema(description = "是否复用了租户内已有任务", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean reused;

}
