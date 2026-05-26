package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 凭证归属 批量应用 Request VO")
@Data
public class ErpVoucherAttributionApplyReqVO {

    @Schema(description = "归属记录编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "归属记录编号列表不能为空")
    private List<Long> ids;

    @Schema(description = "归属年", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "归属年不能为空")
    private Integer attributionYear;

    @Schema(description = "归属月", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "归属月不能为空")
    @Min(value = 1, message = "归属月不能小于 1")
    @Max(value = 12, message = "归属月不能大于 12")
    private Integer attributionMonth;

}
