package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 配件信息异步导出任务创建 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpProductExportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
