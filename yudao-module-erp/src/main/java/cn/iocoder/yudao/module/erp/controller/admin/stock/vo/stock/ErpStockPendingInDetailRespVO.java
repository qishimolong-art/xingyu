package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Admin - ERP stock pending-in detail Response VO")
@Data
public class ErpStockPendingInDetailRespVO {

    @Schema(description = "Document type", example = "PURCHASE_IN")
    private String documentType;

    @Schema(description = "Document id", example = "1")
    private Long documentId;

    @Schema(description = "Purchase in id", example = "1")
    private Long inId;

    @Schema(description = "Stock check id", example = "1")
    private Long checkId;

    @Schema(description = "Purchase in item id", example = "10")
    private Long itemId;

    @Schema(description = "No", example = "CGRK202607130001")
    private String no;

    @Schema(description = "Product id", example = "100")
    private Long productId;

    @Schema(description = "Product code", example = "P000001")
    private String productCode;

    @Schema(description = "Product name", example = "Oil filter")
    private String productName;

    @Schema(description = "Warehouse id", example = "20")
    private Long warehouseId;

    @Schema(description = "Warehouse name", example = "Main warehouse")
    private String warehouseName;

    @Schema(description = "批次号", example = "BATCH-001")
    private String batchNo;

    @Schema(description = "Count", example = "10")
    private BigDecimal count;

    @Schema(description = "Vehicle model", example = "A6L")
    private String vehicleModel;

    @Schema(description = "Origin place", example = "Shanghai")
    private String originPlace;

    @Schema(description = "Drawing no", example = "DR-001")
    private String drawingNo;

    @Schema(description = "Standard", example = "STD")
    private String standard;

    @Schema(description = "Create time")
    private LocalDateTime createTime;

    @Schema(description = "Creator id", example = "1")
    private String creator;

    @Schema(description = "Creator name", example = "Admin")
    private String creatorName;

}
