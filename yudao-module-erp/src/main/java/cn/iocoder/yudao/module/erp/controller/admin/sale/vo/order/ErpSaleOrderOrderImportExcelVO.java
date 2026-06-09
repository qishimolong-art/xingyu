package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleOrderOrderImportExcelVO {

    @ExcelProperty("导入单号")
    private String importNo;

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelRequired
    @ExcelProperty("下单时间")
    private String orderTime;

    @ExcelProperty("销售员")
    private String saleUserName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("税率(%)")
    private BigDecimal taxPercent;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
