package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;

@Schema(description = "管理后台 - 赋予角色表单数据权限 Request VO")
@Data
public class PermissionAssignRoleFormDataScopeReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "表单标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_sale_cart")
    @NotBlank(message = "表单标识不能为空")
    private String formKey;

    @Schema(description = "数据范围，0 表示继承角色默认数据权限，其它值见 DataScopeEnum", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1")
    @NotNull(message = "数据范围不能为空")
    private Integer dataScope;

    @Schema(description = "指定部门编号列表，仅自定义部门时使用", example = "1,2")
    private Set<Long> dataScopeDeptIds = Collections.emptySet();

}
