package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@ExcelIgnoreUnannotated
public class ErpStockTransferExportRespVO {
    @ExcelProperty("调拨编号")
    private Long id;
    @ExcelProperty("调拨单号")
    private String no;
    @ExcelProperty("所属部门ID")
    private Long deptId;
    @ExcelProperty("所属部门")
    private String deptName;
    @ExcelProperty("关联调拨单号")
    private String relatedMoveNo;
    @ExcelProperty("调出部门")
    private String fromDeptName;
    @ExcelProperty("调入部门")
    private String toDeptName;
    @ExcelProperty("调拨时间")
    private LocalDateTime moveTime;
    @ExcelProperty("直发客户")
    private String directCustomerName;
    @ExcelProperty(value = "合计数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalCount;
    @ExcelProperty(value = "合计金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("审核人")
    private String approveUserName;
    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;
    @ExcelProperty("备注")
    private String remark;
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;
    @ExcelProperty("打印次数")
    @NumberFormat("0")
    private Integer printCount;
    @ExcelProperty("打印时间")
    private LocalDateTime lastPrintTime;
    @ExcelProperty("产品信息")
    private String productNames;
    @ExcelProperty("产品编码")
    private String productCodes;
    @ExcelProperty("来源单号")
    private String sourceNo;
    @ExcelProperty("来源类型")
    @NumberFormat("0")
    private Integer sourceType;
    @ExcelProperty("创建人")
    private String creatorName;
    @ExcelProperty("修改人")
    private String updaterName;
    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;
    @ExcelProperty(value = "总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalWeight;
    @ExcelProperty("项数")
    @NumberFormat("0")
    private Integer itemCount;
    @ExcelProperty("调出仓库汇总")
    private String fromWarehouseNames;
    @ExcelProperty("调入仓库汇总")
    private String toWarehouseNames;
    @ExcelProperty("明细-明细ID")
    private Long itemId;
    @ExcelProperty("明细-调出仓库")
    private String itemFromWarehouseName;
    @ExcelProperty("明细-调出仓库部门")
    private String itemFromWarehouseDeptName;
    @ExcelProperty("明细-调入仓库")
    private String itemToWarehouseName;
    @ExcelProperty("明细-调入仓库部门")
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
    @ExcelProperty("明细-调出货架")
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
    @ExcelProperty(value = "明细-调出仓当前库存", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal itemStockCount;
}
