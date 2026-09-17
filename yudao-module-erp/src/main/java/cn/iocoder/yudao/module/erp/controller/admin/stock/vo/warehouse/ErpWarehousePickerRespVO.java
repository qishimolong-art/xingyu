package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "Admin - ERP warehouse picker response")
@Data
public class ErpWarehousePickerRespVO {

    @Schema(description = "Warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "Picker user ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> userIds;

}
