package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionCreateOrUpdateReqDTO;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 权限 API 接口
 *
 * @author 芋道源码
 */
public interface PermissionApi extends PermissionCommonApi {

    /**
     * 获得拥有多个角色的用户编号集合
     *
     * @param roleIds 角色编号集合
     * @return 用户编号集合
     */
    Set<Long> getUserRoleIdListByRoleIds(Collection<Long> roleIds);

    /**
     * 获得当前登录用户在指定模块需要隐藏的字段 key 列表。
     *
     * @param module 模块标识，例如 erp_product
     * @return 字段 key 列表
     */
    List<String> getCurrentUserHiddenFields(String module);

    /**
     * Gets hidden fields using the business document department for the
     * department-level product price permission layer.
     *
     * @param module module key
     * @param businessDeptId business document department; {@code null} falls back to the login department
     * @return hidden field keys
     */
    List<String> getCurrentUserHiddenFields(String module, Long businessDeptId);

    /**
     * Gets hidden fields and optionally skips the product price view permission
     * layer.
     *
     * @param module module key
     * @param businessDeptId business document department
     * @param includeProductPricePermission whether to include product price view permissions
     * @return hidden field keys
     */
    List<String> getCurrentUserHiddenFields(String module, Long businessDeptId,
                                            boolean includeProductPricePermission);

    void createOrUpdateFieldDefinitions(List<FieldDefinitionCreateOrUpdateReqDTO> definitions);

    void deleteFieldDefinitions(String module, List<String> fieldKeys);

    /**
     * 获得指定模块和分组的有效字段目录。
     */
    List<FieldDefinitionRespDTO> getFieldDefinitions(String module, String fieldGroup);

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

    /**
     * 获得登陆用户在指定表单上的数据权限。若该表单无独立配置，则 fallback 到全局数据权限。
     *
     * @param userId  用户编号
     * @param formKey 表单标识，通常与数据库表名一致（如 erp_product）
     * @return 部门数据权限
     */
    DeptDataPermissionRespDTO getDeptDataPermission(Long userId, String formKey);

}
