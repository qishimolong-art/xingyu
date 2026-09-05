package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售开单明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleCartItemPageReqVO extends PageParam {

    @Schema(description = "销售开单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "销售开单编号不能为空")
    private Long cartId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, giftFlag, productCode, productId, warehouseId, deptId, count, "
            + "productPrice, totalPrice, taxPercent, taxPrice, vehicleModel, standard, productUnitName, weight, "
            + "packageQty, warehousePosition, drawingNo, batchNo, brand, originPlace, remark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
