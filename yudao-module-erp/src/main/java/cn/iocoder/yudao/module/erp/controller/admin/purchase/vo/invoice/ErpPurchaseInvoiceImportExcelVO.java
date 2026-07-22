package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseInvoiceImportExcelVO {

    @ExcelProperty("票据单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("开票日期")
    private String invoiceDate;

    @ExcelProperty("票据类型")
    private String invoiceType;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("发票张数")
    private Integer invoiceCount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("来源入库单")
    private String sourceInNo;

    @ExcelProperty("来源入库明细ID")
    private Long sourceInItemId;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("不含税单价")
    private BigDecimal productPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
