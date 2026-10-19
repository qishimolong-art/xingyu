package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/** 采购票据明细查询模型。 */
public final class ErpPurchaseInvoiceDetailReportModels {
    private ErpPurchaseInvoiceDetailReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] createDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
        private LocalDate[] invoiceDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] businessApproveDate;
        private String documentNo;
        private Long supplierId;
        private String invoiceNo;
        private String invoiceType;
        private String businessNo;
        private String productKeyword;
        private String settlementStatus;
        private BigDecimal countMin;
        private BigDecimal countMax;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private BigDecimal amountMin;
        private BigDecimal amountMax;
        private String vehicleModel;
        private String brand;
        private String originPlace;
        private String drawingNo;
        private String standard;
        private BigDecimal weightMin;
        private BigDecimal weightMax;
        private String itemRemark;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getApproveDateFrom() { return first(approveDate); }
        public LocalDateTime getApproveDateTo() { return second(approveDate); }
        public LocalDateTime getCreateDateFrom() { return first(createDate); }
        public LocalDateTime getCreateDateTo() { return second(createDate); }
        public LocalDate getInvoiceDateFrom() { return invoiceDate == null || invoiceDate.length == 0 ? null : invoiceDate[0]; }
        public LocalDate getInvoiceDateTo() { return invoiceDate == null || invoiceDate.length < 2 ? null : invoiceDate[1]; }
        public LocalDateTime getBusinessApproveDateFrom() { return first(businessApproveDate); }
        public LocalDateTime getBusinessApproveDateTo() { return second(businessApproveDate); }
        private static LocalDateTime first(LocalDateTime[] value) { return value == null || value.length == 0 ? null : value[0]; }
        private static LocalDateTime second(LocalDateTime[] value) { return value == null || value.length < 2 ? null : value[1]; }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long itemId;
        private Long invoiceId;
        private Long productId;
        private Long supplierId;
        private Integer sourceType;
        private Long sourceDocId;

        @ExcelProperty(value = "序号", index = 0) private Long sequence;
        @ExcelProperty(value = "创建日期", index = 1)
        @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime createDate;
        @ExcelProperty(value = "单号", index = 2) private String documentNo;
        @ExcelProperty(value = "供应商名称", index = 3) private String supplierName;
        @ExcelProperty(value = "开票日期", index = 4)
        @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDate invoiceDate;
        @ExcelProperty(value = "开票类型", index = 5) private String invoiceType;
        @ExcelProperty(value = "发票号码", index = 6) private String invoiceNo;
        @ExcelProperty(value = "零件编码", index = 7) private String productCode;
        @ExcelProperty(value = "零件名称", index = 8) private String productName;
        @ExcelProperty(value = "单位", index = 9) private String unitName;
        @ExcelProperty(value = "数量", index = 10) private BigDecimal count;
        @ExcelProperty(value = "含税单价", index = 11) private BigDecimal productPrice;
        @ExcelProperty(value = "含税金额", index = 12) private BigDecimal totalAmount;
        @ExcelProperty(value = "结算情况", index = 13) private String settlementStatus;
        @ExcelProperty(value = "税率", index = 14) private String taxRate;
        @ExcelProperty(value = "车型", index = 15) private String vehicleModel;
        @ExcelProperty(value = "品牌", index = 16) private String brand;
        @ExcelProperty(value = "产地", index = 17) private String originPlace;
        @ExcelProperty(value = "图号", index = 18) private String drawingNo;
        @ExcelProperty(value = "规格", index = 19) private String standard;
        @ExcelProperty(value = "体积", index = 20) private String volume;
        @ExcelProperty(value = "重量", index = 21) private BigDecimal weight;
        @ExcelProperty(value = "税收分类编码", index = 22) private String taxCategoryCode;
        @ExcelProperty(value = "税收产品名称", index = 23) private String taxProductName;
        @ExcelProperty(value = "税收产品型号", index = 24) private String taxProductModel;
        @ExcelProperty(value = "税收单位", index = 25) private String taxUnit;
        @ExcelProperty(value = "票据审核日期", index = 26)
        @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime approveDate;
        @ExcelProperty(value = "备注", index = 27) private String itemRemark;
        @ExcelProperty(value = "业务单号", index = 28) private String businessNo;
        @ExcelProperty(value = "业务审核日期", index = 29)
        @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime businessApproveDate;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalAmount;
        private Boolean priceVisible;
        private List<String> hiddenFields;
    }
}
