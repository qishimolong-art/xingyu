package cn.iocoder.yudao.module.erp.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 基础数据新增/修改 Request VO")
@Data
public class ErpBaseDataSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "数据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "region")
    @NotEmpty(message = "数据类型不能为空")
    private String type;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "华东")
    @NotEmpty(message = "名称不能为空")
    private String name;

    @Schema(description = "稳定业务编码", example = "cash")
    private String code;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

}
