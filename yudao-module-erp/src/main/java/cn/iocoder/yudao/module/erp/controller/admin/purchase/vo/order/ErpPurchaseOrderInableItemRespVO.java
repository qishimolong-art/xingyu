package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 采购订单可入库明细 Response VO")
@Data
public class ErpPurchaseOrderInableItemRespVO {

    @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long orderItemId;

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long productId;

    @Schema(description = "产品单位编号", example = "1")
    private Long productUnitId;

    @Schema(description = "产品名称", example = "刹车片")
    private String productName;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品单位名称", example = "个")
    private String productUnitName;

    @Schema(description = "产品单价", example = "100.00")
    private BigDecimal productPrice;

    @Schema(description = "税率", example = "13")
    private BigDecimal taxPercent;

    @Schema(description = "订单数量", example = "100")
    private BigDecimal orderCount;

    @Schema(description = "已入库数量", example = "30")
    private BigDecimal inCount;

    @Schema(description = "可入库数量", example = "70")
    private BigDecimal inableCount;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "部门 ID", example = "100")
    private Long deptId;

    @Schema(description = "是否赠品", example = "false")
    private Boolean gift;

    // ========== 产品扩展字段 ==========

    @Schema(description = "适用车型", example = "大众帕萨特")
    private String vehicleModel;

    @Schema(description = "产地", example = "广东")
    private String originPlace;

    @Schema(description = "品牌", example = "博世")
    private String brand;

    @Schema(description = "规格", example = "200mm")
    private String standard;

    @Schema(description = "图号", example = "DWG-001")
    private String drawingNo;

    @Schema(description = "货架位", example = "A-01-02")
    private String warehousePosition;

    @Schema(description = "包装数", example = "12")
    private Integer packageQty;

}
