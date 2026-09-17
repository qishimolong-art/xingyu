package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购退货新增/修改 Request VO")
@Data
public class ErpPurchaseReturnSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "退货时间")
    private LocalDateTime returnTime;

    @Schema(description = "采购订单编号", example = "17386")
    private Long orderId;

    @Schema(description = "供应商编号（按库存退货时前端必传；按单退货可从原入库单带出）", example = "17386")
    private Long supplierId;

    @Schema(description = "退货模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @javax.validation.constraints.NotNull(message = "退货模式不能为空")
    private Integer returnMode;

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

    @Schema(description = "退货清单列表")
    private List<Item> items;

    // ========== 八期：扩展字段 ==========

    @Schema(description = "税率", example = "13.00")
    private BigDecimal taxRate;

    @Schema(description = "部门ID", example = "100")
    private Long deptId;

    @Schema(description = "经办人/制单人（用户ID）", example = "1")
    private Long handler;

    @Data
    public static class Item {

        @Schema(description = "退货项编号", example = "11756")
        private Long id;

        @Schema(description = "明细操作类型：insert 新增，update 修改，delete 删除", example = "update")
        private String operation;

        @Schema(description = "采购订单项编号", example = "11756")
        private Long orderItemId;

        @Schema(description = "原采购入库单 ID（按单退货时必填）", example = "17386")
        private Long sourceInId;
        @Schema(description = "原采购入库项 ID（按单退货时必填）", example = "11756")
        private Long sourceInItemId;
        @Schema(description = "原采购入库单号（冗余展示用）", example = "RKD20260101001")
        private String sourceInNo;

        @Schema(description = "来源销售退货单 ID", example = "17386")
        private Long sourceSaleReturnId;
        @Schema(description = "来源销售退货项 ID", example = "11756")
        private Long sourceSaleReturnItemId;
        @Schema(description = "来源销售退货单号", example = "XSTH20260101001")
        private String sourceSaleReturnNo;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @javax.validation.constraints.NotNull(message = "仓库编号不能为空")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @javax.validation.constraints.NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品编码", example = "P0001")
        private String productCode;

        @Schema(description = "产品名称", example = "刹车片")
        private String productName;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @javax.validation.constraints.NotNull(message = "产品单位单位不能为空")
        private Long productUnitId;

        @Schema(description = "产品单位名称", example = "件")
        private String productUnitName;

        @Schema(description = "产品重量", example = "1.00")
        private BigDecimal weight;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @javax.validation.constraints.NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 汽配扩展字段 ==========
        @Schema(description = "零件编码", example = "P001")
        private String partCode;

        @Schema(description = "零件名称", example = "刹车片")
        private String partName;

        @Schema(description = "适用车型", example = "大众-朗逸")
        private String vehicleModel;

        @Schema(description = "产地", example = "德国")
        private String originPlace;

        @Schema(description = "包装数", example = "1")
        private Integer packageQty;

        @Schema(description = "整件数", example = "10")
        private Integer wholeQty;

        @Schema(description = "仓位", example = "A-01-02")
        private String warehousePosition;

        @Schema(description = "图号", example = "DWG-001")
        private String drawingNo;

        @Schema(description = "批次", example = "B20260508")
        private String batchNo;

        @Schema(description = "条形码", example = "6901234567890")
        private String barCode;

        @Schema(description = "品牌", example = "博世")
        private String brand;

        @Schema(description = "仓库名称", example = "主仓")
        private String warehouseName;

        @Schema(description = "所属经营", example = "汽配业务")
        private String businessEntity;

    }

}
