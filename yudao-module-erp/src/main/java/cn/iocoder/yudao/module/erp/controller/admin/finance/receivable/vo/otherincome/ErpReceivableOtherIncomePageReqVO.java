package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 其他收入分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpReceivableOtherIncomePageReqVO extends PageParam {
    private List<Long> ids;
    private String no;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;
    private String settleMethod;
    private String incomeType;
    private Long accountId;
    private Long deptId;
    private Long handlerId;
    private String party;
    private Integer status;
    public LocalDateTime getStartTime() { return bizTime == null || bizTime.length == 0 ? null : bizTime[0]; }
    public LocalDateTime getEndTime() { return bizTime == null || bizTime.length < 2 ? null : bizTime[1]; }
}
