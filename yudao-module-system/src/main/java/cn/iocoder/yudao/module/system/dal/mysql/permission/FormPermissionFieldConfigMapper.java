package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionFieldConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface FormPermissionFieldConfigMapper extends BaseMapperX<FormPermissionFieldConfigDO> {

    default List<FormPermissionFieldConfigDO> selectListByFormType(String formType) {
        return selectList(new LambdaQueryWrapperX<FormPermissionFieldConfigDO>()
                .eq(FormPermissionFieldConfigDO::getFormType, formType)
                .orderByAsc(FormPermissionFieldConfigDO::getColumnName));
    }

    default List<FormPermissionFieldConfigDO> selectListByFormTypes(Collection<String> formTypes) {
        if (formTypes == null || formTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<FormPermissionFieldConfigDO>()
                .in(FormPermissionFieldConfigDO::getFormType, formTypes)
                .orderByAsc(FormPermissionFieldConfigDO::getFormType)
                .orderByAsc(FormPermissionFieldConfigDO::getColumnName));
    }

    @Delete("DELETE FROM form_permission_field_config WHERE form_type = #{formType}")
    int deleteByFormType(@Param("formType") String formType);

}
