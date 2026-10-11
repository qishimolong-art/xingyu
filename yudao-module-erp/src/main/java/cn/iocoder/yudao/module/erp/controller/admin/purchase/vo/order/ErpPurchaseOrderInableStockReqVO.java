package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购订单可入库明细库存批量查询 Request VO")
@Data
public class ErpPurchaseOrderInableStockReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "待查询明细")
    @Valid
    @NotEmpty(message = "待查询明细不能为空")
    @Size(max = 200, message = "单次最多查询 200 条明细")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "采购订单项编号不能为空")
        private Long orderItemId;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;

    }

}
