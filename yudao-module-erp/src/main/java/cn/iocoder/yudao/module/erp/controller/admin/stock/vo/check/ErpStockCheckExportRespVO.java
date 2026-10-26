package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Flat selectable export. Saved stock-check snapshots remain authoritative. */
@Data
@ExcelIgnoreUnannotated
public class ErpStockCheckExportRespVO {
    @ExcelProperty("盘点编号")
    private Long id;

    @ExcelProperty("盘点单号")
    private String no;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("盘点时间")
    private LocalDateTime checkTime;

    @ExcelProperty("盘点类型")
    private String checkType;

    @ExcelProperty("盈亏类型")
    private String balanceType;

    @ExcelProperty(value = "盈亏数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.###")
    private BigDecimal totalCount;

    @ExcelProperty(value = "盈亏金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;

    @ExcelProperty(value = "整单总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalWeight;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("仓库汇总")
    private String warehouseNames;

    @ExcelProperty("项数")
    @NumberFormat("0")
    private Integer itemCount;

    @ExcelProperty("产品信息")
    private String productNames;

    @ExcelProperty("产品编码汇总")
    private String productCodes;

    @ExcelProperty("单据备注")
    private String remark;

    @ExcelProperty("建立人")
    private String creatorName;

    @ExcelProperty("建立时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("审核人")
    private String approveUserName;

    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;

    @ExcelProperty("打印次数")
    @NumberFormat("0")
    private Integer printCount;

    @ExcelProperty("打印时间")
    private LocalDateTime printTime;

    @ExcelProperty("明细仓库")
    private String detailWarehouseName;

    @ExcelProperty("明细仓库所属部门")
    private String detailWarehouseDeptName;

    @ExcelProperty("明细产品编码")
    private String detailProductCode;

    @ExcelProperty("明细产品名称")
    private String detailProductName;

    @ExcelProperty("明细产品条码")
    private String detailProductBarCode;

    @ExcelProperty("明细单位")
    private String detailProductUnitName;

    @ExcelProperty("明细批次")
    private String detailBatchNo;

    @ExcelProperty("明细包装数")
    @NumberFormat("0")
    private Integer detailPackageQty;

    @ExcelProperty(value = "明细件数", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.###")
    private BigDecimal detailPieceCount;

    @ExcelProperty(value = "明细单重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal detailWeight;

    @ExcelProperty(value = "明细总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal detailTotalWeight;

    @ExcelProperty(value = "明细账面数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.###")
    private BigDecimal detailStockCount;

    @ExcelProperty(value = "明细实际数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.###")
    private BigDecimal detailActualCount;

    @ExcelProperty(value = "明细调整数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.###")
    private BigDecimal detailCount;

    @ExcelProperty(value = "明细业务单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal detailProductPrice;

    @ExcelProperty(value = "明细金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal detailTotalPrice;

    @ExcelProperty("明细备注")
    private String detailRemark;

}
