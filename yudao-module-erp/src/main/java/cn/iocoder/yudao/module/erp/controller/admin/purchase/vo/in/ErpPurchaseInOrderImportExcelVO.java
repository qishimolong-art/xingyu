package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpYesNoExcelColumnSelectFunction;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseInOrderImportExcelVO {

    @ExcelProperty("入库单号")
    private String no;

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("入库时间")
    private String inTime;

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

    @ExcelRequired
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("入库数量")
    private BigDecimal itemCount;

    @ExcelProperty("入库单价")
    private BigDecimal productPrice;

    @ExcelColumnSelect(functionName = ErpYesNoExcelColumnSelectFunction.NAME)
    @ExcelProperty("赠品")
    private String gift;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
