package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSalePriceAdjustExportRespVO {

    @ExcelProperty("调价单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("调价日期")
    private LocalDateTime adjustDate;

    @ExcelProperty("调价人员")
    private String adjustUserName;

    @ExcelProperty("调价总金额")
    private BigDecimal totalAdjustPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("销售单号")
    private String saleOutNo;

    @ExcelProperty("产品编码")
    private String partCode;

    @ExcelProperty("产品名称")
    private String partName;

    @ExcelProperty("车型")
    private String vehicleModel;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("出库数量")
    private BigDecimal outCount;

    @ExcelProperty("原单价")
    private BigDecimal oldPrice;

    @ExcelProperty("新单价")
    private BigDecimal newPrice;

    @ExcelProperty("调价金额")
    private BigDecimal adjustPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
