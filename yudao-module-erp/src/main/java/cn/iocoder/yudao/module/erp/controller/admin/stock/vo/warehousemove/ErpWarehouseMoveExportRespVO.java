package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@ExcelIgnoreUnannotated
public class ErpWarehouseMoveExportRespVO {
    @ExcelProperty("移货单 ID")
    private Long id;
    @ExcelProperty("移货单号")
    private String no;
    @ExcelProperty("所属部门")
    private String deptName;
    @ExcelProperty("移货日期")
    private LocalDateTime moveTime;
    @ExcelProperty("移出仓库")
    private String fromWarehouseName;
    @ExcelProperty("移入仓库")
    private String toWarehouseName;
    @ExcelProperty("经办人")
    private String handlerName;
    @ExcelProperty("来源单号")
    private String sourceNo;
    @ExcelProperty("项数")
    @NumberFormat("0")
    private Integer itemCount;
    @ExcelProperty(value = "移货数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalCount;
    @ExcelProperty(value = "移货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;
    @ExcelProperty(value = "成本金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalCostAmount;
    @ExcelProperty("状态")
    private String statusName;
    @ExcelProperty("下单确认人")
    private String confirmUserName;
    @ExcelProperty("下单确认时间")
    private LocalDateTime confirmTime;
    @ExcelProperty("拣货进度")
    private String pickProgress;
    @ExcelProperty("审核人")
    private String approveUserName;
    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;
    @ExcelProperty("备注")
    private String remark;
    @ExcelProperty("创建人")
    private String creatorName;
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    @ExcelProperty("产品名称")
    private String productNames;
    @ExcelProperty("产品编码")
    private String productCodes;
    @ExcelProperty("来源类型")
    @NumberFormat("0")
    private Integer sourceType;
    @ExcelProperty("修改人")
    private String updaterName;
    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;
    @ExcelProperty(value = "总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalWeight;
    @ExcelProperty("明细-明细ID")
    private Long itemId;
    @ExcelProperty("明细-移出仓库")
    private String itemFromWarehouseName;
    @ExcelProperty("明细-移出仓库部门")
    private String itemFromWarehouseDeptName;
    @ExcelProperty("明细-移入仓库")
    private String itemToWarehouseName;
    @ExcelProperty("明细-移入仓库部门")
    private String itemToWarehouseDeptName;
    @ExcelProperty("明细-产品编码")
    private String itemProductCode;
    @ExcelProperty("明细-产品名称")
    private String itemProductName;
    @ExcelProperty("明细-条码")
    private String itemProductBarCode;
    @ExcelProperty("明细-单位")
    private String itemProductUnitName;
    @ExcelProperty("明细-批次")
    private String itemBatchNo;
    @ExcelProperty("明细-移出货架")
    private String itemFromShelf;
    @ExcelProperty("明细-包装数")
    @NumberFormat("0")
    private Integer itemPackageQty;
    @ExcelProperty(value = "明细-件数", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemPieceCount;
    @ExcelProperty(value = "明细-单重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemWeight;
    @ExcelProperty(value = "明细-总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemTotalWeight;
    @ExcelProperty(value = "明细-数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemQuantity;
    @ExcelProperty(value = "明细-产品单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemProductPrice;
    @ExcelProperty(value = "明细-金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemTotalPrice;
    @ExcelProperty("明细-备注")
    private String itemRemark;
    @ExcelProperty("明细-移入货架")
    private String itemToShelf;
    @ExcelProperty(value = "明细-成本单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemCostPrice;
    @ExcelProperty(value = "明细-成本金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemCostAmount;
    @ExcelProperty(value = "明细-移出仓当前库存", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemFromStockCount;
    @ExcelProperty(value = "明细-移入仓当前库存", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemToStockCount;
    @ExcelProperty("明细-车型")
    private String itemVehicleModel;
    @ExcelProperty("明细-产地")
    private String itemOriginPlace;
    @ExcelProperty("明细-规格")
    private String itemStandard;
    @ExcelProperty("明细-特征码")
    private String itemFeatureCode;
    @ExcelProperty("明细-品牌")
    private String itemBrand;
    @ExcelProperty("明细-图号")
    private String itemDrawingNo;
}
