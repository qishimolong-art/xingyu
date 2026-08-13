package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Set;

@Schema(description = "管理后台 - 角色表单数据权限 Response VO")
@Data
public class RoleFormDataScopeRespVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long roleId;

    @Schema(description = "表单标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_sale_cart")
    private String formKey;

    @Schema(description = "数据范围，0 表示继承角色默认数据权限，其它值见 DataScopeEnum", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "1")
    private Integer dataScope;

    @Schema(description = "指定部门编号列表，仅自定义部门时使用", example = "1,2")
    private Set<Long> dataScopeDeptIds;

}
