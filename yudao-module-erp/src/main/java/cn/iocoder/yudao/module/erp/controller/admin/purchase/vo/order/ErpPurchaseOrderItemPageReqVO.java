package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购订单明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseOrderItemPageReqVO extends PageParam {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, gift, productCode, productId, warehouseId, deptId, count, productPrice, "
            + "lastPurchasePrice, totalProductPrice, vehicleModel, standard, featureCode, productUnitName, weight, "
            + "packageQty, arrivalCount, warehousePosition, drawingNo, batchNo, factoryCode, brand, remark, inStatus")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
