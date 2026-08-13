package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpStockCheckImportExcelVO {

    @ExcelProperty("盘点类型")
    private String checkTypeName;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

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
