package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售退货可转调拨明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSaleReturnTransferOutableItemPageReqVO extends PageParam {

    @Schema(description = "销售退货编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "销售退货编号不能为空")
    private Long returnId;

    @Schema(description = "产品关键词", example = "HD80W")
    private String productKeyword;

}
