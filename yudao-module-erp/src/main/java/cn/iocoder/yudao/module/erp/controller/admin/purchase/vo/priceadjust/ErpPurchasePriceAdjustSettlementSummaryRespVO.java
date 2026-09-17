package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 采购调价结算核销摘要 Response VO")
@Data
@Accessors(chain = true)
public class ErpPurchasePriceAdjustSettlementSummaryRespVO {

    @Schema(description = "采购调价编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应结算金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "-100.00")
    private BigDecimal settlementAmount;

    @Schema(description = "已核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "-60.00")
    private BigDecimal writtenOffAmount;

    @Schema(description = "未核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "-40.00")
    private BigDecimal unwrittenOffAmount;

    @Schema(description = "结算状态：0 未核销，1 部分核销，2 已核销", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer settlementStatus;

}
