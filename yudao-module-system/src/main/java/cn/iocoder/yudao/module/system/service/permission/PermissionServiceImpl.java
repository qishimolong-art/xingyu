package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
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
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserPermissionDenyMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserPermissionDenyDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.redis.RedisKeyConstants;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.user.UserPriceFieldService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap2;
import static cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.ROLE_DATA_SCOPE_DEPT_IDS_EMPTY;

/**
 * 鏉冮檺 Service 瀹炵幇绫? *
 * @author 鑺嬮亾婧愮爜
 */
@Service
@Slf4j
public class PermissionServiceImpl implements PermissionService {

    private static final String SYSTEM_DEPT_FORM_KEY = "system_dept";
    private static final String ERP_PRODUCT_FIELD_PERMISSION_MODULE = "erp_product";

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
    private UserPermissionDenyMapper userPermissionDenyMapper;

    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private DeptService deptService;
    @Resource
    private AdminUserService userService;
    @Resource
    private UserPriceFieldService userPriceFieldService;

    @Override
    public boolean hasAnyPermissions(Long userId, String... permissions) {
        // 濡傛灉涓虹┖锛岃鏄庡凡缁忔湁鏉冮檺
        if (ArrayUtil.isEmpty(permissions)) {
            return true;
        }

        // 鑾峰緱褰撳墠鐧诲綍鐨勮鑹层€傚鏋滀负绌猴紝璇存槑娌℃湁鏉冮檺
        List<RoleDO> roles = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roles)) {
            return false;
        }

        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        // 瓒呯骇绠＄悊鍛樻部鐢ㄨ鑹茬洿閫氶€昏緫锛屼笉鍙楃敤鎴风骇 deny 褰卞搷锛岄伩鍏嶈閿佸畾绠＄悊鍏ュ彛
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return true;
        }

        Set<String> deniedPermissions = getSelf().getUserDeniedPermissionsFromCache(userId);
        // 鎯呭喌涓€锛氶亶鍘嗗垽鏂瘡涓潈闄愶紝濡傛灉鏈変竴婊¤冻锛岃鏄庢湁鏉冮檺
        for (String permission : permissions) {
            if (!deniedPermissions.contains(permission) && hasAnyPermission(roles, permission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 鍒ゆ柇鎸囧畾瑙掕壊锛屾槸鍚︽嫢鏈夎 permission 鏉冮檺
     *
     * @param roles 鎸囧畾瑙掕壊鏁扮粍
     * @param permission 鏉冮檺鏍囪瘑
     * @return 鏄惁鎷ユ湁
     */
    private boolean hasAnyPermission(List<RoleDO> roles, String permission) {
        List<Long> menuIds = menuService.getMenuIdListByPermissionFromCache(permission);
        // 閲囩敤涓ユ牸妯″紡锛屽鏋滄潈闄愭壘涓嶅埌瀵瑰簲鐨?Menu 鐨勮瘽锛屼篃璁や负娌℃湁鏉冮檺
        if (CollUtil.isEmpty(menuIds)) {
            return false;
        }

        // 鍒ゆ柇鏄惁鏈夋潈闄?
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        for (Long menuId : menuIds) {
            // 鑾峰緱鎷ユ湁璇ヨ彍鍗曠殑瑙掕壊缂栧彿闆嗗悎
            Set<Long> menuRoleIds = getSelf().getMenuRoleIdListByMenuIdFromCache(menuId);
            // 濡傛灉鏈変氦闆嗭紝璇存槑鏈夋潈闄?
            if (CollUtil.containsAny(menuRoleIds, roleIds)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyRoles(Long userId, String... roles) {
        // 濡傛灉涓虹┖锛岃鏄庡凡缁忔湁鏉冮檺
        if (ArrayUtil.isEmpty(roles)) {
            return true;
        }

        // 鑾峰緱褰撳墠鐧诲綍鐨勮鑹层€傚鏋滀负绌猴紝璇存槑娌℃湁鏉冮檺
        List<RoleDO> roleList = getEnableUserRoleListByUserIdFromCache(userId);
        if (CollUtil.isEmpty(roleList)) {
            return false;
        }

        // 鍒ゆ柇鏄惁鏈夎鑹?
        Set<String> userRoles = convertSet(roleList, RoleDO::getCode);
        return CollUtil.containsAny(userRoles, Sets.newHashSet(roles));
    }

    // ========== 瑙掕壊-鑿滃崟鐨勭浉鍏虫柟娉? ==========

    @Override
    @DSTransactional // 澶氭暟鎹簮锛屼娇鐢?@DSTransactional 淇濊瘉鏈湴浜嬪姟锛屼互鍙婃暟鎹簮鐨勫垏鎹?
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.MENU_ROLE_ID_LIST,
            allEntries = true),
            @CacheEvict(value = RedisKeyConstants.PERMISSION_MENU_ID_LIST,
            allEntries = true) // allEntries 娓呯┖鎵€鏈夌紦瀛橈紝涓昏涓€娆℃洿鏂版秹鍙婂埌鐨?menuIds 杈冨锛屽弽鍊掓壒閲忎細鏇村揩
    })
    public void assignRoleMenu(Long roleId, Set<Long> menuIds) {
        // 鑾峰緱瑙掕壊鎷ユ湁鑿滃崟缂栧彿
        Set<Long> dbMenuIds = convertSet(roleMenuMapper.selectListByRoleId(roleId), RoleMenuDO::getMenuId);
        // 璁＄畻鏂板鍜屽垹闄ょ殑鑿滃崟缂栧彿
        Set<Long> menuIdList = CollUtil.emptyIfNull(menuIds);
        Collection<Long> createMenuIds = CollUtil.subtract(menuIdList, dbMenuIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbMenuIds, menuIdList);
        // 鎵ц鏂板鍜屽垹闄ゃ€傚浜庡凡缁忔巿鏉冪殑鑿滃崟锛屼笉鐢ㄥ仛浠讳綍澶勭悊
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
                    allEntries = true), // allEntries 娓呯┖鎵€鏈夌紦瀛橈紝姝ゅ鏃犳硶鏂逛究鑾峰緱 roleId 瀵瑰簲鐨?menu 缂撳瓨浠?
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST,
                    allEntries = true) // allEntries 娓呯┖鎵€鏈夌紦瀛橈紝姝ゅ鏃犳硶鏂逛究鑾峰緱 roleId 瀵瑰簲鐨?user 缂撳瓨浠?
    })
    public void processRoleDeleted(Long roleId) {
        // 鏍囪鍒犻櫎 UserRole
        userRoleMapper.deleteListByRoleId(roleId);
        // 鏍囪鍒犻櫎 RoleMenu
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

        // 濡傛灉鏄鐞嗗憳鐨勬儏鍐典笅锛岃幏鍙栧叏閮ㄨ彍鍗曠紪鍙?
        if (roleService.hasAnySuperAdmin(roleIds)) {
            return convertSet(menuService.getMenuList(), MenuDO::getId);
        }
        // 濡傛灉鏄潪绠＄悊鍛樼殑鎯呭喌涓嬶紝鑾峰緱鎷ユ湁鐨勮彍鍗曠紪鍙?
        return convertSet(roleMenuMapper.selectListByRoleId(roleIds), RoleMenuDO::getMenuId);
    }

    // ========== 瑙掕壊-瀛楁鏉冮檺鐨勭浉鍏虫柟娉?==========

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
        List<String> hiddenFields = new ArrayList<>();
        Set<Long> roleIds = convertSet(getEnableUserRoleListByUserIdFromCache(userId), RoleDO::getId);
        if (CollUtil.isNotEmpty(roleIds)) {
            List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
            if (CollUtil.isNotEmpty(definitions)) {
                Map<Long, String> fieldKeyMap = definitions.stream()
                        .collect(Collectors.toMap(FieldDefinitionDO::getId, FieldDefinitionDO::getFieldKey));
                hiddenFields.addAll(roleFieldPermissionMapper.selectListByRoleIds(roleIds).stream()
                        .filter(permission -> Boolean.TRUE.equals(permission.getHidden()))
                        .map(permission -> fieldKeyMap.get(permission.getFieldId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
        }
        if (ERP_PRODUCT_FIELD_PERMISSION_MODULE.equals(module)) {
            hiddenFields.addAll(userPriceFieldService.getHiddenProductPriceFields(userId));
        }
        return hiddenFields.stream().distinct().collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoleFieldPermission(Long roleId, String module, List<Long> hiddenFieldIds) {
        List<FieldDefinitionDO> definitions = fieldDefinitionMapper.selectListByModule(module);
        if (CollUtil.isEmpty(definitions)) {
            return;
        }
        Set<Long> moduleFieldIds = convertSet(definitions, FieldDefinitionDO::getId);
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        roleFieldPermissionMapper.deleteListByRoleIdAndFieldIds(roleId, tenantId, moduleFieldIds);

        Set<Long> hiddenFieldIdSet = CollUtil.emptyIfNull(hiddenFieldIds).stream()
                .filter(moduleFieldIds::contains)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(hiddenFieldIdSet)) {
            return;
        }
        for (Long fieldId : hiddenFieldIdSet) {
            roleFieldPermissionMapper.insertIgnore(roleId, fieldId, tenantId);
        }
    }

    @Override
    @Cacheable(value = RedisKeyConstants.MENU_ROLE_ID_LIST, key = "#menuId")
    public Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId) {
        return convertSet(roleMenuMapper.selectListByMenuId(menuId), RoleMenuDO::getRoleId);
    }

    // ========== 鐢ㄦ埛-瑙掕壊鐨勭浉鍏虫柟娉? ==========

    @Override
    @DSTransactional // 澶氭暟鎹簮锛屼娇鐢?@DSTransactional 淇濊瘉鏈湴浜嬪姟锛屼互鍙婃暟鎹簮鐨勫垏鎹?
    @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId")
    public void assignUserRole(Long userId, Set<Long> roleIds) {
        // 鑾峰緱瑙掕壊鎷ユ湁瑙掕壊缂栧彿
        Set<Long> dbRoleIds = convertSet(userRoleMapper.selectListByUserId(userId),
                UserRoleDO::getRoleId);
        // 璁＄畻鏂板鍜屽垹闄ょ殑瑙掕壊缂栧彿
        Set<Long> roleIdList = CollUtil.emptyIfNull(roleIds);
        Collection<Long> createRoleIds = CollUtil.subtract(roleIdList, dbRoleIds);
        Collection<Long> deleteMenuIds = CollUtil.subtract(dbRoleIds, roleIdList);
        // 鎵ц鏂板鍜屽垹闄ゃ€傚浜庡凡缁忔巿鏉冪殑瑙掕壊锛屼笉鐢ㄥ仛浠讳綍澶勭悊
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
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = RedisKeyConstants.USER_DENIED_PERMISSION_LIST, key = "#userId")
    public void assignUserDeniedPermissions(Long userId, Set<String> deniedPermissions) {
        userPermissionDenyMapper.deleteListByUserId(userId, TenantContextHolder.getRequiredTenantId());
        Set<String> permissionSet = CollUtil.emptyIfNull(deniedPermissions).stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(permissionSet)) {
            return;
        }
        List<UserPermissionDenyDO> entities = CollectionUtils.convertList(permissionSet, permission -> {
            UserPermissionDenyDO entity = new UserPermissionDenyDO();
            entity.setUserId(userId);
            entity.setPermission(permission);
            return entity;
        });
        for (UserPermissionDenyDO entity : entities) {
            try {
                userPermissionDenyMapper.insert(entity);
            } catch (DuplicateKeyException ignored) {
                // Full replacement normally removes old rows first; this keeps concurrent saves idempotent.
            }
        }
    }

    @Override
    @DSTransactional
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId"),
            @CacheEvict(value = RedisKeyConstants.USER_DENIED_PERMISSION_LIST, key = "#userId")
    })
    public void assignUserRoleAndDeniedPermissions(Long userId, Set<Long> roleIds, Set<String> deniedPermissions) {
        assignUserRole(userId, roleIds);
        if (roleService.hasAnySuperAdmin(CollUtil.emptyIfNull(roleIds))) {
            assignUserDeniedPermissions(userId, Collections.emptySet());
            return;
        }
        Set<String> validDeniedPermissions = filterRoleGrantedButtonPermissions(roleIds, deniedPermissions);
        assignUserDeniedPermissions(userId, validDeniedPermissions);
    }

    private Set<String> filterRoleGrantedButtonPermissions(Set<Long> roleIds, Set<String> permissions) {
        if (CollUtil.isEmpty(roleIds) || CollUtil.isEmpty(permissions)) {
            return Collections.emptySet();
        }
        Set<String> requestPermissions = permissions.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(requestPermissions)) {
            return Collections.emptySet();
        }
        Set<Long> menuIds = getRoleMenuListByRoleId(roleIds);
        if (CollUtil.isEmpty(menuIds)) {
            return Collections.emptySet();
        }
        return menuService.getMenuList(menuIds).stream()
                .filter(menu -> Objects.equals(menu.getType(), MenuTypeEnum.BUTTON.getType()))
                .map(MenuDO::getPermission)
                .filter(requestPermissions::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Set<String> getUserDeniedPermissions(Long userId) {
        return convertSet(userPermissionDenyMapper.selectListByUserId(userId), UserPermissionDenyDO::getPermission);
    }

    @Override
    @Cacheable(value = RedisKeyConstants.USER_DENIED_PERMISSION_LIST, key = "#userId")
    public Set<String> getUserDeniedPermissionsFromCache(Long userId) {
        return getUserDeniedPermissions(userId);
    }

    @Override
    public List<MenuDO> filterDeniedMenus(Long userId, List<RoleDO> roles, List<MenuDO> menuList) {
        if (CollUtil.isEmpty(menuList) || CollUtil.isEmpty(roles)
                || roleService.hasAnySuperAdmin(convertSet(roles, RoleDO::getId))) {
            return menuList;
        }
        Set<String> deniedPermissions = getUserDeniedPermissionsFromCache(userId);
        if (CollUtil.isEmpty(deniedPermissions)) {
            return menuList;
        }
        return menuList.stream()
                .filter(menu -> !Objects.equals(menu.getType(), MenuTypeEnum.BUTTON.getType())
                        || StrUtil.isBlank(menu.getPermission())
                        || !deniedPermissions.contains(menu.getPermission()))
                .collect(Collectors.toList());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = RedisKeyConstants.USER_ROLE_ID_LIST, key = "#userId"),
            @CacheEvict(value = RedisKeyConstants.USER_DENIED_PERMISSION_LIST, key = "#userId")
    })
    public void processUserDeleted(Long userId) {
        userRoleMapper.deleteListByUserId(userId);
        userPermissionDenyMapper.deleteListByUserId(userId, TenantContextHolder.getRequiredTenantId());
    }

    @Override
    public Set<Long> getUserRoleIdListByUserId(Long userId) {
        return convertSet(userRoleMapper.selectListByUserId(userId), UserRoleDO::getRoleId);
    }

    @Override
    public Map<Long, Set<Long>> getUserRoleIdListByUserIds(Collection<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return convertMultiMap2(userRoleMapper.selectListByUserIds(userIds), UserRoleDO::getUserId,
                UserRoleDO::getRoleId);
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
     * 鑾峰緱鐢ㄦ埛鎷ユ湁鐨勮鑹诧紝骞朵笖杩欎簺瑙掕壊鏄紑鍚姸鎬佺殑
     *
     * @param userId 鐢ㄦ埛缂栧彿
     * @return 鐢ㄦ埛鎷ユ湁鐨勮鑹?     */
    @VisibleForTesting
    List<RoleDO> getEnableUserRoleListByUserIdFromCache(Long userId) {
        // 鑾峰緱鐢ㄦ埛鎷ユ湁鐨勮鑹茬紪鍙?
        Set<Long> roleIds = getSelf().getUserRoleIdListByUserIdFromCache(userId);
        // 鑾峰緱瑙掕壊鏁扮粍锛屽苟绉婚櫎琚鐢ㄧ殑
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

    // ========== 鐢ㄦ埛-閮ㄩ棬鐨勭浉鍏虫柟娉? ==========

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
    @DataPermission(enable = false) // 鍏抽棴鏁版嵁鏉冮檺锛屼笉鐒跺氨浼氬嚭鐜伴€掑綊鑾峰彇鏁版嵁鏉冮檺鐨勯棶棰?
    public DeptDataPermissionRespDTO getDeptDataPermission(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user != null && user.getDataScope() != null && !Objects.equals(user.getDataScope(), 0)) {
            Supplier<Set<Long>> userDeptIds = Suppliers.memoize(() -> {
                Set<Long> deptIds = userService.getUserDeptIdListByUserId(userId);
                return deptIds == null ? Collections.emptySet() : deptIds;
            });
            return buildDeptDataPermission(user.getDataScope(), user.getDataScopeDeptIds(), userDeptIds);
        }
        // 鑾峰緱鐢ㄦ埛鐨勮鑹?
        List<RoleDO> roles = getEnableUserRoleListByUserId(userId);

        // 濡傛灉瑙掕壊涓虹┖锛屽垯鍙兘鏌ョ湅鑷繁
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        if (CollUtil.isEmpty(roles)) {
            result.setSelf(true);
            return result;
        }

        // 鑾峰緱鐢ㄦ埛鐨勯儴闂ㄧ紪鍙风殑缂撳瓨锛岄€氳繃 Guava 鐨?Suppliers 鎯版€ф眰鍊硷紝鍗虫湁涓斾粎鏈夌涓€娆″彂璧?DB 鐨勬煡璇?
        Supplier<Set<Long>> userDeptIds = Suppliers.memoize(() -> {
            Set<Long> deptIds = userService.getUserDeptIdListByUserId(userId);
            return deptIds == null ? Collections.emptySet() : deptIds;
        });
        // 閬嶅巻姣忎釜瑙掕壊锛岃绠?
        for (RoleDO role : roles) {
            // 涓虹┖鏃讹紝璺宠繃
            if (role.getDataScope() == null) {
                continue;
            }
            // 鎯呭喌涓€锛孉LL
            if (Objects.equals(role.getDataScope(), DataScopeEnum.ALL.getScope())) {
                result.setAll(true);
                continue;
            }
            // 鎯呭喌浜岋紝DEPT_CUSTOM
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
                CollUtil.addAll(result.getDeptIds(), role.getDataScopeDeptIds());
                continue;
            }
            // 鎯呭喌涓夛紝DEPT_ONLY
            if (Objects.equals(role.getDataScope(), DataScopeEnum.DEPT_ONLY.getScope())) {
                for (Long deptId : userDeptIds.get()) {
                    CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
                }
                continue;
            }
            // 鎯呭喌鍥涳紝DEPT_DEPT_AND_CHILD
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
            // 鎯呭喌浜旓紝SELF
            if (Objects.equals(role.getDataScope(), DataScopeEnum.SELF.getScope())) {
                result.setSelf(true);
                continue;
            }
            // 鏈煡鎯呭喌锛宔rror log 鍗冲彲
            log.error("[getDeptDataPermission][LoginUser({}) role({}) 鏃犳硶澶勭悊]", userId, toJsonString(result));
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
        log.warn("[getDeptDataPermission][LoginUser({}) 鏁版嵁鏉冮檺涓虹┖锛宧asDeptCustom={}, hasDeptCustomEmpty={}, " +
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
        DeptDataPermissionRespDTO userPermission = getUserLevelDeptDataPermission(userId);
        if (userPermission != null) {
            return userPermission;
        }
        List<RoleDO> roles = getEnableUserRoleListByUserId(userId);
        if (CollUtil.isEmpty(roles)) {
            DeptDataPermissionRespDTO r = new DeptDataPermissionRespDTO();
            r.setSelf(true);
            return r;
        }

        // 鏌ヨ鐢ㄦ埛瑙掕壊鍦ㄦ formKey 涓婄殑琛ㄥ崟绾ф潈闄愰厤缃紝杩囨护鎺夊€间负 0锛圛NHERIT锛夌殑璁板綍
        Set<Long> roleIds = convertSet(roles, RoleDO::getId);
        List<RoleFormDataScopeDO> formScopes =
                roleFormDataScopeMapper.selectListByRoleIdsAndFormKey(roleIds, formKey);
        List<RoleFormDataScopeDO> activeFormScopes = formScopes.stream()
                .filter(s -> s.getDataScope() != null && s.getDataScope() != 0)
                .collect(Collectors.toList());

        // 娌℃湁鏈夋晥鐨勮〃鍗曠骇閰嶇疆锛宖allback 鍒板叏灞€鏉冮檺
        if (CollUtil.isEmpty(activeFormScopes)) {
            return getDeptDataPermission(userId);
        }

        // 鎸夎〃鍗曠骇閰嶇疆璁＄畻鏉冮檺锛岄€昏緫涓庡叏灞€璁＄畻鐩稿悓
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

    private DeptDataPermissionRespDTO getUserLevelDeptDataPermission(Long userId) {
        AdminUserDO user = userService.getUser(userId);
        if (user == null || user.getDataScope() == null || Objects.equals(user.getDataScope(), 0)) {
            return null;
        }
        Supplier<Set<Long>> userDeptIds = Suppliers.memoize(() -> {
            Set<Long> deptIds = userService.getUserDeptIdListByUserId(userId);
            return deptIds == null ? Collections.emptySet() : deptIds;
        });
        return buildDeptDataPermission(user.getDataScope(), user.getDataScopeDeptIds(), userDeptIds);
    }

    private DeptDataPermissionRespDTO buildDeptDataPermission(Integer dataScope, Set<Long> dataScopeDeptIds,
                                                              Supplier<Set<Long>> userDeptIds) {
        DeptDataPermissionRespDTO result = new DeptDataPermissionRespDTO();
        if (Objects.equals(dataScope, DataScopeEnum.ALL.getScope())) {
            result.setAll(true);
            return result;
        }
        if (Objects.equals(dataScope, DataScopeEnum.DEPT_CUSTOM.getScope())) {
            CollUtil.addAll(result.getDeptIds(), dataScopeDeptIds);
            return result;
        }
        if (Objects.equals(dataScope, DataScopeEnum.DEPT_ONLY.getScope())) {
            for (Long deptId : userDeptIds.get()) {
                CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
            }
            return result;
        }
        if (Objects.equals(dataScope, DataScopeEnum.DEPT_AND_CHILD.getScope())) {
            for (Long deptId : userDeptIds.get()) {
                if (deptId == null) {
                    continue;
                }
                CollectionUtils.addIfNotNull(result.getDeptIds(), deptId);
                CollUtil.addAll(result.getDeptIds(), deptService.getChildDeptIdListFromCache(deptId));
            }
            return result;
        }
        if (Objects.equals(dataScope, DataScopeEnum.SELF.getScope())) {
            result.setSelf(true);
        }
        return result;
    }

    // ========== 瑙掕壊-琛ㄥ崟鏁版嵁鏉冮檺鐨勭浉鍏虫柟娉?==========

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
            if (SYSTEM_DEPT_FORM_KEY.equals(item.getFormKey())
                    && Objects.equals(item.getDataScope(), DataScopeEnum.SELF.getScope())) {
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
     * 鑾峰緱鑷韩鐨勪唬鐞嗗璞★紝瑙ｅ喅 AOP 鐢熸晥闂
     *
     * @return 鑷繁
     */
    private PermissionServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}
