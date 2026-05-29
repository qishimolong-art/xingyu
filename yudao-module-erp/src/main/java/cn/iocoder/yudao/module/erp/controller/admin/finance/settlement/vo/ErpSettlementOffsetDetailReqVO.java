package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 应收冲应付明细 Request VO")
@Data
public class ErpSettlementOffsetDetailReqVO {

    @Schema(description = "客户 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "业务时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    public LocalDateTime getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDateTime getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }
}
