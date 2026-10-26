package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpStockOutBillExportRespVO {

    @ExcelProperty("优先级")
    private String priority;

    @ExcelProperty("单号")
    private String no;

    @ExcelProperty("拣货")
    private String pick;

    @ExcelProperty("拣货人")
    private String pickUserName;

    @ExcelProperty("日期")
    private LocalDateTime billDate;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("发货区")
    private String shippingArea;

    @ExcelProperty("来源单位")
    private String sourceUnitName;

    @ExcelProperty("来源单号")
    private String sourceNo;

    @ExcelProperty("状态")
    @NumberFormat("0.######")
    private Integer status;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("审核人")
    private String auditorName;

    @ExcelProperty("审核时间")
    private LocalDateTime auditTime;

    @ExcelProperty("打印时间")
    private LocalDateTime printTime;

    @ExcelProperty("打印次数")
    @NumberFormat("0.######")
    private Integer printCount;

    @ExcelProperty("来源单据备注")
    private String sourceRemark;

    @ExcelProperty(value = "总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalWeight;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("超时")
    private Boolean timeoutFlag;

    @ExcelProperty(value = "整件数", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal wholeQty;

    @ExcelProperty(value = "散件数", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal looseQty;

    @ExcelProperty(value = "应拣数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @ExcelProperty(value = "已拣数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal pickedCount;

    @ExcelProperty("明细来源单号")
    private String itemSourceNo;

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("产品名称")
    private String productName;

    @ExcelProperty("单位")
    private String productUnitName;

    @ExcelProperty("包装数")
    @NumberFormat("0.######")
    private Integer packageQty;

    @ExcelProperty(value = "单重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal weight;

    @ExcelProperty(value = "明细总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemTotalWeight;

    @ExcelProperty("明细整件数")
    @NumberFormat("0.######")
    private Integer itemWholeQty;

    @ExcelProperty(value = "应拣明细数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemCount;

    @ExcelProperty(value = "已拣明细数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemPickedCount;

    @ExcelProperty(value = "剩余待拣数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemRemainCount;

    @ExcelProperty(value = "单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal productPrice;

    @ExcelProperty(value = "金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细仓库")
    private String itemWarehouseName;

    @ExcelProperty("货架位")
    private String itemWarehousePosition;

    @ExcelProperty("图号")
    private String drawingNo;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("条形码")
    private String barCode;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("产地")
    private String originPlace;

    @ExcelProperty("明细备注")
    private String itemRemark;

    @ExcelProperty("仓库所属部门")
    private String warehouseDeptName;
    @ExcelProperty(value = "历史待处理数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal remainingCount;
    @ExcelProperty("明细仓库所属部门")
    private String itemWarehouseDeptName;

}
