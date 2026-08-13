package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSalePriceAdjustImportExcelVO {

    @ExcelRequired
    @ExcelProperty("销售单号")
    private String saleOutNo;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("调后价")
    private BigDecimal newPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("备注")
    private String itemRemark;
}
