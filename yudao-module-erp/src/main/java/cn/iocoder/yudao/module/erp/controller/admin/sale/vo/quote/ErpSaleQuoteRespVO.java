package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 报价订单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSaleQuoteRespVO {

    @Schema(description = "编号")
    @ExcelProperty("编号")
    private Long id;
    @Schema(description = "报价单号")
    @ExcelProperty("报价单号")
    private String no;
    @Schema(description = "状态")
    @ExcelProperty("状态")
    private Integer status;
    @Schema(description = "客户编号")
    private Long customerId;
    @Schema(description = "客户名称")
    @ExcelProperty("客户名称")
    private String customerName;
    @Schema(description = "客户编码")
    private String customerCode;
    private Long accountId;
    private Long saleUserId;
    @Schema(description = "业务员名称")
    private String saleUserName;
    private Long deptId;
    @Schema(description = "部门名称")
    private String deptName;
    @Schema(description = "报价时间")
    @ExcelProperty("报价时间")
    private LocalDateTime quoteTime;
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;
    private BigDecimal totalProductPrice;
    private BigDecimal totalTaxPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountPrice;
    private BigDecimal feeAmount;
    private BigDecimal otherPrice;
    private Integer sourceType;
    private Long sourceId;
    private String sourceNo;
    private String fileUrl;
    @ExcelProperty("备注")
    private String remark;
    private String orderType;
    private String settleMethod;
    private Boolean prepayment;
    private String priority;
    private String deliveryMethod;
    private Boolean proxyDelivery;
    private String deliveryAddress;
    private BigDecimal allowancePrice;
    private String ticketNo;
    private String invoiceType;
    private String freightType;
    private BigDecimal freightAmount;
    private String logisticsCompany;
    private String receiverName;
    private String receiverPhone;
    private String priceType;
    private String branchDelivery;
    private LocalDateTime expectedDeliveryTime;
    private String billingMethod;
    private String vehiclePlateNo;
    private String businessType;
    private Long developerUserId;
    private String vin;
    private String internalRemark;
    @Schema(description = "生成的销售单号")
    private String generatedSaleOutNo;
    private String creator;
    private String creatorName;
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    private String updater;
    private String updaterName;
    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;
    private List<Item> items;
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
        @ExcelProperty("数量")
        private BigDecimal count;
        @ExcelProperty("是否为赠品")
        private Boolean giftFlag;
        @ExcelProperty("已转数量")
        private BigDecimal convertedCount;
        @ExcelProperty("单价")
        private BigDecimal productPrice;
        private BigDecimal salePrice;
        private BigDecimal lastSalePrice;
        @ExcelProperty("金额")
        private BigDecimal totalPrice;
        @ExcelProperty("税率")
        private BigDecimal taxPercent;
        @ExcelProperty("税额")
        private BigDecimal taxPrice;
        @ExcelProperty("品牌")
        private String brand;
        @ExcelProperty("车型")
        private String vehicleModel;
        @ExcelProperty("规格")
        private String standard;
        @ExcelProperty("产地")
        private String originPlace;
        @ExcelProperty("仓位")
        private String warehousePosition;
        @ExcelProperty("备注")
        private String remark;
        private Long id;
        private Long productId;
        private Long productUnitId;
        private Long warehouseId;
        private String warehouseName;
        private Long warehouseDeptId;
        private String warehouseDeptName;
        private Long deptId;
        private String deptName;
        private String drawingNo;
        private String batchNo;
        private Boolean batchNoEnabled;
        private String barCode;
        private String productBarCode;
    }

}
