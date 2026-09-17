package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.DictTypeConstants.AUDIT_STATUS;

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
    private Integer sourceType;

    @Schema(description = "来源单据 ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    @ExcelProperty("来源单号")
    private String sourceNo;

    @Schema(description = "项数")
    @ExcelProperty("项数")
    private Integer itemCount;

    @Schema(description = "移货数量合计")
    @ExcelProperty("移货数量")
    private BigDecimal totalCount;

    @Schema(description = "移货金额合计")
    @ExcelProperty("移货金额")
    private BigDecimal totalPrice;

    @Schema(description = "成本金额合计")
    @ExcelProperty("成本金额")
    private BigDecimal totalCostAmount;

    @Schema(description = "状态")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(AUDIT_STATUS)
    private Integer status;

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
    @ExcelProperty("产品编码")
    private String productCodes;

    @Schema(description = "明细")
    private List<Item> items;

    @Data
    public static class Item {

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
        private Integer packageQty;
        private String fromShelf;
        private String toShelf;
        private BigDecimal productPurchasePrice;
        private BigDecimal salePrice;
        private BigDecimal lastSalePrice;
        private BigDecimal minPrice;
        private BigDecimal referencePrice;
        private BigDecimal retailPrice;
        private BigDecimal lastPurchasePrice;
        private Integer grossProfitRate;
        private BigDecimal backupPrice1;
        private BigDecimal wholesalePrice;
        private BigDecimal sharePrice;
        private Boolean priceVisible;
        private BigDecimal productPrice;
        private BigDecimal count;
        private BigDecimal totalPrice;
        private BigDecimal costPrice;
        private BigDecimal costAmount;
        private BigDecimal weight;
        private BigDecimal totalWeight;
        private String batchNo;
        private BigDecimal fromStockCount;
        private BigDecimal toStockCount;
        private String remark;

    }

}
