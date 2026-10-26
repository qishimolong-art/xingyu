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

public final class ErpPaymentDetailReportModels {

    private ErpPaymentDetailReportModels() {}

    public static final int TYPE_PURCHASE_IN = 11;
    public static final int TYPE_PURCHASE_RETURN = 12;
    public static final int TYPE_PURCHASE_PRICE_ADJUST = 13;
    public static final int TYPE_PAYABLE_MISC = 14;

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] paymentDate;
        private Long supplierId;
        private String paymentNo;
        private String businessNo;
        private List<Integer> businessTypes;
        private Long accountId;
        private Long payerId;
        private Long sourceCreatorId;
        private String supplierType;
        private Integer paymentMethod;
        private String settlementMethod;
        private Long deptId;
        private String paymentVoucherNo;
        private String voucherNo;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getPaymentDateFrom() { return range(paymentDate, 0); }
        public LocalDateTime getPaymentDateTo() { return range(paymentDate, 1); }

        private static LocalDateTime range(LocalDateTime[] values, int index) {
            return values == null || values.length <= index ? null : values[index];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long paymentId;
        private Long paymentItemId;
        private Long businessId;
        private Long supplierId;
        private Long accountId;
        private Long payerId;
        private Long sourceCreatorId;
        private Long deptId;
        private Integer businessTypeCode;

        @ExcelProperty(value = "供应商", index = 0)
        private String supplierName;
        @ExcelProperty(value = "付款日期", index = 1)
        private LocalDateTime paymentDate;
        @ExcelProperty(value = "付款单号", index = 2)
        private String paymentNo;
        @ExcelProperty(value = "业务单号", index = 3)
        private String businessNo;
        @ExcelProperty(value = "业务类型", index = 4)
        private String businessType;
        @ExcelProperty(value = "结算金额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal settlementAmount;
        @ExcelProperty(value = "实付金额", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal actualPaymentAmount;
        @ExcelProperty(value = "付款账户", index = 7)
        private String accountName;
        @ExcelProperty(value = "凭证号", index = 8)
        private String paymentVoucherNo;
        @ExcelProperty(value = "会计凭证号", index = 9)
        private String voucherNo;
        @ExcelProperty(value = "付款人", index = 10)
        private String payerName;
        @ExcelProperty(value = "采购员", index = 11)
        private String purchaserName;
        @ExcelProperty(value = "供应商类型", index = 12)
        private String supplierType;
        @ExcelProperty(value = "付款方式", index = 13)
        private String paymentMethod;
        @ExcelProperty(value = "制单人", index = 14)
        private String creatorName;
        @ExcelProperty(value = "结算方式", index = 15)
        private String settlementMethod;
        @ExcelProperty(value = "部门", index = 16)
        private String deptName;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal settlementAmount;
        private BigDecimal actualPaymentAmount;
        private Boolean amountVisible;
    }
}
