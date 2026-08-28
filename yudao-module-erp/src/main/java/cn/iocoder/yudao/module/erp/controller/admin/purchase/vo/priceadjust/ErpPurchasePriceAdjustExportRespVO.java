package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchasePriceAdjustExportRespVO {

    @ExcelProperty("调价单号")
    private String no;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("部门")
    private String deptName;

    @ExcelProperty("调价日期")
    private LocalDateTime adjustTime;

    @ExcelProperty("调价方式")
    private String adjustTypeName;

    @ExcelProperty("状态")
    @DictFormat(AUDIT_STATUS)
    private Integer status;

    @ExcelProperty("调价人")
    private String adjusterName;

    @ExcelProperty("调价总额")
    private BigDecimal totalAdjustPrice;

    @ExcelProperty("已结算金额")
    private BigDecimal paymentPrice;

    @ExcelProperty("审批通过时间")
    private LocalDateTime approveTime;

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

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("重量")
    private BigDecimal weight;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("车型")
    private String vehicleModel;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("特征码")
    private String featureCode;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("货架位")
    private String warehousePosition;

    @ExcelProperty("入库单号")
    private String inNo;

    @ExcelProperty("原单价")
    private BigDecimal oldPrice;

    @ExcelProperty("调价后单价")
    private BigDecimal newPrice;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("调价比率(%)")
    private BigDecimal adjustRatio;

    @ExcelProperty("调价差额")
    private BigDecimal adjustPrice;
}
