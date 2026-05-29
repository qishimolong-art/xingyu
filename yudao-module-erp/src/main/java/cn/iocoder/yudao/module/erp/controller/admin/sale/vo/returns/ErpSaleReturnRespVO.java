package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售退货 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSaleReturnRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "退货单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    @ExcelProperty("退货单编号")
    private String no;

    @Schema(description = "退货状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("退货状态")
    private Integer status;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long customerId;

    @Schema(description = "客户名称", example = "芋道")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "311")
    @ExcelProperty("结算账户编号")
    private Long accountId;

    @Schema(description = "退货员编号", example = "1888")
    private Long saleUserId;

    @Schema(description = "退货模式", example = "0")
    private Integer returnMode;

    @Schema(description = "退货时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @Schema(description = "销售订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long orderId;

    @Schema(description = "销售订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    private String orderNo;

    @Schema(description = "来源销售单编号")
    private Long sourceOutId;

    @Schema(description = "来源销售单号")
    private String sourceOutNo;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @Schema(description = "最终合计价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("最终合计价格")
    private BigDecimal totalPrice;

    @Schema(description = "已退款金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal refundPrice;

    @Schema(description = "合计产品价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalTaxPrice;

    @Schema(description = "优惠率", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "优惠金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal discountPrice;

    @Schema(description = "其他金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "优先级别", example = "正常件")
    private String priority;

    @Schema(description = "开票类型", example = "收据")
    private String invoiceType;

    @Schema(description = "票据号", example = "PJ20260513001")
    private String billNo;

    @Schema(description = "退货方式", example = "客户自提")
    private String deliveryMethod;

    @Schema(description = "减收金额", example = "0")
    private BigDecimal reductionAmount;

    @Schema(description = "运费类型", example = "我方自付")
    private String freightType;

    @Schema(description = "运费金额", example = "0")
    private BigDecimal freightAmount;

    @Schema(description = "结算方式", example = "挂账")
    private String settleMethod;

    @Schema(description = "物流公司", example = "顺丰")
    private String logisticsCompany;

    @Schema(description = "车牌号", example = "粤A12345")
    private String vehicleNo;

    @Schema(description = "货到分店", example = "总店")
    private String branchStore;

    @Schema(description = "货到分店启用", example = "false")
    private Boolean branchStoreEnabled;

    @Schema(description = "进货区", example = "进货A区")
    private String purchaseArea;

    @Schema(description = "业务类型", example = "普通销售")
    private String businessType;

    @Schema(description = "开单方式", example = "正常单")
    private String orderMethod;

    @Schema(description = "开发员编号", example = "1")
    private Long developerUserId;

    @Schema(description = "创建人", example = "1")
    private String creator;

    @Schema(description = "创建人名称", example = "管理员")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "1")
    private String updater;

    @Schema(description = "更新人名称", example = "管理员")
    private String updaterName;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "退货项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Data
    public static class Item {

        @ExcelProperty("产品编码")
        private String productCode;

        @ExcelProperty("产品名称")
        private String productName;

        @ExcelProperty("产品单位")
        private String productUnitName;

        private String warehouseName;

        @ExcelProperty("仓库编号")
        private Long warehouseId;

        @ExcelProperty("退货数量")
        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        @ExcelProperty("退货单价")
        private BigDecimal productPrice;

        @ExcelProperty("退货原因")
        private String returnReason;

        @ExcelProperty("库位")
        private String warehousePosition;

        @ExcelProperty("备注")
        private String remark;

        @Schema(description = "退货项编号", example = "11756")
        private Long id;

        @Schema(description = "销售订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private Long orderItemId;

        @Schema(description = "来源销售单项编号")
        private Long sourceOutItemId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品单位编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productUnitId;

        @Schema(description = "税率", example = "99.88")
        private BigDecimal taxPercent;

        @Schema(description = "税额", example = "100.00")
        private BigDecimal taxPrice;

        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;

        @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount; // 该字段仅在详情和编辑时使用
    }

}
