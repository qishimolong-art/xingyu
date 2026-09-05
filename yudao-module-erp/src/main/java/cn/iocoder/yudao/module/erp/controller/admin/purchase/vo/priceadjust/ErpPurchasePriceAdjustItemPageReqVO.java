package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购调价明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchasePriceAdjustItemPageReqVO extends PageParam {

    @Schema(description = "采购调价单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购调价单编号不能为空")
    private Long adjustId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, inId, inNo, inItemId, productId, productCode, productName, warehouseId, "
            + "deptId, oldPrice, newPrice, count, adjustRatio, adjustPrice, vehicleModel, standard, featureCode, "
            + "originPlace, brand, drawingNo, warehousePosition")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
