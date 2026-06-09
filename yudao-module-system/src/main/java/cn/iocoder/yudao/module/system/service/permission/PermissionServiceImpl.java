package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFieldPermissionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleFormDataScopeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleFormDataScopeRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFormDataScopeDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FieldDefinitionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleFieldPermissionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleFormDataScopeMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ROLE_DATA_SCOPE_DEPT_IDS_EMPTY;

/**
 * 权限 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private RoleMenuMapper roleMenuMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private FieldDefinitionMapper fieldDefinitionMapper;
    @Resource
    private RoleFieldPermissionMapper roleFieldPermissionMapper;
    @Resource
    private RoleFormDataScopeMapper roleFormDataScopeMapper;

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private DeptService deptService;
    @Resource
    private AdminUserService userService;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(permissions)) {
            return true;
        }

        // 获得当前登录的角色。如果为空，说明没有权限
        List<RoleDO> roles = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }

        // 情况一：遍历判断每个权限，如果有一满足，说明有权限
        for (String permission : permissions) {
            if (hasAnyPermission(roles, permission)) {
                return true;
            }
        }

        // 情况二：如果是超管，也说明有权限
        return roleService.hasAnySuperAdmin(convertSet(roles, RoleDO::getId));
    }

    /**
     * 判断指定角色，是否拥有该 permission 权限
     *
     * @param roles 指定角色数组
     * @param permission 权限标识
     * @return 是否拥有
     */
    private boolean hasAnyPermission(List<RoleDO> roles, String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        // 采用严格模式，如果权限找不到对应的 Menu 的话，也认为没有权限
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }

        // 判断是否有权限
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        for (Long menuId : menuIds) {
            // 获得拥有该菜单的角色编号集合
            Set<Long> menuRoleIds = getSelf().getMenuRoleIdListByMenuIdFromCache(menuId);
            // 如果有交集，说明有权限
            if (CollUtil.containsAny(menuRoleIds, roleIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        // 如果为空，说明已经有权限
        if (ArrayUtil.isEmpty(roles)) {
            return true;
        }

        // 获得当前登录的角色。如果为空，说明没有权限
        List<RoleDO> roleList = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleList)) {
            return false;
        }

        // 判断是否有角色
        Set<String> userRoles = convertSet(roleList, RoleDO::getCode);
        return CollUtil.containsAny(userRoles, Sets.newHashSet(roles));
    }

    // ========== 角色-菜单的相关方法  ==========

    @Override
    @DSTransactional // 多数据源，使用 @DSTransactional 保证本地事务，以及数据源的切换
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
            allEntries = true),
            @CacheEvict(value = RedisKeyConstants.PERMISSION_MENU_ID_LIST,
            allEntries = true) // allEntries 清空所有缓存，主要一次更新涉及到的 menuIds 较多，反倒批量会更快
    })
    public void assignRoleMenu(Long roleId, Set<Long> menuIds) {
        // 获得角色拥有菜单编号
        Set<Long> dbMenuIds = convertSet(roleMenuMapper.selectListByRoleId(roleId), RoleMenuDO::getMenuId);
        // 计算新增和删除的菜单编号
        Set<Long> menuIdList = CollUtil.emptyIfNull(menuIds);
        Collection<Long> createMenuIds = CollUtil.subtract(menuIdList, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, menuIdList);
        // 执行新增和删除。对于已经授权的菜单，不用做任何处理
        if (CollUtil.isNotEmpty(createMenuIds)) {
            roleMenuMapper.insertBatch(CollectionUtils.convertList(createMenuIds, menuId -> {
                RoleMenuDO entity = new RoleMenuDO();
                entity.setRoleId(roleId);
                entity.setMenuId(menuId);
                return entity;
            }));
        }
        if (CollUtil.isNotEmpty(deleteMenuIds)) {
            roleMenuMapper.deleteListByRoleIdAndMenuIds(roleId, deleteMenuIds);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
                    allEntries = true), // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 menu 缓存们
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST,
                    allEntries = true) // allEntries 清空所有缓存，此处无法方便获得 roleId 对应的 user 缓存们
    })
    public void processRoleDeleted(Long roleId) {
        // 标记删除 UserRole
        userRoleMapper.deleteListByRoleId(roleId);
        // 标记删除 RoleMenu
        roleMenuMapper.deleteListByRoleId(roleId);
        roleFieldPermissionMapper.deleteListByRoleId(roleId, TenantContextHolder.getRequiredTenantId());
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public void processMenuDeleted(Long menuId) {
        roleMenuMapper.deleteListByMenuId(menuId);
    }

    @Override
    public Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptySet();
        }

        // 如果是管理员的情况下，获取全部菜单编号
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return convertSet(menuService.getMenuList(), MenuDO::getId);
        }
        // 如果是非管理员的情况下，获得拥有的菜单编号
        return convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
    }

    // ========== 角色-字段权限的相关方法 ==========

    @Override
    public List<FieldDefinitionDO> getFieldDefinitions(String module) {
        return fieldDefinitionMapper.selectListByModule(module);
    }

    @Override
    public List<String> getRoleHiddenFields(Long roleId, String module) {
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
        if (CollUtil.isEmpty(definitions)) {
            return Collections.emptyList();
        }
        Map<Long, String> fieldKeyMap = definitions.stream()
                .collect(Collectors.toMap(FieldDefinitionDO::getId, FieldDefinitionDO::getFieldKey));
        return roleFieldPermissionMapper.selectListByRoleId(roleId).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getHidden()))
                .map(permission -> fieldKeyMap.get(permission.getFieldId()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleHiddenFieldIds(Long roleId, String module) {
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
        if (CollUtil.isEmpty(definitions)) {
            return Collections.emptyList();
        }
        Set<Long> moduleFieldIds = convertSet(definitions, FieldDefinitionDO::getId);
        return roleFieldPermissionMapper.selectListByRoleId(roleId).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getHidden()))
                .map(RoleFieldPermissionDO::getFieldId)
                .filter(moduleFieldIds::contains)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getCurrentUserHiddenFields(String module) {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        Set<Long> roleIds = convertSet(getEnableUserRoleListByUserIdFromCache(userId), RoleDO::getId);
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
        if (CollUtil.isEmpty(definitions)) {
            return Collections.emptyList();
        }
        Map<Long, String> fieldKeyMap = definitions.stream()
                .collect(Collectors.toMap(FieldDefinitionDO::getId, FieldDefinitionDO::getFieldKey));
        return roleFieldPermissionMapper.selectListByRoleIds(roleIds).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getHidden()))
                .map(permission -> fieldKeyMap.get(permission.getFieldId()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleFieldPermission(Long roleId, String module, List<Long> hiddenFieldIds) {
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
        if (CollUtil.isEmpty(definitions)) {
            return;
        }
        Set<Long> moduleFieldIds = convertSet(definitions, FieldDefinitionDO::getId);
        roleFieldPermissionMapper.deleteListByRoleIdAndFieldIds(roleId, TenantContextHolder.getRequiredTenantId(), moduleFieldIds);

        Set<Long> hiddenFieldIdSet = CollUtil.emptyIfNull(hiddenFieldIds).stream()
                .filter(moduleFieldIds::contains)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(hiddenFieldIdSet)) {
            return;
        }
        for (RoleFieldPermissionDO entity : CollectionUtils.convertList(hiddenFieldIdSet, fieldId -> {
            RoleFieldPermissionDO entity = new RoleFieldPermissionDO();
            entity.setRoleId(roleId);
            entity.setFieldId(fieldId);
            entity.setHidden(true);
            return entity;
        })) {
            try {
                roleFieldPermissionMapper.insert(entity);
            } catch (DuplicateKeyException ignored) {
                // 历史重复数据或并发保存时，黑名单记录已经存在，保存结果仍然是“隐藏”。
            }
        }
    }

    @Override
    @Cacheable(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId) {
        return convertSet(roleMenuMapper.selectListByMenuId(menuId), RoleMenuDO::getRoleId);
    }

    // ========== 用户-角色的相关方法  ==========

    @Override
    @DSTransactional // 多数据源，使用 @DSTransactional 保证本地事务，以及数据源的切换
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void assignUserRole(Long userId, Set<Long> roleIds) {
        // 获得角色拥有角色编号
        Set<Long> dbRoleIds = convertSet(userRoleMapper.selectListByUserId(userId),
                UserRoleDO::getRoleId);
        // 计算新增和删除的角色编号
        Set<Long> roleIdList = CollUtil.emptyIfNull(roleIds);
        Collection<Long> createRoleIds = CollUtil.subtract(roleIdList, dbRoleIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbRoleIds, roleIdList);
        // 执行新增和删除。对于已经授权的角色，不用做任何处理
        if (!CollectionUtil.isEmpty(createRoleIds)) {
            userRoleMapper.insertBatch(CollectionUtils.convertList(createRoleIds, roleId -> {
                UserRoleDO entity = new UserRoleDO();
                entity.setUserId(userId);
                entity.setRoleId(roleId);
                return entity;
            }));
        }
        if (!CollectionUtil.isEmpty(deleteMenuIds)) {
            userRoleMapper.deleteListByUserIdAndRoleIdIds(userId, deleteMenuIds);
        }
    }

    @Override
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void processUserDeleted(Long userId) {
        userRoleMapper.deleteListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public Set<Long> getUserRoleIdListByUserIdFromCache(Long userId) {
        return getUserRoleIdListByUserId(userId);
    }

    @Override
    public Set<Long> getUserRoleIdListByRoleId(Collection<Long> roleIds) {
        return convertSet(userRoleMapper.selectListByRoleIds(roleIds), UserRoleDO::getUserId);
    }

    /**
     * 获得用户拥有的角色，并且这些角色是开启状态的
     *
     * @param userId 用户编号
     * @return 用户拥有的角色
     */
    @VisibleForTesting
    List<RoleDO> getEnableUserRoleListByUserIdFromCache(Long userId) {
        // 获得用户拥有的角色编号
        Set<Long> roleIds = getSelf().getUserRoleIdListByUserIdFromCache(userId);
        // 获得角色数组，并移除被禁用的
        List<RoleDO> roles = roleService.getRoleListFromCache(roleIds);
        roles.removeIf(role -> !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        return roles;
    }

    /**
     * Gets enabled roles directly from DB for data permission calculation.
     */
    @VisibleForTesting
    List<RoleDO> getEnableUserRoleListByUserId(Long userId) {
        Set<Long> roleIds = getUserRoleIdListByUserId(userId);
        List<RoleDO> roles = roleService.getRoleList(roleIds);
        roles.removeIf(role -> role == null || !CommonStatusEnum.ENABLE.getStatus().equals(role.getStatus()));
        return roles;
    }

    // ========== 用户-部门的相关方法  ==========

    @Override
    public void assignRoleDataScope(Long roleId, Integer dataScope, Set<Long> dataScopeDeptIds) {
        validateRoleDataScope(dataScope, dataScopeDeptIds);
        roleService.updateRoleDataScope(roleId, dataScope, dataScopeDeptIds);
    }

    private void validateRoleDataScope(Integer dataScope, Set<Long> dataScopeDeptIds) {
        if (Objects.equals(dataScope, DataScopeEnum.DEPT_CUSTOM.getScope())
                && CollUtil.isEmpty(dataScopeDeptIds)) {
            throw exception(ROLE_DATA_SCOPE_DEPT_IDS_EMPTY);
        }
    }

    @Override
    @DataPermission(enable = false) // 关闭数据权限，不然就会出现递归获取数据权限的问题
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        // 获得用户的角色
        List<RoleDO> roles = getEnableUserRoleListByUserId(userId);

        // 如果角色为空，则只能查看自己
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        if (CollUtil.isEmpty(roles)) {
            result.setSelf(true);
            return result;
        }

        // 获得用户的部门编号的缓存，通过 Guava 的 Suppliers 惰性求值，即有且仅有第一次发起 DB 的查询
        Supplier<Set<Long>> userDeptIds = Suppliers.memoize(() -> {
            Set<Long> deptIds = userService.getUserDeptIdListByUserId(userId);
            return deptIds == null ? Collections.emptySet() : deptIds;
        });
        // 遍历每个角色，计算
        for (RoleDO role : roles) {
            // 为空时，跳过
            if (role.getDataScope() == null) {
                continue;
            }
            // 情况一，ALL
            if (Objects.equals(role.getDataScope(), DataScopeEnum.ALL.getScope())) {
                result.setAll(true);
                continue;
            }
            // 情况二，DEPT_CUSTOM
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
                CollUtil.addAll(result.getDeptIds(), role.getDataScopeDeptIds());
                continue;
            }
            // 情况三，DEPT_ONLY
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())) {
                for (Long deptId : userDeptIds.get()) {
                    CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
                }
                continue;
            }
            // 情况四，DEPT_DEPT_AND_CHILD
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_AND_CHILD.getScope())) {
                for (Long deptId : userDeptIds.get()) {
                    if (deptId == null) {
                        continue;
                    }
                    CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
                    CollUtil.addAll(result.getDeptIds(), deptService.getChildDeptIdListFromCache(deptId));
                }
                continue;
            }
            // 情况五，SELF
            if (Objects.equals(role.getDataScope(), DataScopeEnum.SELF.getScope())) {
                result.setSelf(true);
                continue;
            }
            // 未知情况，error log 即可
            log.error("[getDeptDataPermission][LoginUser({}) role({}) 无法处理]", userId, toJsonString(result));
        }
        logEmptyDeptDataPermission(userId, roles, result, userDeptIds);
        return result;
    }

    private void logEmptyDeptDataPermission(Long userId, List<RoleDO> roles, DeptDataPermissionRespDTO result,
                                            Supplier<Set<Long>> userDeptIds) {
        if (Boolean.TRUE.equals(result.getAll()) || Boolean.TRUE.equals(result.getSelf())
                || CollUtil.isNotEmpty(result.getDeptIds())) {
            return;
        }
        boolean hasDeptCustom = roles.stream()
                .anyMatch(role -> Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope()));
        boolean hasDeptCustomEmpty = roles.stream()
                .filter(role -> Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope()))
                .anyMatch(role -> CollUtil.isEmpty(role.getDataScopeDeptIds()));
        boolean needsUserDept = roles.stream()
                .anyMatch(role -> Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())
                        || Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_AND_CHILD.getScope()));
        Set<Long> userDeptIdSet = needsUserDept ? userDeptIds.get() : Collections.emptySet();
        boolean userDeptEmpty = needsUserDept && CollUtil.isEmpty(userDeptIdSet);
        log.warn("[getDeptDataPermission][LoginUser({}) 数据权限为空，hasDeptCustom={}, hasDeptCustomEmpty={}, " +
                        "needsUserDept={}, userDeptEmpty={}, userDeptIds={}, roles={}]",
                userId, hasDeptCustom, hasDeptCustomEmpty, needsUserDept, userDeptEmpty, userDeptIdSet,
                roles.stream()
                        .map(role -> String.format("{id=%s,dataScope=%s,dataScopeDeptIds=%s}",
                                role.getId(), role.getDataScope(), role.getDataScopeDeptIds()))
                        .collect(Collectors.toList()));
    }

    @Override
    @DataPermission(enable = false)
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId, String formKey) {
        List<RoleDO> roles = getEnableUserRoleListByUserId(userId);
        if (CollUtil.isEmpty(roles)) {
            DeptDataPermissionRespDTO r = new DeptDataPermissionRespDTO();
            r.setSelf(true);
            return r;
        }

        // 查该用户角色在此 formKey 上的表单级权限配置，过滤掉值为 0（INHERIT）的记录
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        List<RoleFormDataScopeDO> formScopes =
                roleFormDataScopeMapper.selectListByRoleIdsAndFormKey(roleIds, formKey);
        List<RoleFormDataScopeDO> activeFormScopes = formScopes.stream()
                .filter(s -> s.getDataScope() != null && s.getDataScope() != 0)
                .collect(Collectors.toList());

        // 没有有效的表单级配置，fallback 到全局权限
        if (CollUtil.isEmpty(activeFormScopes)) {
            return getDeptDataPermission(userId);
        }

        // 按表单级配置计算权限，逻辑与全局计算相同
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        Supplier<Set<Long>> userDeptIds = Suppliers.memoize(() -> {
            Set<Long> deptIds = userService.getUserDeptIdListByUserId(userId);
            return deptIds == null ? Collections.emptySet() : deptIds;
        });
        for (RoleFormDataScopeDO scope : activeFormScopes) {
            if (Objects.equals(scope.getDataScope(), DataScopeEnum.ALL.getScope())) {
                result.setAll(true);
                continue;
            }
            if (Objects.equals(scope.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
                CollUtil.addAll(result.getDeptIds(), scope.getDataScopeDeptIds());
                continue;
            }
            if (Objects.equals(scope.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())) {
                userDeptIds.get().forEach(id -> CollectionUtils.addIfNotNull(result.getDeptIds(), id));
                continue;
            }
            if (Objects.equals(scope.getDataScope(), DataScopeEnum.DEPT_AND_CHILD.getScope())) {
                for (Long deptId : userDeptIds.get()) {
                    if (deptId == null) {
                        continue;
                    }
                    CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
                    CollUtil.addAll(result.getDeptIds(), deptService.getChildDeptIdListFromCache(deptId));
                }
                continue;
            }
            if (Objects.equals(scope.getDataScope(), DataScopeEnum.SELF.getScope())) {
                result.setSelf(true);
            }
        }
        return result;
    }

    // ========== 角色-表单数据权限的相关方法 ==========

    @Override
    public List<RoleFormDataScopeRespVO> getRoleFormDataScopeList(Long roleId) {
        return CollectionUtils.convertList(
            roleFormDataScopeMapper.selectListByRoleId(roleId),
            item -> {
                RoleFormDataScopeRespVO vo = new RoleFormDataScopeRespVO();
                vo.setFormKey(item.getFormKey());
                vo.setDataScope(item.getDataScope());
                vo.setDataScopeDeptIds(
                    CollUtil.defaultIfEmpty(item.getDataScopeDeptIds(), Collections.emptySet()));
                return vo;
            });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleFormDataScope(Long roleId,
            List<PermissionAssignRoleFormDataScopeReqVO.FormDataScopeItem> items) {
        roleFormDataScopeMapper.deleteListByRoleId(roleId, TenantContextHolder.getRequiredTenantId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Integer> validDataScopes = Arrays.stream(DataScopeEnum.ARRAYS).collect(Collectors.toSet());
        Map<String, PermissionAssignRoleFormDataScopeReqVO.FormDataScopeItem> itemMap = new LinkedHashMap<>();
        for (PermissionAssignRoleFormDataScopeReqVO.FormDataScopeItem item : items) {
            if (item == null || item.getFormKey() == null || item.getDataScope() == null
                    || !validDataScopes.contains(item.getDataScope())) {
                continue;
            }
            itemMap.put(item.getFormKey(), item);
        }
        if (CollUtil.isEmpty(itemMap)) {
            return;
        }
        List<RoleFormDataScopeDO> entities = CollectionUtils.convertList(itemMap.values(), item -> {
            RoleFormDataScopeDO entity = new RoleFormDataScopeDO();
            entity.setRoleId(roleId);
            entity.setFormKey(item.getFormKey());
            entity.setDataScope(item.getDataScope());
            entity.setDataScopeDeptIds(
                    Objects.equals(item.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())
                            ? CollUtil.defaultIfEmpty(item.getDataScopeDeptIds(), Collections.emptySet())
                            : Collections.emptySet());
            return entity;
        });
        for (RoleFormDataScopeDO entity : entities) {
            try {
                roleFormDataScopeMapper.insert(entity);
            } catch (DuplicateKeyException ignored) {
                roleFormDataScopeMapper.updateByRoleIdAndFormKey(entity);
            }
        }
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private PermissionServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}
