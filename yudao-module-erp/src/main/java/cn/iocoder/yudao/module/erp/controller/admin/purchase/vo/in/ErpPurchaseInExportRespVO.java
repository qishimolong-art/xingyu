package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInExportRespVO {

    @ExcelProperty("入库单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("入库时间")
    private LocalDateTime inTime;

    @ExcelProperty("关联订单")
    private String orderNo;

    @ExcelProperty("结算账户")
    private String accountName;

    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

    @ExcelProperty("单据状态")
    private Integer status;

    @ExcelProperty("发票状态")
    private String invoiceStatus;

    @ExcelProperty("调价状态")
    private Integer adjustStatus;

    @ExcelProperty("退货状态")
    private Integer returnStatus;

    @ExcelProperty("转销售状态")
    private Integer transferOutStatus;

    @ExcelProperty("付款状态")
    private Integer paymentStatus;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("记账员")
    private String accountantName;

    @ExcelProperty("经办人")
    private String handlerName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("总数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("总金额")
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("商品金额")
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("优惠率")
    @NumberFormat("0.######")
    private BigDecimal discountPercent;

    @ExcelProperty("总税额")
    @NumberFormat("0.00####")
    private BigDecimal totalTaxPrice;

    @ExcelProperty("优惠金额")
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("费用金额")
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("其他金额")
    @NumberFormat("0.00####")
    private BigDecimal otherPrice;

    @ExcelProperty("已付金额")
    @NumberFormat("0.00####")
    private BigDecimal paymentPrice;

    @ExcelProperty("开票类型")
    private String invoiceType;

    @ExcelProperty("运输方式")
    private String transportMethod;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("进货区")
    private String purchaseArea;

    @ExcelProperty("浮动率")
    @NumberFormat("0.######")
    private BigDecimal floatRate;

    @ExcelProperty("件数")
    private Integer packageCount;

    @ExcelProperty("开单方式")
    private String orderMethod;

    @ExcelProperty("运费类型一")
    private String freightType1;

    @ExcelProperty("运费类型二")
    private String freightType2;

    @ExcelProperty("运费对象一")
    private String freightObject1;

    @ExcelProperty("运费对象二")
    private String freightObject2;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal taxRate;

    @ExcelProperty("采购折让")
    @NumberFormat("0.00####")
    private BigDecimal purchaseDiscount;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("卸货员")
    private String unloader;

    @ExcelProperty("浮动记录")
    private String floatRecord;

    @ExcelProperty("收货单位")
    private String receiveUnit;

    @ExcelProperty("总运费一")
    @NumberFormat("0.00####")
    private BigDecimal totalFreight1;

    @ExcelProperty("总运费二")
    @NumberFormat("0.00####")
    private BigDecimal totalFreight2;

    @ExcelProperty("付款日期")
    private LocalDateTime paymentDate;

    @ExcelProperty("是否有发票")
    private Boolean hasInvoice;

    @ExcelProperty("所属经营")
    private String businessEntity;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("明细所属部门")
    private String itemDeptName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("整件数")
    private Integer wholeQty;

    @ExcelProperty("入库数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("最后采购价")
    @NumberFormat("0.00####")
    private BigDecimal lastPurchasePrice;

    @ExcelProperty("赠品")
    private String gift;

    @ExcelProperty("金额")
    @NumberFormat("0.00####")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal itemTaxPercent;

    @ExcelProperty("税额")
    @NumberFormat("0.00####")
    private BigDecimal itemTaxPrice;

    @ExcelProperty("原采购价")
    @NumberFormat("0.00####")
    private BigDecimal originalProductPrice;

    @ExcelProperty("是否调价")
    private String itemAdjusted;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("条码")
    private String barCode;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("特征码")
    private String featureCode;

    @ExcelProperty("厂家编码")
    private String factoryCode;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("明细所属经营")
    private String itemBusinessEntity;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
