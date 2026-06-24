package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleFieldPermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface RoleFieldPermissionMapper extends BaseMapperX<RoleFieldPermissionDO> {

    default List<RoleFieldPermissionDO> selectListByRoleId(Long roleId) {
        return selectList(RoleFieldPermissionDO::getRoleId, roleId);
    }

    default List<RoleFieldPermissionDO> selectListByRoleIds(Collection<Long> roleIds) {
        return selectList(RoleFieldPermissionDO::getRoleId, roleIds);
    }

    @Delete("DELETE FROM system_role_field_permission WHERE role_id = #{roleId} AND tenant_id = #{tenantId}")
    void deleteListByRoleId(@Param("roleId") Long roleId, @Param("tenantId") Long tenantId);

    @Delete("<script>"
            + "DELETE FROM system_role_field_permission "
            + "WHERE role_id = #{roleId} AND tenant_id = #{tenantId} AND field_id IN "
            + "<foreach collection='fieldIds' item='fieldId' open='(' separator=',' close=')'>"
            + "#{fieldId}"
            + "</foreach>"
            + "</script>")
    void deleteListByRoleIdAndFieldIds(@Param("roleId") Long roleId,
                                       @Param("tenantId") Long tenantId,
                                       @Param("fieldIds") Collection<Long> fieldIds);

    @Insert("INSERT IGNORE INTO system_role_field_permission "
            + "(role_id, field_id, hidden, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{roleId}, #{fieldId}, b'1', '1', NOW(), '1', NOW(), b'0', #{tenantId})")
    int insertIgnore(@Param("roleId") Long roleId,
                     @Param("fieldId") Long fieldId,
                     @Param("tenantId") Long tenantId);

}
