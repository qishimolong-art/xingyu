package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "Admin - ERP purchase in sale-cartable item page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInSaleCartableItemPageReqVO extends PageParam {

    @Schema(description = "Purchase in id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购入库编号不能为空")
    private Long inId;

    @Schema(description = "Order field, supports: id, productCode, productName, warehouseId, count, productPrice, "
            + "warehousePosition, batchNo, brand, remark")
    private String orderField;

    @Schema(description = "Order direction: asc or desc")
    private String orderDirection;

}
