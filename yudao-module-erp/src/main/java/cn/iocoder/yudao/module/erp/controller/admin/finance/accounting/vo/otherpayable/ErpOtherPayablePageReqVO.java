package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.otherpayable;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 其他应付单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpOtherPayablePageReqVO extends PageParam {

    @Schema(description = "单号", example = "QTYF20260516000001")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "往来单位类型", example = "1")
    private Integer partyType;

    @Schema(description = "往来单位 ID", example = "1024")
    private Long partyId;

    @Schema(description = "往来单位名称", example = "张三")
    private String partyName;

    @Schema(description = "结算账户 ID", example = "1")
    private Long accountId;

    @Schema(description = "业务时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否返回明细，默认 true 保持兼容")
    private Boolean includeItems;

}
