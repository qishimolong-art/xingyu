package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售出库新增/修改 Request VO")
@Data
public class ErpSaleOutSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "销售员编号", example = "1888")
    private Long saleUserId;

    @Schema(description = "客户编号，新销售单自动生成时必填", example = "1724")
    private Long customerId;

    @Schema(description = "出库时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "出库时间不能为空")
    private LocalDateTime outTime;

    @Schema(description = "销售订单编号，旧销售订单出库时必填", example = "17386")
    private Long orderId;

    @Schema(description = "业务来源类型", example = "20")
    private Integer sourceType;
    @Schema(description = "来源单据编号", example = "17386")
    private Long sourceId;
    @Schema(description = "来源单据号", example = "XSBJ20260509000001")
    private String sourceNo;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "费用金额，单位：元", example = "7127")
    private BigDecimal feeAmount;

    @Schema(description = "其它金额，单位：元", example = "7127")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    // ========== 业务扩展字段 ==========

    @Schema(description = "结算状态")
    private Integer settleStatus;

    @Schema(description = "订单类型")
    private String orderType;

    @Schema(description = "额外费用")
    private BigDecimal extraFee;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "送货方式")
    private String deliveryMethod;

    @Schema(description = "发货方")
    private String shipper;

    @Schema(description = "收货人")
    private String receiverName;

    @Schema(description = "收货电话")
    private String receiverPhone;

    @Schema(description = "配送单号")
    private String deliveryNo;

    @Schema(description = "物流单号")
    private String logisticsNo;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "发货人")
    private String senderName;

    @Schema(description = "保险公司")
    private String insuranceCompany;

    @Schema(description = "第三方单号")
    private String thirdPartyNo;

    @Schema(description = "第三方上游单号")
    private String thirdPartyUpstreamNo;

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "运费")
    private BigDecimal freight;

    @Schema(description = "票据类型")
    private String billType;

    @Schema(description = "票据号")
    private String billNo;

    @Schema(description = "审核人编号")
    private Long auditorId;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "总重")
    private BigDecimal totalWeight;

    @Schema(description = "内部说明")
    private String internalNote;

    @Schema(description = "VIN")
    private String vin;

    @Schema(description = "出库清单列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "出库项编号", example = "11756")
        private Long id;

        @Schema(description = "销售订单项编号", example = "11756")
        private Long orderItemId;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "跨部门销售调拨前的来源仓库编号（系统生成，只读）", accessMode = Schema.AccessMode.READ_ONLY)
        private Long sourceWarehouseId;

        @Schema(description = "跨部门销售调拨前的来源部门编号（系统生成，只读）", accessMode = Schema.AccessMode.READ_ONLY)
        private Long sourceDeptId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品单位单位不能为空")
        private Long productUnitId;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;

        @Schema(description = "gift flag")
        private Boolean giftFlag;

        @Schema(description = "备注", example = "随便")
        private String remark;

        @Schema(description = "车型")
        private String vehicleModel;

        @Schema(description = "规格")
        private String standard;

        @Schema(description = "特征码")
        private String featureCode;

        @Schema(description = "品牌")
        private String brand;

        @Schema(description = "图号")
        private String drawingNo;

        @Schema(description = "批次")
        private String batchNo;

        @Schema(description = "仓位")
        private String warehousePosition;

        @Schema(description = "单重")
        private BigDecimal unitWeight;

        @Schema(description = "产地")
        private String originPlace;

        @Schema(description = "供应商名称")
        private String supplierName;

    }

}
