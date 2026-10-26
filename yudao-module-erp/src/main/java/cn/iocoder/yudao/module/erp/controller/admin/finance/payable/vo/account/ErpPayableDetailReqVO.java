package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 应付明细查询 Request VO")
@Data
public class ErpPayableDetailReqVO {

    @Schema(description = "供应商 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "业务时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @Schema(description = "审批时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] approveTime;

    private String dateBasis;

    public LocalDateTime getStartTime() {
        LocalDateTime[] timeRange = isApproveTimeBasis() ? approveTime : bizTime;
        return timeRange == null || timeRange.length == 0 ? null : timeRange[0];
    }

    public LocalDateTime getEndTime() {
        LocalDateTime[] timeRange = isApproveTimeBasis() ? approveTime : bizTime;
        return timeRange == null || timeRange.length < 2 ? null : timeRange[1];
    }

    public boolean isApproveTimeBasis() {
        return "approveTime".equals(dateBasis);
    }
}
