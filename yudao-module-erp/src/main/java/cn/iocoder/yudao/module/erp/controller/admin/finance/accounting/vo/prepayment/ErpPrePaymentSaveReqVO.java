package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 预付款单新增/修改 Request VO")
@Data
public class ErpPrePaymentSaveReqVO {

    @Schema(description = "编号（更新时必填）")
    private Long id;

    @Schema(description = "业务时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务日期不能为空")
    private LocalDateTime bizTime;

    @Schema(description = "往来单位类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "往来单位类型不能为空")
    private Integer partyType;

    @Schema(description = "往来单位 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "往来单位不能为空")
    private Long partyId;

    @Schema(description = "往来单位名称")
    private String partyName;

    @Schema(description = "结算账户 ID")
    private Long accountId;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件")
    private String fileUrl;

    @Schema(description = "子表明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "预付款明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号（更新时必填）")
        private Long id;

        @Schema(description = "摘要")
        private String summary;

        @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "金额不能为空")
        private BigDecimal amount;

        @Schema(description = "备注")
        private String remark;

    }

}
