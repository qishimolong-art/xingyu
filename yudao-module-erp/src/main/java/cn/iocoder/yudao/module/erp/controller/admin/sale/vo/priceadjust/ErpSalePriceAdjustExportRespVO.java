package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
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

    private Long customerId;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("调价日期")
    private LocalDateTime adjustDate;

    @ExcelProperty("调价人员")
    private String adjustUserName;

    @ExcelProperty("调价类型")
    private Integer adjustType;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("原单金额")
    @NumberFormat("0.00####")
    private BigDecimal totalOriginalPrice;

    @ExcelProperty("调后金额")
    @NumberFormat("0.00####")
    private BigDecimal totalAdjustedPrice;

    @ExcelProperty("调价总金额")
    @NumberFormat("0.00####")
    private BigDecimal totalAdjustPrice;

    @ExcelProperty("已结算金额")
    @NumberFormat("0.00####")
    private BigDecimal receiptPrice;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

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

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("来源部门")
    private String warehouseDeptName;

    @ExcelProperty("最近销售价")
    @NumberFormat("0.00####")
    private BigDecimal lastSalePrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("出库数量")
    @NumberFormat("0.######")
    private BigDecimal outCount;

    @ExcelProperty("原单价")
    @NumberFormat("0.00####")
    private BigDecimal oldPrice;

    @ExcelProperty("新单价")
    @NumberFormat("0.00####")
    private BigDecimal newPrice;

    @ExcelProperty("调价金额")
    @NumberFormat("0.00####")
    private BigDecimal adjustPrice;

    @ExcelProperty("调价原因")
    private String adjustReason;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
