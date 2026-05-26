package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 凭证归属分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVoucherAttributionPageReqVO extends PageParam {

    @Schema(description = "业务单据类型", example = "1")
    private Integer bizType;

    @Schema(description = "业务单号", example = "CGRK202605000001")
    private String bizNo;

    @Schema(description = "业务发生日期范围", example = "['2026-05-01 00:00:00','2026-05-31 23:59:59']")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizDate;

    @Schema(description = "归属状态", example = "10")
    private Integer attributionStatus;

    @Schema(description = "归属年", example = "2026")
    private Integer attributionYear;

    @Schema(description = "归属月", example = "5")
    private Integer attributionMonth;

    @Schema(description = "业务员 ID", example = "100")
    private Long handlerUserId;

    @Schema(description = "审核人 ID", example = "100")
    private Long auditorUserId;

    @Schema(description = "结算方式", example = "现金")
    private String settleMethod;

    @Schema(description = "部门 ID", example = "1024")
    private Long deptId;

    @Schema(description = "应收确认", example = "true")
    private Boolean receivableConfirmed;

    @Schema(description = "是否开票", example = "false")
    private Boolean invoiceIssued;

    @Schema(description = "发货状态", example = "10")
    private Integer shipStatus;

    @Schema(description = "交易单位（模糊）", example = "客户A")
    private String transactionParty;

    @Schema(description = "摘要（模糊）", example = "采购")
    private String summary;

}
