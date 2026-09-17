package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 销售退货退款核销摘要 Response VO")
@Data
@Accessors(chain = true)
public class ErpSaleReturnRefundSummaryRespVO {

    @Schema(description = "销售退货编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应退金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal refundableAmount;

    @Schema(description = "已退金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal refundedAmount;

    @Schema(description = "未退金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "40.00")
    private BigDecimal unrefundedAmount;

    @Schema(description = "退款状态，0=未退款，1=部分退款，2=已退款", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer refundStatus;

}
