package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;

@Schema(description = "Admin - ERP warehouse user permission response")
@Data
public class ErpWarehouseUserPermissionRespVO {

    @Schema(description = "Warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "Authorized user ids", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> userIds;

    @Schema(description = "已选人员展示信息，包含历史停用账号")
    private List<AdminUserRespDTO> selectedUsers;

}
