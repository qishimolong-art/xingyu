package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
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

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("赠品")
    private String gift;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
