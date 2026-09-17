package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售退货可退出库明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleOutReturnableItemPageReqVO extends PageParam {

    @Schema(description = "销售出库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "销售出库单编号不能为空")
    private Long outId;

    @Schema(description = "产品关键词")
    private String productKeyword;

}
