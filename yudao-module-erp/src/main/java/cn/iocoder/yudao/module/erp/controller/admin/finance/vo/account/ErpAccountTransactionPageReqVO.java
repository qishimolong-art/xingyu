package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 账户收付款流水分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAccountTransactionPageReqVO extends PageParam {

    @Schema(description = "账户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "账户编号不能为空")
    private Long accountId;

    @Schema(description = "账单类型：receipt-收款，payment-付款")
    private String billType;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] transactionTime;

    public LocalDateTime getStartTime() {
        return transactionTime == null || transactionTime.length == 0 ? null : transactionTime[0];
    }

    public LocalDateTime getEndTime() {
        return transactionTime == null || transactionTime.length < 2 ? null : transactionTime[1];
    }

}
