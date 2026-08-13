package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleCartImportExcelVO {

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("赠品")
    private Boolean giftFlag;

    @ExcelProperty("备注")
    private String remark;

}
