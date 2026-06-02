package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.List;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "ERP 其他应付分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPayableOtherPageReqVO extends PageParam {
    private List<Long> ids;

    @Schema(description = "单据编号")
    private String no;

    @Schema(description = "业务日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] bizTime;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "经手人编号")
    private Long handlerId;

    @Schema(description = "状态")
    private Integer status;

    public LocalDate getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDate getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }

}
