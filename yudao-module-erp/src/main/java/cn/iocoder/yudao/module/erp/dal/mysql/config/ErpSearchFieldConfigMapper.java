package cn.iocoder.yudao.module.erp.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpSearchFieldConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSearchFieldConfigMapper extends BaseMapperX<ErpSearchFieldConfigDO> {

    default List<ErpSearchFieldConfigDO> selectListByModuleKey(String moduleKey) {
        return selectList(new LambdaQueryWrapperX<ErpSearchFieldConfigDO>()
                .eqIfPresent(ErpSearchFieldConfigDO::getModuleKey, moduleKey)
                .orderByAsc(ErpSearchFieldConfigDO::getSort)
                .orderByAsc(ErpSearchFieldConfigDO::getId));
    }

    default List<ErpSearchFieldConfigDO> selectEnabledListByModuleKey(String moduleKey) {
        return selectList(new LambdaQueryWrapperX<ErpSearchFieldConfigDO>()
                .eq(ErpSearchFieldConfigDO::getModuleKey, moduleKey)
                .eq(ErpSearchFieldConfigDO::getEnabled, Boolean.TRUE)
                .orderByAsc(ErpSearchFieldConfigDO::getSort)
                .orderByAsc(ErpSearchFieldConfigDO::getId));
    }

    /**
     * 物理删除：绕过 MyBatis-Plus 逻辑删除，避免唯一索引冲突。
     */
    @Delete("<script>DELETE FROM erp_search_field_config WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);

}
