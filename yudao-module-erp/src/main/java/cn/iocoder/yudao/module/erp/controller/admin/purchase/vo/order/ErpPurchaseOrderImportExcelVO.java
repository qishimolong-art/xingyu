package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 采购订单整单导入 Excel VO。
 */
@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseOrderImportExcelVO {

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

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

    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelColumnSelect(functionName = ErpYesNoExcelColumnSelectFunction.NAME)
    @ExcelProperty("赠品")
    private String gift;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
