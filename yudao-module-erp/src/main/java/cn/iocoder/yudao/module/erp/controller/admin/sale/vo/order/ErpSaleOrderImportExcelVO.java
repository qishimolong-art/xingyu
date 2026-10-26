package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleOrderImportExcelVO {

    @ExcelChoiceRequired
    @ExcelProperty("配件编码（三选一）")
    private String productCode;

    @ExcelChoiceRequired
    @ExcelProperty("配件名称（三选一）")
    private String productName;

    @ExcelChoiceRequired
    @ExcelProperty("厂家编码（三选一）")
    private String factoryCode;

    @ExcelRequired
    @ExcelProperty("数量")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "count最多支持 6 位小数")
    private BigDecimal count;

    @ExcelProperty("单价")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "productPrice最多支持 6 位小数")
    private BigDecimal productPrice;

    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "taxPercent最多支持 6 位小数")
    private BigDecimal taxPercent;

    @ExcelProperty("备注")
    private String remark;
}
