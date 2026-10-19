package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 收款单候选业务单据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpFinanceReceiptFormCandidatePageReqVO extends PageParam {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "单据编号", example = "XSCK20260902000001")
    private String no;

    @Schema(description = "当前单据金额")
    private BigDecimal documentTotalPrice;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向")
    private String orderDirection;

    @Valid
    @Schema(description = "跨页已选业务单据")
    private List<SelectedItem> selectedItems;

    @Data
    public static class SelectedItem {

        @NotNull(message = "业务类型不能为空")
        private Integer bizType;

        @NotNull(message = "业务编号不能为空")
        private Long bizId;

        private BigDecimal amount;
    }
}
