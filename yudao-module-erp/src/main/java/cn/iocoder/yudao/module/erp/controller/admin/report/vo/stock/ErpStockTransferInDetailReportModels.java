package cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import cn.idev.excel.annotation.format.NumberFormat;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/** 调拨入库逐明细查询报表模型。 */
public final class ErpStockTransferInDetailReportModels {

    private ErpStockTransferInDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @org.springframework.format.annotation.DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] moveDate;
        private String documentNo;
        private String productKeyword;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private BigDecimal countMin;
        private BigDecimal countMax;
        private BigDecimal priceMin;
        private BigDecimal priceMax;
        private BigDecimal amountMin;
        private BigDecimal amountMax;
        private Long creatorId;
        private String vehicleModel;
        private String originPlace;
        private String standard;
        private String drawingNo;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getMoveDateFrom() {
            return moveDate == null || moveDate.length == 0 ? null : moveDate[0];
        }

        public LocalDateTime getMoveDateTo() {
            return moveDate == null || moveDate.length < 2 ? null : moveDate[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long moveId;
        private Long itemId;
        private Long productId;
        private Long fromWarehouseId;
        private Long toWarehouseId;

        @ExcelProperty(value = "发生日期", index = 0)
        @DateTimeFormat("yyyy-MM-dd")
        private LocalDateTime moveDate;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "零件编码", index = 2)
        private String productCode;
        @ExcelProperty(value = "零件名称", index = 3)
        private String productName;
        @ExcelProperty(value = "适用车型", index = 4)
        private String vehicleModel;
        @ExcelProperty(value = "产地", index = 5)
        private String originPlace;
        @ExcelProperty(value = "调入仓库", index = 6)
        private String toWarehouseName;
        @ExcelProperty(value = "数量", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        @NumberFormat("0.####################")
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal productPrice;
        @ExcelProperty(value = "金额", index = 9, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal totalAmount;
        @ExcelProperty(value = "成本单价", index = 10, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costUnitPrice;
        @ExcelProperty(value = "成本金额", index = 11, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costAmount;
        @ExcelProperty(value = "制单人", index = 12)
        private String creatorName;
        @ExcelProperty(value = "调出仓库", index = 13)
        private String fromWarehouseName;
        @ExcelProperty(value = "规格", index = 14)
        private String standard;
        @ExcelProperty(value = "图号", index = 15, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        private String drawingNo;

        private Boolean priceVisible;
        private Boolean amountVisible;
        private Set<String> hiddenFields;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalAmount;
        private BigDecimal totalCostAmount;
        private Boolean priceVisible;
        private Boolean amountVisible;
        private Set<String> hiddenFields;
    }
}
