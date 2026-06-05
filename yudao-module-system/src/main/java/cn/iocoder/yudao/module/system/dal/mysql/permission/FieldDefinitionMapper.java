package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FieldDefinitionMapper extends BaseMapperX<FieldDefinitionDO> {

    default List<FieldDefinitionDO> selectListByModule(String module) {
        return selectList(new LambdaQueryWrapperX<FieldDefinitionDO>()
                .eq(FieldDefinitionDO::getModule, module)
                .orderByAsc(FieldDefinitionDO::getSort)
                .orderByAsc(FieldDefinitionDO::getId));
    }

}
