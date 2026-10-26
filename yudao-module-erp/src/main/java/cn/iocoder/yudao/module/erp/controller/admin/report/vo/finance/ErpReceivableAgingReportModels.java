package cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/** 应收账龄分析报表模型。 */
public final class ErpReceivableAgingReportModels {

    public static final String BUCKET_ALL = "ALL";
    public static final String BUCKET_WITHIN_1_MONTH = "WITHIN_1_MONTH";
    public static final String BUCKET_WITHIN_2_MONTHS = "WITHIN_2_MONTHS";
    public static final String BUCKET_WITHIN_3_MONTHS = "WITHIN_3_MONTHS";
    public static final String BUCKET_3_TO_6_MONTHS = "MONTHS_3_TO_6";
    public static final String BUCKET_6_TO_12_MONTHS = "MONTHS_6_TO_12";
    public static final String BUCKET_OVER_1_YEAR = "OVER_1_YEAR";

    private ErpReceivableAgingReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @NotNull(message = "分析日期不能为空")
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
        private LocalDate analysisDate;
        private Long customerId;
        private Integer customerType;
        private Long areaId;
        private Long saleUserId;
        private Long deptId;
        private Long developerUserId;
        private String orderField;
        private String orderDirection;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class DetailFilter extends PageParam {
        @NotNull(message = "分析日期不能为空")
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
        private LocalDate analysisDate;
        @NotNull(message = "客户不能为空")
        private Long customerId;
        private Long deptId;
        private Boolean deptUnassigned;
        private String bucket;
        private String orderField;
        private String orderDirection;
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private String rowKey;
        private Long customerId;
        private Long deptId;
        private Integer customerTypeId;
        private Long areaId;
        private Long saleUserId;
        private Long developerUserId;

        @ExcelProperty(value = "客户", index = 0)
        private String customerName;
        @ExcelProperty(value = "客户类型", index = 1)
        private String customerTypeName;
        @ExcelProperty(value = "余额", index = 2, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal balance;
        @ExcelProperty(value = "信用额度", index = 3, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal creditLimit;
        @ExcelProperty(value = "信用余额", index = 4, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal creditBalance;
        @ExcelProperty(value = "1个月内", index = 5, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal within1Month;
        @ExcelProperty(value = "2个月内", index = 6, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal within2Months;
        @ExcelProperty(value = "3个月内", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal within3Months;
        @ExcelProperty(value = "3个月至半年内", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal months3To6;
        @ExcelProperty(value = "半年至一年内", index = 9, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal months6To12;
        @ExcelProperty(value = "一年以上", index = 10, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal over1Year;
        @ExcelProperty(value = "区域", index = 11)
        private String areaName;
        @ExcelProperty(value = "业务员", index = 12)
        private String saleUserName;
        @ExcelProperty(value = "部门", index = 13)
        private String deptName;
        @ExcelProperty(value = "开发员", index = 14)
        private String developerName;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal balance;
        private BigDecimal creditLimit;
        private BigDecimal creditBalance;
        private BigDecimal within1Month;
        private BigDecimal within2Months;
        private BigDecimal within3Months;
        private BigDecimal months3To6;
        private BigDecimal months6To12;
        private BigDecimal over1Year;
        private Boolean amountVisible;
        private Boolean creditVisible;
        private Boolean customerNameVisible;
        private Boolean customerTypeVisible;
        private Boolean areaVisible;
        private Boolean saleUserVisible;
        private Boolean deptVisible;
        private Boolean developerVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class DetailRow {
        private String rowKey;
        private String documentTypeCode;
        private Long businessId;
        private Long customerId;
        private Long deptId;
        private Long businessUserId;
        private LocalDateTime businessDate;
        private String businessType;
        private String documentNo;
        private String customerName;
        private String deptName;
        private String businessUserName;
        private BigDecimal amount;
        private String description;
        private Boolean amountVisible;
    }
}
