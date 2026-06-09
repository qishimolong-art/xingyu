package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
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
    private BigDecimal totalCount;

    @ExcelProperty("总金额")
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

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("整件数")
    private Integer wholeQty;

    @ExcelProperty("入库数量")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("金额")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("明细备注")
    private String itemRemark;
}
