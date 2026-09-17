package cn.iocoder.yudao.module.erp.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 新核算口径的独立报表契约，不复用旧流水中含义不同的金额字段。 */
public final class ErpStockReportV2Models {
    private ErpStockReportV2Models() { }

    @Data @EqualsAndHashCode(callSuper = true)
    public static class Filter extends PageParam {
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime postedFrom;
        @NotNull @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime postedTo;
        private Long productId;
        private Long warehouseId;
        private List<Long> stockDeptIds;
        private List<Long> accountingDeptIds;
        private List<Integer> bizTypes;
        private String batchNo;
        private String orderField;
        private String orderDirection;
    }

    @Data
    public static class Status {
        private String status;
        private String reason;
        @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime cutoverAt;
    }

    @Data @ExcelIgnoreUnannotated
    public static class Amounts {
        @ExcelProperty(value="期初数量", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal openingQuantity;
        @ExcelProperty(value="本期入库数量", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal inQuantity;
        @ExcelProperty(value="本期出库数量", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal outQuantity;
        @ExcelProperty(value="期末数量", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal closingQuantity;
        @ExcelProperty(value="期初财务成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal openingFinancialAmount;
        @ExcelProperty(value="入库财务成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal financialInAmount;
        @ExcelProperty(value="出库财务成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal financialOutAmount;
        @ExcelProperty(value="期末财务成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal closingFinancialAmount;
        @ExcelProperty(value="期初结算成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal openingSettlementAmount;
        @ExcelProperty(value="入库结算成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal settlementInAmount;
        @ExcelProperty(value="出库结算成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal settlementOutAmount;
        @ExcelProperty(value="期末结算成本", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal closingSettlementAmount;
        private Boolean financialMasked;
        private Boolean settlementMasked;
        /** 按业务类型的本期有符号发生额；不完整时为null，已知无发生时为空数组。 */
        private List<BizTypeAmount> byBizType;
    }

    @Data @EqualsAndHashCode(callSuper = true) @ExcelIgnoreUnannotated
    public static class BalanceRow extends Amounts {
        @ExcelProperty("库存编号") private Long stockId;
        @ExcelProperty("商品编号") private Long productId;
        @ExcelProperty("商品编码") private String productCode;
        @ExcelProperty("商品名称") private String productName;
        @ExcelProperty("仓库编号") private Long warehouseId;
        @ExcelProperty("仓库名称") private String warehouseName;
        @ExcelProperty("库存归属部门") private Long stockDeptId;
        @ExcelProperty("切换时间") @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime cutoverAt;
        @ExcelProperty("核算起点类型") private String originKind;
        @ExcelProperty(value="可计算起点", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDateTimeTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime availableFrom;
        @ExcelProperty("数据状态") private String dataStatus;
    }

    @Data @ExcelIgnoreUnannotated
    public static class MovementRow {
        @ExcelProperty("过账流水编号") private Long postingId;
        @ExcelProperty("库存编号") private Long stockId;
        @ExcelProperty("商品编号") private Long productId;
        @ExcelProperty("商品编码") private String productCode;
        @ExcelProperty("商品名称") private String productName;
        @ExcelProperty("仓库编号") private Long warehouseId;
        @ExcelProperty("仓库名称") private String warehouseName;
        @ExcelProperty("库存归属部门") private Long stockDeptId;
        @ExcelProperty("核算部门") private Long accountingDeptId;
        @ExcelProperty("业务类型") private Integer bizType;
        @ExcelProperty("业务类型名称") private String bizTypeName;
        @ExcelProperty("单据编号") private Long bizId;
        @ExcelProperty("明细编号") private Long bizItemId;
        @ExcelProperty("业务单号") private String bizNo;
        @ExcelProperty("批次号") private String batchNo;
        @ExcelProperty("来源业务类型") private Integer sourceBizType;
        @ExcelProperty("来源单据编号") private Long sourceBizId;
        @ExcelProperty("来源明细编号") private Long sourceBizItemId;
        @ExcelProperty("原过账编号") private Long reversalPostingId;
        @ExcelProperty("过账时间") @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime postedAt;
        @ExcelProperty("核算起点类型") private String originKind;
        @ExcelProperty(value="可计算起点", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDateTimeTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime availableFrom;
        @ExcelProperty("业务日期") @JsonSerialize(using = ToStringSerializer.class) private LocalDateTime bizDate;
        @ExcelProperty(value="数量发生额", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal quantity;
        @ExcelProperty(value="财务成本发生额", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal financialMovement;
        @ExcelProperty(value="结算成本发生额", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal settlementMovement;
        @ExcelProperty(value="结存数量", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal balanceQuantity;
        @ExcelProperty(value="财务成本结存", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal financialBalance;
        @ExcelProperty(value="结算成本结存", converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class) @JsonSerialize(using = ToStringSerializer.class) private BigDecimal settlementBalance;
        @ExcelProperty("数据状态") private String dataStatus;
        private Boolean financialMasked;
        private Boolean settlementMasked;
        private Boolean balanceMasked;
    }

    @Data @EqualsAndHashCode(callSuper = true)
    public static class Summary extends Amounts {
        private Long rowCount;
        private String dataStatus;
        private Long missingOpeningCount;
        private Long outsideCoverageCount;
        private Long staleCount;
    }

    @Data
    public static class ProductOption {
        private Long id;
        private String code;
        private String name;
    }

    @Data @ExcelIgnoreUnannotated
    public static class BizTypeAmount {
        @ExcelProperty("库存编号") private Long stockId;
        @ExcelProperty("业务类型编号") private Integer bizType;
        @ExcelProperty("业务类型名称") private String bizTypeName;
        @ExcelProperty(value="数量净发生",converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class)
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal quantity;
        @ExcelProperty(value="财务成本净发生",converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class)
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal financialMovement;
        @ExcelProperty(value="结算成本净发生",converter=cn.iocoder.yudao.module.erp.service.stock.report.ErpReportDecimalTextConverter.class)
        @JsonSerialize(using=ToStringSerializer.class) private BigDecimal settlementMovement;
    }

    @Data
    public static class HistoryOption {
        private Long id;
        private String name;
        private Integer status;
        private Boolean archived;
    }

    @Data
    public static class ReportOptions {
        private List<HistoryOption> warehouses;
        private List<HistoryOption> stockDepartments;
    }
}
