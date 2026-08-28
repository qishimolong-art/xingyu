package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售出库 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSaleOutRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "出库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    @ExcelProperty("出库单编号")
    private String no;

    @Schema(description = "出库状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("出库状态")
    private Integer status;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long customerId;
    @Schema(description = "客户名称", example = "芋道")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "311.89")
    @ExcelProperty("结算账户编号")
    private Long accountId;

    @Schema(description = "出库员编号", example = "1888")
    private Long saleUserId;

    @Schema(description = "出库时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("出库时间")
    private LocalDateTime outTime;

    @Schema(description = "销售订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long orderId;
    @Schema(description = "销售订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    private String orderNo;

    @Schema(description = "业务来源类型", example = "20")
    private Integer sourceType;
    @Schema(description = "来源单据编号", example = "17386")
    private Long sourceId;
    @Schema(description = "来源单据号", example = "XSBJ20260509000001")
    private String sourceNo;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @Schema(description = "最终合计价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("最终合计价格")
    private BigDecimal totalPrice;
    @Schema(description = "已收款金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal receiptPrice;

    @Schema(description = "合计产品价格，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalProductPrice;

    @Schema(description = "合计税额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal totalTaxPrice;

    @Schema(description = "优惠率，百分比", requiredMode = Schema.RequiredMode.REQUIRED, example = "99.88")
    private BigDecimal discountPercent;

    @Schema(description = "优惠金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal discountPrice;

    @Schema(description = "费用金额，单位：元", example = "7127")
    private BigDecimal feeAmount;

    @Schema(description = "其它金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "快递单图片地址", example = "https://example.com/express.jpg")
    private String expressFileUrl;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;

    @Schema(description = "是否已被调价")
    private Boolean adjusted;

    @Schema(description = "调价源销售单编号")
    private Long adjustSourceOutId;

    @Schema(description = "调价生成的新销售单编号")
    private Long adjustNewOutId;

    @Schema(description = "关联调价单编号")
    private Long adjustPriceAdjustId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人", example = "1")
    private String updater;
    @Schema(description = "更新人名称", example = "管理员")
    private String updaterName;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "出库项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "是否存在对应出仓单")
    private Boolean hasStockOutBill;

    @Schema(description = "出仓单简要信息")
    private List<StockOutBillBrief> stockOutBills;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    // ========== 业务扩展字段 ==========

    @Schema(description = "结算状态")
    private Integer settleStatus;

    @Schema(description = "订单类型")
    private String orderType;

    @Schema(description = "额外费用")
    private BigDecimal extraFee;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "客户签收状态")
    private Integer signStatus;

    @Schema(description = "签收图片")
    private String signImageUrl;

    // ========== 物流信息 ==========

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

    // ========== 财务信息 ==========

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "开票金额")
    private BigDecimal invoiceAmount;

    @Schema(description = "减收金额")
    private BigDecimal reductionAmount;

    @Schema(description = "减后金额")
    private BigDecimal afterReductionAmount;

    @Schema(description = "票据金额")
    private BigDecimal billAmount;

    @Schema(description = "运费")
    private BigDecimal freight;

    @Schema(description = "运费类型")
    private String freightType;

    @Schema(description = "票据类型")
    private String billType;

    @Schema(description = "票据号")
    private String billNo;

    // ========== 取消信息 ==========

    @Schema(description = "取消数量")
    private BigDecimal cancelCount;

    @Schema(description = "取消金额")
    private BigDecimal cancelAmount;

    @Schema(description = "取消后金额")
    private BigDecimal afterCancelAmount;

    // ========== 人员/部门 ==========

    @Schema(description = "审核人编号")
    private Long auditorId;

    @Schema(description = "审核人名称")
    private String auditorName;

    @Schema(description = "业务员名称")
    private String saleUserName;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "客户编码")
    private String customerCode;

    // ========== 时间 ==========

    @Schema(description = "总重")
    private BigDecimal totalWeight;

    @Schema(description = "审核时间")
    private LocalDateTime approveTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "来源单制单日期")
    private LocalDateTime sourceCreateTime;

    @Schema(description = "来源单制单人")
    private String sourceCreatorName;

    // ========== 其他 ==========

    @Schema(description = "内部说明")
    private String internalNote;

    @Schema(description = "VIN")
    private String vin;

    @Schema(description = "打印次数")
    private Integer printCount;

    // ========== 退货状态（计算字段） ==========

    @Schema(description = "退货状态：0=未退, 1=部分退, 2=整退")
    private Integer returnStatus;

    @Data
    public static class StockOutBillBrief {

        @Schema(description = "出仓单编号")
        private Long id;

        @Schema(description = "出仓单号")
        private String no;

        @Schema(description = "仓库编号")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "仓库名称")
        private String warehouseName;

        @Schema(description = "出仓状态")
        private Integer status;

        @Schema(description = "出仓状态名称")
        private String statusName;

        @Schema(description = "出仓数量")
        private BigDecimal totalCount;

        @Schema(description = "已拣货数量")
        private BigDecimal pickedCount;

        @Schema(description = "单据日期")
        private LocalDateTime billDate;

    }

    @Data
    public static class Item {

        @Schema(description = "出库项编号", example = "11756")
        private Long id;

        @Schema(description = "销售订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private Long orderItemId;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "跨部门销售调拨前的来源仓库编号")
        private Long sourceWarehouseId;

        @Schema(description = "跨部门销售调拨前的来源部门编号")
        private Long sourceDeptId;

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

        @Schema(description = "gift flag")
        private Boolean giftFlag;

        @Schema(description = "备注", example = "随便")
        private String remark;

        // ========== 关联字段 ==========

        @Schema(description = "产品名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "巧克力")
        private String productName;
        @Schema(description = "产品条码", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;
        @Schema(description = "产品单位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "盒")
        private String productUnitName;

        @Schema(description = "库存数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount; // 该字段仅仅在"详情"和"编辑"时使用

        // ========== 产品扩展字段 ==========

        @Schema(description = "产品编码")
        private String productCode;

        @Schema(description = "仓库名称")
        private String warehouseName;
        private Long warehouseDeptId;
        private String warehouseDeptName;

        @Schema(description = "跨部门销售调拨前的来源仓库名称")
        private String sourceWarehouseName;

        @Schema(description = "跨部门销售调拨前的来源部门名称")
        private String sourceDeptName;

        @Schema(description = "是否为跨部门销售明细")
        private Boolean crossDept;

        @Schema(description = "部门名称")
        private String deptName;

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

        @Schema(description = "包装数")
        private Integer packageQty;

        @Schema(description = "总重")
        private BigDecimal totalWeight;

        @Schema(description = "减后价")
        private BigDecimal afterReductionPrice;

        @Schema(description = "减后金额")
        private BigDecimal afterReductionAmount;

        @Schema(description = "实际销售金额")
        private BigDecimal actualSaleAmount;

        @Schema(description = "产地")
        private String originPlace;

        @Schema(description = "供应商名称")
        private String supplierName;

        @Schema(description = "浮动前价格（调价前原价）")
        private BigDecimal originalProductPrice;

        @Schema(description = "产品金额")
        private BigDecimal totalProductPrice;

        @Schema(description = "已退数量")
        private BigDecimal returnedCount;

        @Schema(description = "是否存在对应出仓单")
        private Boolean hasStockOutBill;

        @Schema(description = "出仓单号")
        private String stockOutBillNos;

        @Schema(description = "出仓状态")
        private Integer stockOutBillStatus;

        @Schema(description = "出仓状态名称")
        private String stockOutBillStatusName;

        @Schema(description = "出仓数量")
        private BigDecimal stockOutBillCount;

        @Schema(description = "已拣货数量")
        private BigDecimal stockOutBillPickedCount;

        @Schema(description = "待拣货数量")
        private BigDecimal stockOutBillRemainCount;

    }

}
