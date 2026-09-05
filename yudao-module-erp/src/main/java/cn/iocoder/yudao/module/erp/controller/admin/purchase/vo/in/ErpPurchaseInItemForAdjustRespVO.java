package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购入库项 - 供采购调价"添加明细"方式选择使用的明细 VO
 */
@Schema(description = "管理后台 - ERP 采购入库明细（供调价选择） Response VO")
@Data
public class ErpPurchaseInItemForAdjustRespVO {

    @Schema(description = "入库项编号（即 inItemId）", example = "23001")
    private Long id;

    @Schema(description = "入库单编号", example = "17386")
    private Long inId;

    @Schema(description = "入库单号", example = "PI20260512001")
    private String inNo;

    @Schema(description = "入库时间")
    private LocalDateTime inTime;

    @Schema(description = "产品编号", example = "3001")
    private Long productId;

    @Schema(description = "产品编码", example = "P-0001")
    private String productCode;

    @Schema(description = "产品名称", example = "博世火花塞")
    private String productName;

    @Schema(description = "批次号", example = "BATCH-001")
    private String batchNo;

    @Schema(description = "产品单位编号", example = "1")
    private Long productUnitId;

    @Schema(description = "产品单位名称", example = "件")
    private String productUnitName;

    @Schema(description = "产品重量", example = "1.00")
    private BigDecimal weight;

    @Schema(description = "包装数", example = "1")
    private Integer packageQty;

    @Schema(description = "仓库编号", example = "101")
    private Long warehouseId;
    private Long deptId;

    @Schema(description = "仓库名称", example = "主仓库")
    private String warehouseName;

    @Schema(description = "当前进价", example = "100.00")
    private BigDecimal productPrice;

    @Schema(description = "原进价（首次调价前的快照，null 表示未调过）", example = "90.00")
    private BigDecimal originalProductPrice;

    @Schema(description = "入库数量", example = "10.00")
    private BigDecimal count;

    @Schema(description = "总价", example = "1000.00")
    private BigDecimal totalPrice;

    @Schema(description = "适用车型", example = "大众-朗逸")
    private String vehicleModel;

    @Schema(description = "规格", example = "M14x1.25")
    private String standard;

    @Schema(description = "特征码", example = "SP-01")
    private String featureCode;

    @Schema(description = "产地", example = "德国")
    private String originPlace;

    @Schema(description = "品牌", example = "博世")
    private String brand;

    @Schema(description = "图号", example = "DWG-001")
    private String drawingNo;

    @Schema(description = "货架位", example = "A-01-02")
    private String warehousePosition;

    @Schema(description = "是否被调价过", example = "false")
    private Boolean adjusted;

    @Schema(description = "来源入库单是否已完成采购票据开具", example = "false")
    private Boolean hasInvoice;

}
