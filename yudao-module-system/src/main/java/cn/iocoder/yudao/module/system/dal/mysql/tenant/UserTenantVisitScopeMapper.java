package cn.iocoder.yudao.module.system.dal.mysql.tenant;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.UserTenantVisitScopeDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserTenantVisitScopeMapper extends BaseMapperX<UserTenantVisitScopeDO> {

    default UserTenantVisitScopeDO selectByUserId(Long userId) {
        return selectOne(UserTenantVisitScopeDO::getUserId, userId);
    }

    @Delete("DELETE FROM system_user_tenant_visit_scope WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    int deletePhysicallyByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

}
