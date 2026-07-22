package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "ERP 费用支付分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPayableExpensePageReqVO extends PageParam {
    private List<Long> ids;

    private String no;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] bizTime;

    private String settleMethod;

    private Long accountId;

    private String expenseType;

    private Long deptId;

    private Long handlerId;

    private String party;

    private String itemName;

    private String invoiceNo;

    private String remark;

    private Integer status;

    public LocalDate getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDate getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
