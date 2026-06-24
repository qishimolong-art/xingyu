package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "User role permission aggregation response")
@Data
public class UserRolePermissionRespVO {

    @Schema(description = "Assigned roles")
    private List<RoleInfo> roleInfos;

    @Schema(description = "All enabled roles")
    private List<RoleInfo> allRoles;

    @Schema(description = "Aggregated button permissions")
    private List<PermissionInfo> permissions;

    @Data
    public static class RoleInfo {
        private Long id;
        private String name;
    }

    @Data
    public static class PermissionInfo {
        @Schema(description = "Permission code, for example system:user:create")
        private String code;
        @Schema(description = "Button name")
        private String name;
        @Schema(description = "Current user has this button permission")
        private Boolean checked;
        @Schema(description = "Role aggregation grants this button permission")
        private Boolean roleGranted;
        @Schema(description = "Denied by user-level configuration")
        private Boolean denied;
    }

}
