package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 付款单后续核销 Request VO")
@Data
public class ErpFinancePaymentWriteOffReqVO {

    @NotNull(message = "付款单编号不能为空")
    private Long paymentId;

    @Valid
    @NotEmpty(message = "核销明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "业务类型不能为空")
        private Integer bizType;

        @NotNull(message = "业务编号不能为空")
        private Long bizId;

        @NotNull(message = "核销金额不能为空")
        private BigDecimal writeOffAmount;

        private String remark;

    }

}
