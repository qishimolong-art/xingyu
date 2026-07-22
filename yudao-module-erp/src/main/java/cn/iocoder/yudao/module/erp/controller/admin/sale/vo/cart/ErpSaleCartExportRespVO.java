package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleCartExportRespVO {

    @ExcelProperty("手推车单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("开单时间")
    private LocalDateTime cartTime;

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

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("占用数量")
    private BigDecimal lockCount;

    @ExcelProperty("数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("车型")
    private String vehicleModel;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("仓位")
    private String warehousePosition;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
