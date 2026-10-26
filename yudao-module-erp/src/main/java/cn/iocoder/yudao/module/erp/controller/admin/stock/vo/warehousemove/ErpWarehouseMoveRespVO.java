package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - ERP warehouse move Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpWarehouseMoveRespVO {

    @Schema(description = "移货单 ID")
    @ExcelProperty("移货单 ID")
    private Long id;

    @Schema(description = "移货单号")
    @ExcelProperty("移货单号")
    private String no;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "所属部门")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "移货日期")
    @ExcelProperty("移货日期")
    private LocalDateTime moveTime;

    @Schema(description = "移出仓库 ID")
    private Long fromWarehouseId;

    @Schema(description = "移出仓库")
    @ExcelProperty("移出仓库")
    private String fromWarehouseName;

    @Schema(description = "移入仓库 ID")
    private Long toWarehouseId;

    @Schema(description = "移入仓库")
    @ExcelProperty("移入仓库")
    private String toWarehouseName;

    @Schema(description = "经办人")
    private Long handlerId;

    @Schema(description = "经办人")
    @ExcelProperty("经办人")
    private String handlerName;

    @Schema(description = "来源类型")
    @NumberFormat("0")
    private Integer sourceType;

    @Schema(description = "来源单据 ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    @ExcelProperty("来源单号")
    private String sourceNo;

    @Schema(description = "项数")
    @ExcelProperty("项数")
    @NumberFormat("0")
    private Integer itemCount;

    @Schema(description = "移货数量合计")
    @ExcelProperty(value = "移货数量", converter = cn.iocoder.yudao.module.erp.framework.excel.StockDecimalExcelConverter.class)
    @NumberFormat("0.######")
    private BigDecimal totalCount;

    @Schema(description = "移货金额合计")
    @ExcelProperty(value = "移货金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalPrice;

    @Schema(description = "成本金额合计")
    @ExcelProperty(value = "成本金额", converter = cn.iocoder.yudao.module.erp.framework.excel.FinanceDecimalExcelConverter.class)
    private BigDecimal totalCostAmount;

    @Schema(description = "整单总重")
    @NumberFormat("0.######")
    private BigDecimal totalWeight;

    @Schema(description = "状态")
    @ExcelIgnore
    @NumberFormat("0")
    private Integer status;

    @Schema(description = "状态名称")
    @ExcelProperty("状态")
    private String statusName;

    @Schema(description = "下单确认人 ID")
    private Long confirmUserId;

    @Schema(description = "下单确认人")
    @ExcelProperty("下单确认人")
    private String confirmUserName;

    @Schema(description = "下单确认时间")
    @ExcelProperty("下单确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "拣货状态")
    @NumberFormat("0")
    private Integer pickStatus;

    @Schema(description = "已拣货明细数")
    @NumberFormat("0")
    private Integer pickedItemCount;

    @Schema(description = "拣货明细总数")
    @NumberFormat("0")
    private Integer pickTotalItemCount;

    @Schema(description = "拣货进度")
    @ExcelProperty("拣货进度")
    private String pickProgress;

    @Schema(description = "审核人 ID")
    private Long approveUserId;

    @Schema(description = "审核人")
    @ExcelProperty("审核人")
    private String approveUserName;

    @Schema(description = "审核时间")
    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;

    @Schema(description = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "创建时间")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    private String updater;

    @Schema(description = "修改人")
    private String updaterName;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "产品名称")
    @ExcelProperty("产品名称")
    private String productNames;

    @Schema(description = "产品编码")
    @ExcelProperty(value = "产品编码")
    private String productCodes;

    @Schema(description = "明细")
    private List<Item> items;

    @Data
    public static class Item {
        private String fromWarehouseDeptName;
        private String toWarehouseDeptName;

        @Schema(description = "明细 ID")
        private Long id;
        private Long fromWarehouseId;
        private String fromWarehouseName;
        private Long toWarehouseId;
        private String toWarehouseName;
        private Long productId;
        private String productName;
        private String productCode;
        private String productBarCode;
        private String productUnitName;
        private String vehicleModel;
        private String originPlace;
        private String standard;
        private String featureCode;
        private String brand;
        private String drawingNo;
        private Long productUnitId;
        @NumberFormat("0")
    private Integer packageQty;
        private String fromShelf;
        private String toShelf;
        @NumberFormat("0.####################")
    private BigDecimal productPurchasePrice;
        @NumberFormat("0.####################")
    private BigDecimal salePrice;
        @NumberFormat("0.####################")
    private BigDecimal lastSalePrice;
        @NumberFormat("0.####################")
    private BigDecimal minPrice;
        @NumberFormat("0.####################")
    private BigDecimal referencePrice;
        @NumberFormat("0.####################")
    private BigDecimal retailPrice;
        @NumberFormat("0.####################")
    private BigDecimal lastPurchasePrice;
        @NumberFormat("0")
    private Integer grossProfitRate;
        @NumberFormat("0.####################")
    private BigDecimal backupPrice1;
        @NumberFormat("0.####################")
    private BigDecimal wholesalePrice;
        @NumberFormat("0.####################")
    private BigDecimal sharePrice;
        private Boolean priceVisible;
        @NumberFormat("0.####################")
    private BigDecimal productPrice;
        @NumberFormat("0.######")
    private BigDecimal count;
        @NumberFormat("0.####################")
    private BigDecimal totalPrice;
        @NumberFormat("0.####################")
    private BigDecimal costPrice;
        @NumberFormat("0.####################")
    private BigDecimal costAmount;
        @NumberFormat("0.######")
    private BigDecimal weight;
        @NumberFormat("0.######")
    private BigDecimal totalWeight;
        private String batchNo;
        @NumberFormat("0.######")
    private BigDecimal fromStockCount;
        @NumberFormat("0.######")
    private BigDecimal toStockCount;
        private String remark;

    }

}
