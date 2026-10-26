package cn.iocoder.yudao.module.erp.controller.admin.report.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库龄及长期未出库辅助报表契约。 */
public final class ErpStockAgeReportModels {

    public static final String BUCKET_ALL = "ALL";
    public static final String BUCKET_WITHIN_1_MONTH = "WITHIN_1_MONTH";
    public static final String BUCKET_MONTHS_2_TO_3 = "MONTHS_2_TO_3";
    public static final String BUCKET_MONTHS_4_TO_6 = "MONTHS_4_TO_6";
    public static final String BUCKET_MONTHS_7_TO_12 = "MONTHS_7_TO_12";
    public static final String BUCKET_YEARS_1_TO_2 = "YEARS_1_TO_2";
    public static final String BUCKET_YEARS_2_TO_3 = "YEARS_2_TO_3";
    public static final String BUCKET_OVER_3_YEARS = "OVER_3_YEARS";
    public static final String BUCKET_UNCLASSIFIED = "UNCLASSIFIED";

    public static final String OUTBOUND_ALL = "ALL";
    public static final String OUTBOUND_NEVER = "NEVER_OUT";
    public static final String OUTBOUND_HAS = "HAS_OUT";

    private ErpStockAgeReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        private String keyword;
        private Long deptId;
        private Long warehouseId;
        private Long categoryId;
        private String brand;
        private String ageBucket;
        private String outboundState;
        private String orderField;
        private String orderDirection;
    }

    @Data
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long stockId;
        private Long productId;
        private Long warehouseId;
        private Long deptId;

        @ExcelProperty("仓库")
        private String warehouseName;
        @ExcelProperty("所属部门")
        private String deptName;
        @ExcelProperty("编码")
        private String productCode;
        @ExcelProperty("配件名称")
        private String productName;
        @ExcelProperty("库存数量")
        private BigDecimal stockQuantity;
        @ExcelProperty(value = "库存成本单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costUnitPrice;
        @ExcelProperty(value = "库存金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costAmount;

        @ExcelProperty("1月以内数量")
        private BigDecimal within1MonthQuantity;
        @ExcelProperty(value = "1月以内金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal within1MonthAmount;
        @ExcelProperty("2~3月数量")
        private BigDecimal months2To3Quantity;
        @ExcelProperty(value = "2~3月金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal months2To3Amount;
        @ExcelProperty("4~6月数量")
        private BigDecimal months4To6Quantity;
        @ExcelProperty(value = "4~6月金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal months4To6Amount;
        @ExcelProperty("7~12月数量")
        private BigDecimal months7To12Quantity;
        @ExcelProperty(value = "7~12月金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal months7To12Amount;
        @ExcelProperty("1~2年数量")
        private BigDecimal years1To2Quantity;
        @ExcelProperty(value = "1~2年金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal years1To2Amount;
        @ExcelProperty("2~3年数量")
        private BigDecimal years2To3Quantity;
        @ExcelProperty(value = "2~3年金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal years2To3Amount;
        @ExcelProperty("3年以上数量")
        private BigDecimal over3YearsQuantity;
        @ExcelProperty(value = "3年以上金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal over3YearsAmount;

        @ExcelProperty("最后一次入库时间")
        private LocalDateTime lastInboundTime;
        @ExcelProperty("最后一次移仓入库时间")
        private LocalDateTime lastWarehouseMoveInboundTime;
        @ExcelProperty("最后一次出库时间")
        private LocalDateTime lastOutboundTime;
        @ExcelProperty("库龄天数")
        private Integer ageDays;
        @ExcelProperty("计算依据")
        private String agingBasis;

        @ExcelProperty("规格")
        private String standard;
        @ExcelProperty("车型")
        private String vehicleModel;
        @ExcelProperty("图号")
        private String drawingNo;
        @ExcelProperty("产地")
        private String originPlace;
        @ExcelProperty("类别")
        private String categoryName;
        @ExcelProperty("特征码")
        private String featureCode;
        @ExcelProperty("品牌")
        private String brand;

        private Boolean amountVisible;
    }

    @Data
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalQuantity;
        private BigDecimal totalAmount;
        private BigDecimal within1MonthQuantity;
        private BigDecimal within1MonthAmount;
        private BigDecimal months2To3Quantity;
        private BigDecimal months2To3Amount;
        private BigDecimal months4To6Quantity;
        private BigDecimal months4To6Amount;
        private BigDecimal months7To12Quantity;
        private BigDecimal months7To12Amount;
        private BigDecimal years1To2Quantity;
        private BigDecimal years1To2Amount;
        private BigDecimal years2To3Quantity;
        private BigDecimal years2To3Amount;
        private BigDecimal over3YearsQuantity;
        private BigDecimal over3YearsAmount;
        private Long unclassifiedRowCount;
        private BigDecimal unclassifiedQuantity;
        private BigDecimal unclassifiedAmount;
        private Boolean amountVisible;
    }
}
