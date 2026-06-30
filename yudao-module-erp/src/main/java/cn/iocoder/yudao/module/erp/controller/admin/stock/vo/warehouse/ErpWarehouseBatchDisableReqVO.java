package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 仓库批量停用 Request VO")
@Data
public class ErpWarehouseBatchDisableReqVO {

    @Schema(description = "仓库编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "仓库编号列表不能为空")
    private List<Long> ids;

}
