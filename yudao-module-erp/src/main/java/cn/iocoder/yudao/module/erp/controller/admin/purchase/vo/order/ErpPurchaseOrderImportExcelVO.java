package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购订单导入 Excel VO
 */
@Data
public class ErpPurchaseOrderImportExcelVO {

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("采购员")
    private String purchaserName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("税率(%)")
    private BigDecimal taxPercent;

    @ExcelProperty("订货日期")
    private LocalDateTime orderDate;

    @ExcelProperty("采购周期(天)")
    private Integer purchaseCycle;

    @ExcelProperty("到货日期")
    private LocalDateTime arrivalDate;

    @ExcelProperty("送货方式")
    private String deliveryMethod;

    @ExcelProperty("采购方式")
    private String purchaseType;

    @ExcelProperty("结算方式")
    private String settleMethod;

    @ExcelProperty("开票类型")
    private String invoiceType;

    @ExcelProperty("厂家单号")
    private String factoryOrderNo;

    @ExcelProperty("收货地址")
    private String receiveAddress;

    @ExcelProperty("订货公司")
    private String orderCompany;

    @ExcelProperty("备注")
    private String remark;

}
