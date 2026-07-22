package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购订单新增/修改 Request VO")
@Data
public class ErpPurchaseOrderSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "采购时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "采购时间不能为空")
    private LocalDateTime orderTime;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "费用金额，单位：元", example = "7127")
    private BigDecimal feeAmount;

    @Schema(description = "定金金额，单位：元", example = "7127")
    private BigDecimal depositPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "订单清单列表")
    private List<Item> items;

    // ========== 汽配扩展字段 ==========

    @Schema(description = "到货日期")
    private LocalDateTime arrivalDate;

    @Schema(description = "送货方式", example = "物流配送")
    private String deliveryMethod;

    @Schema(description = "采购方式", example = "常规采购")
    private String purchaseType;

    @Schema(description = "订货公式", example = "公式1")
    private String orderFormula;

    @Schema(description = "发出日期")
    private LocalDateTime sendDate;

    @Schema(description = "最近到货日期")
    private LocalDateTime latestArrivalDate;

    @Schema(description = "销售日期从")
    private LocalDateTime saleDateFrom;

    @Schema(description = "到销售日期")
    private LocalDateTime saleDateTo;

    @Schema(description = "厂家单号", example = "F20240101")
    private String factoryOrderNo;

    @Schema(description = "收货地址", example = "XX仓库")
    private String receiveAddress;

    @Schema(description = "开票类型", example = "增值税专用发票")
    private String invoiceType;

    @Schema(description = "结算方式", example = "月结")
    private String settleMethod;

    // ========== 新增字段 ==========

    @Schema(description = "采购员（用户ID）", example = "1")
    private Long purchaser;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "订货日期")
    private LocalDateTime orderDate;

    @Schema(description = "采购周期(天)", example = "7")
    private Integer purchaseCycle;

    @Schema(description = "订货公司", example = "XX公司")
    private String orderCompany;

    @Schema(description = "税率(%)", example = "13.00")
    private BigDecimal taxPercent;

    // ========== 八期：单据类型 + 最近订货日期 ==========

    @Schema(description = "单据类型", example = "正常采购单")
    private String documentType;

    @Schema(description = "最近订货日期（只读）")
    private LocalDateTime latestOrderDate;

    @Data
    public static class Item {

        @Schema(description = "订单项编号", example = "11756")
        private Long id;

        @Schema(description = "产品编码", example = "P0001")
        private String productCode;

        @Schema(description = "产品单位名称", example = "件")
        private String productUnitName;

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

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 汽配扩展字段 ==========

        @Schema(description = "仓库编号", example = "1")
        private Long warehouseId;

        @Schema(description = "所属部门", example = "100")
        private Long deptId;

        @Schema(description = "货架位", example = "A-01-02")
        private String warehousePosition;

        @Schema(description = "适用车型", example = "大众帕萨特")
        private String vehicleModel;

        @Schema(description = "产地", example = "广东")
        private String originPlace;

        @Schema(description = "规格", example = "200mm")
        private String standard;

        @Schema(description = "特征码", example = "TC001")
        private String featureCode;

        @Schema(description = "图号", example = "DWG-001")
        private String drawingNo;

        @Schema(description = "批次", example = "20240101")
        private String batchNo;

        @Schema(description = "厂家编码", example = "FC001")
        private String factoryCode;

        @Schema(description = "品牌", example = "博世")
        private String brand;

        @Schema(description = "默认供应商编号", example = "1")
        private Long supplierId;

        @Schema(description = "是否赠品", example = "false")
        private Boolean gift;

    }

}
