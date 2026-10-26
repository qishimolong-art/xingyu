package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ContentStyle;
import cn.idev.excel.annotation.format.NumberFormat;
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

    @ExcelProperty("客户编码")
    private String customerCode;

    private Long customerId;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("开单时间")
    private LocalDateTime cartTime;

    @ExcelProperty("业务员")
    private String saleUserName;

    @ExcelProperty("部门")
    private String deptName;

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

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("运费类型")
    private String freightType;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("送货地址")
    private String deliveryAddress;

    @ExcelProperty("车牌号")
    private String vehiclePlateNo;

    @ExcelProperty("VIN")
    private String vin;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("发票类型")
    private String invoiceType;

    @ExcelProperty("总运费")
    @ContentStyle(dataFormat = 2)
    @NumberFormat("0.00####")
    private BigDecimal totalFreight;

    @ExcelProperty("送货日期")
    private LocalDateTime deliveryDate;

    @ExcelProperty("付款日期")
    private LocalDateTime paymentDate;

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

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("仓库所属部门")
    private String warehouseDeptName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("占用数量")
    @NumberFormat("0.######")
    private BigDecimal lockCount;

    @ExcelProperty("数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

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

    @ExcelProperty("赠品")
    private String giftFlag;

    @ExcelProperty("批次号")
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
