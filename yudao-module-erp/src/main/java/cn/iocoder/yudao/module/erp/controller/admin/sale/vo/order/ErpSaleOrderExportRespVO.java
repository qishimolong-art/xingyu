package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleOrderExportRespVO {

    @ExcelProperty("销售单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("下单时间")
    private LocalDateTime orderTime;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @ExcelProperty("费用金额")
    private BigDecimal feeAmount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("税率")
    private BigDecimal itemTaxPercent;

    @ExcelProperty("税额")
    private BigDecimal itemTaxPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
