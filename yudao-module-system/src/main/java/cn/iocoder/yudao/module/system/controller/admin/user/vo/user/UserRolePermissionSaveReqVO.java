package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Schema(description = "保存用户角色 Request VO")
@Data
public class UserRolePermissionSaveReqVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    @Schema(description = "角色 ID 列表")
    private Set<Long> roleIds;

    @Schema(description = "用户级禁用的按钮权限编码列表")
    private Set<String> deniedPermissions;

}
