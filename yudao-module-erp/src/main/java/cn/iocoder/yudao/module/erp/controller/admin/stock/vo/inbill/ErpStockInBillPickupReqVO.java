package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 入仓单提货 Request VO")
@Data
public class ErpStockInBillPickupReqVO {

    @Schema(description = "入仓单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入仓单编号不能为空")
    private Long id;

    @Schema(description = "提货时间")
    private LocalDateTime pickupTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "提货明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "提货明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "入仓单明细编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "入仓单明细编号不能为空")
        private Long itemId;

        @Schema(description = "本次提货数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "本次提货数量不能为空")
        @DecimalMin(value = "0.000001", message = "本次提货数量必须大于 0")
        private BigDecimal pickupCount;

    }

}
