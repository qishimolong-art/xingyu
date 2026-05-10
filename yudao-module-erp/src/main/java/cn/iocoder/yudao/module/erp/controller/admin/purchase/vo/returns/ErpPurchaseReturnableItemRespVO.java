package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 采购入库项的可退信息 Response VO")
@Data
public class ErpPurchaseReturnableItemRespVO {

    @Schema(description = "原采购入库单 ID", example = "17386")
    private Long sourceInId;
    @Schema(description = "原采购入库项 ID", example = "11756")
    private Long sourceInItemId;
    @Schema(description = "原采购入库单号", example = "RKD20260101001")
    private String sourceInNo;

    @Schema(description = "产品编号", example = "10001")
    private Long productId;
    @Schema(description = "产品单位编号", example = "1")
    private Long productUnitId;
    @Schema(description = "仓库编号", example = "2")
    private Long warehouseId;

    @Schema(description = "原入库单价", example = "12.34")
    private BigDecimal productPrice;
    @Schema(description = "原入库数量", example = "100")
    private BigDecimal inCount;
    @Schema(description = "已累计退货数量", example = "20")
    private BigDecimal returnedCount;
    @Schema(description = "可退数量 = inCount - returnedCount", example = "80")
    private BigDecimal returnableCount;

    @Schema(description = "税率", example = "13.00")
    private BigDecimal taxPercent;

    // ========== 子表业务字段（供前端直接带入退货单子表） ==========
    private Integer packageQty;
    private Integer wholeQty;
    private String warehousePosition;
    private String drawingNo;
    private String batchNo;
    private String barCode;
    private String brand;
    private String vehicleModel;
    private String originPlace;
    private String businessEntity;

    // ========== 冗余展示字段（前端可选用）==========
    @Schema(description = "备注")
    private String remark;

}
