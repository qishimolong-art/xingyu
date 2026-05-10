package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.erp.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 库存进出流水明细账 Response VO（五期）
 *
 * 列定义对齐客户文档 4.3：发生日期 / 交易类型 / 单号 /
 * 入库数 / 入库单价 / 入库金额 / 出库数 / 出库成本单价 / 出库成本金额 /
 * 结存数 / 结存单价 / 结存金额
 */
@Schema(description = "管理后台 - 库存进出流水明细账 Response VO")
@Data
@Accessors(chain = true)
@ExcelIgnoreUnannotated
public class ErpStockRecordReportRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "发生日期")
    @ExcelProperty("发生日期")
    private LocalDateTime bizDate;

    @Schema(description = "交易类型", example = "10")
    @ExcelProperty(value = "交易类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.STOCK_RECORD_BIZ_TYPE)
    private Integer bizType;

    @Schema(description = "业务单号")
    @ExcelProperty("单号")
    private String bizNo;

    @Schema(description = "产品编号")
    private Long productId;
    @Schema(description = "产品编码")
    @ExcelProperty("零件编码")
    private String productCode;
    @Schema(description = "产品名称")
    @ExcelProperty("产品名称")
    private String productName;

    @Schema(description = "仓库编号")
    private Long warehouseId;
    @Schema(description = "仓库名称")
    @ExcelProperty("仓库")
    private String warehouseName;

    // ========== 入库分列（当 count > 0 填充） ==========
    @Schema(description = "入库数")
    @ExcelProperty("入库数")
    private BigDecimal inCount;
    @Schema(description = "入库单价")
    @ExcelProperty("入库单价")
    private BigDecimal inUnitPrice;
    @Schema(description = "入库金额")
    @ExcelProperty("入库金额")
    private BigDecimal inAmount;

    // ========== 出库分列（当 count < 0 填充，数值取绝对值） ==========
    @Schema(description = "出库数")
    @ExcelProperty("出库数")
    private BigDecimal outCount;
    @Schema(description = "出库成本单价")
    @ExcelProperty("出库成本单价")
    private BigDecimal outUnitPrice;
    @Schema(description = "出库成本金额")
    @ExcelProperty("出库成本金额")
    private BigDecimal outAmount;

    // ========== 结存列 ==========
    @Schema(description = "结存数")
    @ExcelProperty("结存数")
    private BigDecimal totalCount;
    @Schema(description = "结存单价")
    @ExcelProperty("结存单价")
    private BigDecimal costPrice;
    @Schema(description = "结存金额")
    @ExcelProperty("结存金额")
    private BigDecimal costAmount;

}
