package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 报价订单新增/修改 Request VO")
@Data
public class ErpSaleQuoteSaveReqVO {

    @Schema(description = "编号", example = "17386")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "销售员编号", example = "1888")
    private Long saleUserId;

    @Schema(description = "部门编号", example = "100")
    private Long deptId;

    @Schema(description = "报价时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报价时间不能为空")
    private LocalDateTime quoteTime;

    @Schema(description = "优惠率，百分比")
    private BigDecimal discountPercent;

    @Schema(description = "其它金额，单位：元")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址")
    private String fileUrl;

    @Schema(description = "备注")
    private String remark;

    @Valid
    @NotEmpty(message = "报价明细不能为空")
    @Schema(description = "报价清单列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "报价项编号")
        private Long id;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品单价")
        private BigDecimal productPrice;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比")
        private BigDecimal taxPercent;

        @Schema(description = "货架位")
        private String warehousePosition;
        @Schema(description = "图号")
        private String drawingNo;
        @Schema(description = "批次号")
        private String batchNo;
        @Schema(description = "条码")
        private String barCode;
        @Schema(description = "品牌")
        private String brand;
        @Schema(description = "车型")
        private String vehicleModel;
        @Schema(description = "产地")
        private String originPlace;
        @Schema(description = "规格")
        private String standard;
        @Schema(description = "备注")
        private String remark;

    }

}
