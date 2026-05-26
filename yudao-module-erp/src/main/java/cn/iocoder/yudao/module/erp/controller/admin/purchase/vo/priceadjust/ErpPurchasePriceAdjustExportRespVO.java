package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchasePriceAdjustExportRespVO {

    @ExcelProperty("调价单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("调价日期")
    private LocalDateTime adjustTime;

    @ExcelProperty("调价方式")
    private Integer adjustType;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("调价人")
    private String adjusterName;

    @ExcelProperty("调价总额")
    private BigDecimal totalAdjustPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("入库单号")
    private String inNo;

    @ExcelProperty("原价")
    private BigDecimal oldPrice;

    @ExcelProperty("新价")
    private BigDecimal newPrice;

    @ExcelProperty("调价数量")
    private BigDecimal count;

    @ExcelProperty("调价金额")
    private BigDecimal adjustPrice;
}
