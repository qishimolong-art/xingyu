package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
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

    @ExcelProperty("供应商类型")
    private String supplierType;

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
    @NumberFormat("0.00####")
    private BigDecimal taxExclusiveAmount;

    @ExcelProperty("票据税额")
    @NumberFormat("0.00####")
    private BigDecimal taxAmount;

    @ExcelProperty("票据价税合计")
    @NumberFormat("0.00####")
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

    @ExcelProperty("审核人")
    private String auditUserName;

    @ExcelProperty("审核时间")
    private LocalDateTime auditTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("来源类型")
    private String sourceTypeName;

    @ExcelProperty("来源单号")
    private String sourceDocNo;

    @ExcelProperty("来源明细ID")
    private Long sourceItemId;

    @ExcelProperty("来源部门")
    private String sourceDeptName;

    @ExcelProperty("来源总数量")
    @NumberFormat("0.######")
    private BigDecimal sourceTotalCount;

    @ExcelProperty("剩余可开数量")
    @NumberFormat("0.######")
    private BigDecimal sourceRemainingCount;

    @ExcelProperty("金额方向")
    private String amountDirectionName;

    @ExcelProperty("来源入库单（兼容）")
    private String sourceInNo;

    @ExcelProperty("来源入库明细ID")
    private Long sourceInItemId;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("厂家编码")
    private String factoryCode;

    @ExcelProperty("产品条码")
    private String productBarCode;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("库存")
    @NumberFormat("0.######")
    private BigDecimal stockCount;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("件数")
    private Integer wholeQty;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("业务主体")
    private String businessEntity;

    @ExcelProperty("数量")
    @NumberFormat("0.######")
    private BigDecimal count;

    @ExcelProperty("不含税单价")
    @NumberFormat("0.00####")
    private BigDecimal productPrice;

    @ExcelProperty("最近采购价")
    @NumberFormat("0.00####")
    private BigDecimal lastPurchasePrice;

    @ExcelProperty("明细不含税金额")
    @NumberFormat("0.00####")
    private BigDecimal taxExclusivePrice;

    @ExcelProperty("税率(%)")
    @NumberFormat("0.######")
    private BigDecimal taxPercent;

    @ExcelProperty("明细税额")
    @NumberFormat("0.00####")
    private BigDecimal taxPrice;

    @ExcelProperty("明细价税合计")
    @NumberFormat("0.00####")
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
