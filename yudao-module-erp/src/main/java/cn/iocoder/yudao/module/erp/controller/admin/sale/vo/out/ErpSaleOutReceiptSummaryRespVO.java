package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 销售出库收款核销摘要 Response VO")
@Data
@Accessors(chain = true)
public class ErpSaleOutReceiptSummaryRespVO {

    @Schema(description = "销售出库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "应收金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal receivableAmount;

    @Schema(description = "已收金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "60.00")
    private BigDecimal receivedAmount;

    @Schema(description = "未收金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "40.00")
    private BigDecimal unreceivedAmount;

    @Schema(description = "收款状态，0=未收款，1=部分收款，2=已收款", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer receiptStatus;

}
