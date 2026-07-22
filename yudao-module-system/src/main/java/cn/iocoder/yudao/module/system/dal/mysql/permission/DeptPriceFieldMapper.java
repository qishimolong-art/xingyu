package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.DeptPriceFieldDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface DeptPriceFieldMapper extends BaseMapperX<DeptPriceFieldDO> {

    default List<DeptPriceFieldDO> selectListByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DeptPriceFieldDO>()
                .in(DeptPriceFieldDO::getDeptId, deptIds));
    }

    default List<DeptPriceFieldDO> selectListByFieldKeys(Collection<String> fieldKeys) {
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<DeptPriceFieldDO>()
                .in(DeptPriceFieldDO::getFieldKey, fieldKeys));
    }

    @Insert("INSERT INTO system_dept_price_field "
            + "(dept_id, field_key, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{deptId}, #{fieldKey}, #{operator}, NOW(), #{operator}, NOW(), b'0', #{tenantId}) "
            + "ON DUPLICATE KEY UPDATE deleted = b'0', updater = VALUES(updater), update_time = NOW()")
    int restoreOrInsert(@Param("deptId") Long deptId,
                        @Param("fieldKey") String fieldKey,
                        @Param("operator") String operator,
                        @Param("tenantId") Long tenantId);

    @Update("<script>"
            + "UPDATE system_dept_price_field SET deleted = b'1', updater = #{operator}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND field_key = #{fieldKey} AND deleted = b'0' AND dept_id IN "
            + "<foreach collection='deptIds' item='deptId' open='(' separator=',' close=')'>#{deptId}</foreach>"
            + "</script>")
    int softDeleteByFieldKeyAndDeptIds(@Param("fieldKey") String fieldKey,
                                       @Param("deptIds") Collection<Long> deptIds,
                                       @Param("operator") String operator,
                                       @Param("tenantId") Long tenantId);

    @Delete("<script>"
            + "DELETE FROM system_dept_price_field WHERE tenant_id = #{tenantId} AND field_key IN "
            + "<foreach collection='fieldKeys' item='fieldKey' open='(' separator=',' close=')'>#{fieldKey}</foreach>"
            + "</script>")
    int physicalDeleteByFieldKeys(@Param("fieldKeys") Collection<String> fieldKeys,
                                  @Param("tenantId") Long tenantId);

}
