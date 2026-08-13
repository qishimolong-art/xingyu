package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchasePriceAdjustOrderImportExcelVO {

    @ExcelProperty("调价单号")
    private String no;

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("调价时间")
    private String adjustTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelRequired
    @ExcelProperty("调价后单价")
    private BigDecimal newPrice;

}
