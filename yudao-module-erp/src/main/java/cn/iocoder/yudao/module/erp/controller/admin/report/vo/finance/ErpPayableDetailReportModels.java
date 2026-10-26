package cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

public final class ErpPayableDetailReportModels {

    private ErpPayableDetailReportModels() {}

    public static final String TYPE_PURCHASE_IN = "PURCHASE_IN";
    public static final String TYPE_PURCHASE_RETURN = "PURCHASE_RETURN";
    public static final String TYPE_PURCHASE_ADJUST = "PURCHASE_ADJUST";
    public static final String TYPE_PAYABLE_ADJUST = "PAYABLE_ADJUST";
    public static final String TYPE_PAYMENT = "FINANCE_PAYMENT";

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] documentDate;
        private List<String> documentTypes;
        private Long supplierId;
        private String supplierCode;
        private String supplierType;
        private String documentNo;
        private Long creatorId;
        private String voucherNo;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getDocumentDateFrom() { return range(documentDate, 0); }
        public LocalDateTime getDocumentDateTo() { return range(documentDate, 1); }

        private static LocalDateTime range(LocalDateTime[] range, int index) {
            return range == null || range.length <= index ? null : range[index];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private String rowKey;
        private Long businessId;
        private Long supplierId;
        private Long deptId;
        private Long creatorId;
        private String documentTypeCode;
        private Integer eventOrder;

        @ExcelProperty(value = "供应商编码", index = 0)
        private String supplierCode;
        @ExcelProperty(value = "供应商", index = 1)
        private String supplierName;
        @ExcelProperty(value = "单据日期", index = 2)
        private LocalDateTime documentDate;
        @ExcelProperty(value = "单据类型", index = 3)
        private String documentType;
        @ExcelProperty(value = "业务单号", index = 4)
        private String documentNo;
        @ExcelProperty(value = "制单人", index = 5)
        private String creatorName;
        @ExcelProperty(value = "增加应付", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal increasePayable;
        @ExcelProperty(value = "付款金额", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal paymentAmount;
        @ExcelProperty(value = "上日余额", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal previousBalance;
        @ExcelProperty(value = "末期应付", index = 9, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal endingPayable;
        @ExcelProperty(value = "凭证号", index = 10)
        private String voucherNo;
        @ExcelProperty(value = "备注", index = 11)
        private String remark;
        @ExcelProperty(value = "供应商类型", index = 12)
        private String supplierType;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal increasePayable;
        private BigDecimal paymentAmount;
        private BigDecimal endingPayable;
        private Boolean amountVisible;
    }
}
