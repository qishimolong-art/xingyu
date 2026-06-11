package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleQuoteOrderImportExcelVO {

    @ExcelProperty("导入单号")
    private String importNo;

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("报价时间")
    private String quoteTime;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    private BigDecimal taxPercent;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
