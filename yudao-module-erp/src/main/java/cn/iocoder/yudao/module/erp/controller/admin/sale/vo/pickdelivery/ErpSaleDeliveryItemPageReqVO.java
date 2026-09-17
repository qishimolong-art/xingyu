package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售送货单明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleDeliveryItemPageReqVO extends PageParam {

    @Schema(description = "送货单编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "送货单编号不能为空")
    private Long orderId;

}
