package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 报价订单转销售手推车 Request VO")
@Data
public class ErpSaleQuoteConvertCartReqVO {

    @Schema(description = "报价订单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报价订单编号不能为空")
    private Long quoteId;

    @Valid
    @NotEmpty(message = "转换明细不能为空")
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {

        @Schema(description = "报价订单项编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "报价订单项编号不能为空")
        private Long quoteItemId;

        @Schema(description = "本次转换数量", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "本次转换数量不能为空")
        private BigDecimal count;

    }

}
