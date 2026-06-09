package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.settlement;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 收付款统一分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFinanceBillPageReqVO extends PageParam {

    @Schema(description = "单据编号")
    private String no;

    @Schema(description = "账单类型：receipt-收款，payment-付款")
    private String billType;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billTime;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "财务人员编号")
    private Long financeUserId;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "账户编号")
    private Long accountId;

    @Schema(description = "审核状态")
    private Integer status;

    @Schema(description = "业务单据号")
    private String bizNo;

    @Schema(description = "备注")
    private String remark;

    public LocalDateTime getStartTime() {
        return billTime == null || billTime.length == 0 ? null : billTime[0];
    }

    public LocalDateTime getEndTime() {
        return billTime == null || billTime.length < 2 ? null : billTime[1];
    }

}
