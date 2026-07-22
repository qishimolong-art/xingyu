package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - ERP create sale cart from purchase in Request VO")
@Data
public class ErpPurchaseInCreateSaleCartReqVO {

    @Schema(description = "Source purchase in id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "来源采购入库单不能为空")
    private Long sourceInId;

    @Schema(description = "Customer id", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "Sale department id", example = "101")
    private Long deptId;

    @Schema(description = "Cart time")
    private LocalDateTime cartTime;

    @Schema(description = "Sale user id")
    private Long saleUserId;

    @Schema(description = "Account id")
    private Long accountId;

    @Schema(description = "Settle method")
    private String settleMethod;

    @Schema(description = "Invoice type")
    private String invoiceType;

    @Schema(description = "Delivery method")
    private String deliveryMethod;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Sale cart items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "手推车明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "Source purchase in item id", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
        @NotNull(message = "来源采购入库明细不能为空")
        private Long sourceInItemId;

        @Schema(description = "Sale department id")
        private Long deptId;

        @Schema(description = "Sale warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
        @NotNull(message = "销售仓库不能为空")
        private Long warehouseId;

        @Schema(description = "Convert count", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
        @NotNull(message = "本次数量不能为空")
        private BigDecimal count;

        @Schema(description = "Sale price", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
        @NotNull(message = "销售价不能为空")
        private BigDecimal productPrice;

        @Schema(description = "Gift flag", example = "false")
        private Boolean giftFlag;

        @Schema(description = "Batch no")
        private String batchNo;

        @Schema(description = "Remark")
        private String remark;

    }

}
