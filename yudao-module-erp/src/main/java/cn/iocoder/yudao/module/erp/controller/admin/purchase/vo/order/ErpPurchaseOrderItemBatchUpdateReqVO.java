package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购订单明细批量修改 Request VO")
@Data
public class ErpPurchaseOrderItemBatchUpdateReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "采购订单明细编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "采购订单明细不能为空")
    private List<Long> itemIds;

    @Schema(description = "目标仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "目标部门编号", example = "100")
    private Long deptId;

}
