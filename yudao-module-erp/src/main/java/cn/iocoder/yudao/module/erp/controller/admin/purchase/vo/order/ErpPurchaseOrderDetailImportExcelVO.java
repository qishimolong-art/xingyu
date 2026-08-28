package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 采购订单明细导入 Excel VO
 */
@Data
public class ErpPurchaseOrderDetailImportExcelVO {

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("产品单价")
    private BigDecimal productPrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("赠品")
    private String gift;

}
