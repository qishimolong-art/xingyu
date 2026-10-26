package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "管理后台 - ERP 供应商异步导入任务创建 Response VO")
public class ErpSupplierImportTaskStartRespVO {

    private Long taskId;
    private Boolean reused;

}
