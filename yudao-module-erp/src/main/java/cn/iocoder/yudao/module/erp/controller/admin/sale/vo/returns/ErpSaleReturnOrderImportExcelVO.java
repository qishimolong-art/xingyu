package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleReturnOrderImportExcelVO {

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("退货数量")
    private BigDecimal itemCount;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelProperty("退货原因")
    private String returnReason;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
