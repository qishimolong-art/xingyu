package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 采购订单明细导入 Excel VO
 */
@Data
public class ErpPurchaseOrderDetailImportExcelVO {

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
    private BigDecimal count;

    @ExcelProperty("产品单价")
    private BigDecimal productPrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelColumnSelect(functionName = ErpYesNoExcelColumnSelectFunction.NAME)
    @ExcelProperty("赠品")
    private String gift;

}
