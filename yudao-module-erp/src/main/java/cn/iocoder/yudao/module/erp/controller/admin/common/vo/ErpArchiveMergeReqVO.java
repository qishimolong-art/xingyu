package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 基础档案合并 Request VO")
@Data
public class ErpArchiveMergeReqVO {

    @Schema(description = "被合并档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "被合并档案编号不能为空")
    private Long sourceId;

    @Schema(description = "保留档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "保留档案编号不能为空")
    private Long keepId;

}
