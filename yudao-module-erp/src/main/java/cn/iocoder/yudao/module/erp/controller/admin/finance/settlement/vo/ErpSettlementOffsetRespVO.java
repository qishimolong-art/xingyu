package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "ERP 应收冲应付 Response VO")
@Data
public class ErpSettlementOffsetRespVO {

    @Schema(description = "客户编码")
    private String customerCode;

    @Schema(description = "客户名称")
    private String subjectName;

    @Schema(description = "客户 ID")
    private Long customerId;

    @Schema(description = "供应商 ID")
    private Long supplierId;

    @Schema(description = "联系人")
    private String contact;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "应收金额")
    private BigDecimal receivableBalance;

    @Schema(description = "应付金额")
    private BigDecimal payableBalance;

    @Schema(description = "余额")
    private BigDecimal offsetBalance;
}
