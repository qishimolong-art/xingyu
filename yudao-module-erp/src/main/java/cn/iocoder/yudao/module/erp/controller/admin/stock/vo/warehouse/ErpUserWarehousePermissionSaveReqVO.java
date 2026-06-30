package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Admin - ERP user warehouse permission save request")
@Data
public class ErpUserWarehousePermissionSaveReqVO {

    @Schema(description = "User id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "User id cannot be empty")
    private Long userId;

    @Schema(description = "Authorized warehouse ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> warehouseIds;

}
