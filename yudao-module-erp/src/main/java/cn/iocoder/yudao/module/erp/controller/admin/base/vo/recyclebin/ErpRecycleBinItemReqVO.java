package cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 基础档案回收站操作项 Request VO")
@Data
public class ErpRecycleBinItemReqVO {

    @Schema(description = "来源类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "supplier")
    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @Schema(description = "来源编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotNull(message = "来源编号不能为空")
    private Long sourceId;

}
