package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionTableConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface FormPermissionTableConfigMapper extends BaseMapperX<FormPermissionTableConfigDO> {

    default FormPermissionTableConfigDO selectByFormType(String formType) {
        return selectOne(new LambdaQueryWrapperX<FormPermissionTableConfigDO>()
                .eq(FormPermissionTableConfigDO::getFormType, formType));
    }

    default List<FormPermissionTableConfigDO> selectListByEnabled(Boolean enabled) {
        return selectList(new LambdaQueryWrapperX<FormPermissionTableConfigDO>()
                .eqIfPresent(FormPermissionTableConfigDO::getEnabled, enabled)
                .orderByAsc(FormPermissionTableConfigDO::getFormType));
    }

}
