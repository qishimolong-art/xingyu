package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售调价可选出库明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSalePriceAdjustableItemPageReqVO extends PageParam {

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "限定销售出库单编号", example = "17386")
    private Long saleOutId;

    @Schema(description = "是否排除已调价明细", example = "true")
    private Boolean excludeAdjusted;

    @Schema(description = "产品关键词")
    private String productKeyword;

}
