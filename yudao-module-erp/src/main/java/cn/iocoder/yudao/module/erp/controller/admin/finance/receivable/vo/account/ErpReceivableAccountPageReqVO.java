package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 应收账款分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpReceivableAccountPageReqVO extends PageParam {

    private Long customerId;
    private String customerName;
    private Long saleUserId;
    private Long deptId;
    private Integer customerType;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    private Boolean showZeroBalance;

    public LocalDateTime getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDateTime getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }
}
