package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInvoiceExportRespVO {

    @ExcelProperty("票据单号")
    private String no;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("开票日期")
    private LocalDate invoiceDate;

    @ExcelProperty("票据类型")
    private String invoiceType;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("发票张数")
    private Integer invoiceCount;

    @ExcelProperty("不含税金额")
    private BigDecimal taxExclusiveAmount;

    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    @ExcelProperty("价税合计")
    private BigDecimal totalAmount;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("来源入库单")
    private String sourceInNo;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("不含税单价")
    private BigDecimal productPrice;

    @ExcelProperty("不含税金额")
    private BigDecimal taxExclusivePrice;

    @ExcelProperty("税率(%)")
    private BigDecimal taxPercent;

    @ExcelProperty("税额")
    private BigDecimal taxPrice;

    @ExcelProperty("价税合计")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
