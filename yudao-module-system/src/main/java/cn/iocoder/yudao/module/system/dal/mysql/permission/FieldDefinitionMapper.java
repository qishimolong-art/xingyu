package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface FieldDefinitionMapper extends BaseMapperX<FieldDefinitionDO> {

    default List<FieldDefinitionDO> selectListByModule(String module) {
        return selectList(new LambdaQueryWrapperX<FieldDefinitionDO>()
                .eq(FieldDefinitionDO::getModule, module)
                .orderByAsc(FieldDefinitionDO::getSort)
                .orderByAsc(FieldDefinitionDO::getId));
    }

    default List<FieldDefinitionDO> selectListByModuleAndGroup(String module, String fieldGroup) {
        return selectList(new LambdaQueryWrapperX<FieldDefinitionDO>()
                .eq(FieldDefinitionDO::getModule, module)
                .eq(FieldDefinitionDO::getFieldGroup, fieldGroup)
                .orderByAsc(FieldDefinitionDO::getSort)
                .orderByAsc(FieldDefinitionDO::getId));
    }

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM system_field_definition "
            + "WHERE tenant_id = #{tenantId} AND module = #{module} AND field_group = #{fieldGroup} "
            + "AND deleted = b'0' FOR UPDATE")
    List<FieldDefinitionDO> selectListByModuleAndGroupForUpdate(@Param("tenantId") Long tenantId,
                                                                @Param("module") String module,
                                                                @Param("fieldGroup") String fieldGroup);

    default FieldDefinitionDO selectByModuleAndFieldKey(String module, String fieldKey) {
        return selectOne(new LambdaQueryWrapperX<FieldDefinitionDO>()
                .eq(FieldDefinitionDO::getModule, module)
                .eq(FieldDefinitionDO::getFieldKey, fieldKey));
    }

    default List<FieldDefinitionDO> selectListByModuleAndFieldKeys(String module, Collection<String> fieldKeys) {
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<FieldDefinitionDO>()
                .eq(FieldDefinitionDO::getModule, module)
                .in(FieldDefinitionDO::getFieldKey, fieldKeys));
    }

}
