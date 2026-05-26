package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购退货 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseReturnRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "退货单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    @ExcelProperty("退货单编号")
    private String no;

    @Schema(description = "退货状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("退货状态")
    private Integer status;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "芋道")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "311.89")
    @ExcelProperty("结算账户编号")
    private Long accountId;

    @Schema(description = "退货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long orderId;
    @Schema(description = "采购订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    private String orderNo;

    @Schema(description = "退货模式", example = "10")
    @ExcelProperty(value = "退货模式")
    private Integer returnMode;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @Schema(description = "最终合计价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("最终合计价格")
    private BigDecimal totalPrice;
    @Schema(description = "已退款金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal refundPrice;

    @Schema(description = "合计产品价格，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalTaxPrice;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "优惠金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal discountPrice;

    @Schema(description = "定金金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "退货项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

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

        @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private Long orderItemId;

        @Schema(description = "原采购入库单 ID", example = "17386")
        private Long sourceInId;
        @Schema(description = "原采购入库项 ID", example = "11756")
        private Long sourceInItemId;
        @Schema(description = "原采购入库单号", example = "RKD20260101001")
        private String sourceInNo;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long warehouseId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productUnitId;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "产品数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;

        @Schema(description = "税额，单位：元", example = "100.00")
        private BigDecimal taxPrice;

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

        @Schema(description = "所属经营", example = "汽配业务")
        private String businessEntity;

        // ========== 关联字段 ==========

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "巧克力")
        private String productName;
        private String productCode;
        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;
        @Schema(description = "产品单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
        private String productUnitName;

        @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount;

        @Schema(description = "原入库数量(按单退货时回显)", example = "100.00")
        private BigDecimal inCount;

        @Schema(description = "可退数量(按单退货时回显)", example = "80.00")
        private BigDecimal returnableCount;

    }

}
