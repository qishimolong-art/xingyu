package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购订单分批入库 Request VO")
@Data
public class ErpPurchaseInFromOrderReqVO {

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购订单编号不能为空")
    private Long orderId;

    @Schema(description = "入库时间")
    private LocalDateTime inTime;

    @Schema(description = "结算账户编号", example = "1")
    private Long accountId;

    @Schema(description = "入库项列表")
    @NotEmpty(message = "入库项不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "采购订单项编号不能为空")
        private Long orderItemId;

        @Schema(description = "本次入库数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "50")
        @NotNull(message = "入库数量不能为空")
        private BigDecimal count;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;
        private Long deptId;

    }

}
