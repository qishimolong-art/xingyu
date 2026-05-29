package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP finance transfer page request")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFinanceTransferPageReqVO extends PageParam {

    @Schema(description = "Transfer number", example = "YHZZ202605000001")
    private String no;

    @Schema(description = "Transfer time range")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] transferTime;

    @Schema(description = "Out account id", example = "1")
    private Long outAccountId;

    @Schema(description = "In account id", example = "2")
    private Long inAccountId;

    @Schema(description = "Finance user id", example = "100")
    private Long financeUserId;

    @Schema(description = "Creator", example = "1")
    private String creator;

    @Schema(description = "Status", example = "10")
    private Integer status;

    @Schema(description = "Remark", example = "same-bank transfer")
    private String remark;

}
