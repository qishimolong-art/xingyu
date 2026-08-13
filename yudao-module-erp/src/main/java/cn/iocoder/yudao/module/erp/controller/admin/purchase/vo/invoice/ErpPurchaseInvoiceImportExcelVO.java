package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseInvoiceImportExcelVO {

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelRequired
    @ExcelProperty("开票日期")
    private String invoiceDate;

    @ExcelRequired
    @ExcelProperty("票据类型")
    private String invoiceType;

    @ExcelRequired
    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("发票张数")
    private Integer invoiceCount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("来源入库单号")
    private String sourceInNo;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelRequired
    @ExcelProperty("不含税单价")
    private BigDecimal productPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
