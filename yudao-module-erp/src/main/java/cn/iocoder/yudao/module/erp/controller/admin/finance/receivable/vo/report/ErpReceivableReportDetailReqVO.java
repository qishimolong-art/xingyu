package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.report;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
public class ErpReceivableReportDetailReqVO {

    @NotNull
    private Long customerId;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    public LocalDateTime getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDateTime getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }

    public LocalDate getStartDate() {
        return getStartTime() == null ? null : getStartTime().toLocalDate();
    }

    public LocalDate getEndDate() {
        return getEndTime() == null ? null : getEndTime().toLocalDate();
    }
}
