package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 供应商批量停用 Request VO")
@Data
public class ErpSupplierBatchDisableReqVO {

    @Schema(description = "供应商编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商编号列表不能为空")
    private List<Long> ids;

}
