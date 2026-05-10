package cn.iocoder.yudao.module.erp.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpFieldConfigMapper extends BaseMapperX<ErpFieldConfigDO> {

    default List<ErpFieldConfigDO> selectListByModuleKey(String moduleKey) {
        return selectList(new LambdaQueryWrapperX<ErpFieldConfigDO>()
                .eqIfPresent(ErpFieldConfigDO::getModuleKey, moduleKey)
                .orderByAsc(ErpFieldConfigDO::getSort));
    }

    default ErpFieldConfigDO selectByModuleKeyAndFieldName(String moduleKey, String fieldName) {
        return selectOne(new LambdaQueryWrapperX<ErpFieldConfigDO>()
                .eq(ErpFieldConfigDO::getModuleKey, moduleKey)
                .eq(ErpFieldConfigDO::getFieldName, fieldName));
    }
}
