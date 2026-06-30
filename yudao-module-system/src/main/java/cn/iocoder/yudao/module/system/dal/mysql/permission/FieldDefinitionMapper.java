package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

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
