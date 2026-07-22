package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Schema(description = "Admin - ERP warehouse sale department permission Response VO")
@Data
public class ErpWarehouseSaleDeptPermissionRespVO {

    @Schema(description = "Warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long warehouseId;

    @Schema(description = "Sales department ids")
    private Set<Long> deptIds;

}
