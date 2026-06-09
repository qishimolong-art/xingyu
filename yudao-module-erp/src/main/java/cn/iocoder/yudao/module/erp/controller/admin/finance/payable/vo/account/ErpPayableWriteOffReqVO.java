package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "ERP 应付账款核销 Request VO")
@Data
public class ErpPayableWriteOffReqVO {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "业务类型")
    private Integer bizType;

    @Schema(description = "业务单据编号")
    private Long bizId;

    @Schema(description = "业务单据号")
    private String bizNo;

    @Schema(description = "核销金额", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "核销金额不能为空")
    @DecimalMin(value = "0", inclusive = false, message = "核销金额必须大于 0")
    private BigDecimal writeOffAmount;

    @Schema(description = "核销备注")
    private String remark;

}
