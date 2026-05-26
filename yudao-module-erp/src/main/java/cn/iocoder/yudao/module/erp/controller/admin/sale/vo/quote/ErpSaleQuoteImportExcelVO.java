package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleQuoteImportExcelVO {

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("税率(%)")
    private BigDecimal taxPercent;

    @ExcelProperty("备注")
    private String remark;
}
