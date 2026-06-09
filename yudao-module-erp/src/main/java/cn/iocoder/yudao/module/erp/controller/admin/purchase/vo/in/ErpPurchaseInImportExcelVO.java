package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseInImportExcelVO {

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("入库数量")
    private BigDecimal count;

    @ExcelProperty("入库单价")
    private BigDecimal productPrice;

    @ExcelProperty("整件数")
    private Integer wholeQty;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("备注")
    private String remark;
}
