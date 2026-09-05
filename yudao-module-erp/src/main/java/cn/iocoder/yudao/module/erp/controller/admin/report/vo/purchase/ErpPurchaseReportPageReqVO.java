package cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseReportPageReqVO extends PageParam {

    private Long supplierId;
    private Long deptId;
    private String purchaser;
    private String keyword;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    private String orderField;
    private String orderDirection;

    public LocalDateTime getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDateTime getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }
}
