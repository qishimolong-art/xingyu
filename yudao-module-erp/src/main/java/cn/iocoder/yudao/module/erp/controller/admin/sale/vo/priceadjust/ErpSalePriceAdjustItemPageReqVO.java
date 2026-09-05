package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售调价明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSalePriceAdjustItemPageReqVO extends PageParam {

    @Schema(description = "销售调价编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "销售调价编号不能为空")
    private Long adjustId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, saleOutNo, partCode, partName, vehicleModel, originPlace, brand, unit, "
            + "weight, packageQty, outCount, oldPrice, newPrice, adjustPrice, productId, deptId, saleOutItemId, "
            + "saleOutId, adjustReason, itemRemark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
