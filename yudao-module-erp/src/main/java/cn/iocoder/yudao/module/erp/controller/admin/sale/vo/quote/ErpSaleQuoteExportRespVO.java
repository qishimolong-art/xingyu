package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ContentStyle;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpSaleQuoteExportRespVO {

    @ExcelProperty("报价单号")
    private String no;

    @ExcelProperty("客户名称")
    private String customerName;

    private Long customerId;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("下订状态")
    private Integer orderStatus;

    @ExcelProperty("出库状态")
    private Integer outStatus;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("客户编码")
    private String customerCode;

    @ExcelProperty("报价时间")
    private LocalDateTime quoteTime;

    @ExcelProperty("合计数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("合计金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("商品金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("优惠金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("费用金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("其他金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal otherPrice;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("送货地址")
    private String deliveryAddress;

    @ExcelProperty("付款方式")
    private String settleMethod;

    @ExcelProperty("预计发货时间")
    private LocalDateTime expectedDeliveryTime;

    @ExcelProperty("内部说明")
    private String internalRemark;

    @ExcelProperty("建立人")
    private String creatorName;

    @ExcelProperty("建立时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

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

    @ExcelProperty("件数")
    @NumberFormat("0.######")
    private BigDecimal pieceCount;

    @ExcelProperty("数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty("已转数量")
    @NumberFormat("0.######")
    private BigDecimal convertedCount;

    @ExcelProperty("单价")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("最后销售价")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal lastSalePrice;

    @ExcelProperty("金额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal itemTaxPercent;

    @ExcelProperty("税额")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal itemTaxPrice;

    @ExcelProperty("赠品")
    private String giftFlag;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("明细所属部门")
    private String itemDeptName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("可用库存")
    @NumberFormat("0.######")
    private BigDecimal availableCount;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("图号")
    private String drawingNo;

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
