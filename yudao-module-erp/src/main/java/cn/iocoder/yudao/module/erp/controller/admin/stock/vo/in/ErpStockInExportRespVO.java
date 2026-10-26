package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpStockInExportRespVO {
    @ExcelProperty("编号")
    private Long id;

    @ExcelProperty("单号")
    private String no;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("仓库汇总")
    private String warehouseNames;

    @ExcelProperty(value = "合计数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal totalCount;

    @ExcelProperty(value = "合计金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;

    @ExcelProperty(value = "合计重量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal totalWeight;

    @ExcelProperty("项数")
    private Integer itemCount;

    @ExcelProperty("状态")
    private String status;

    @ExcelProperty("下单确认人")
    private String confirmUserName;

    @ExcelProperty("下单确认时间")
    private LocalDateTime confirmTime;

    @ExcelProperty("拣货状态")
    private String pickStatusName;

    @ExcelProperty("拣货进度")
    private String pickProgress;

    @ExcelProperty("拣货明细总数")
    private Integer pickTotalItemCount;

    @ExcelProperty("已拣货明细数")
    private Integer pickedItemCount;

    @ExcelProperty("审批人")
    private String approveUserName;

    @ExcelProperty("审批时间")
    private LocalDateTime approveTime;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建人")
    private String creatorName;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("修改人")
    private String updaterName;

    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

    @ExcelProperty("产品信息")
    private String productNames;

    @ExcelProperty("产品编码汇总")
    private String productCodes;

    @ExcelProperty("打印次数")
    private Integer printCount;

    @ExcelProperty("最后打印时间")
    private LocalDateTime lastPrintTime;

    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("入库时间")
    private LocalDateTime inTime;

    @ExcelProperty("产品编码")
    private String itemProductCode;

    @ExcelProperty("产品名称")
    private String itemProductName;

    @ExcelProperty("条码")
    private String itemProductBarCode;

    @ExcelProperty("单位")
    private String itemProductUnitName;

    @ExcelProperty("仓库")
    private String itemWarehouseName;

    @ExcelProperty("仓库所属部门")
    private String itemWarehouseDeptName;

    @ExcelProperty("批次")
    private String itemBatchNo;

    @ExcelProperty("当前商品货架位")
    private String itemShelf;

    @ExcelProperty("厂家编码")
    private String itemFactoryCode;

    @ExcelProperty("图号")
    private String itemDrawingNo;

    @ExcelProperty("品牌")
    private String itemBrand;

    @ExcelProperty("车型")
    private String itemVehicleModel;

    @ExcelProperty("产地")
    private String itemOriginPlace;

    @ExcelProperty("包装数")
    private Integer itemPackageQty;

    @ExcelProperty(value = "件数", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal itemPieceCount;

    @ExcelProperty(value = "单重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal itemWeight;

    @ExcelProperty(value = "总重", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal itemTotalWeight;

    @ExcelProperty(value = "数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.####################")
    private BigDecimal itemQuantity;

    @ExcelProperty(value = "业务单价", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemProductPrice;

    @ExcelProperty(value = "业务金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal itemTotalPrice;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
