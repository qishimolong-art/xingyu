package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 付款单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFinancePaymentPageReqVO extends PageParam {

    @Schema(description = "付款单编号", example = "XS001")
    private String no;

    @Schema(description = "付款时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] paymentTime;

    @Schema(description = "供应商编号", example = "1724")
    private Long supplierId;

    @Schema(description = "创建者", example = "666")
    private String creator;

    @Schema(description = "经手人编号", example = "888")
    private String financeUserId;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "结算账户编号", example = "31189")
    private Long accountId;

    @Schema(description = "付款状态", example = "2")
    private Integer status;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "业务编号", example = "123")
    private String bizNo;

    @Schema(description = "勾选导出的付款单编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "是否返回明细；为空或 true 时兼容旧行为返回明细，false 时仅返回列表主表和轻量汇总")
    private Boolean includeItems;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
