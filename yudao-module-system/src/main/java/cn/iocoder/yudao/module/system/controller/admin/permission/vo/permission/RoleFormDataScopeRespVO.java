package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Schema(description = "管理后台 - 角色表单级数据权限 Response VO")
@Data
public class RoleFormDataScopeRespVO {

    @Schema(description = "表单标识")
    private String formKey;

    @Schema(description = "数据范围，参见 DataScopeEnum")
    private Integer dataScope;

    @Schema(description = "自定义部门编号列表")
    private Set<Long> dataScopeDeptIds;

}
