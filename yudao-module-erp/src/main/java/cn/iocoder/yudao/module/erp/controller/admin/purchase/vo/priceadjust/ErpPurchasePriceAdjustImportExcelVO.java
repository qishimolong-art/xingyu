package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchasePriceAdjustImportExcelVO {

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("调价后单价")
    private BigDecimal newPrice;
}
