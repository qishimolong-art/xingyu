package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 采购订单可入库明细库存 Response VO")
@Data
public class ErpPurchaseOrderInableStockRespVO {

    @Schema(description = "采购订单项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long orderItemId;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long warehouseId;

    @Schema(description = "账面库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private BigDecimal stockCount;

    @Schema(description = "可用库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private BigDecimal availableCount;

}
