package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.PermissionAssignRoleFormDataScopeReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.permission.RoleFormDataScopeRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singleton;

/**
 * 权限 Service 接口
 * <p>
 * 提供用户-角色、角色-菜单、角色-部门的关联权限处理
 *
 * @author 芋道源码
 */
public interface PermissionService {

    /**
     * 判断是否有权限，任一一个即可
     *
     * @param userId      用户编号
     * @param permissions 权限
     * @return 是否
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * 判断是否有角色，任一一个即可
     *
     * @param roles 角色数组
     * @return 是否
     */
    boolean hasAnyRoles(Long userId, String... roles);

    // ========== 角色-菜单的相关方法  ==========

    /**
     * 设置角色菜单
     *
     * @param roleId  角色编号
     * @param menuIds 菜单编号集合
     */
    void assignRoleMenu(Long roleId, Set<Long> menuIds);

    /**
     * 处理角色删除时，删除关联授权数据
     *
     * @param roleId 角色编号
     */
    void processRoleDeleted(Long roleId);

    /**
     * 处理菜单删除时，删除关联授权数据
     *
     * @param menuId 菜单编号
     */
    void processMenuDeleted(Long menuId);

    /**
     * 获得角色拥有的菜单编号集合
     *
     * @param roleId 角色编号
     * @return 菜单编号集合
     */
    default Set<Long> getRoleMenuListByRoleId(Long roleId) {
        return getRoleMenuListByRoleId(singleton(roleId));
    }

    /**
     * 获得角色们拥有的菜单编号集合
     *
     * @param roleIds 角色编号数组
     * @return 菜单编号集合
     */
    Set<Long> getRoleMenuListByRoleId(Collection<Long> roleIds);

    // ========== 角色-字段权限的相关方法 ==========

    List<FieldDefinitionDO> getFieldDefinitions(String module);

    List<String> getRoleHiddenFields(Long roleId, String module);

    List<Long> getRoleHiddenFieldIds(Long roleId, String module);

    List<String> getCurrentUserHiddenFields(String module);

    void assignRoleFieldPermission(Long roleId, String module, List<Long> hiddenFieldIds);

    /**
     * 获得拥有指定菜单的角色编号数组，从缓存中获取
     *
     * @param menuId 菜单编号
     * @return 角色编号数组
     */
    Set<Long> getMenuRoleIdListByMenuIdFromCache(Long menuId);

    // ========== 用户-角色的相关方法  ==========

    /**
     * 设置用户角色
     *
     * @param userId  角色编号
     * @param roleIds 角色编号集合
     */
    void assignUserRole(Long userId, Set<Long> roleIds);

    /**
     * Save user-level denied button permissions. These records only subtract from role-granted permissions.
     *
     * @param userId user id
     * @param deniedPermissions denied permission codes
     */
    void assignUserDeniedPermissions(Long userId, Set<String> deniedPermissions);

    /**
     * Save user roles and user-level denied button permissions together.
     *
     * @param userId user id
     * @param roleIds role ids
     * @param deniedPermissions denied permission codes
     */
    void assignUserRoleAndDeniedPermissions(Long userId, Set<Long> roleIds, Set<String> deniedPermissions);

    /**
     * Get user-level denied button permission codes.
     *
     * @param userId user id
     * @return denied permission codes
     */
    Set<String> getUserDeniedPermissions(Long userId);

    /**
     * Get user-level denied button permission codes from cache.
     *
     * @param userId user id
     * @return denied permission codes
     */
    Set<String> getUserDeniedPermissionsFromCache(Long userId);

    /**
     * Remove denied button permissions from role-granted menu list for normal users.
     *
     * @param userId user id
     * @param roles enabled user roles
     * @param menuList role-granted menu list
     * @return filtered menu list
     */
    List<MenuDO> filterDeniedMenus(Long userId, List<RoleDO> roles, List<MenuDO> menuList);

    /**
     * 处理用户删除时，删除关联授权数据
     *
     * @param userId 用户编号
     */
    void processUserDeleted(Long userId);

    /**
     * 获得拥有多个角色的用户编号集合
     *
     * @param roleIds 角色编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserRoleIdListByRoleId(Collection<Long> roleIds);

    /**
     * 获得用户拥有的角色编号集合
     *
     * @param userId 用户编号
     * @return 角色编号集合
     */
    Set<Long> getUserRoleIdListByUserId(Long userId);

    /**
     * 获得多个用户拥有的角色编号集合
     *
     * @param userIds 用户编号集合
     * @return 用户编号与角色编号集合的映射
     */
    Map<Long, Set<Long>> getUserRoleIdListByUserIds(Collection<Long> userIds);

    /**
     * 获得用户拥有的角色编号集合，从缓存中获取
     *
     * @param userId 用户编号
     * @return 角色编号集合
     */
    Set<Long> getUserRoleIdListByUserIdFromCache(Long userId);

    // ========== 用户-部门的相关方法  ==========

    /**
     * 设置角色的数据权限
     *
     * @param roleId           角色编号
     * @param dataScope        数据范围
     * @param dataScopeDeptIds 部门编号数组
     */
    void assignRoleDataScope(Long roleId, Integer dataScope, Set<Long> dataScopeDeptIds);

    /**
     * 获得登陆用户的部门数据权限
     *
     * @param userId 用户编号
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);

    /**
     * 获得登陆用户在指定表单上的数据权限。若该表单无独立配置，则 fallback 到全局数据权限。
     *
     * @param userId  用户编号
     * @param formKey 表单标识，通常与数据库表名一致（如 erp_product）
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId, String formKey);

    // ========== 角色-表单数据权限的相关方法 ==========

    /** 获得角色在所有表单上的数据权限列表 */
    List<RoleFormDataScopeRespVO> getRoleFormDataScopeList(Long roleId);

    /**
     * 批量保存角色的表单级数据权限（全量替换该角色的所有表单配置）
     *
     * @param roleId 角色编号
     * @param items  表单级配置列表；传空 list 表示清空
     */
    void assignRoleFormDataScope(Long roleId,
            List<PermissionAssignRoleFormDataScopeReqVO.FormDataScopeItem> items);

}
