package cn.iocoder.yudao.module.system.service.tenant;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.date.DateUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.tenant.vo.tenant.TenantRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.TenantVisitScopeRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.TenantVisitScopeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
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
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import cn.iocoder.yudao.module.system.service.logger.SystemOperateLogService;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.TENANT_VISIT_FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
public class TenantVisitScopeServiceImpl implements TenantVisitScopeService {

    @Resource
    private UserTenantVisitScopeMapper visitScopeMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private MenuMapper menuMapper;
    @Resource
    private UserPermissionDenyMapper userPermissionDenyMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private SystemOperateLogService operateLogService;

    @Override
    @Cacheable(value = RedisKeyConstants.USER_TENANT_VISIT_SCOPE, key = "#userId", unless = "#result == null")
    public UserTenantVisitScopeDO getUserScopeFromCache(Long userId) {
        return visitScopeMapper.selectByUserId(userId);
    }

    @Override
    public TenantVisitScopeRespVO getUserTenantVisitScope(Long userId) {
        requireSystemTenant();
        AdminUserDO user = requirePlatformUser(userId);
        UserTenantVisitScopeDO scope = getSelf().getUserScopeFromCache(user.getId());
        Set<Long> selectedIds = scope == null || scope.getTenantIds() == null
                ? Collections.emptySet() : scope.getTenantIds();

        List<TenantDO> tenants = tenantMapper.selectList().stream()
                .filter(tenant -> !Objects.equals(tenant.getId(), SYSTEM_TENANT_ID))
                .sorted(Comparator.comparing(TenantDO::getId))
                .collect(Collectors.toList());
        List<TenantVisitScopeRespVO.TenantOption> options = new ArrayList<>();
        Set<Long> existingIds = new HashSet<>();
        for (TenantDO tenant : tenants) {
            existingIds.add(tenant.getId());
            TenantVisitScopeRespVO.TenantOption option = new TenantVisitScopeRespVO.TenantOption();
            option.setId(tenant.getId());
            option.setName(tenant.getName());
            option.setAvailable(isTenantAvailable(tenant));
            option.setUnavailableReason(getUnavailableReason(tenant));
            options.add(option);
        }
        selectedIds.stream().filter(id -> !existingIds.contains(id)).sorted().forEach(id -> {
            TenantVisitScopeRespVO.TenantOption option = new TenantVisitScopeRespVO.TenantOption();
            option.setId(id);
            option.setName("租户 " + id);
            option.setAvailable(false);
            option.setUnavailableReason("租户不存在或已删除");
            options.add(option);
        });

        TenantVisitScopeRespVO result = new TenantVisitScopeRespVO();
        result.setConfigured(scope != null);
        result.setScopeType(scope == null ? null : scope.getScopeType());
        result.setTenantIds(new LinkedHashSet<>(selectedIds));
        result.setTenantOptions(options);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.USER_TENANT_VISIT_SCOPE, key = "#userId"),
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId"),
            @CacheEvict(value = RedisKeyConstants.USER_DENIED_PERMISSION_LIST, key = "#userId")
    })
    public void saveUserTenantVisitScope(Long userId, TenantVisitScopeSaveReqVO reqVO) {
        validateUserTenantVisitScopes(Collections.singleton(userId), reqVO);
        UserTenantVisitScopeDO oldScope = visitScopeMapper.selectByUserId(userId);
        Map<String, Object> before = toAuditScope(oldScope);
        before.put("managedRoleAssigned", hasManagedTenantVisitRole(userId));
        if (Boolean.TRUE.equals(reqVO.getClear())) {
            getSelf().clearUserScope(userId);
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("configured", false);
            after.put("managedRoleAssigned", false);
            recordScopeChange(userId, before, after);
            return;
        }
        String scopeType = reqVO.getScopeType() == null ? "" : reqVO.getScopeType().trim().toUpperCase();
        Set<Long> tenantIds = new LinkedHashSet<>(CollUtil.emptyIfNull(reqVO.getTenantIds()));
        if (SCOPE_ALL.equals(scopeType)) {
            tenantIds.clear();
        }

        ensureManagedTenantVisitRole(userId);
        userPermissionDenyMapper.deleteListByUserIdAndPermissions(userId,
                Collections.singleton(PERMISSION_TENANT_VISIT));

        UserTenantVisitScopeDO scope = oldScope;
        if (scope == null) {
            scope = new UserTenantVisitScopeDO();
            scope.setUserId(userId);
            scope.setScopeType(scopeType);
            scope.setTenantIds(tenantIds);
            visitScopeMapper.insert(scope);
        } else {
            scope.setScopeType(scopeType);
            scope.setTenantIds(tenantIds);
            visitScopeMapper.updateById(scope);
        }
        Map<String, Object> after = toAuditScope(scope);
        after.put("managedRoleAssigned", true);
        recordScopeChange(userId, before, after);
    }

    @Override
    public void validateUserTenantVisitScopes(Collection<Long> userIds, TenantVisitScopeSaveReqVO reqVO) {
        requireSystemTenant();
        if (reqVO == null) {
            throw invalidParamException("请设置租户访问范围");
        }
        Set<Long> distinctUserIds = new LinkedHashSet<>(userIds == null ? Collections.emptyList() : userIds);
        if (userIds == null || distinctUserIds.isEmpty() || distinctUserIds.contains(null)
                || distinctUserIds.size() != userIds.size()) {
            throw invalidParamException("目标用户编号无效或重复");
        }
        distinctUserIds.forEach(this::requireConfigurablePlatformUser);
        if (Boolean.TRUE.equals(reqVO.getClear())) {
            if ((reqVO.getScopeType() != null && !reqVO.getScopeType().trim().isEmpty())
                    || CollUtil.isNotEmpty(reqVO.getTenantIds())) {
                throw invalidParamException("清空租户访问范围时不能同时设置范围类型或租户");
            }
            return;
        }
        validateManagedTenantVisitRole();
        String scopeType = reqVO.getScopeType() == null ? "" : reqVO.getScopeType().trim().toUpperCase();
        Set<Long> tenantIds = new LinkedHashSet<>(CollUtil.emptyIfNull(reqVO.getTenantIds()));
        if (SCOPE_ALL.equals(scopeType)) {
            if (CollUtil.isNotEmpty(tenantIds)) {
                throw invalidParamException("全部租户模式不能指定租户编号");
            }
            return;
        }
        if (!SCOPE_CUSTOM.equals(scopeType)) {
            throw invalidParamException("租户访问范围类型必须为 ALL 或 CUSTOM");
        }
        if (tenantIds.isEmpty()) {
            throw invalidParamException("指定租户模式至少选择一个租户");
        }
        if (tenantIds.remove(SYSTEM_TENANT_ID)) {
            throw invalidParamException("登录所属租户不能加入访问范围");
        }
        tenantIds.forEach(this::validateConfigurableTenant);
    }

    @Override
    public Long getManagedTenantVisitRoleId() {
        return validateManagedTenantVisitRole().getId();
    }

    @Override
    public Set<Long> preserveManagedTenantVisitRole(Long userId, Collection<Long> roleIds) {
        Set<Long> result = new LinkedHashSet<>(roleIds == null ? Collections.emptyList() : roleIds);
        if (!isSystemTenant()) {
            return result;
        }
        Long managedRoleId = getManagedTenantVisitRoleId();
        result.remove(managedRoleId);
        if (visitScopeMapper.selectByUserId(userId) != null) {
            result.add(managedRoleId);
        }
        return result;
    }

    @Override
    public List<TenantRespVO> getCurrentUserVisitTenantList(Long userId, Long loginTenantId) {
        if (!Objects.equals(loginTenantId, SYSTEM_TENANT_ID) || !hasEffectiveTenantVisitPermission(userId)) {
            throwTenantVisitForbidden();
        }
        TenantDO loginTenant = tenantMapper.selectById(loginTenantId);
        if (!isTenantAvailable(loginTenant)) {
            throwTenantVisitForbidden();
        }
        List<TenantRespVO> result = new ArrayList<>();
        result.add(toSimpleTenant(loginTenant));

        UserTenantVisitScopeDO scope = getSelf().getUserScopeFromCache(userId);
        if (scope == null) {
            return result;
        }
        Set<Long> customIds = CollUtil.emptyIfNull(scope.getTenantIds());
        tenantMapper.selectList().stream()
                .filter(tenant -> !Objects.equals(tenant.getId(), loginTenantId))
                .filter(this::isTenantAvailable)
                .filter(tenant -> SCOPE_ALL.equals(scope.getScopeType())
                        || SCOPE_CUSTOM.equals(scope.getScopeType()) && customIds.contains(tenant.getId()))
                .sorted(Comparator.comparing(TenantDO::getId))
                .map(this::toSimpleTenant)
                .forEach(result::add);
        return result;
    }

    @Override
    public void validateTenantVisit(Long userId, Long loginTenantId, Long visitTenantId) {
        if (!Objects.equals(loginTenantId, SYSTEM_TENANT_ID)
                || Objects.equals(visitTenantId, loginTenantId)
                || !hasEffectiveTenantVisitPermission(userId)) {
            throwTenantVisitForbidden();
        }
        UserTenantVisitScopeDO scope = getSelf().getUserScopeFromCache(userId);
        if (scope == null) {
            throwTenantVisitForbidden();
        }
        if (!SCOPE_ALL.equals(scope.getScopeType())
                && (!SCOPE_CUSTOM.equals(scope.getScopeType())
                || !CollUtil.emptyIfNull(scope.getTenantIds()).contains(visitTenantId))) {
            throwTenantVisitForbidden();
        }
        if (!isTenantAvailable(tenantMapper.selectById(visitTenantId))) {
            throwTenantVisitForbidden();
        }
    }

    @Override
    public boolean hasEffectiveTenantVisitPermission(Long userId) {
        AdminUserDO user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), SYSTEM_TENANT_ID)
                || !CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
            return false;
        }
        Set<Long> roleIds = userRoleMapper.selectListByUserId(userId).stream()
                .map(UserRoleDO::getRoleId).collect(Collectors.toSet());
        if (roleIds.isEmpty()) {
            return false;
        }
        List<RoleDO> roles = roleMapper.selectByIds(roleIds).stream()
                .filter(role -> CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()))
                .collect(Collectors.toList());
        if (roles.isEmpty() || userPermissionDenyMapper.selectListByUserId(userId).stream()
                .map(UserPermissionDenyDO::getPermission)
                .anyMatch(PERMISSION_TENANT_VISIT::equals)) {
            return false;
        }
        if (roles.stream().anyMatch(role -> RoleCodeEnum.SUPER_ADMIN.getCode().equals(role.getCode()))) {
            return true;
        }
        Set<Long> enabledRoleIds = roles.stream().map(RoleDO::getId).collect(Collectors.toSet());
        Set<Long> visitMenuIds = menuMapper.selectListByPermission(PERMISSION_TENANT_VISIT).stream()
                .filter(menu -> CommonStatusEnum.ENABLE.getStatus().equals(menu.getStatus()))
                .map(MenuDO::getId).collect(Collectors.toSet());
        return roleMenuMapper.selectListByRoleId(enabledRoleIds).stream()
                .anyMatch(item -> visitMenuIds.contains(item.getMenuId()));
    }

    @Override
    public void reconcileUserScope(Long userId) {
        if (!isSystemTenant()) {
            return;
        }
        if (!hasEffectiveTenantVisitPermission(userId)) {
            getSelf().clearUserScope(userId);
        }
    }

    @Override
    public void reconcileUserScopes(Collection<Long> userIds) {
        if (!isSystemTenant()) {
            return;
        }
        if (userIds != null) {
            userIds.forEach(this::reconcileUserScope);
        }
    }

    @Override
    public void reconcileUserScopesByRoleId(Long roleId) {
        if (!isSystemTenant()) {
            return;
        }
        reconcileUserScopes(userRoleMapper.selectListByRoleIds(Collections.singleton(roleId)).stream()
                .map(UserRoleDO::getUserId).collect(Collectors.toSet()));
    }

    @Override
    public void reconcileAllUserScopes() {
        if (!isSystemTenant()) {
            return;
        }
        reconcileUserScopes(visitScopeMapper.selectList().stream()
                .map(UserTenantVisitScopeDO::getUserId).collect(Collectors.toSet()));
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.USER_TENANT_VISIT_SCOPE, key = "#userId"),
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    })
    public void clearUserScope(Long userId) {
        if (!isSystemTenant()) {
            return;
        }
        visitScopeMapper.deletePhysicallyByUserId(userId, TenantContextHolder.getRequiredTenantId());
        RoleDO managedRole = roleMapper.selectByCode(ROLE_CODE_TENANT_VISIT_USER);
        if (managedRole != null) {
            userRoleMapper.deleteListByUserIdAndRoleIdIds(userId, Collections.singleton(managedRole.getId()));
        }
    }

    private void requireSystemTenant() {
        if (!Objects.equals(TenantContextHolder.getRequiredTenantId(), SYSTEM_TENANT_ID)) {
            throwTenantVisitForbidden();
        }
    }

    private boolean isSystemTenant() {
        return Objects.equals(TenantContextHolder.getTenantId(), SYSTEM_TENANT_ID);
    }

    private AdminUserDO requirePlatformUser(Long userId) {
        AdminUserDO user = userMapper.selectById(userId);
        if (user == null || !Objects.equals(user.getTenantId(), SYSTEM_TENANT_ID)) {
            throw invalidParamException("目标平台用户不存在");
        }
        return user;
    }

    private AdminUserDO requireConfigurablePlatformUser(Long userId) {
        AdminUserDO user = requirePlatformUser(userId);
        if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
            throw invalidParamException("目标用户已停用，不能配置租户访问范围：" + userId);
        }
        return user;
    }

    private RoleDO validateManagedTenantVisitRole() {
        RoleDO role = roleMapper.selectByCode(ROLE_CODE_TENANT_VISIT_USER);
        if (role == null
                || !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus())
                || !RoleTypeEnum.SYSTEM.getType().equals(role.getType())) {
            throw invalidParamException("系统自动角色未正确初始化，请先执行租户访问角色迁移");
        }
        return role;
    }

    private void ensureManagedTenantVisitRole(Long userId) {
        Long roleId = getManagedTenantVisitRoleId();
        boolean assigned = hasRole(userId, roleId);
        if (!assigned) {
            UserRoleDO userRole = new UserRoleDO();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleMapper.insert(userRole);
        }
    }

    private boolean hasManagedTenantVisitRole(Long userId) {
        RoleDO role = roleMapper.selectByCode(ROLE_CODE_TENANT_VISIT_USER);
        return role != null && hasRole(userId, role.getId());
    }

    private boolean hasRole(Long userId, Long roleId) {
        return userRoleMapper.selectListByUserId(userId).stream()
                .anyMatch(item -> Objects.equals(item.getRoleId(), roleId));
    }

    private Map<String, Object> toAuditScope(UserTenantVisitScopeDO scope) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("configured", scope != null);
        if (scope != null) {
            result.put("scopeType", scope.getScopeType());
            result.put("tenantIds", new LinkedHashSet<>(CollUtil.emptyIfNull(scope.getTenantIds())));
        }
        return result;
    }

    private void recordScopeChange(Long userId, Map<String, Object> before, Map<String, Object> after) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("before", before);
        extra.put("after", after);
        extra.put("managedRoleCode", ROLE_CODE_TENANT_VISIT_USER);
        operateLogService.record("用户", "修改租户访问范围", userId, "修改租户访问范围", extra);
    }

    private void validateConfigurableTenant(Long tenantId) {
        if (!isTenantAvailable(tenantMapper.selectById(tenantId))) {
            throw invalidParamException("租户不存在、已停用或已过期：" + tenantId);
        }
    }

    private boolean isTenantAvailable(TenantDO tenant) {
        return tenant != null
                && CommonStatusEnum.ENABLE.getStatus().equals(tenant.getStatus())
                && tenant.getExpireTime() != null
                && !DateUtils.isExpired(tenant.getExpireTime());
    }

    private String getUnavailableReason(TenantDO tenant) {
        if (tenant == null) {
            return "租户不存在或已删除";
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(tenant.getStatus())) {
            return "租户已停用";
        }
        if (tenant.getExpireTime() == null || DateUtils.isExpired(tenant.getExpireTime())) {
            return "租户已过期";
        }
        return null;
    }

    private TenantRespVO toSimpleTenant(TenantDO tenant) {
        return new TenantRespVO().setId(tenant.getId()).setName(tenant.getName());
    }

    private void throwTenantVisitForbidden() {
        throw exception0(TENANT_VISIT_FORBIDDEN.getCode(), TENANT_VISIT_FORBIDDEN.getMsg());
    }

    TenantVisitScopeService getSelf() {
        return SpringUtil.getBean(TenantVisitScopeService.class);
    }

}
