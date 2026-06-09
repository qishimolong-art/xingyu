package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFormDataScopeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface RoleFormDataScopeMapper extends BaseMapperX<RoleFormDataScopeDO> {

    default List<RoleFormDataScopeDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .eq(RoleFormDataScopeDO::getRoleId, roleId));
    }

    default List<RoleFormDataScopeDO> selectListByRoleIdsAndFormKey(
            Collection<Long> roleIds, String formKey) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .in(RoleFormDataScopeDO::getRoleId, roleIds)
                .eq(RoleFormDataScopeDO::getFormKey, formKey));
    }

    default void deleteListByRoleId(Long roleId) {
        delete(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .eq(RoleFormDataScopeDO::getRoleId, roleId));
    }

    @Delete("DELETE FROM system_role_form_data_scope WHERE role_id = #{roleId} AND tenant_id = #{tenantId}")
    void deleteListByRoleId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    default void updateByRoleIdAndFormKey(RoleFormDataScopeDO updateObj) {
        update(updateObj, new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .eq(RoleFormDataScopeDO::getRoleId, updateObj.getRoleId())
                .eq(RoleFormDataScopeDO::getFormKey, updateObj.getFormKey()));
    }

}
