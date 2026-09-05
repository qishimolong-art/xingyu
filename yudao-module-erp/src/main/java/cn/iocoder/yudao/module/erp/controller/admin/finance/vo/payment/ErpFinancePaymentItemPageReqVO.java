package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 付款单明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFinancePaymentItemPageReqVO extends PageParam {

    @Schema(description = "付款单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "付款单编号不能为空")
    private Long paymentId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, bizType, bizId, bizNo, totalPrice, paidPrice, paymentPrice, writeOffStatus, remark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
