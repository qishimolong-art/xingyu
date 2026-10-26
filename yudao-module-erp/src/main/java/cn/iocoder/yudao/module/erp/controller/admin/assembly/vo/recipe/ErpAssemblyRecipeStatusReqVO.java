package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 组装配方状态更新 Request VO")
@Data
public class ErpAssemblyRecipeStatusReqVO {

    @NotNull(message = "配方编号不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    private Integer status;

}
