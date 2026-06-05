package cn.iocoder.yudao.module.system.api.permission;

import cn.iocoder.yudao.framework.common.biz.system.permission.PermissionCommonApi;

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

}
