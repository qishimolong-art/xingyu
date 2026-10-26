package cn.iocoder.yudao.module.erp.dal.mysql.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpAssemblyRecipeItemMapper extends BaseMapperX<ErpAssemblyRecipeItemDO> {

    default List<ErpAssemblyRecipeItemDO> selectListByRecipeId(Long recipeId) {
        return selectList(new LambdaQueryWrapperX<ErpAssemblyRecipeItemDO>()
                .eq(ErpAssemblyRecipeItemDO::getRecipeId, recipeId)
                .orderByAsc(ErpAssemblyRecipeItemDO::getSort)
                .orderByAsc(ErpAssemblyRecipeItemDO::getId));
    }

    default List<ErpAssemblyRecipeItemDO> selectListByRecipeIds(Collection<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpAssemblyRecipeItemDO>()
                .in(ErpAssemblyRecipeItemDO::getRecipeId, recipeIds)
                .orderByAsc(ErpAssemblyRecipeItemDO::getRecipeId)
                .orderByAsc(ErpAssemblyRecipeItemDO::getSort)
                .orderByAsc(ErpAssemblyRecipeItemDO::getId));
    }

    default PageResult<ErpAssemblyRecipeItemDO> selectPage(ErpAssemblyRecipeItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAssemblyRecipeItemDO>()
                .eq(ErpAssemblyRecipeItemDO::getRecipeId, reqVO.getRecipeId())
                .orderByAsc(ErpAssemblyRecipeItemDO::getSort)
                .orderByAsc(ErpAssemblyRecipeItemDO::getId));
    }

    default int deleteByRecipeId(Long recipeId) {
        return delete(ErpAssemblyRecipeItemDO::getRecipeId, recipeId);
    }

    default List<Map<String, Object>> selectCountRowsByRecipeIds(Collection<Long> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectCountRowsByRecipeIdsInternal(recipeIds);
    }

    @Select({"<script>",
            "SELECT recipe_id AS recipeId, COUNT(*) AS itemCount FROM erp_assembly_recipe_item",
            "WHERE deleted = b'0' AND recipe_id IN",
            "<foreach collection='recipeIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "GROUP BY recipe_id",
            "</script>"})
    List<Map<String, Object>> selectCountRowsByRecipeIdsInternal(
            @Param("recipeIds") Collection<Long> recipeIds);

}
