package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 赋予角色表单级数据权限 Request VO")
@Data
public class PermissionAssignRoleFormDataScopeReqVO {

    @Schema(description = "角色编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @Schema(description = "表单级数据权限列表，为空表示清空所有表单级配置")
    private List<FormDataScopeItem> items = Collections.emptyList();

    @Data
    public static class FormDataScopeItem {

        @Schema(description = "表单标识", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "表单标识不能为空")
        private String formKey;

        @Schema(description = "数据范围，参见 DataScopeEnum", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "数据范围不能为空")
        @InEnum(value = DataScopeEnum.class, message = "数据范围值不合法")
        private Integer dataScope;

        @Schema(description = "自定义部门编号列表，仅 DEPT_CUSTOM 时需要")
        private Set<Long> dataScopeDeptIds = Collections.emptySet();
    }

}
