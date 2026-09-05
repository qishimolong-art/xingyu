package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSalePriceAdjustImportExcelVO {

    @ExcelRequired
    @ExcelProperty("销售单号")
    private String saleOutNo;

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
    @ExcelProperty("调后价")
    private BigDecimal newPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("备注")
    private String itemRemark;
}
