package cn.iocoder.yudao.module.system.controller.admin.auth;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO.ID_ROOT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AdminUserService userService;
    @Mock
    private RoleService roleService;
    @Mock
    private MenuService menuService;
    @Mock
    private PermissionService permissionService;

    @Test
    void testGetPermissionInfo_buttonPermissionWithoutAssignedParent() {
        Long userId = 7L;
        Long roleId = 19L;
        Long parentMenuId = 1138L;
        Long buttonMenuId = 5010L;
        AdminUserDO user = new AdminUserDO().setId(userId).setUsername("13541313592");
        RoleDO role = new RoleDO().setId(roleId).setCode("tenant_visit_user")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO parentMenu = new MenuDO().setId(parentMenuId).setParentId(ID_ROOT)
                .setType(MenuTypeEnum.MENU.getType()).setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO visitButton = new MenuDO().setId(buttonMenuId).setParentId(parentMenuId)
                .setType(MenuTypeEnum.BUTTON.getType()).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPermission("system:tenant:visit");
        Set<Long> roleIds = asSet(roleId);
        Set<Long> roleMenuIds = asSet(buttonMenuId);
        List<MenuDO> allMenus = asList(parentMenu, visitButton);
        List<MenuDO> assignedMenus = new ArrayList<>(singletonList(visitButton));

        when(userService.getUser(userId)).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(userId)).thenReturn(roleIds);
        when(roleService.getRoleList(roleIds)).thenReturn(new ArrayList<>(singletonList(role)));
        when(permissionService.getRoleMenuListByRoleId(roleIds)).thenReturn(roleMenuIds);
        when(menuService.getMenuList()).thenReturn(allMenus);
        when(menuService.filterDisableMenus(allMenus)).thenReturn(allMenus);
        when(menuService.getMenuList(roleMenuIds)).thenReturn(assignedMenus);
        when(permissionService.filterDeniedMenus(eq(userId), eq(singletonList(role)), eq(assignedMenus)))
                .thenReturn(assignedMenus);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            CommonResult<AuthPermissionInfoRespVO> result = authController.getPermissionInfo();

            assertEquals(0, result.getCode());
            assertTrue(result.getData().getPermissions().contains("system:tenant:visit"));
            assertTrue(result.getData().getMenus().isEmpty());
        }
        verify(menuService).filterDisableMenus(allMenus);
        verify(menuService).getMenuList(roleMenuIds);
    }

    @Test
    void testGetPermissionInfo_excludesMenuDisabledByFullTree() {
        Long userId = 7L;
        Long roleId = 19L;
        Long parentMenuId = 1138L;
        Long buttonMenuId = 5010L;
        AdminUserDO user = new AdminUserDO().setId(userId).setUsername("13541313592");
        RoleDO role = new RoleDO().setId(roleId).setCode("tenant_visit_user")
                .setStatus(CommonStatusEnum.ENABLE.getStatus());
        MenuDO disabledParent = new MenuDO().setId(parentMenuId).setParentId(ID_ROOT)
                .setType(MenuTypeEnum.MENU.getType()).setStatus(CommonStatusEnum.DISABLE.getStatus());
        MenuDO visitButton = new MenuDO().setId(buttonMenuId).setParentId(parentMenuId)
                .setType(MenuTypeEnum.BUTTON.getType()).setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setPermission("system:tenant:visit");
        Set<Long> roleIds = asSet(roleId);
        Set<Long> roleMenuIds = asSet(buttonMenuId);
        List<MenuDO> allMenus = asList(disabledParent, visitButton);
        List<MenuDO> assignedMenus = new ArrayList<>(singletonList(visitButton));

        when(userService.getUser(userId)).thenReturn(user);
        when(permissionService.getUserRoleIdListByUserId(userId)).thenReturn(roleIds);
        when(roleService.getRoleList(roleIds)).thenReturn(new ArrayList<>(singletonList(role)));
        when(permissionService.getRoleMenuListByRoleId(roleIds)).thenReturn(roleMenuIds);
        when(menuService.getMenuList()).thenReturn(allMenus);
        when(menuService.filterDisableMenus(allMenus)).thenReturn(emptyList());
        when(menuService.getMenuList(roleMenuIds)).thenReturn(assignedMenus);
        when(permissionService.filterDeniedMenus(eq(userId), eq(singletonList(role)), eq(assignedMenus)))
                .thenReturn(assignedMenus);

        try (MockedStatic<SecurityFrameworkUtils> security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(userId);
            CommonResult<AuthPermissionInfoRespVO> result = authController.getPermissionInfo();

            assertEquals(0, result.getCode());
            assertTrue(result.getData().getPermissions().isEmpty());
            assertTrue(result.getData().getMenus().isEmpty());
        }
    }

}
