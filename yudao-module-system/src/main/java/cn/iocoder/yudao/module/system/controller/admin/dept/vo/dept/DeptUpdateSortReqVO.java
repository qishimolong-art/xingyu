package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 部门批量更新排序 Request VO")
@Data
public class DeptUpdateSortReqVO {

    @Schema(description = "排序明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "排序明细不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "管理后台 - 部门排序明细")
    @Data
    public static class Item {

        @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "部门编号不能为空")
        private Long id;

        @Schema(description = "显示顺序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "显示顺序不能为空")
        private Integer sort;

    }

}
