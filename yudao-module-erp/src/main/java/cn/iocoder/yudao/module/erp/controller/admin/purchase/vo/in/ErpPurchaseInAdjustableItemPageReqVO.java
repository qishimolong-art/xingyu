package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购调价可选入库明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInAdjustableItemPageReqVO extends PageParam {

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "供应商编号不能为空")
    private Long supplierId;

    @Schema(description = "限定入库单编号数组，用于按入库单调价", example = "1,2,3")
    private List<Long> inIds;

    @Schema(description = "是否排除已调价明细", example = "true")
    private Boolean excludeAdjusted;

    @Schema(description = "是否排除已开票入库单", example = "true")
    private Boolean excludeInvoiced;

    @Schema(description = "产品关键词")
    private String productKeyword;

}
