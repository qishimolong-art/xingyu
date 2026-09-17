package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "ERP 其他应收分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpReceivableMiscPageReqVO extends PageParam {

    private List<Long> ids;

    @Schema(description = "单据编号")
    private String no;

    @Schema(description = "业务时间范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "账户编号")
    private Long accountId;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "经手人编号")
    private Long handlerId;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "关键字")
    private String keyword;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
