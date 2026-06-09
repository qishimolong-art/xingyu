package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSalePriceAdjustOrderImportExcelVO {

    @ExcelProperty("导入单号")
    private String importNo;

    @ExcelRequired
    @ExcelProperty("客户编号")
    private Long customerId;

    @ExcelProperty("调价日期")
    private String adjustDate;

    @ExcelProperty("调价人")
    private String adjustUserName;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("备注")
    private String remark;

    @ExcelRequired
    @ExcelProperty("销售单号")
    private String saleOutNo;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("调后价")
    private BigDecimal newPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
