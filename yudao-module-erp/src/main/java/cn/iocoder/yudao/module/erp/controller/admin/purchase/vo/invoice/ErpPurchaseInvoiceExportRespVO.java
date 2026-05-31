package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInvoiceExportRespVO {

    @ExcelProperty("票据单号")
    private String no;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("开票日期")
    private LocalDate invoiceDate;

    @ExcelProperty("票据类型")
    private String invoiceType;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("发票张数")
    private Integer invoiceCount;

    @ExcelProperty("票据不含税金额")
    private BigDecimal taxExclusiveAmount;

    @ExcelProperty("票据税额")
    private BigDecimal taxAmount;

    @ExcelProperty("票据价税合计")
    private BigDecimal totalAmount;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("经手人")
    private String handlerName;

    @ExcelProperty("附件地址")
    private String fileUrl;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("来源入库单")
    private String sourceInNo;

    @ExcelProperty("来源入库明细ID")
    private Long sourceInItemId;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("不含税单价")
    private BigDecimal productPrice;

    @ExcelProperty("明细不含税金额")
    private BigDecimal taxExclusivePrice;

    @ExcelProperty("税率(%)")
    private BigDecimal taxPercent;

    @ExcelProperty("明细税额")
    private BigDecimal taxPrice;

    @ExcelProperty("明细价税合计")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
