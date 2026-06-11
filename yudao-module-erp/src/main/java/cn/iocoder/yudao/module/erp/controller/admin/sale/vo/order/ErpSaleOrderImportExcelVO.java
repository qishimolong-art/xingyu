package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleOrderImportExcelVO {

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    private BigDecimal taxPercent;

    @ExcelProperty("备注")
    private String remark;
}
