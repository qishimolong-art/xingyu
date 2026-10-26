package cn.iocoder.yudao.module.erp.service.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpAssemblyRecipeService {

    Long createRecipe(@Valid ErpAssemblyRecipeSaveReqVO reqVO);

    void updateRecipe(@Valid ErpAssemblyRecipeSaveReqVO reqVO);

    void updateRecipeStatus(Long id, Integer status);

    void deleteRecipe(Long id);

    ErpAssemblyRecipeDO getRecipe(Long id);

    PageResult<ErpAssemblyRecipeDO> getRecipePage(ErpAssemblyRecipePageReqVO reqVO);

    List<ErpAssemblyRecipeItemDO> getRecipeItemList(Long recipeId);

    List<ErpAssemblyRecipeItemDO> getRecipeItemList(Collection<Long> recipeIds);

    PageResult<ErpAssemblyRecipeItemDO> getRecipeItemPage(ErpAssemblyRecipeItemPageReqVO reqVO);

}
