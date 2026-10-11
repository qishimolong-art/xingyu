package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.enums.finance.ErpReceivableDrilldownTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.AssertTrue;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 应收账款金额列下钻 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpReceivableDrilldownReqVO extends PageParam {

    @NotNull
    private Long customerId;
    private Long deptId;
    private Boolean deptUnassigned;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @NotBlank
    private String detailType;
    private String orderField;
    private String orderDirection;

    public LocalDateTime getStartTime() {
        return bizTime == null || bizTime.length == 0 ? null : bizTime[0];
    }

    public LocalDateTime getEndTime() {
        return bizTime == null || bizTime.length < 2 ? null : bizTime[1];
    }

    public boolean isMiscDetail() {
        return ErpReceivableDrilldownTypeEnum.MISC_RECEIVABLE.name().equals(detailType);
    }

    @AssertTrue(message = "应收下钻类型不正确")
    public boolean isDetailTypeValid() {
        return ErpReceivableDrilldownTypeEnum.fromValue(detailType) != null;
    }
}
