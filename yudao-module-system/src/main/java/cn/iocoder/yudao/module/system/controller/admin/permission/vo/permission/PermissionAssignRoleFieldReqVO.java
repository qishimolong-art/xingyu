package cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@Data
public class PermissionAssignRoleFieldReqVO {

    @NotNull(message = "角色编号不能为空")
    private Long roleId;

    @NotNull(message = "模块标识不能为空")
    private String module;

    private List<Long> hiddenFieldIds = Collections.emptyList();

}
