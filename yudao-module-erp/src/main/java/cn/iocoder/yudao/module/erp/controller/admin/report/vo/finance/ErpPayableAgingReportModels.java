package cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ErpPayableAgingReportModels {

    private ErpPayableAgingReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate asOfDate;
        private Long supplierId;
        private String supplierType;
        private String purchaser;
        private String orderField;
        private String orderDirection;
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long supplierId;

        @ExcelProperty(value = {"供应商", "供应商"}, index = 0)
        private String supplierName;
        @ExcelProperty(value = {"供应商类型", "供应商类型"}, index = 1)
        private String supplierType;
        @ExcelProperty(value = {"采购员", "采购员"}, index = 2)
        private String purchaser;
        @ExcelProperty(value = {"余额", "余额"}, index = 3)
        private BigDecimal balance;
        @ExcelProperty(value = {"一年内", "一月"}, index = 4)
        private BigDecimal month01;
        @ExcelProperty(value = {"一年内", "二月"}, index = 5)
        private BigDecimal month02;
        @ExcelProperty(value = {"一年内", "三月"}, index = 6)
        private BigDecimal month03;
        @ExcelProperty(value = {"一年内", "四月"}, index = 7)
        private BigDecimal month04;
        @ExcelProperty(value = {"一年内", "五月"}, index = 8)
        private BigDecimal month05;
        @ExcelProperty(value = {"一年内", "六月"}, index = 9)
        private BigDecimal month06;
        @ExcelProperty(value = {"一年内", "七月"}, index = 10)
        private BigDecimal month07;
        @ExcelProperty(value = {"一年内", "八月"}, index = 11)
        private BigDecimal month08;
        @ExcelProperty(value = {"一年内", "九月"}, index = 12)
        private BigDecimal month09;
        @ExcelProperty(value = {"一年内", "十月"}, index = 13)
        private BigDecimal month10;
        @ExcelProperty(value = {"一年内", "十一月"}, index = 14)
        private BigDecimal month11;
        @ExcelProperty(value = {"一年内", "十二月"}, index = 15)
        private BigDecimal month12;
        @ExcelProperty(value = {"两年内", "两年内"}, index = 16)
        private BigDecimal withinTwoYears;
        @ExcelProperty(value = {"三年内", "三年内"}, index = 17)
        private BigDecimal withinThreeYears;
        @ExcelProperty(value = {"三年以上", "三年以上"}, index = 18)
        private BigDecimal overThreeYears;

        private Boolean amountVisible;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal balance;
        private BigDecimal month01;
        private BigDecimal month02;
        private BigDecimal month03;
        private BigDecimal month04;
        private BigDecimal month05;
        private BigDecimal month06;
        private BigDecimal month07;
        private BigDecimal month08;
        private BigDecimal month09;
        private BigDecimal month10;
        private BigDecimal month11;
        private BigDecimal month12;
        private BigDecimal withinTwoYears;
        private BigDecimal withinThreeYears;
        private BigDecimal overThreeYears;
        private Boolean amountVisible;
        private Boolean supplierTypeVisible;
        private Boolean purchaserVisible;
    }
}
