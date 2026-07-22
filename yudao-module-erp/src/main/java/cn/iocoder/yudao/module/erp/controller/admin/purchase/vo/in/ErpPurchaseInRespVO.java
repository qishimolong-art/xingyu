package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购入库 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "入库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    @ExcelProperty("入库单编号")
    private String no;

    @Schema(description = "入库状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @ExcelProperty("入库状态")
    private Integer status;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    private Long supplierId;
    @Schema(description = "供应商名称", example = "芋道")
    @ExcelProperty("供应商名称")
    private String supplierName;

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "311.89")
    @ExcelProperty("结算账户编号")
    private Long accountId;

    @Schema(description = "入库时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("入库时间")
    private LocalDateTime inTime;

    @Schema(description = "采购订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long orderId;
    @Schema(description = "采购订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XS001")
    private String orderNo;

    @Schema(description = "合计数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @Schema(description = "最终合计价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("最终合计价格")
    private BigDecimal totalPrice;
    @Schema(description = "已付款金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal paymentPrice;

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

    @Schema(description = "定金金额，单位：元", requiredMode = Schema.RequiredMode.REQUIRED, example = "7127")
    private BigDecimal otherPrice;

    @Schema(description = "附件地址", example = "https://www.iocoder.cn")
    @ExcelProperty("附件地址")
    private String fileUrl;

    @Schema(description = "备注", example = "你猜")
    @ExcelProperty("备注")
    private String remark;

    // ========== 汽配扩展字段 ==========

    // ========== 系统信息 ==========
    @Schema(description = "采购员", example = "张三")
    @ExcelProperty("采购员")
    private String purchaser;
    @Schema(description = "采购员名称", example = "张三")
    private String purchaserName;

    @Schema(description = "开票类型", example = "普票")
    @ExcelProperty("开票类型")
    private String invoiceType;

    @Schema(description = "运输方式", example = "快递")
    @ExcelProperty("运输方式")
    private String transportMethod;

    @Schema(description = "结算方式", example = "月结")
    @ExcelProperty("结算方式")
    private String settleMethod;

    @Schema(description = "进货区", example = "华东")
    @ExcelProperty("进货区")
    private String purchaseArea;

    @Schema(description = "记账员", example = "李四")
    @ExcelProperty("记账员")
    private String accountant;
    @Schema(description = "记账员名称", example = "李四")
    private String accountantName;

    @Schema(description = "浮动率", example = "1.00")
    @ExcelProperty("浮动率")
    private BigDecimal floatRate;

    @Schema(description = "件数", example = "0")
    @ExcelProperty("件数")
    private Integer packageCount;

    @Schema(description = "厂家单号", example = "F20260508")
    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

    @Schema(description = "开单方式", example = "正常单")
    @ExcelProperty("开单方式")
    private String orderMethod;

    // ========== 运费信息 ==========
    @Schema(description = "运费类型1", example = "到付")
    @ExcelProperty("运费类型1")
    private String freightType1;

    @Schema(description = "运费类型2", example = "预付")
    @ExcelProperty("运费类型2")
    private String freightType2;

    @Schema(description = "运费对象1", example = "供应商")
    @ExcelProperty("运费对象1")
    private String freightObject1;

    @Schema(description = "运费对象2", example = "客户")
    @ExcelProperty("运费对象2")
    private String freightObject2;

    @Schema(description = "物流公司", example = "顺丰")
    @ExcelProperty("物流公司")
    private String logisticsCompany;

    // ========== 供应商信息 ==========
    @Schema(description = "经办人", example = "王五")
    @ExcelProperty("经办人")
    private String handler;
    @Schema(description = "经办人名称", example = "王五")
    private String handlerName;

    @Schema(description = "税率", example = "13.00")
    @ExcelProperty("税率")
    private BigDecimal taxRate;

    @Schema(description = "部门", example = "100")
    @ExcelProperty("部门")
    private Long deptId;
    @Schema(description = "部门名称", example = "采购部")
    private String deptName;

    @Schema(description = "采购折让", example = "0.00")
    @ExcelProperty("采购折让")
    private BigDecimal purchaseDiscount;

    @Schema(description = "优先级", example = "正常件")
    @ExcelProperty("优先级")
    private String priority;

    @Schema(description = "卸货员", example = "赵六")
    @ExcelProperty("卸货员")
    private String unloader;

    @Schema(description = "浮动记录", example = "无")
    @ExcelProperty("浮动记录")
    private String floatRecord;

    @Schema(description = "收货单位", example = "某某汽配")
    @ExcelProperty("收货单位")
    private String receiveUnit;

    @Schema(description = "总运费1", example = "0.00")
    @ExcelProperty("总运费1")
    private BigDecimal totalFreight1;

    @Schema(description = "总运费2", example = "0.00")
    @ExcelProperty("总运费2")
    private BigDecimal totalFreight2;

    @Schema(description = "付款日期")
    @ExcelProperty("付款日期")
    private LocalDateTime paymentDate;

    @Schema(description = "是否发票", example = "true")
    @ExcelProperty("是否发票")
    private Boolean hasInvoice;

    // ========== 其他信息 ==========
    @Schema(description = "所属经营", example = "汽配业务")
    @ExcelProperty("所属经营")
    private String businessEntity;

    @Schema(description = "是否被调过价", example = "true")
    private Boolean adjusted;

    @Schema(description = "退货数量", example = "10")
    private BigDecimal returnCount;

    @Schema(description = "退货状态：0=未退货，1=部分退货，2=全部退货", example = "1")
    private Integer returnStatus;

    @Schema(description = "调拨出库数量", example = "10")
    private BigDecimal transferOutCount;

    @Schema(description = "调拨出库状态：0=未调拨，1=部分调拨，2=全部调拨", example = "1")
    private Integer transferOutStatus;

    @Schema(description = "创建人", example = "芋道")
    private String creator;
    @Schema(description = "创建人名称", example = "芋道")
    private String creatorName;
    @Schema(description = "修改人", example = "芋道")
    private String updater;
    @Schema(description = "修改人名称", example = "芋道")
    private String updaterName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "入库项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "是否存在对应入仓单")
    private Boolean hasStockInBill;

    @Schema(description = "入仓单简要信息")
    private List<StockInBillBrief> stockInBills;

    @Schema(description = "项数", example = "0")
    private Integer itemCount;

    @Schema(description = "产品信息", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Data
    public static class StockInBillBrief {

        @Schema(description = "入仓单编号")
        private Long id;

        @Schema(description = "入仓单号")
        private String no;

        @Schema(description = "仓库编号")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "仓库名称")
        private String warehouseName;

        @Schema(description = "入仓状态")
        private Integer status;

        @Schema(description = "入仓状态名称")
        private String statusName;

        @Schema(description = "入仓数量")
        private BigDecimal totalCount;

        @Schema(description = "已提货数量")
        private BigDecimal pickedCount;

        @Schema(description = "单据日期")
        private LocalDateTime billDate;

    }

    @Data
    public static class Item {

        @Schema(description = "入库项编号", example = "11756")
        private Long id;

        @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
        private Long orderItemId;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productUnitId;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "调价前原价快照（首次调价前为 null）", example = "80.00")
        private BigDecimal originalProductPrice;

        @Schema(description = "本明细是否被调过价", example = "true")
        private Boolean adjusted;

        @Schema(description = "最近一次调价单 ID", example = "1024")
        private Long adjustId;

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

        private Boolean batchNoEnabled;

        @Schema(description = "条形码", example = "6901234567890")
        private String barCode;

        @Schema(description = "品牌", example = "博世")
        private String brand;

        @Schema(description = "适用车型", example = "大众-朗逸")
        private String vehicleModel;

        @Schema(description = "产地", example = "德国")
        private String originPlace;

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
        private BigDecimal stockCount; // 该字段仅仅在“详情”和“编辑”时使用

        @Schema(description = "退货数量", example = "10")
        private BigDecimal returnCount;

        @Schema(description = "退货状态：0=未退货，1=部分退货，2=全部退货", example = "1")
        private Integer returnStatus;

        @Schema(description = "是否存在对应入仓单")
        private Boolean hasStockInBill;

        @Schema(description = "入仓单号")
        private String stockInBillNos;

        @Schema(description = "入仓状态")
        private Integer stockInBillStatus;

        @Schema(description = "入仓状态名称")
        private String stockInBillStatusName;

        @Schema(description = "入仓数量")
        private BigDecimal stockInBillCount;

        @Schema(description = "已提货数量")
        private BigDecimal stockInBillPickedCount;

        @Schema(description = "待提货数量")
        private BigDecimal stockInBillRemainCount;

    }

}
