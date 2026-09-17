package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 采购入库付款核销摘要 Response VO")
@Data
@Accessors(chain = true)
public class ErpPurchaseInPaymentSummaryRespVO {

    @Schema(description = "采购入库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal payableAmount;

    @Schema(description = "已付/已核销金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal paidAmount;

    @Schema(description = "未付金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "40.00")
    private BigDecimal unpaidAmount;

    @Schema(description = "付款状态：0 未付款，1 部分付款，2 已付款", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer paymentStatus;

}
