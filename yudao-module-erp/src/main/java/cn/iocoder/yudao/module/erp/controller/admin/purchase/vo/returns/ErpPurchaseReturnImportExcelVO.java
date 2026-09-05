package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseReturnImportExcelVO {

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
    @ExcelProperty("退货数量")
    private BigDecimal count;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelRequired
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("备注")
    private String remark;
}
