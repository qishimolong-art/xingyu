package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 付款核销撤销 Request VO")
@Data
public class ErpFinancePaymentWriteOffReverseReqVO {

    @NotNull(message = "核销明细编号不能为空")
    private Long itemId;

    @NotBlank(message = "撤销原因不能为空")
    private String reason;

}
