package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

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

/** 销售拣货、送货逐明细报表模型。 */
public final class ErpSalePickDeliveryDetailReportModels {
    private ErpSalePickDeliveryDetailReportModels() {}

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) private LocalDateTime[] taskCreateDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) private LocalDateTime[] saleOutDate;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) private LocalDateTime[] operationTime;
        @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND) private LocalDateTime[] completeTime;
        private String sourceCartNo;
        private String saleOutNo;
        private Long customerId;
        private Long warehouseId;
        private Long deptId;
        private Long saleUserId;
        private Long operationUserId;
        private Integer status;
        private String productKeyword;
        private BigDecimal countMin;
        private BigDecimal countMax;
        private BigDecimal pickedCountMin;
        private BigDecimal pickedCountMax;
        private BigDecimal deliveredCountMin;
        private BigDecimal deliveredCountMax;
        private BigDecimal availableCountMin;
        private BigDecimal availableCountMax;
        private BigDecimal remainingCountMin;
        private BigDecimal remainingCountMax;
        private String orderField;
        private String orderDirection;

        public LocalDateTime getTaskCreateDateFrom() { return first(taskCreateDate); }
        public LocalDateTime getTaskCreateDateTo() { return second(taskCreateDate); }
        public LocalDateTime getSaleOutDateFrom() { return first(saleOutDate); }
        public LocalDateTime getSaleOutDateTo() { return second(saleOutDate); }
        public LocalDateTime getOperationTimeFrom() { return first(operationTime); }
        public LocalDateTime getOperationTimeTo() { return second(operationTime); }
        public LocalDateTime getCompleteTimeFrom() { return first(completeTime); }
        public LocalDateTime getCompleteTimeTo() { return second(completeTime); }
        private LocalDateTime first(LocalDateTime[] value) { return value == null || value.length == 0 ? null : value[0]; }
        private LocalDateTime second(LocalDateTime[] value) { return value == null || value.length < 2 ? null : value[1]; }
    }

    /** 页面行；技术字段不会进入独立导出 DTO。 */
    @Data
    @Accessors(chain = true)
    public static class Row {
        private Long itemId;
        private Long taskId;
        private Long orderId;
        private Integer sourceType;
        private Long sourceCartId;
        private Long saleOutId;
        private Long customerId;
        private Long deptId;
        private Long saleUserId;
        private Long warehouseId;
        private Long productId;
        private Long sequence;
        private LocalDateTime taskCreateDate;
        private LocalDateTime saleOutDate;
        private String sourceCartNo;
        private String saleOutNo;
        private String customerName;
        private String deptName;
        private String saleUserName;
        private String warehouseName;
        private String productCode;
        private String productName;
        private String standard;
        private String warehousePosition;
        private BigDecimal count;
        private BigDecimal pickedCount;
        private BigDecimal remainingPickCount;
        private BigDecimal deliveredCount;
        private BigDecimal availableDeliveryCount;
        private BigDecimal remainingDeliveryCount;
        private Integer packageQty;
        private BigDecimal pieceCount;
        private Integer status;
        private String statusText;
        private Long operationUserId;
        private String operationUserName;
        private LocalDateTime operationTime;
        private LocalDateTime completeTime;
        private Set<String> hiddenFields;
    }

    @Data
    @ExcelIgnoreUnannotated
    public static class PickExportRow {
        @ExcelProperty(value = "序号", index = 0) private Long sequence;
        @ExcelProperty(value = "任务创建日期", index = 1) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime taskCreateDate;
        @ExcelProperty(value = "销售出库日期", index = 2) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime saleOutDate;
        @ExcelProperty(value = "销售手推车单号", index = 3) private String sourceCartNo;
        @ExcelProperty(value = "销售出库单号", index = 4) private String saleOutNo;
        @ExcelProperty(value = "客户名称", index = 5) private String customerName;
        @ExcelProperty(value = "开单部门", index = 6) private String deptName;
        @ExcelProperty(value = "业务员", index = 7) private String saleUserName;
        @ExcelProperty(value = "仓库", index = 8) private String warehouseName;
        @ExcelProperty(value = "零件编码", index = 9) private String productCode;
        @ExcelProperty(value = "零件名称", index = 10) private String productName;
        @ExcelProperty(value = "规格", index = 11) private String standard;
        @ExcelProperty(value = "仓位", index = 12) private String warehousePosition;
        @ExcelProperty(value = "应拣数量", index = 13) private BigDecimal count;
        @ExcelProperty(value = "已拣数量", index = 14) private BigDecimal pickedCount;
        @ExcelProperty(value = "未拣数量", index = 15) private BigDecimal remainingPickCount;
        @ExcelProperty(value = "包装数", index = 16) private Integer packageQty;
        @ExcelProperty(value = "件数", index = 17) private BigDecimal pieceCount;
        @ExcelProperty(value = "拣货状态", index = 18) private String statusText;
        @ExcelProperty(value = "最近拣货人", index = 19) private String operationUserName;
        @ExcelProperty(value = "最近拣货时间", index = 20) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd HH:mm:ss") private LocalDateTime operationTime;
        @ExcelProperty(value = "拣货任务完成时间", index = 21) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd HH:mm:ss") private LocalDateTime completeTime;
    }

    @Data
    @ExcelIgnoreUnannotated
    public static class DeliveryExportRow {
        @ExcelProperty(value = "序号", index = 0) private Long sequence;
        @ExcelProperty(value = "任务创建日期", index = 1) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime taskCreateDate;
        @ExcelProperty(value = "销售出库日期", index = 2) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd") private LocalDateTime saleOutDate;
        @ExcelProperty(value = "销售手推车单号", index = 3) private String sourceCartNo;
        @ExcelProperty(value = "销售出库单号", index = 4) private String saleOutNo;
        @ExcelProperty(value = "客户名称", index = 5) private String customerName;
        @ExcelProperty(value = "开单部门", index = 6) private String deptName;
        @ExcelProperty(value = "业务员", index = 7) private String saleUserName;
        @ExcelProperty(value = "仓库", index = 8) private String warehouseName;
        @ExcelProperty(value = "零件编码", index = 9) private String productCode;
        @ExcelProperty(value = "零件名称", index = 10) private String productName;
        @ExcelProperty(value = "规格", index = 11) private String standard;
        @ExcelProperty(value = "仓位", index = 12) private String warehousePosition;
        @ExcelProperty(value = "应送数量", index = 13) private BigDecimal count;
        @ExcelProperty(value = "已拣数量", index = 14) private BigDecimal pickedCount;
        @ExcelProperty(value = "已送数量", index = 15) private BigDecimal deliveredCount;
        @ExcelProperty(value = "当前可送数量", index = 16) private BigDecimal availableDeliveryCount;
        @ExcelProperty(value = "未送数量", index = 17) private BigDecimal remainingDeliveryCount;
        @ExcelProperty(value = "包装数", index = 18) private Integer packageQty;
        @ExcelProperty(value = "件数", index = 19) private BigDecimal pieceCount;
        @ExcelProperty(value = "送货状态", index = 20) private String statusText;
        @ExcelProperty(value = "最近送货人", index = 21) private String operationUserName;
        @ExcelProperty(value = "最近送货时间", index = 22) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd HH:mm:ss") private LocalDateTime operationTime;
        @ExcelProperty(value = "送货任务完成时间", index = 23) @cn.idev.excel.annotation.format.DateTimeFormat("yyyy-MM-dd HH:mm:ss") private LocalDateTime completeTime;
    }

    @Data
    @Accessors(chain = true)
    public static class Summary {
        private Long rowCount;
        private BigDecimal totalCount;
        private BigDecimal pickedCount;
        private BigDecimal remainingPickCount;
        private BigDecimal deliveredCount;
        private BigDecimal availableDeliveryCount;
        private BigDecimal remainingDeliveryCount;
        private BigDecimal pieceCount;
        private Set<String> hiddenFields;
    }
}
