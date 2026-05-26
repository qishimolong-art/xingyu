package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 预付款单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPrePaymentPageReqVO extends PageParam {

    @Schema(description = "预付款单号")
    private String no;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "往来单位类型")
    private Integer partyType;

    @Schema(description = "往来单位 ID")
    private Long partyId;

    @Schema(description = "往来单位名称")
    private String partyName;

    @Schema(description = "结算账户 ID")
    private Long accountId;

    @Schema(description = "业务时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

}
