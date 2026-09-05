package cn.iocoder.yudao.module.erp.controller.admin.report.vo.sale;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpSaleReportDetailExportRespVO {

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    @ExcelProperty("业务数量")
    private BigDecimal bizCount;

    @ExcelProperty("出库金额")
    private String saleAmount;

    @ExcelProperty("退货金额")
    private String returnAmount;

    @ExcelProperty("销售净额")
    private String netAmount;
}
