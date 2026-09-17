package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.attribution;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 凭证归属 批量生成凭证 Request VO")
@Data
public class ErpVoucherAttributionGenerateReqVO {

    @Schema(description = "归属记录编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "归属记录编号列表不能为空")
    private List<Long> ids;

    private java.util.Map<Long, String> previewTokens;
}
