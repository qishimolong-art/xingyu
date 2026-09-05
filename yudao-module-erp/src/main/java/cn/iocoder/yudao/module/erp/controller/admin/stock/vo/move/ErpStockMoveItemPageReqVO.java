package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 库存调拨明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockMoveItemPageReqVO extends PageParam {

    @Schema(description = "库存调拨编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "库存调拨编号不能为空")
    private Long moveId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, productCode, productId, fromWarehouseId, toWarehouseId, fromDeptId, "
            + "toDeptId, count, productPrice, totalPrice, productUnitName, weight, packageQty, totalWeight, "
            + "batchNo, fromShelf, sourceCount, remark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
