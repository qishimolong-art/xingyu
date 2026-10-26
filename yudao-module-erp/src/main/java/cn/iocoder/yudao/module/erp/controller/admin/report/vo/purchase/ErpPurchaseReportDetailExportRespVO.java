package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@ExcelIgnoreUnannotated
@Data
public class ErpPurchaseReportDetailExportRespVO {

    @ExcelProperty("单据类型")
    private String docType;

    @ExcelProperty("单据日期")
    private LocalDateTime docDate;

    @ExcelProperty("单据编号")
    private String docNo;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("业务数量")
    @cn.idev.excel.annotation.format.NumberFormat("0.######")
    private BigDecimal bizCount;

    @ExcelProperty(value = "入库金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String purchaseAmount;

    @ExcelProperty(value = "退货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String returnAmount;

    @ExcelProperty(value = "采购净额", converter = cn.iocoder.yudao.module.erp.framework.excel.ErpTradeAmountExcelConverter.class)
    private String netAmount;
}
