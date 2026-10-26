package cn.iocoder.yudao.module.erp.controller.admin.finance.settlement.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 应收冲应付分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSettlementOffsetPageReqVO extends PageParam {

    @Schema(description = "往来对象名称")
    private String subjectName;

    @Schema(description = "业务时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @Schema(description = "审批时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] approveTime;

    private String dateBasis;

    @Schema(description = "是否显示零余额", example = "false")
    private Boolean showZeroBalance;
    private String orderField;
    private String orderDirection;

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
