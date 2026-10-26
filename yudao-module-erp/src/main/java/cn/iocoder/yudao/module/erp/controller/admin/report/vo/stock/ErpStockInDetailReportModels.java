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

/** 其他入库逐明细报表模型。 */
public final class ErpStockInDetailReportModels {

    private ErpStockInDetailReportModels() {
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @org.springframework.format.annotation.DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] approveTime;
        @org.springframework.format.annotation.DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
        private LocalDateTime[] documentTime;
        private String keyword;
        private Long warehouseId;
        private Long relatedPartyId;
        private Long categoryId;
        private Long deptId;
        private Long businessUserId;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getApproveTimeFrom() {
            return approveTime == null || approveTime.length == 0 ? null : approveTime[0];
        }

        public LocalDateTime getApproveTimeTo() {
            return approveTime == null || approveTime.length < 2 ? null : approveTime[1];
        }

        public LocalDateTime getDocumentTimeFrom() {
            return documentTime == null || documentTime.length == 0 ? null : documentTime[0];
        }

        public LocalDateTime getDocumentTimeTo() {
            return documentTime == null || documentTime.length < 2 ? null : documentTime[1];
        }
    }

    @Data
    @Accessors(chain = true)
    @ExcelIgnoreUnannotated
    public static class Row {
        private Long stockInId;
        private Long itemId;
        private Long warehouseId;
        private Long relatedPartyId;
        private Long productId;
        private Long categoryId;
        private Long deptId;
        private Long businessUserId;

        @ExcelProperty(value = "仓库", index = 0)
        private String warehouseName;
        @ExcelProperty(value = "单号", index = 1)
        private String documentNo;
        @ExcelProperty(value = "相关对象", index = 2)
        private String relatedPartyName;
        @ExcelProperty(value = "编码", index = 3)
        private String productCode;
        @ExcelProperty(value = "图号", index = 4)
        private String drawingNo;
        @ExcelProperty(value = "产品名称", index = 5)
        private String productName;
        @ExcelProperty(value = "适用车型", index = 6)
        private String vehicleModel;
        @ExcelProperty(value = "数量", index = 7, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        private BigDecimal count;
        @ExcelProperty(value = "单价", index = 8, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal productPrice;
        @ExcelProperty(value = "金额", index = 9, converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
        private BigDecimal totalAmount;
        @ExcelProperty(value = "品牌", index = 10)
        private String brand;
        @ExcelProperty(value = "产地", index = 11)
        private String originPlace;
        @ExcelProperty(value = "规格", index = 12)
        private String standard;
        @ExcelProperty(value = "商品类型", index = 13)
        private String categoryName;
        @ExcelProperty(value = "货架", index = 14)
        private String shelf;
        @ExcelProperty(value = "审核日期", index = 15)
        @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime approveTime;
        @ExcelProperty(value = "单据日期", index = 16)
        @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
        private LocalDateTime documentTime;
        @ExcelProperty(value = "备注", index = 17)
        private String itemRemark;
        @ExcelProperty(value = "部门", index = 18)
        private String deptName;
        @ExcelProperty(value = "业务员", index = 19, converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
        private String businessUserName;

        private Boolean priceVisible;
        private Set<String> hiddenFields;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal totalAmount;
        private Boolean priceVisible;
        private Set<String> hiddenFields;
    }
}
