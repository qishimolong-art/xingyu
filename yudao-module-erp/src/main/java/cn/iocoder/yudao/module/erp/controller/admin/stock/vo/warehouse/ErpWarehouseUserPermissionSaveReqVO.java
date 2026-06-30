package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "Admin - ERP warehouse user permission save request")
@Data
public class ErpWarehouseUserPermissionSaveReqVO {

    @Schema(description = "Warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "Warehouse id cannot be empty")
    private Long warehouseId;

    @Schema(description = "Authorized user ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> userIds;

}
