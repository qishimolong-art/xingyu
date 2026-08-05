package cn.iocoder.yudao.module.system.dal.mysql.permission;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormDataPermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface FormDataPermissionMapper extends BaseMapperX<FormDataPermissionDO> {

    default List<FormDataPermissionDO> selectListByFormAndRelation(String formType, Long formId, String relation) {
        return selectList(new LambdaQueryWrapperX<FormDataPermissionDO>()
                .eq(FormDataPermissionDO::getFormType, formType)
                .eq(FormDataPermissionDO::getFormId, formId)
                .eq(FormDataPermissionDO::getRelation, relation));
    }

    default FormDataPermissionDO selectOneByFormUserRelation(String formType, Long formId, Long userId, String relation) {
        return selectOne(new LambdaQueryWrapperX<FormDataPermissionDO>()
                .eq(FormDataPermissionDO::getFormType, formType)
                .eq(FormDataPermissionDO::getFormId, formId)
                .eq(FormDataPermissionDO::getUserId, userId)
                .eq(FormDataPermissionDO::getRelation, relation));
    }

    default Long selectCountByFormAndUser(String formType, Long formId, Long userId) {
        return selectCount(new LambdaQueryWrapperX<FormDataPermissionDO>()
                .eq(FormDataPermissionDO::getFormType, formType)
                .eq(FormDataPermissionDO::getFormId, formId)
                .eq(FormDataPermissionDO::getUserId, userId));
    }

    default List<FormDataPermissionDO> selectListByFormTypes(Collection<String> formTypes) {
        if (formTypes == null || formTypes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<FormDataPermissionDO>()
                .in(FormDataPermissionDO::getFormType, formTypes));
    }

    @Delete("DELETE FROM form_data_permission WHERE form_type = #{formType} AND form_id = #{formId} AND relation = #{relation}")
    int deleteByFormAndRelation(@Param("formType") String formType,
                                @Param("formId") Long formId,
                                @Param("relation") String relation);

    @Delete("DELETE FROM form_data_permission WHERE form_type = #{formType} AND form_id = #{formId} "
            + "AND relation = #{relation} AND user_id = #{userId}")
    int deleteByFormUserRelation(@Param("formType") String formType,
                                 @Param("formId") Long formId,
                                 @Param("relation") String relation,
                                 @Param("userId") Long userId);

    @Delete("DELETE FROM form_data_permission WHERE form_type = #{formType} AND form_id = #{formId}")
    int deleteByForm(@Param("formType") String formType, @Param("formId") Long formId);

    @Select("SELECT DISTINCT form_type FROM form_data_permission")
    List<String> selectDistinctFormTypes();

}
