package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleReturnOrderImportExcelVO {

    @ExcelRequired
    @ExcelProperty("导入单号")
    private String importNo;

    @ExcelRequired
    @ExcelProperty("退货模式")
    private Integer returnMode;

    @ExcelProperty("来源销售单号")
    private String sourceOutNo;

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

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
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("退货数量")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "itemCount最多支持 6 位小数")
    private BigDecimal itemCount;

    @ExcelProperty("退货单价")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "productPrice最多支持 6 位小数")
    private BigDecimal productPrice;

    @ExcelProperty("退货原因")
    private String returnReason;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
