package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购入库新增/修改 Request VO")
@Data
public class ErpPurchaseInSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    private Long id;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1724")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "入库时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入库时间不能为空")
    private LocalDateTime inTime;

    @Schema(description = "采购订单编号", example = "17386")
    private Long orderId;

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

    // ========== 汽配扩展字段 ==========

    // ========== 系统信息 ==========
    @Schema(description = "采购员", example = "张三")
    private String purchaser;

    @Schema(description = "开票类型", example = "普票")
    private String invoiceType;

    @Schema(description = "运输方式", example = "快递")
    private String transportMethod;

    @Schema(description = "结算方式", example = "月结")
    private String settleMethod;

    @Schema(description = "进货区", example = "华东")
    private String purchaseArea;

    @Schema(description = "记账员", example = "李四")
    private String accountant;

    @Schema(description = "浮动率", example = "1.00")
    private BigDecimal floatRate;

    @Schema(description = "件数", example = "0")
    private Integer packageCount;

    @Schema(description = "厂家单号", example = "F20260508")
    private String factoryOrderNo;

    @Schema(description = "开单方式", example = "正常单")
    private String orderMethod;

    // ========== 运费信息 ==========
    @Schema(description = "运费类型1", example = "到付")
    private String freightType1;

    @Schema(description = "运费类型2", example = "预付")
    private String freightType2;

    @Schema(description = "运费对象1", example = "供应商")
    private String freightObject1;

    @Schema(description = "运费对象2", example = "客户")
    private String freightObject2;

    @Schema(description = "物流公司", example = "顺丰")
    private String logisticsCompany;

    // ========== 供应商信息 ==========
    @Schema(description = "经办人", example = "王五")
    private String handler;

    @Schema(description = "税率", example = "13.00")
    private BigDecimal taxRate;

    @Schema(description = "部门 ID", example = "100")
    private Long deptId;

    @Schema(description = "采购折让", example = "0.00")
    private BigDecimal purchaseDiscount;

    @Schema(description = "优先级", example = "正常件")
    private String priority;

    @Schema(description = "卸货员", example = "赵六")
    private String unloader;

    @Schema(description = "浮动记录", example = "无")
    private String floatRecord;

    @Schema(description = "收货单位", example = "某某汽配")
    private String receiveUnit;

    @Schema(description = "总运费1", example = "0.00")
    private BigDecimal totalFreight1;

    @Schema(description = "总运费2", example = "0.00")
    private BigDecimal totalFreight2;

    @Schema(description = "付款日期")
    private LocalDateTime paymentDate;

    @Schema(description = "是否发票", example = "true")
    private Boolean hasInvoice;

    // ========== 其他信息 ==========
    @Schema(description = "所属经营", example = "汽配业务")
    private String businessEntity;

    @Schema(description = "入库清单列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "入库项编号", example = "11756")
        private Long id;

        @Schema(description = "采购订单项编号", example = "11756")
        private Long orderItemId;

        @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;
        private Long deptId;

        @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品编号不能为空")
        private Long productId;

        @Schema(description = "产品编码", example = "P0001")
        private String productCode;

        @Schema(description = "产品名称", example = "刹车片")
        private String productName;

        @Schema(description = "产品单位单位", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "产品单位单位不能为空")
        private Long productUnitId;

        @Schema(description = "产品单位名称", example = "件")
        private String productUnitName;

        @Schema(description = "产品单价", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "是否赠品", example = "false")
        private Boolean gift;

        @Schema(description = "产品数量", example = "100.00")
        private BigDecimal count;

        @Schema(description = "税率，百分比", example = "99.88")
        private BigDecimal taxPercent;

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

        @Schema(description = "仓库名称", example = "主仓")
        private String warehouseName;

    }

}
