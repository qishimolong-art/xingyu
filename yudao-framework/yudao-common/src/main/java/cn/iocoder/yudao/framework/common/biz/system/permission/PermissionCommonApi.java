package cn.iocoder.yudao.framework.common.biz.system.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;

import java.util.Collection;
import java.util.Set;

/**
 * 权限 API 接口
 *
 * @author 芋道源码
 */
public interface PermissionCommonApi {

    /**
     * 判断是否有权限，任一一个即可
     *
     * @param userId 用户编号
     * @param permissions 权限
     * @return 是否
     */
    boolean hasAnyPermissions(Long userId, String... permissions);

    /**
     * 判断是否有角色，任一一个即可
     *
     * @param userId 用户编号
     * @param roles 角色数组
     * @return 是否
     */
    boolean hasAnyRoles(Long userId, String... roles);

    /**
     * 获得登陆用户的部门数据权限
     *
     * @param userId 用户编号
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId);

    /**
     * 获得登陆用户在指定表单（formKey）上的数据权限。
     * 若该表单无独立配置，则 fallback 到全局数据权限。
     *
     * @param userId  用户编号
     * @param formKey 表单标识，通常与数据库表名一致（如 erp_product）
     * @return 部门数据权限
     */
    default DeptDataPermissionRespDTO getDeptDataPermission(Long userId, String formKey) {
        return getDeptDataPermission(userId);
    }

    /**
     * 获得用户真实关联的部门编号集合。
     *
     * @param userId 用户编号
     * @return 部门编号集合
     */
    Set<Long> getDeptIdsByUserId(Long userId);

    /**
     * 获得指定部门下的用户编号集合。
     *
     * @param deptIds 部门编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserIdsByDeptIds(Collection<Long> deptIds);

}
