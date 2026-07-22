package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "Admin - ERP purchase in sale-cartable item Response VO")
@Data
public class ErpPurchaseInSaleCartableItemRespVO {

    @Schema(description = "Source purchase in id", example = "1024")
    private Long sourceInId;

    @Schema(description = "Source purchase in item id", example = "2048")
    private Long sourceInItemId;

    @Schema(description = "Source purchase in no", example = "CGRK202607140001")
    private String sourceInNo;

    @Schema(description = "Product id", example = "100")
    private Long productId;

    @Schema(description = "Product name")
    private String productName;

    @Schema(description = "Product code")
    private String productCode;

    @Schema(description = "Product unit id")
    private Long productUnitId;

    @Schema(description = "Product unit name")
    private String productUnitName;

    @Schema(description = "Source warehouse id")
    private Long warehouseId;

    @Schema(description = "Source warehouse name")
    private String warehouseName;

    @Schema(description = "Source warehouse dept id")
    private Long warehouseDeptId;

    @Schema(description = "Source warehouse dept name")
    private String warehouseDeptName;

    @Schema(description = "Purchase in count")
    private BigDecimal inCount;

    @Schema(description = "Converted sale cart count")
    private BigDecimal convertedCount;

    @Schema(description = "Sale-cartable count")
    private BigDecimal saleCartableCount;

    @Schema(description = "Purchase price")
    private BigDecimal purchasePrice;

    @Schema(description = "Default sale price")
    private BigDecimal salePrice;

    @Schema(description = "Warehouse position")
    private String warehousePosition;

    @Schema(description = "Batch no")
    private String batchNo;

    @Schema(description = "Whether batch no is enabled")
    private Boolean batchNoEnabled;

    @Schema(description = "Product bar code")
    private String barCode;

    @Schema(description = "Brand")
    private String brand;

    @Schema(description = "Vehicle model")
    private String vehicleModel;

    @Schema(description = "Origin place")
    private String originPlace;

    @Schema(description = "Standard")
    private String standard;

    @Schema(description = "Business entity")
    private String businessEntity;

    @Schema(description = "Remark")
    private String remark;

}
