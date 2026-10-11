package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售单按批次拣货送货人员明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleOutPickDeliveryOperationItemPageReqVO extends PageParam {

    @Schema(description = "销售单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "销售单编号不能为空")
    private Long outId;

    @Schema(description = "提交类型：10 拣货，20 送货", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "提交类型不能为空")
    private Integer type;

}
