package cn.iocoder.yudao.module.erp.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpAutoWriteOffDeptConfigDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ErpAutoWriteOffDeptConfigMapper extends BaseMapperX<ErpAutoWriteOffDeptConfigDO> {

    default List<ErpAutoWriteOffDeptConfigDO> selectActiveList() {
        return selectList(new LambdaQueryWrapperX<ErpAutoWriteOffDeptConfigDO>()
                .orderByAsc(ErpAutoWriteOffDeptConfigDO::getDeptId));
    }

    @Update("UPDATE erp_auto_write_off_dept_config "
            + "SET deleted = b'1', updater = #{operator}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND deleted = b'0'")
    int softDeleteAll(@Param("operator") String operator, @Param("tenantId") Long tenantId);

    @Insert("INSERT INTO erp_auto_write_off_dept_config "
            + "(dept_id, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{deptId}, #{operator}, NOW(), #{operator}, NOW(), b'0', #{tenantId}) "
            + "ON DUPLICATE KEY UPDATE deleted = b'0', updater = VALUES(updater), update_time = NOW()")
    int restoreOrInsert(@Param("deptId") Long deptId,
                        @Param("operator") String operator,
                        @Param("tenantId") Long tenantId);

}
