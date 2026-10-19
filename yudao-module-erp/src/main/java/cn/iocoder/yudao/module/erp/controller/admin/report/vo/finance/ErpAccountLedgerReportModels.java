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

/** 账户流水报表模型。 */
public final class ErpAccountLedgerReportModels {

    public static final String TYPE_RECEIPT = "receipt";
    public static final String TYPE_PAYMENT = "payment";
    public static final String TYPE_OTHER_INCOME = "other_income";
    public static final String TYPE_EXPENSE = "expense";
    public static final String TYPE_TRANSFER = "transfer";
    public static final String DIRECTION_DEBIT = "debit";
    public static final String DIRECTION_CREDIT = "credit";

    private ErpAccountLedgerReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] businessDate;
        private String partyName;
        private String partyCode;
        private Long accountId;
        private String businessNo;
        private List<String> documentTypes;
        private String direction;
        private Long receiverId;
        private Long payerId;
        private String remark;
        private String paymentVoucherNo;
        private String voucherNo;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getBusinessDateFrom() {
            return range(businessDate, 0);
        }

        public LocalDateTime getBusinessDateTo() {
            return range(businessDate, 1);
        }

        private static LocalDateTime range(LocalDateTime[] range, int index) {
            return range == null || range.length <= index ? null : range[index];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private String rowKey;
        private String sourceType;
        private Long sourceId;
        private Integer sourceRank;
        private Integer legRank;
        private Long accountId;
        private Long receiverId;
        private Long payerId;

        @ExcelProperty(value = "交易单位", index = 0)
        private String partyName;
        @ExcelProperty(value = "交易单位编码", index = 1)
        private String partyCode;
        @ExcelProperty(value = "账户", index = 2)
        private String accountName;
        @ExcelProperty(value = "业务日期", index = 3)
        private String businessDate;
        @ExcelProperty(value = "业务单号", index = 4)
        private String businessNo;
        @ExcelProperty(value = "单据类型", index = 5)
        private String documentType;
        @ExcelProperty(value = "方向", index = 6)
        private String direction;
        @ExcelProperty(value = "收入金额", index = 7)
        private BigDecimal incomeAmount;
        @ExcelProperty(value = "支出金额", index = 8)
        private BigDecimal expenseAmount;
        @ExcelProperty(value = "余额", index = 9)
        private BigDecimal balance;
        @ExcelProperty(value = "收款人", index = 10)
        private String receiverName;
        @ExcelProperty(value = "付款人", index = 11)
        private String payerName;
        @ExcelProperty(value = "备注", index = 12)
        private String remark;
        @ExcelProperty(value = "凭证号", index = 13)
        private String paymentVoucherNo;
        @ExcelProperty(value = "会计凭证号", index = 14)
        private String voucherNo;
        @ExcelProperty(value = "在线支付订单号", index = 15)
        private String onlinePaymentOrderNo;
        @ExcelProperty(value = "在线支付凭证号", index = 16)
        private String onlinePaymentVoucherNo;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal incomeAmount;
        private BigDecimal expenseAmount;
        private BigDecimal netAmount;
        private Boolean amountVisible;
    }
}
