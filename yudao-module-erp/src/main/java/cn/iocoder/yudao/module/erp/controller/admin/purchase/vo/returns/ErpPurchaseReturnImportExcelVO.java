package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseReturnImportExcelVO {

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("退货数量")
    private BigDecimal count;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("备注")
    private String remark;
}
