package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - ERP user warehouse permission response")
@Data
public class ErpUserWarehousePermissionRespVO {

    @Schema(description = "User id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long userId;

    @Schema(description = "Authorized warehouse ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> warehouseIds;

}
