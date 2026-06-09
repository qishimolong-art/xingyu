package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleReturnImportExcelVO {

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("退货数量")
    private BigDecimal count;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("退货原因")
    private String returnReason;

    @ExcelProperty("备注")
    private String remark;
}
