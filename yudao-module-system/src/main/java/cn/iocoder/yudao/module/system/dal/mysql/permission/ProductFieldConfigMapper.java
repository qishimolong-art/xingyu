package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.ProductFieldConfigDO;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductFieldConfigMapper extends BaseMapperX<ProductFieldConfigDO> {

    default List<ProductFieldConfigDO> selectListByModuleAndGroup(String moduleKey, String fieldGroup) {
        return selectList(new LambdaQueryWrapperX<ProductFieldConfigDO>()
                .eq(ProductFieldConfigDO::getModuleKey, moduleKey)
                .eq(ProductFieldConfigDO::getFieldGroup, fieldGroup)
                .orderByAsc(ProductFieldConfigDO::getSort)
                .orderByAsc(ProductFieldConfigDO::getId));
    }

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT id, module_key, field_name, field_label, field_group, sort, "
            + "creator, create_time, updater, update_time, deleted "
            + "FROM erp_field_config "
            + "WHERE tenant_id = #{tenantId} AND module_key = #{moduleKey} AND field_group = #{fieldGroup} "
            + "AND deleted = b'0' "
            + "ORDER BY sort ASC, id ASC FOR UPDATE")
    List<ProductFieldConfigDO> selectListByModuleAndGroupForUpdate(@Param("tenantId") Long tenantId,
                                                                   @Param("moduleKey") String moduleKey,
                                                                   @Param("fieldGroup") String fieldGroup);

}
