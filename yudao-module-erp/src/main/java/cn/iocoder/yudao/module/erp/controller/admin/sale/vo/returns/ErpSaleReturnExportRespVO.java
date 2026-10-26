package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleReturnExportRespVO {

    @ExcelProperty("退货单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    private Long customerId;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("退货模式")
    private Integer returnMode;

    @ExcelProperty("来源销售单")
    private String sourceOutNo;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("下单确认人")
    private String confirmUserName;

    @ExcelProperty("下单确认时间")
    private LocalDateTime confirmTime;

    @ExcelProperty("拣货状态")
    private Integer pickStatus;

    @ExcelProperty("合计数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("合计金额")
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("商品金额")
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("优惠金额")
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("退款金额")
    @NumberFormat("0.00####")
    private BigDecimal refundPrice;

    @ExcelProperty("费用金额")
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("来源部门")
    private String warehouseDeptName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("最近销售价")
    @NumberFormat("0.00####")
    private BigDecimal lastSalePrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("退货数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty("退货单价")
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal taxPercent;

    @ExcelProperty("税额")
    @NumberFormat("0.00####")
    private BigDecimal taxPrice;

    @ExcelProperty("退货原因")
    private String returnReason;

    @ExcelProperty("仓位")
    private String warehousePosition;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
