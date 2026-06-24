package cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 产品单位批量修改 Request VO")
@Data
public class ErpProductUnitBatchUpdateReqVO {

    @Schema(description = "产品单位编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "产品单位编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "单位状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "单位状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "单位状态必须是 {value}")
    private Integer status;

}
