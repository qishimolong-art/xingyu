package cn.iocoder.yudao.module.system.controller.admin.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleDataScopeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleFieldReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleFormDataScopeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleMenuReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignUserRoleReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleFormDataScopeRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - Permission")
@RestController
@RequestMapping("/system/permission")
public class PermissionController {

    @Resource
    private PermissionService permissionService;
    @Resource
    private TenantService tenantService;

    @Operation(summary = "Get role menu ids")
    @Parameter(name = "roleId", description = "Role id", required = true)
    @GetMapping("/list-role-menus")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Set<Long>> getRoleMenuList(@RequestParam("roleId") Long roleId) {
        return success(permissionService.getRoleMenuListByRoleId(roleId));
    }

    @PostMapping("/assign-role-menu")
    @Operation(summary = "Assign role menu permissions")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-menu')")
    public CommonResult<Boolean> assignRoleMenu(@Validated @RequestBody PermissionAssignRoleMenuReqVO reqVO) {
        tenantService.handleTenantMenu(menuIds -> reqVO.getMenuIds().removeIf(menuId -> !CollUtil.contains(menuIds, menuId)));
        permissionService.assignRoleMenu(reqVO.getRoleId(), reqVO.getMenuIds());
        return success(true);
    }

    @PostMapping("/assign-role-data-scope")
    @Operation(summary = "Assign role data scope")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<Boolean> assignRoleDataScope(@Valid @RequestBody PermissionAssignRoleDataScopeReqVO reqVO) {
        permissionService.assignRoleDataScope(reqVO.getRoleId(), reqVO.getDataScope(), reqVO.getDataScopeDeptIds());
        return success(true);
    }

    @GetMapping("/list-role-fields")
    @Operation(summary = "Get role field permissions")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-field-permission')")
    public CommonResult<Map<String, Object>> listRoleFields(@RequestParam("roleId") Long roleId,
                                                            @RequestParam("module") String module) {
        List<FieldDefinitionDO> definitions = permissionService.getFieldDefinitions(module);
        List<Long> hiddenFieldIds = permissionService.getRoleHiddenFieldIds(roleId, module);
        Map<String, Object> result = new HashMap<>();
        result.put("definitions", definitions);
        result.put("hiddenFieldIds", hiddenFieldIds);
        return success(result);
    }

    @PutMapping("/assign-role-field-permission")
    @Operation(summary = "Assign role field permissions")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-field-permission')")
    public CommonResult<Boolean> assignRoleFieldPermission(
            @Valid @RequestBody PermissionAssignRoleFieldReqVO reqVO) {
        permissionService.assignRoleFieldPermission(reqVO.getRoleId(), reqVO.getModule(), reqVO.getHiddenFieldIds());
        return success(true);
    }

    @GetMapping("/get-current-user-hidden-fields")
    @Operation(summary = "Get current user hidden fields")
    public CommonResult<List<String>> getCurrentUserHiddenFields(@RequestParam("module") String module,
                                                                 @RequestParam(value = "businessDeptId", required = false)
                                                                 Long businessDeptId,
                                                                 @RequestParam(value = "includeProductPricePermission",
                                                                         required = false, defaultValue = "true")
                                                                 boolean includeProductPricePermission) {
        return success(permissionService.getCurrentUserHiddenFields(module, businessDeptId,
                includeProductPricePermission));
    }

    @Operation(summary = "Get admin role ids")
    @Parameter(name = "userId", description = "User id", required = true)
    @GetMapping("/list-user-roles")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Set<Long>> listAdminRoles(@RequestParam("userId") Long userId) {
        return success(permissionService.getUserRoleIdListByUserId(userId));
    }

    @Operation(summary = "Assign user roles")
    @PostMapping("/assign-user-role")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-user-role')")
    public CommonResult<Boolean> assignUserRole(@Validated @RequestBody PermissionAssignUserRoleReqVO reqVO) {
        permissionService.assignUserRole(reqVO.getUserId(), reqVO.getRoleIds());
        return success(true);
    }

    @GetMapping("/list-role-form-data-scopes")
    @Operation(summary = "获得角色的表单级数据权限列表")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<List<RoleFormDataScopeRespVO>> getRoleFormDataScopeList(
            @RequestParam("roleId") Long roleId) {
        return success(permissionService.getRoleFormDataScopeList(roleId));
    }

    @PostMapping("/assign-role-form-data-scope")
    @Operation(summary = "设置角色的表单级数据权限")
    @PreAuthorize("@ss.hasPermission('system:permission:assign-role-data-scope')")
    public CommonResult<Boolean> assignRoleFormDataScope(
            @Valid @RequestBody PermissionAssignRoleFormDataScopeReqVO reqVO) {
        permissionService.assignRoleFormDataScope(reqVO.getRoleId(), reqVO.getItems());
        return success(true);
    }

}
