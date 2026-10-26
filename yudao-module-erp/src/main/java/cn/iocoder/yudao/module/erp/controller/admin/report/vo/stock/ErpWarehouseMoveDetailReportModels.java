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

/** 仓库移货逐明细报表模型。 */
public final class ErpWarehouseMoveDetailReportModels {

    private ErpWarehouseMoveDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @org.springframework.format.annotation.DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveTime;
        private String keyword;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getApproveTimeFrom() {
            return approveTime == null || approveTime.length == 0 ? null : approveTime[0];
        }

        public LocalDateTime getApproveTimeTo() {
            return approveTime == null || approveTime.length < 2 ? null : approveTime[1];
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

        @ExcelProperty(value = "编码", index = 0)
        private String productCode;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "审核日期", index = 2)
        @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime approveTime;
        @ExcelProperty(value = "名称", index = 3)
        private String productName;
        @ExcelProperty(value = "品牌", index = 4)
        private String brand;
        @ExcelProperty(value = "车型", index = 5)
        private String vehicleModel;
        @ExcelProperty(value = "产地", index = 6)
        private String originPlace;
        @ExcelProperty(value = "单位", index = 7)
        private String unitName;
        @ExcelProperty(value = "移动数", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 9, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costPrice;
        @ExcelProperty(value = "金额", index = 10, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal costAmount;
        @ExcelProperty(value = "移货单价", index = 11, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal movePrice;
        @ExcelProperty(value = "移货金额", index = 12, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal moveAmount;
        @ExcelProperty(value = "移出仓库", index = 13)
        private String fromWarehouseName;
        @ExcelProperty(value = "移出货架", index = 14)
        private String fromShelf;
        @ExcelProperty(value = "移入仓库", index = 15)
        private String toWarehouseName;
        @ExcelProperty(value = "移入货架", index = 16)
        private String toShelf;
        @ExcelProperty(value = "制单人", index = 17)
        private String creatorName;
        @ExcelProperty(value = "审核人", index = 18)
        private String approveUserName;
        @ExcelProperty(value = "规格", index = 19)
        private String standard;
        @ExcelProperty(value = "图号", index = 20)
        private String drawingNo;
        @ExcelProperty(value = "特征码", index = 21)
        private String featureCode;
        @ExcelProperty(value = "明细备注", index = 22, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        private String itemRemark;

        private Boolean costVisible;
        private Boolean movePriceVisible;
        private Set<String> hiddenFields;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalCostAmount;
        private BigDecimal totalMoveAmount;
        private Boolean costVisible;
        private Boolean movePriceVisible;
        private Set<String> hiddenFields;
    }
}
