package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.AssertTrue;

@Schema(description = "管理后台 - 组装配方明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpAssemblyRecipeItemPageReqVO extends PageParam {

    @Schema(description = "配方编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "配方编号不能为空")
    private Long recipeId;

    @AssertTrue(message = "单次明细最多查询100条")
    public boolean isBoundedPageSize() { return getPageSize() != null && getPageSize() > 0 && getPageSize() <= 100; }

}
