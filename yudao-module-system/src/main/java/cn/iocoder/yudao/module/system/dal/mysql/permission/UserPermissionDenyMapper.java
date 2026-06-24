package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserPermissionDenyDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface UserPermissionDenyMapper extends BaseMapperX<UserPermissionDenyDO> {

    default List<UserPermissionDenyDO> selectListByUserId(Long userId) {
        return selectList(UserPermissionDenyDO::getUserId, userId);
    }

    @Delete("DELETE FROM system_user_permission_deny WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    void deleteListByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    default void deleteListByUserId(Long userId) {
        delete(UserPermissionDenyDO::getUserId, userId);
    }

    @Delete("<script>"
            + "DELETE FROM system_user_permission_deny "
            + "WHERE user_id = #{userId} AND permission IN "
            + "<foreach collection='permissions' item='permission' open='(' separator=',' close=')'>"
            + "#{permission}"
            + "</foreach>"
            + "</script>")
    void deleteListByUserIdAndPermissions(@Param("userId") Long userId,
                                          @Param("permissions") Collection<String> permissions);

}
