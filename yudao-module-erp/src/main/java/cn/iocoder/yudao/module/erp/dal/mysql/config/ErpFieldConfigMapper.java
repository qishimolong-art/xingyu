package cn.iocoder.yudao.module.erp.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpFieldConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
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

    /**
     * 物理删除：绕过 MyBatis-Plus 逻辑删除，直接 DELETE FROM
     */
    @Delete("<script>DELETE FROM erp_field_config WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    int physicalDeleteByIds(@Param("ids") Collection<Long> ids);

    @Select("SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = #{tableName} AND COLUMN_NAME = #{columnName}")
    Long selectColumnCount(@Param("tableName") String tableName, @Param("columnName") String columnName);

    @Update("ALTER TABLE `${tableName}` ADD COLUMN `${columnName}` ${columnDefinition}")
    void addColumn(@Param("tableName") String tableName,
                   @Param("columnName") String columnName,
                   @Param("columnDefinition") String columnDefinition);
}
