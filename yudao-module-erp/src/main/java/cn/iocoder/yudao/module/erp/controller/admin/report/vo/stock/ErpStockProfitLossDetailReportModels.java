package cn.iocoder.yudao.module.erp.controller.admin.report.vo.stock;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/** 配件盈亏逐明细报表模型。 */
public final class ErpStockProfitLossDetailReportModels {

    private ErpStockProfitLossDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        private String keyword;
        private Integer checkType;
        private String documentNo;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] checkDate;
        private Long warehouseId;
        private String creatorKeyword;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getCheckDateFrom() {
            return checkDate == null || checkDate.length == 0 ? null : checkDate[0];
        }

        public LocalDateTime getCheckDateTo() {
            return checkDate == null || checkDate.length < 2 ? null : checkDate[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long itemId;
        private Long checkId;
        private Long warehouseId;
        private Long productId;
        private Integer checkType;

        @ExcelProperty(value = "盘点类型", index = 0)
        private String checkTypeName;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "日期", index = 2)
        @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime checkDate;
        @ExcelProperty(value = "编码", index = 3)
        private String productCode;
        @ExcelProperty(value = "名称", index = 4)
        private String productName;
        @ExcelProperty(value = "仓库", index = 5)
        private String warehouseName;
        @ExcelProperty(value = "货架", index = 6)
        private String shelf;
        @ExcelProperty(value = "盈亏数", index = 7)
        private BigDecimal profitLossCount;
        @ExcelProperty(value = "成本单价", index = 8)
        private BigDecimal costUnitPrice;
        @ExcelProperty(value = "调整单价", index = 9)
        private BigDecimal adjustedUnitPrice;
        @ExcelProperty(value = "实盘数", index = 10)
        private BigDecimal actualCount;
        @ExcelProperty(value = "盈亏额", index = 11)
        private BigDecimal profitLossAmount;
        @ExcelProperty(value = "制单人", index = 12)
        private String creatorName;
        @ExcelProperty(value = "车型", index = 13)
        private String vehicleModel;
        @ExcelProperty(value = "原因", index = 14)
        private String reason;
        @ExcelProperty(value = "产地", index = 15)
        private String originPlace;
        @ExcelProperty(value = "规格", index = 16)
        private String standard;
        @ExcelProperty(value = "图号", index = 17)
        private String drawingNo;
        @ExcelProperty(value = "关联单号", index = 18)
        private String relatedNo;
        @ExcelProperty(value = "明细备注", index = 19)
        private String itemRemark;

        private Boolean priceVisible;
        private Set<String> hiddenFields;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal profitLossCount;
        private BigDecimal profitLossAmount;
        private Boolean priceVisible;
        private Set<String> hiddenFields;
    }
}
