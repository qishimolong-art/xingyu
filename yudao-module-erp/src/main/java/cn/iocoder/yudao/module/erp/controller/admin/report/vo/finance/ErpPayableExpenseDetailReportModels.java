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

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/** 费用支付查询报表模型。 */
public final class ErpPayableExpenseDetailReportModels {

    private ErpPayableExpenseDetailReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveDate;
        private Long accountId;
        private String documentNo;
        private String itemName;
        private BigDecimal amountMin;
        private BigDecimal amountMax;
        private String voucherNo;
        private String summary;
        private Long handlerId;
        private String party;
        private Long billingDeptId;
        private Long deptId;
        private Long businessUserId;
        private String orderField;
        private String orderDirection;

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
        private Long expenseId;
        private Long itemId;
        private Long accountId;
        private Long handlerId;
        private Long billingDeptId;
        private Long deptId;
        private Long businessUserId;

        @ExcelProperty(value = "审批日期", index = 0)
        private String approveDate;
        @ExcelProperty(value = "结算账户", index = 1)
        private String accountName;
        @ExcelProperty(value = "单号", index = 2)
        private String documentNo;
        @ExcelProperty(value = "费用项目名称", index = 3)
        private String itemName;
        @ExcelProperty(value = "金额", index = 4)
        private BigDecimal amount;
        @ExcelProperty(value = "凭证号", index = 5)
        private String voucherNo;
        @ExcelProperty(value = "摘要", index = 6)
        private String summary;
        @ExcelProperty(value = "经办人", index = 7)
        private String handlerName;
        @ExcelProperty(value = "对象", index = 8)
        private String party;
        @ExcelProperty(value = "类型", index = 9)
        private String type;
        @ExcelProperty(value = "开单部门", index = 10)
        private String billingDeptName;
        @ExcelProperty(value = "部门", index = 11)
        private String deptName;
        @ExcelProperty(value = "业务员", index = 12)
        private String businessUserName;
        @ExcelProperty(value = "发票", index = 13)
        private String invoice;
        @ExcelProperty(value = "发生日期", index = 14)
        private String occurrenceDate;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal amount;
        private Boolean amountVisible;
    }
}
