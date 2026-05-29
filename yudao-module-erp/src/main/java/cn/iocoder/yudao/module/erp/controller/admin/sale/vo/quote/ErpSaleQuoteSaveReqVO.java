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

    @Schema(description = "报价时间")
    private LocalDateTime quoteTime;

    @Schema(description = "优惠率，百分比")
    private BigDecimal discountPercent;

    @Schema(description = "其它金额，单位：元")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址")
    private String fileUrl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "订单类型")
    private String orderType;

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "先款后货")
    private Boolean prepayment;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "送货方式")
    private String deliveryMethod;

    @Schema(description = "代客户发货")
    private Boolean proxyDelivery;

    @Schema(description = "收货地址")
    private String deliveryAddress;

    @Schema(description = "折让金额")
    private BigDecimal allowancePrice;

    @Schema(description = "票据号")
    private String ticketNo;

    @Schema(description = "开票类型")
    private String invoiceType;

    @Schema(description = "运费类型")
    private String freightType;

    @Schema(description = "费用金额")
    private BigDecimal freightAmount;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "收货人")
    private String receiverName;

    @Schema(description = "收货电话")
    private String receiverPhone;

    @Schema(description = "价格类型")
    private String priceType;

    @Schema(description = "分店发货")
    private String branchDelivery;

    @Schema(description = "预计发货时间")
    private LocalDateTime expectedDeliveryTime;

    @Schema(description = "开单方式")
    private String billingMethod;

    @Schema(description = "车牌号")
    private String vehiclePlateNo;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "开发员编号")
    private Long developerUserId;

    @Schema(description = "VIN车架号")
    private String vin;

    @Schema(description = "内部说明")
    private String internalRemark;

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
