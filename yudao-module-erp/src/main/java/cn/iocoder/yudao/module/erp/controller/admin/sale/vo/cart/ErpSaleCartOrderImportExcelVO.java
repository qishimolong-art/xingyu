package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.framework.excel.core.convert.YesNoBooleanConvert;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleCartOrderImportExcelVO {

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("备注")
    private String remark;

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
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelColumnSelect(functionName = ErpYesNoExcelColumnSelectFunction.NAME)
    @ExcelProperty(value = "赠品", converter = YesNoBooleanConvert.class)
    private Boolean giftFlag;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
