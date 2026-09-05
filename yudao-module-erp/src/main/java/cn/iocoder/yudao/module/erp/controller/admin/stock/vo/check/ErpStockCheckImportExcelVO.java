package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelColumnSelect;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import cn.iocoder.yudao.module.erp.framework.excel.core.ErpStockCheckTypeExcelColumnSelectFunction;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpStockCheckImportExcelVO {

    @ExcelColumnSelect(functionName = ErpStockCheckTypeExcelColumnSelectFunction.NAME)
    @ExcelProperty("盘点类型")
    private String checkTypeName;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelChoiceRequired
    @ExcelProperty("配件编码（三选一）")
    private String productCode;

    @ExcelChoiceRequired
    @ExcelProperty("配件名称（三选一）")
    private String productName;

    @ExcelChoiceRequired
    @ExcelProperty("厂家编码（三选一）")
    private String factoryCode;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelRequired
    @ExcelProperty("实际库存")
    private BigDecimal actualCount;

    @ExcelProperty("盘点单价")
    private BigDecimal productPrice;

    @ExcelProperty("盘点金额")
    private BigDecimal totalPrice;

    @ExcelProperty("单据备注")
    private String remark;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
