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

public final class ErpReceivableDetailReportModels {

    private ErpReceivableDetailReportModels() {}

    public static final String TYPE_SALE_OUT = "SALE_OUT";
    public static final String TYPE_SALE_RETURN = "SALE_RETURN";
    public static final String TYPE_SALE_ADJUST = "SALE_ADJUST";
    public static final String TYPE_RECEIPT = "FINANCE_RECEIPT";
    public static final String TYPE_RECEIVABLE_ADJUST = "RECEIVABLE_ADJUST";

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] businessDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveDate;
        private List<String> documentTypes;
        private Long customerId;
        private String customerCode;
        private String settleMethod;
        private String deliveryMethod;
        private String documentNo;
        private String sourceNo;
        private Long creatorId;
        private Long updaterId;
        private Long businessUserId;
        private String voucherNo;
        private String settledStatus;
        private Long deptId;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getBusinessDateFrom() { return range(businessDate, 0); }
        public LocalDateTime getBusinessDateTo() { return range(businessDate, 1); }
        public LocalDateTime getApproveDateFrom() { return range(approveDate, 0); }
        public LocalDateTime getApproveDateTo() { return range(approveDate, 1); }
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
        private Long customerId;
        private Long deptId;
        private String documentTypeCode;
        private Integer eventOrder;

        @ExcelProperty(value = "日期", index = 0)
        private LocalDateTime businessDate;
        @ExcelProperty(value = "客户", index = 1)
        private String customerName;
        @ExcelProperty(value = "单据类型", index = 2)
        private String documentType;
        @ExcelProperty(value = "结算方式", index = 3)
        private String settleMethod;
        @ExcelProperty(value = "修改人", index = 4)
        private String updaterName;
        @ExcelProperty(value = "制单人", index = 5)
        private String creatorName;
        @ExcelProperty(value = "业务员", index = 6)
        private String businessUserName;
        @ExcelProperty(value = "运输方式", index = 7)
        private String deliveryMethod;
        @ExcelProperty(value = "客户编码", index = 8)
        private String customerCode;
        @ExcelProperty(value = "单号", index = 9)
        private String documentNo;
        @ExcelProperty(value = "上笔余额", index = 10)
        private BigDecimal previousBalance;
        @ExcelProperty(value = "增加应收", index = 11)
        private BigDecimal increaseAmount;
        @ExcelProperty(value = "收款金额", index = 12)
        private BigDecimal receiptAmount;
        @ExcelProperty(value = "余额", index = 13)
        private BigDecimal balance;
        @ExcelProperty(value = "未确认应收", index = 14)
        private BigDecimal unconfirmedReceivable;
        @ExcelProperty(value = "审核时间", index = 15)
        private LocalDateTime approveTime;
        @ExcelProperty(value = "凭证号", index = 16)
        private String voucherNo;
        @ExcelProperty(value = "已核销金额", index = 17)
        private BigDecimal writeOffAmount;
        @ExcelProperty(value = "已收款金额", index = 18)
        private BigDecimal receivedAmount;
        @ExcelProperty(value = "来源单号", index = 19)
        private String sourceNo;
        private Long sourceBusinessId;
        private String sourceDocumentTypeCode;
        @ExcelProperty(value = "说明", index = 20)
        private String description;
        @ExcelProperty(value = "是否结算", index = 21)
        private String settledStatus;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal increaseAmount;
        private BigDecimal receiptAmount;
        private BigDecimal writeOffAmount;
        private BigDecimal endingBalance;
        private Boolean amountVisible;
    }
}
