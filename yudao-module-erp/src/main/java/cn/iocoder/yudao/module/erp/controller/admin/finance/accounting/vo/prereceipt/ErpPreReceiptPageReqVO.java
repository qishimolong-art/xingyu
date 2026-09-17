package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 预收款单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPreReceiptPageReqVO extends PageParam {

    @Schema(description = "预收款单号", example = "YSKD20260516")
    private String no;

    @Schema(description = "业务时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizTime;

    @Schema(description = "往来类型：1=客户 2=供应商 3=员工", example = "1")
    private Integer partyType;

    @Schema(description = "往来方 ID", example = "100")
    private Long partyId;

    @Schema(description = "往来方名称", example = "张三")
    private String partyName;

    @Schema(description = "结算账户编号", example = "1")
    private Long accountId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "是否返回明细，默认 true 保持兼容")
    private Boolean includeItems;

}
