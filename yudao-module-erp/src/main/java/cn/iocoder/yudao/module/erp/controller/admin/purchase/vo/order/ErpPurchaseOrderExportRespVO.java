package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
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

    @ExcelProperty("单据状态")
    private Integer status;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("总数量")
    private BigDecimal totalCount;

    @ExcelProperty("入库数量")
    private BigDecimal inCount;

    @ExcelProperty("退货数量")
    private BigDecimal returnCount;

    @ExcelProperty("总金额")
    private BigDecimal totalPrice;

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
