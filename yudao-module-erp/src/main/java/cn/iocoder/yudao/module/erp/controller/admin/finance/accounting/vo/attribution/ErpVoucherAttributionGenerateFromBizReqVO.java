package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 凭证生成-从业务单据快照直接生成凭证 Request VO。
 *
 * 「凭证生成」页面流程：选单据类型 + 日期 → 查询返回业务单据快照（attribution 表里还不存在记录）→
 * 用户勾选 → 设制单日期 + 归属年月 → 调本接口；后端先按 bizType+bizId 落 attribution，再生成凭证。
 */
@Schema(description = "管理后台 - 凭证生成-从业务单据快照生成凭证 Request VO")
@Data
public class ErpVoucherAttributionGenerateFromBizReqVO {

    @Schema(description = "业务单据快照列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "业务单据列表不能为空")
    @Valid
    private List<BizItem> items;

    @Schema(description = "制单日期（实际生成日期）", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-05-19")
    @NotNull(message = "制单日期不能为空")
    private LocalDate voucherMakeDate;

    @Schema(description = "归属年", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "归属年不能为空")
    private Integer attributionYear;

    @Schema(description = "归属月", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "归属月不能为空")
    private Integer attributionMonth;

    @Schema(description = "业务单据快照")
    @Data
    public static class BizItem {

        @Schema(description = "业务单据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
        @NotNull(message = "业务单据类型不能为空")
        private Integer bizType;

        @Schema(description = "业务单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "业务单 ID 不能为空")
        private Long bizId;

        @Schema(description = "业务单号", example = "CGRK202605000001")
        private String bizNo;

        @Schema(description = "业务发生日期", example = "2026-05-14 10:00:00")
        private LocalDateTime bizDate;

        @Schema(description = "原始金额", example = "100.00")
        private BigDecimal bizAmount;

        @Schema(description = "交易单位", example = "客户A")
        private String transactionParty;

        @Schema(description = "备注", example = "")
        private String remark;

    }

}
