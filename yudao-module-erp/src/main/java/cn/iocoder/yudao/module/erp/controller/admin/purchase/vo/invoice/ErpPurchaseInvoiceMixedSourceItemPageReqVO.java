package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购票据混合来源明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInvoiceMixedSourceItemPageReqVO extends PageParam {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "来源单据引用", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "来源单据不能为空")
    private List<SourceRef> sourceRefs;

    @Schema(description = "编辑态待删除的来源明细键，格式为 来源类型:来源明细编号")
    private List<String> excludedSourceItemKeys;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Data
    public static class SourceRef {

        @Schema(description = "来源类型：10 入库、20 调价、30 退货", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "来源类型不能为空")
        private Integer sourceType;

        @Schema(description = "来源单据编号", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "来源单据编号不能为空")
        private Long sourceDocId;
    }
}
