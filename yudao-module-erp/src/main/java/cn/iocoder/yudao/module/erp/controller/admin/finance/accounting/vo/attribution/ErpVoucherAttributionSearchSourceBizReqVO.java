package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

/**
 * ERP 凭证生成-按单据来源查询业务单据 Request VO
 *
 * 用于「凭证生成」页面：先选单据类型 + 日期 → 查询，返回对应的业务单据列表（封装为 attribution 形态）。
 * 见 刘/财务问题汇总-ds修订版.md 5 节。
 */
@Schema(description = "管理后台 - ERP 凭证生成-按单据来源查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVoucherAttributionSearchSourceBizReqVO extends PageParam {

    @Schema(description = "单据来源类型（1-20，见 ErpVoucherSourceBizTypeEnum）", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    @NotNull(message = "单据来源类型不能为空")
    private Integer sourceBizType;

    @Schema(description = "业务发生开始日期", example = "2026-05-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate bizDateStart;

    @Schema(description = "业务发生结束日期", example = "2026-05-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate bizDateEnd;

    @Schema(description = "单号（模糊）", example = "CGRK")
    private String bizNo;

    @Schema(description = "交易单位 / 客户 / 供应商名称（模糊）", example = "客户A")
    private String partyName;

    /** 给 Service 使用的开始时刻（当日 00:00:00），避免 LocalDate 与 LocalDateTime 列直接比较丢当日数据 */
    @JsonIgnore
    public LocalDateTime getBizDateStartTime() {
        return bizDateStart == null ? null : bizDateStart.atStartOfDay();
    }

    /** 给 Service 使用的结束时刻（当日 23:59:59） */
    @JsonIgnore
    public LocalDateTime getBizDateEndTime() {
        return bizDateEnd == null ? null : bizDateEnd.atTime(LocalTime.MAX);
    }

}
