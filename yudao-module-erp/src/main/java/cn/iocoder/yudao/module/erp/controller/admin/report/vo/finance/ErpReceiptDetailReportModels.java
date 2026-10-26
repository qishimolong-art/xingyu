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

public final class ErpReceiptDetailReportModels {

    private ErpReceiptDetailReportModels() {}

    public static final String TYPE_SALE_OUT = "SALE_OUT";
    public static final String TYPE_SALE_RETURN = "SALE_RETURN";
    public static final String TYPE_SALE_PRICE_ADJUST = "SALE_PRICE_ADJUST";
    public static final String TYPE_RECEIVABLE_MISC = "RECEIVABLE_MISC";

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] receiptDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] businessDate;
        private Long customerId;
        private String receiptNo;
        private String businessNo;
        private List<String> businessTypes;
        private String settleMethod;
        private Integer accountType;
        private Long accountId;
        private Long receiptUserId;
        private Long businessUserId;
        private Long businessCreatorId;
        private Long customerTypeId;
        private Long deptId;
        private String paymentVoucherNo;
        private String voucherNo;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getReceiptDateFrom() { return range(receiptDate, 0); }
        public LocalDateTime getReceiptDateTo() { return range(receiptDate, 1); }
        public LocalDateTime getBusinessDateFrom() { return range(businessDate, 0); }
        public LocalDateTime getBusinessDateTo() { return range(businessDate, 1); }

        private static LocalDateTime range(LocalDateTime[] values, int index) {
            return values == null || values.length <= index ? null : values[index];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private String rowKey;
        private Long receiptId;
        private Long receiptItemId;
        private Long businessId;
        private Long customerId;
        private Long accountId;
        private Long deptId;
        private Long receiptUserId;
        private Long businessUserId;
        private Long businessCreatorId;
        private Long customerTypeId;
        private String businessTypeCode;

        @ExcelProperty(value = "客户", index = 0)
        private String customerName;
        @ExcelProperty(value = "收款日期", index = 1)
        private LocalDateTime receiptDate;
        @ExcelProperty(value = "收款单号", index = 2)
        private String receiptNo;
        @ExcelProperty(value = "业务单号", index = 3)
        private String businessNo;
        @ExcelProperty(value = "业务类型", index = 4)
        private String businessType;
        @ExcelProperty(value = "结算金额", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal settlementAmount;
        @ExcelProperty(value = "实收金额", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal actualReceiptAmount;
        @ExcelProperty(value = "结算方式", index = 7)
        private String settleMethod;
        @ExcelProperty(value = "账户类型", index = 8)
        private String accountType;
        @ExcelProperty(value = "收款账户", index = 9)
        private String accountName;
        @ExcelProperty(value = "凭证号", index = 10)
        private String paymentVoucherNo;
        @ExcelProperty(value = "会计凭证号", index = 11)
        private String voucherNo;
        @ExcelProperty(value = "收款人", index = 12)
        private String receiptUserName;
        @ExcelProperty(value = "业务员", index = 13)
        private String businessUserName;
        @ExcelProperty(value = "开发员", index = 14)
        private String developerName;
        @ExcelProperty(value = "制单人", index = 15)
        private String businessCreatorName;
        @ExcelProperty(value = "业务日期", index = 16)
        private LocalDateTime businessDate;
        @ExcelProperty(value = "成本金额", index = 17, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costAmount;
        @ExcelProperty(value = "毛利", index = 18, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal grossProfit;
        @ExcelProperty(value = "毛利率", index = 19)
        private BigDecimal grossProfitRate;
        @ExcelProperty(value = "客户类型", index = 20)
        private String customerType;
        @ExcelProperty(value = "部门", index = 21)
        private String deptName;
        @ExcelProperty(value = "备注", index = 22)
        private String remark;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal settlementAmount;
        private BigDecimal actualReceiptAmount;
        private Boolean amountVisible;
    }
}
