package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFormDataScopeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface RoleFormDataScopeMapper extends BaseMapperX<RoleFormDataScopeDO> {

    default List<RoleFormDataScopeDO> selectListByRoleIdsAndFormKey(
            Collection<Long> roleIds, String formKey) {
        if (CollUtil.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .in(RoleFormDataScopeDO::getRoleId, roleIds)
                .eq(RoleFormDataScopeDO::getFormKey, formKey));
    }

    default List<RoleFormDataScopeDO> selectListByRoleId(Long roleId) {
        return selectList(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .eq(RoleFormDataScopeDO::getRoleId, roleId));
    }

    default RoleFormDataScopeDO selectByRoleIdAndFormKey(Long roleId, String formKey) {
        return selectOne(new LambdaQueryWrapperX<RoleFormDataScopeDO>()
                .eq(RoleFormDataScopeDO::getRoleId, roleId)
                .eq(RoleFormDataScopeDO::getFormKey, formKey));
    }

}
