package cn.iocoder.yudao.module.erp.dal.mysql.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.ErpAssemblyRecipePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface ErpAssemblyRecipeMapper extends BaseMapperX<ErpAssemblyRecipeDO> {

    default PageResult<ErpAssemblyRecipeDO> selectPage(ErpAssemblyRecipePageReqVO reqVO) {
        QueryWrapper<ErpAssemblyRecipeDO> query = new QueryWrapper<>();
        query.eq(reqVO.getStatus() != null, "status", reqVO.getStatus());
        query.eq(reqVO.getPurpose() != null, "purpose", reqVO.getPurpose());
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String value = "%" + reqVO.getKeyword().trim().replaceAll("\\s+", "%") + "%";
            query.and(w -> w.like("module_code", value)
                    .or().like("module_name", value)
                    .or().apply("EXISTS (SELECT 1 FROM erp_product p WHERE p.id = erp_assembly_recipe.product_id "
                            + "AND p.tenant_id = erp_assembly_recipe.tenant_id AND p.deleted = b'0' "
                            + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.factory_code LIKE {0}))", value));
        }
        query.orderByDesc("id");
        return selectPage(reqVO, query);
    }

    default ErpAssemblyRecipeDO selectByProductId(Long productId) {
        return selectOne(ErpAssemblyRecipeDO::getProductId, productId);
    }

    default ErpAssemblyRecipeDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpAssemblyRecipeDO>()
                .eq(ErpAssemblyRecipeDO::getId, id).last("FOR UPDATE"));
    }

    default ErpAssemblyRecipeDO selectByProductIdForUpdate(Long productId) {
        return selectOne(new LambdaQueryWrapperX<ErpAssemblyRecipeDO>()
                .eq(ErpAssemblyRecipeDO::getProductId, productId).last("FOR UPDATE"));
    }

    default ErpAssemblyRecipeDO selectByModuleCodeForUpdate(String moduleCode) {
        return selectOne(new LambdaQueryWrapperX<ErpAssemblyRecipeDO>()
                .eq(ErpAssemblyRecipeDO::getModuleCode, moduleCode).last("FOR UPDATE"));
    }

    default int updateStatus(Long id, Integer oldStatus, Integer newStatus) {
        return update(new ErpAssemblyRecipeDO().setStatus(newStatus),
                new LambdaUpdateWrapper<ErpAssemblyRecipeDO>()
                        .eq(ErpAssemblyRecipeDO::getId, id)
                        .eq(ErpAssemblyRecipeDO::getStatus, oldStatus));
    }

}
