package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - ERP create stock transfer-out from purchase in Request VO")
@Data
public class ErpPurchaseInCreateTransferOutReqVO {

    @Schema(description = "Source purchase in id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "来源采购入库单不能为空")
    private Long sourceInId;

    @Schema(description = "Move time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调拨日期不能为空")
    private LocalDateTime moveTime;

    @Schema(description = "To department id", example = "101")
    private Long toDeptId;

    @Schema(description = "Default to warehouse id", example = "1001")
    private Long toWarehouseId;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Transfer-out items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "调拨明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "Source purchase in item id", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "来源采购入库明细不能为空")
        private Long sourceInItemId;

        @Schema(description = "Transfer-in warehouse id", example = "1001")
        private Long toWarehouseId;

        @Schema(description = "Transfer-out count", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
        @NotNull(message = "本次调拨数量不能为空")
        private BigDecimal count;

        @Schema(description = "Remark")
        private String remark;

    }

}
