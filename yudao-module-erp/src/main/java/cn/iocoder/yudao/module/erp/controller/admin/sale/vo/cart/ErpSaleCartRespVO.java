package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleCartRespVO {

    @ExcelProperty("编号")
    private Long id;
    @ExcelProperty("手推车单号")
    private String no;
    @ExcelProperty("状态")
    private Integer status;
    private Boolean firstApproveRequired;
    private Long customerId;
    @ExcelProperty("客户名称")
    private String customerName;
    private String customerCode;
    private Long accountId;
    private Long saleUserId;
    private String saleUserName;
    private Long deptId;
    private String deptName;
    @ExcelProperty("开单时间")
    private LocalDateTime cartTime;
    private Long firstAuditUserId;
    private LocalDateTime firstAuditTime;
    private Long finalAuditUserId;
    private LocalDateTime finalAuditTime;
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
    private String quoteNo;
    private String vehiclePlateNo;
    private String creator;
    private String updater;
    private String fileUrl;
    @ExcelProperty("备注")
    private String remark;
    private String creatorName;
    private String updaterName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ========== 扩展字段 ==========
    private String businessType;
    private String orderType;
    private String billingMethod;
    private String settleMethod;
    private String invoiceType;
    private String deliveryMethod;
    private String freightType;
    private String vin;
    private String priority;
    private String priceType;
    private String logisticsCompany;
    private Long developerUserId;
    private String contactPerson;
    private String contactPhone;
    private String deliveryAddress;
    private LocalDateTime deliveryDate;
    private BigDecimal taxRate;
    private BigDecimal totalFreight;
    private LocalDateTime paymentDate;
    private String businessEntity;
    private String orderMethod;
    private String sourceType2;
    private String remark2;

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
        @ExcelProperty("重量")
        private BigDecimal weight;
        @ExcelProperty("包装数")
        private Integer packageQty;
        @ExcelProperty("仓库名称")
        private String warehouseName;
        @ExcelProperty("占用数量")
        private BigDecimal lockCount;
        @ExcelProperty("数量")
        private BigDecimal count;
        @ExcelProperty("是否为赠品")
        private Boolean giftFlag;
        @ExcelProperty("单价")
        private BigDecimal productPrice;
        @ExcelProperty("金额")
        private BigDecimal totalPrice;
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
        private Long warehouseDeptId;
        private String warehouseDeptName;
        private Long deptId;
        private BigDecimal taxPercent;
        private BigDecimal taxPrice;
        private BigDecimal stockCount;
        private String drawingNo;
        private String batchNo;
        private Boolean batchNoEnabled;
        private String barCode;
        private String productBarCode;
    }

}
