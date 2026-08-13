package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpWarehouseMoveImportExcelVO {

    @ExcelRequired
    @ExcelProperty("移出仓库")
    private String fromWarehouseName;

    @ExcelRequired
    @ExcelProperty("移入仓库")
    private String toWarehouseName;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("移货数量")
    private BigDecimal count;

    @ExcelProperty("移出货架")
    private String fromShelf;

    @ExcelProperty("移入货架")
    private String toShelf;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("移货单价")
    private BigDecimal productPrice;

    @ExcelProperty("单据备注")
    private String remark;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
