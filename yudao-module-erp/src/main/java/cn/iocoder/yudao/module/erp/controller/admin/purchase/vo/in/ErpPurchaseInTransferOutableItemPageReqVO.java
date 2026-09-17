package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购入库可调拨明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInTransferOutableItemPageReqVO extends PageParam {

    @Schema(description = "采购入库单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "采购入库单编号不能为空")
    private Long inId;

    @Schema(description = "产品关键词")
    private String productKeyword;

}
