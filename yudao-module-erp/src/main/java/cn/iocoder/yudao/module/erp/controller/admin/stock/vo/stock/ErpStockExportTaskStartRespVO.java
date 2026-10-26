package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 产品库存异步导出任务创建 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockExportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
