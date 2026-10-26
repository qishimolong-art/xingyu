package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseOrderExportRespVO {

    @ExcelProperty("采购单号")
    private String no;

    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("采购时间")
    private LocalDateTime orderTime;

    @ExcelProperty("订货日期")
    private LocalDateTime orderDate;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("单据状态")
    private Integer status;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("总数量")
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty("入库数量")
    @NumberFormat("0.######")
    private BigDecimal inCount;

    @ExcelProperty("退货数量")
    @NumberFormat("0.######")
    private BigDecimal returnCount;

    @ExcelProperty("总金额")
    @NumberFormat("0.00####")
    private BigDecimal totalPrice;

    @ExcelProperty("商品金额")
    @NumberFormat("0.00####")
    private BigDecimal totalProductPrice;

    @ExcelProperty("优惠金额")
    @NumberFormat("0.00####")
    private BigDecimal discountPrice;

    @ExcelProperty("总税额")
    @NumberFormat("0.00####")
    private BigDecimal totalTaxPrice;

    @ExcelProperty("费用金额")
    @NumberFormat("0.00####")
    private BigDecimal feeAmount;

    @ExcelProperty("预付金额")
    @NumberFormat("0.00####")
    private BigDecimal depositPrice;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("入库状态")
    private String itemInStatus;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("明细所属部门")
    private String itemDeptName;

    @ExcelProperty("当前库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("重量")
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("数量")
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

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("特征码")
    private String featureCode;

    @ExcelProperty("件数")
    @NumberFormat("0.######")
    private BigDecimal pieceCount;

    @ExcelProperty("到货数")
    @NumberFormat("0.######")
    private BigDecimal arrivalCount;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("厂家编码")
    private String factoryCode;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("明细入库数量")
    @NumberFormat("0.######")
    private BigDecimal itemInCount;

    @ExcelProperty("明细退货数量")
    @NumberFormat("0.######")
    private BigDecimal itemReturnCount;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
