package cn.iocoder.yudao.module.trade.controller.app.order.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - 交易订单创建 Response VO")
@Data
public class AppTradeOrderCreateRespVO {

    @Schema(description = "订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "订单流水号", requiredMode = Schema.RequiredMode.REQUIRED, example = "202308260001")
    private String no;

    @Schema(description = "支付订单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long payOrderId;

    @Schema(description = "ERP 销售手推车草稿编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long saleCartId;

    @Schema(description = "ERP 销售手推车草稿单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "XSTC202308260001")
    private String saleCartNo;

    @Schema(description = "ERP 销售手推车草稿状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer saleCartStatus;

}
