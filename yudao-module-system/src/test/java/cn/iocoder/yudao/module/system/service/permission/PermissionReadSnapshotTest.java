package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFieldPermissionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleFieldPermissionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserPermissionDenyMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionReadSnapshotTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void batchesMenuAndFieldPermissionReadsWithinOneSnapshot() {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(100L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, "", Collections.emptyList()));

        PermissionServiceImpl service = spy(new PermissionServiceImpl());
        RoleDO role = new RoleDO().setId(1L).setStatus(CommonStatusEnum.ENABLE.getStatus());
        doReturn(Collections.singletonList(role)).when(service).getEnableUserRoleListByUserIdFromCache(100L);

        RoleService roleService = mock(RoleService.class);
        MenuService menuService = mock(MenuService.class);
        RoleMenuMapper roleMenuMapper = mock(RoleMenuMapper.class);
        UserPermissionDenyMapper denyMapper = mock(UserPermissionDenyMapper.class);
        FieldDefinitionMapper fieldMapper = mock(FieldDefinitionMapper.class);
        RoleFieldPermissionMapper roleFieldMapper = mock(RoleFieldPermissionMapper.class);
        set(service, "roleService", roleService);
        set(service, "menuService", menuService);
        set(service, "roleMenuMapper", roleMenuMapper);
        set(service, "userPermissionDenyMapper", denyMapper);
        set(service, "fieldDefinitionMapper", fieldMapper);
        set(service, "roleFieldPermissionMapper", roleFieldMapper);

        RoleMenuDO roleMenu = new RoleMenuDO().setRoleId(1L).setMenuId(10L);
        MenuDO menu = new MenuDO().setId(10L).setParentId(0L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setPermission("permission:a");
        when(roleMenuMapper.selectListByRoleId(anyCollection())).thenReturn(Collections.singletonList(roleMenu));
        when(menuService.getMenuList()).thenReturn(Collections.singletonList(menu));
        when(menuService.filterDisableMenus(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(denyMapper.selectListByUserId(100L)).thenReturn(Collections.emptyList());

        FieldDefinitionDO fieldA = new FieldDefinitionDO().setId(11L).setModule("module_a").setFieldKey("secretA");
        FieldDefinitionDO fieldB = new FieldDefinitionDO().setId(12L).setModule("module_b").setFieldKey("secretB");
        when(fieldMapper.selectList()).thenReturn(Arrays.asList(fieldA, fieldB));
        RoleFieldPermissionDO hiddenA = new RoleFieldPermissionDO().setRoleId(1L).setFieldId(11L).setHidden(true);
        when(roleFieldMapper.selectListByRoleIds(any())).thenReturn(Collections.singletonList(hiddenA));

        service.executeInReadSnapshot(() -> {
            assertTrue(service.hasAnyPermissions(100L, "permission:a"));
            assertFalse(service.hasAnyPermissions(100L, "permission:b"));
            assertTrue(service.getCurrentUserHiddenFields("module_a", null, false).contains("secretA"));
            assertFalse(service.getCurrentUserHiddenFields("module_b", null, false).contains("secretA"));
            return null;
        });

        verify(roleMenuMapper, times(1)).selectListByRoleId(anyCollection());
        verify(menuService, times(1)).getMenuList();
        verify(denyMapper, times(1)).selectListByUserId(100L);
        verify(fieldMapper, times(1)).selectList();
        verify(roleFieldMapper, times(1)).selectListByRoleIds(any());
    }

    private static void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }
}
