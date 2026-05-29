package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSalePriceAdjustImportExcelVO {

    @ExcelProperty("客户编号")
    private Long customerId;

    @ExcelProperty("销售单号")
    private String saleOutNo;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("调后价")
    private BigDecimal newPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("备注")
    private String itemRemark;
}
