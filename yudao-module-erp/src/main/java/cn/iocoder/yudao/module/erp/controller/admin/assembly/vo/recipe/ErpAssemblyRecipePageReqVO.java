package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 组装配方分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpAssemblyRecipePageReqVO extends PageParam {

    @Schema(description = "模块编码、模块名称、组成件编号、名称、工厂编号或配方编号关键词")
    private String keyword;

    @Schema(description = "用途：1 装配")
    private Integer purpose;

    @Schema(description = "状态：0 停用，1 启用")
    private Integer status;

}
