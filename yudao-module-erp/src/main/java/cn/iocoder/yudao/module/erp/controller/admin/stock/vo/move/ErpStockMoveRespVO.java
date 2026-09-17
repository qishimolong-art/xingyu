package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

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

@Schema(description = "Admin - ERP stock move Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpStockMoveRespVO {

    @Schema(description = "Stock move id", requiredMode = Schema.RequiredMode.REQUIRED, example = "11756")
    @ExcelProperty("调拨编号")
    private Long id;

    @Schema(description = "Stock move no", requiredMode = Schema.RequiredMode.REQUIRED, example = "S123")
    @ExcelProperty("调拨单号")
    private String no;

    @Schema(description = "Department id", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "Department name")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "Transfer direction, 10 transfer out, 20 transfer in", example = "10")
    private Integer transferDirection;

    @Schema(description = "Related stock move id", example = "1024")
    private Long relatedMoveId;

    @Schema(description = "Related stock move no", example = "QCDB20260714000001")
    @ExcelProperty("关联调拨单号")
    private String relatedMoveNo;

    @Schema(description = "From department id", example = "100")
    private Long fromDeptId;

    @Schema(description = "From department name")
    @ExcelProperty("调出部门")
    private String fromDeptName;

    @Schema(description = "To department id", example = "101")
    private Long toDeptId;

    @Schema(description = "To department name")
    @ExcelProperty("调入部门")
    private String toDeptName;

    @Schema(description = "Move time", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("调拨时间")
    private LocalDateTime moveTime;

    @Schema(description = "Source type", example = "30")
    private Integer sourceType;

    @Schema(description = "Source document id", example = "1024")
    private Long sourceId;

    @Schema(description = "Source document no", example = "SC202606100001")
    private String sourceNo;

    @Schema(description = "Direct shipment customer name")
    @ExcelProperty("直发客户")
    private String directCustomerName;

    @Schema(description = "Total count", requiredMode = Schema.RequiredMode.REQUIRED, example = "15663")
    @ExcelProperty("合计数量")
    private BigDecimal totalCount;

    @Schema(description = "Total price", requiredMode = Schema.RequiredMode.REQUIRED, example = "24906")
    @ExcelProperty("合计金额")
    private BigDecimal totalPrice;

    @Schema(description = "Status", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(AUDIT_STATUS)
    private Integer status;

    @Schema(description = "Approve user id", example = "1")
    private Long approveUserId;

    @Schema(description = "Approve user name")
    @ExcelProperty("审核人")
    private String approveUserName;

    @Schema(description = "Approve time")
    @ExcelProperty("审核时间")
    private LocalDateTime approveTime;

    @Schema(description = "Remark", example = "remark")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "Attachment URL", example = "https://www.iocoder.cn/1.doc")
    private String fileUrl;

    @Schema(description = "Creator", example = "admin")
    private String creator;

    @Schema(description = "Creator name", example = "admin")
    private String creatorName;

    private String updater;

    @Schema(description = "Create time", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "Updater name")
    private String updaterName;

    @Schema(description = "Update time")
    private LocalDateTime updateTime;

    @Schema(description = "Print count")
    @ExcelProperty("打印次数")
    private Integer printCount;

    @Schema(description = "Last print time")
    @ExcelProperty("打印时间")
    private LocalDateTime lastPrintTime;

    @Schema(description = "Move items", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Item> items;

    @Schema(description = "Product names", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("产品信息")
    private String productNames;

    @Schema(description = "Product codes")
    @ExcelProperty("产品编码")
    private String productCodes;

    @Schema(description = "Distinct from warehouse names")
    private String fromWarehouseNames;

    @Schema(description = "Distinct to warehouse names")
    private String toWarehouseNames;

    @Schema(description = "Item count")
    private Integer itemCount;

    @Schema(description = "Whether current user can approve")
    private Boolean approveAllowed;

    @Schema(description = "Approve disabled reason")
    private String approveDisabledReason;

    @Schema(description = "Whether current user can delete")
    private Boolean deleteAllowed;

    @Schema(description = "Delete disabled reason")
    private String deleteDisabledReason;

    @Schema(description = "Whether current user can unlock the source sale cart")
    private Boolean unlockCartAllowed;

    @Schema(description = "Unlock sale cart disabled reason")
    private String unlockCartDisabledReason;

    @Data
    public static class Item {

        @Schema(description = "Move item id", example = "11756")
        private Long id;

        @Schema(description = "From warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long fromWarehouseId;

        @Schema(description = "From warehouse name", example = "Main warehouse")
        private String fromWarehouseName;

        @Schema(description = "From warehouse department id", example = "100")
        private Long fromWarehouseDeptId;

        @Schema(description = "From warehouse department name", example = "Sales Department")
        private String fromWarehouseDeptName;

        @Schema(description = "To warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "888")
        private Long toWarehouseId;

        @Schema(description = "To warehouse name", example = "Branch warehouse")
        private String toWarehouseName;

        @Schema(description = "To warehouse department id", example = "101")
        private Long toWarehouseDeptId;

        @Schema(description = "To warehouse department name", example = "Warehouse Department")
        private String toWarehouseDeptName;

        @Schema(description = "From department id", example = "100")
        private Long fromDeptId;

        @Schema(description = "From department name")
        private String fromDeptName;

        @Schema(description = "To department id", example = "101")
        private Long toDeptId;

        @Schema(description = "To department name")
        private String toDeptName;

        @Schema(description = "Product id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        private Long productId;

        @Schema(description = "Product purchase price from product or stock", example = "88.00")
        private BigDecimal productPurchasePrice;
        @Schema(description = "Sale price", example = "120.00")
        private BigDecimal salePrice;
        @Schema(description = "Latest sale price", example = "118.00")
        private BigDecimal lastSalePrice;
        @Schema(description = "Minimum price", example = "95.00")
        private BigDecimal minPrice;
        @Schema(description = "Reference price", example = "100.00")
        private BigDecimal referencePrice;
        @Schema(description = "Retail price", example = "128.00")
        private BigDecimal retailPrice;
        @Schema(description = "Last purchase price", example = "86.00")
        private BigDecimal lastPurchasePrice;
        @Schema(description = "Gross profit rate", example = "20")
        private Integer grossProfitRate;
        @Schema(description = "Backup price 1", example = "98.00")
        private BigDecimal backupPrice1;
        @Schema(description = "Wholesale price", example = "98.00")
        private BigDecimal wholesalePrice;
        @Schema(description = "Share price", example = "90.00")
        private BigDecimal sharePrice;
        @Schema(description = "Price visible", example = "true")
        private Boolean priceVisible;

        @Schema(description = "Product price", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "Product count", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal count;

        @Schema(description = "Product unit id")
        private Long productUnitId;

        @Schema(description = "Package quantity", example = "12")
        private Integer packageQty;

        @Schema(description = "Unit weight", example = "1.25")
        private BigDecimal weight;

        @Schema(description = "Total weight", example = "125.00")
        private BigDecimal totalWeight;

        @Schema(description = "Remark", example = "remark")
        private String remark;

        @Schema(description = "Product name", requiredMode = Schema.RequiredMode.REQUIRED, example = "product")
        private String productName;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Product bar code", requiredMode = Schema.RequiredMode.REQUIRED, example = "A9985")
        private String productBarCode;

        @Schema(description = "Product unit name", requiredMode = Schema.RequiredMode.REQUIRED, example = "pcs")
        private String productUnitName;

        @Schema(description = "Stock count", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        private BigDecimal stockCount;

        @Schema(description = "From shelf", example = "A-01")
        private String fromShelf;

        @Schema(description = "Batch no", example = "B20260714001")
        private String batchNo;

        @Schema(description = "Source purchase in id", example = "1024")
        private Long sourceInId;

        @Schema(description = "Source purchase in item id", example = "2048")
        private Long sourceInItemId;

        @Schema(description = "Source purchase in no", example = "CGRK202607140001")
        private String sourceInNo;

        @Schema(description = "Source purchase in count", example = "100.00")
        private BigDecimal sourceCount;

        @Schema(description = "Source sale return id", example = "1024")
        private Long sourceSaleReturnId;

        @Schema(description = "Source sale return item id", example = "2048")
        private Long sourceSaleReturnItemId;

        @Schema(description = "Source sale return no", example = "XSTH202607140001")
        private String sourceSaleReturnNo;

    }

}
