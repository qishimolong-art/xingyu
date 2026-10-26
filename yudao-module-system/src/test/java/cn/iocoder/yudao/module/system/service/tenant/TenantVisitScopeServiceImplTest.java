package cn.iocoder.yudao.module.system.service.tenant;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.TenantVisitScopeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserPermissionDenyDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.UserTenantVisitScopeDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserPermissionDenyMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.UserTenantVisitScopeMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.service.logger.SystemOperateLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.TENANT_VISIT_FORBIDDEN;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantVisitScopeServiceImplTest {

    @Mock private UserTenantVisitScopeMapper visitScopeMapper;
    @Mock private AdminUserMapper userMapper;
    @Mock private UserRoleMapper userRoleMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private RoleMenuMapper roleMenuMapper;
    @Mock private MenuMapper menuMapper;
    @Mock private UserPermissionDenyMapper userPermissionDenyMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private SystemOperateLogService operateLogService;

    @Spy
    @InjectMocks
    private TenantVisitScopeServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().doReturn(service).when(service).getSelf();
        AdminUserDO user = new AdminUserDO();
        user.setId(10L);
        user.setTenantId(1L);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus());
        lenient().when(userMapper.selectById(10L)).thenReturn(user);

        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(10L);
        userRole.setRoleId(20L);
        lenient().when(userRoleMapper.selectListByUserId(10L)).thenReturn(Collections.singletonList(userRole));

        RoleDO role = new RoleDO();
        role.setId(20L);
        role.setCode(RoleCodeEnum.SUPER_ADMIN.getCode());
        role.setStatus(CommonStatusEnum.ENABLE.getStatus());
        lenient().when(roleMapper.selectByIds(Collections.singleton(20L))).thenReturn(Collections.singletonList(role));
        RoleDO managedRole = new RoleDO();
        managedRole.setId(30L);
        managedRole.setCode(RoleCodeEnum.TENANT_VISIT_USER.getCode());
        managedRole.setType(RoleTypeEnum.SYSTEM.getType());
        managedRole.setStatus(CommonStatusEnum.ENABLE.getStatus());
        lenient().when(roleMapper.selectByCode(TenantVisitScopeService.ROLE_CODE_TENANT_VISIT_USER))
                .thenReturn(managedRole);
        lenient().when(userPermissionDenyMapper.selectListByUserId(10L)).thenReturn(Collections.emptyList());
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void validateTenantVisit_rejectsMissingScopeForSuperAdmin() {
        when(visitScopeMapper.selectByUserId(10L)).thenReturn(null);
        assertForbidden(() -> service.validateTenantVisit(10L, 1L, 162L));
    }

    @Test
    void validateTenantVisit_acceptsCustomHitAndRejectsOutsideScope() {
        UserTenantVisitScopeDO scope = new UserTenantVisitScopeDO();
        scope.setUserId(10L);
        scope.setScopeType(TenantVisitScopeService.SCOPE_CUSTOM);
        scope.setTenantIds(Collections.singleton(162L));
        when(visitScopeMapper.selectByUserId(10L)).thenReturn(scope);
        when(tenantMapper.selectById(162L)).thenReturn(availableTenant(162L));

        assertDoesNotThrow(() -> service.validateTenantVisit(10L, 1L, 162L));
        assertForbidden(() -> service.validateTenantVisit(10L, 1L, 163L));
    }

    @Test
    void validateTenantVisit_rejectsDisabledTarget() {
        UserTenantVisitScopeDO scope = new UserTenantVisitScopeDO();
        scope.setUserId(10L);
        scope.setScopeType(TenantVisitScopeService.SCOPE_ALL);
        scope.setTenantIds(Collections.emptySet());
        when(visitScopeMapper.selectByUserId(10L)).thenReturn(scope);
        TenantDO disabled = availableTenant(162L);
        disabled.setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(tenantMapper.selectById(162L)).thenReturn(disabled);

        assertForbidden(() -> service.validateTenantVisit(10L, 1L, 162L));
    }

    @Test
    void hasEffectiveTenantVisitPermission_respectsPersonalDenyForSuperAdmin() {
        UserPermissionDenyDO deny = new UserPermissionDenyDO();
        deny.setUserId(10L);
        deny.setPermission(TenantVisitScopeService.PERMISSION_TENANT_VISIT);
        when(userPermissionDenyMapper.selectListByUserId(10L))
                .thenReturn(Collections.singletonList(deny));

        assertEquals(false, service.hasEffectiveTenantVisitPermission(10L));
    }

    @Test
    void saveUserTenantVisitScope_addsManagedRoleAndRemovesDeny() {
        TenantVisitScopeSaveReqVO reqVO = new TenantVisitScopeSaveReqVO();
        reqVO.setScopeType(TenantVisitScopeService.SCOPE_CUSTOM);
        reqVO.setTenantIds(Collections.singleton(162L));
        when(tenantMapper.selectById(162L)).thenReturn(availableTenant(162L));
        when(visitScopeMapper.selectByUserId(10L)).thenReturn(null);

        service.saveUserTenantVisitScope(10L, reqVO);

        verify(userRoleMapper).insert(org.mockito.ArgumentMatchers.<UserRoleDO>argThat(role ->
                role.getUserId().equals(10L) && role.getRoleId().equals(30L)));
        verify(userPermissionDenyMapper).deleteListByUserIdAndPermissions(10L,
                Collections.singleton(TenantVisitScopeService.PERMISSION_TENANT_VISIT));
        verify(visitScopeMapper).insert(org.mockito.ArgumentMatchers.<UserTenantVisitScopeDO>argThat(scope ->
                TenantVisitScopeService.SCOPE_CUSTOM.equals(scope.getScopeType())
                        && scope.getTenantIds().equals(Collections.singleton(162L))));
    }

    @Test
    void saveUserTenantVisitScope_clearRemovesScopeAndManagedRole() {
        TenantVisitScopeSaveReqVO reqVO = new TenantVisitScopeSaveReqVO();
        reqVO.setClear(true);

        service.saveUserTenantVisitScope(10L, reqVO);

        verify(visitScopeMapper).deletePhysicallyByUserId(10L, 1L);
        verify(userRoleMapper).deleteListByUserIdAndRoleIdIds(10L, Collections.singleton(30L));
        verify(userPermissionDenyMapper, never()).deleteListByUserIdAndPermissions(
                10L, Collections.singleton(TenantVisitScopeService.PERMISSION_TENANT_VISIT));
    }

    @Test
    void preserveManagedTenantVisitRole_followsScopeRecord() {
        UserTenantVisitScopeDO scope = new UserTenantVisitScopeDO();
        scope.setUserId(10L);
        when(visitScopeMapper.selectByUserId(10L)).thenReturn(scope);

        Set<Long> result = service.preserveManagedTenantVisitRole(10L, Collections.singleton(20L));

        assertEquals(Set.of(20L, 30L), result);
    }

    @Test
    void preserveManagedTenantVisitRole_nonSystemTenantKeepsOriginalRoles() {
        TenantContextHolder.setTenantId(162L);

        Set<Long> result = service.preserveManagedTenantVisitRole(10L, Set.of(20L, 21L));

        assertEquals(Set.of(20L, 21L), result);
        verifyNoInteractions(visitScopeMapper, roleMapper);
    }

    @Test
    void reconcileHooks_nonSystemTenantSkipPlatformScopeMaintenance() {
        TenantContextHolder.setTenantId(162L);

        service.reconcileUserScope(10L);
        service.reconcileUserScopes(Collections.singleton(10L));
        service.reconcileUserScopesByRoleId(20L);
        service.reconcileAllUserScopes();
        service.clearUserScope(10L);

        verifyNoInteractions(visitScopeMapper, userMapper, userRoleMapper, roleMapper,
                roleMenuMapper, menuMapper, userPermissionDenyMapper);
    }

    private TenantDO availableTenant(Long id) {
        TenantDO tenant = new TenantDO();
        tenant.setId(id);
        tenant.setName("租户 " + id);
        tenant.setStatus(CommonStatusEnum.ENABLE.getStatus());
        tenant.setExpireTime(LocalDateTime.now().plusDays(1));
        return tenant;
    }

    private void assertForbidden(Runnable action) {
        ServiceException exception = assertThrows(ServiceException.class, action::run);
        assertEquals(TENANT_VISIT_FORBIDDEN.getCode(), exception.getCode());
    }
}
