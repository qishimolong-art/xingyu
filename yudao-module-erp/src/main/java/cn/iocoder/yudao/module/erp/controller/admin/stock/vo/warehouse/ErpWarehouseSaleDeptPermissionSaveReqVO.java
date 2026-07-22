package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "Admin - ERP warehouse sale department permission save Request VO")
@Data
public class ErpWarehouseSaleDeptPermissionSaveReqVO {

    @Schema(description = "Warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "Warehouse id cannot be empty")
    private Long warehouseId;

    @Schema(description = "Sales department ids")
    @NotNull(message = "Department permissions cannot be empty")
    private Set<Long> deptIds;

}
