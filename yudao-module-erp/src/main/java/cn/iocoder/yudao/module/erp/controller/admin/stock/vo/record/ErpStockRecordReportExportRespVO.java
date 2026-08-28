package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.erp.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@ExcelIgnoreUnannotated
public class ErpStockRecordReportExportRespVO {

    @ExcelProperty("发生日期")
    private LocalDateTime bizDate;

    @ExcelProperty(value = "交易类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.STOCK_RECORD_BIZ_TYPE)
    private Integer bizType;

    @ExcelProperty("单号")
    private String bizNo;

    @ExcelProperty("零件编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("单重")
    private BigDecimal weight;

    @ExcelProperty("总重")
    private BigDecimal totalWeight;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("入库数")
    private BigDecimal inCount;

    @ExcelProperty("入库单价")
    private String inUnitPrice;

    @ExcelProperty("入库金额")
    private String inAmount;

    @ExcelProperty("出库数")
    private BigDecimal outCount;

    @ExcelProperty("出库成本单价")
    private String outUnitPrice;

    @ExcelProperty("出库成本金额")
    private String outAmount;

    @ExcelProperty("结存数")
    private BigDecimal totalCount;

    @ExcelProperty("结存单价")
    private String costPrice;

    @ExcelProperty("结存金额")
    private String costAmount;

}
