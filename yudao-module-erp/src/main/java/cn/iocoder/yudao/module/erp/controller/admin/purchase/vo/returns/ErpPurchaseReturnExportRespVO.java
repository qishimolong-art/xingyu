package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseReturnExportRespVO {

    @ExcelProperty("退货单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("退货时间")
    private LocalDateTime returnTime;

    @ExcelProperty("退货模式")
    private Integer returnMode;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("经办人/制单人")
    private String handlerName;

    @ExcelProperty("单据状态")
    private Integer status;

    @ExcelProperty("总数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("商品金额")
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("优惠率")
    @NumberFormat("0.######")
    private BigDecimal discountPercent;

    @ExcelProperty("总税额")
    @NumberFormat("0.00####")
    private BigDecimal totalTaxAmount;

    @ExcelProperty("优惠金额")
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("费用金额")
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("其他金额")
    @NumberFormat("0.00####")
    private BigDecimal otherPrice;

    @ExcelProperty("已退款金额")
    @NumberFormat("0.00####")
    private BigDecimal refundPrice;

    @ExcelProperty("总额")
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("税率")
    @NumberFormat("0.######")
    private BigDecimal mainTaxPercent;

    @ExcelProperty("发票类型")
    private String invoiceType;

    @ExcelProperty("运输方式")
    private String transportMethod;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("运费")
    @NumberFormat("0.00####")
    private BigDecimal freightAmount;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("开单方式")
    private String orderMethod;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("附件")
    private String fileUrl;

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

    @ExcelProperty("原入库数量")
    @NumberFormat("0.######")
    private BigDecimal inCount;

    @ExcelProperty("可退数量")
    @NumberFormat("0.######")
    private BigDecimal returnableCount;

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("整件数")
    private Integer wholeQty;

    @ExcelProperty("原入库单")
    private String sourceInNo;

    @ExcelProperty("退货数量")
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("最后采购价")
    @NumberFormat("0.00####")
    private BigDecimal lastPurchasePrice;

    @ExcelProperty("金额")
    @NumberFormat("0.00####")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细税率")
    @NumberFormat("0.######")
    private BigDecimal itemTaxPercent;

    @ExcelProperty("明细税额")
    @NumberFormat("0.00####")
    private BigDecimal itemTaxPrice;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("特征码")
    private String featureCode;

    @ExcelProperty("厂家编码")
    private String factoryCode;

    @ExcelProperty("零件编码")
    private String partCode;

    @ExcelProperty("零件名称")
    private String partName;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("条码")
    private String barCode;

    @ExcelProperty("所属经营")
    private String businessEntity;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("明细备注")
    private String itemRemark;

    @ExcelProperty("备注")
    private String remark;
}
