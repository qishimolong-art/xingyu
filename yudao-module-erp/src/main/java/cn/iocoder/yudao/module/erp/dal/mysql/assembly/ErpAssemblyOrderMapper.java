package cn.iocoder.yudao.module.erp.dal.mysql.assembly;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.ErpAssemblyOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

@Mapper
public interface ErpAssemblyOrderMapper extends BaseMapperX<ErpAssemblyOrderDO> {

    default PageResult<ErpAssemblyOrderDO> selectPage(ErpAssemblyOrderPageReqVO reqVO) {
        QueryWrapper<ErpAssemblyOrderDO> query = new QueryWrapper<>();
        query.eq(reqVO.getWarehouseId() != null, "warehouse_id", reqVO.getWarehouseId())
                .eq(reqVO.getStatus() != null, "status", reqVO.getStatus())
                .between(reqVO.getAssemblyTime() != null && reqVO.getAssemblyTime().length == 2,
                        "assembly_time",
                        reqVO.getAssemblyTime() != null ? reqVO.getAssemblyTime()[0] : null,
                        reqVO.getAssemblyTime() != null ? reqVO.getAssemblyTime()[1] : null);
        if (StringUtils.hasText(reqVO.getKeyword())) {
            String value = "%" + reqVO.getKeyword().trim().replaceAll("\\s+", "%") + "%";
            query.and(w -> w.like("no", reqVO.getKeyword().trim())
                    .or().like("recipe_module_code", value)
                    .or().like("recipe_module_name", value)
                    .or().apply("EXISTS (SELECT 1 FROM erp_product p WHERE p.id = erp_assembly_order.product_id "
                            + "AND p.tenant_id = erp_assembly_order.tenant_id AND p.deleted = b'0' "
                            + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.factory_code LIKE {0}))", value));
        }
        query.orderByDesc("id");
        return selectPage(reqVO, query);
    }

    default ErpAssemblyOrderDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpAssemblyOrderDO>()
                .eq(ErpAssemblyOrderDO::getId, id).last("FOR UPDATE"));
    }

    default ErpAssemblyOrderDO selectByNo(String no) {
        return selectOne(ErpAssemblyOrderDO::getNo, no);
    }

    default Long selectCountByRecipeId(Long recipeId) {
        return selectCount(ErpAssemblyOrderDO::getRecipeId, recipeId);
    }

    default int updateByIdAndStatus(Long id, Integer oldStatus, ErpAssemblyOrderDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpAssemblyOrderDO>()
                .eq(ErpAssemblyOrderDO::getId, id)
                .eq(ErpAssemblyOrderDO::getStatus, oldStatus));
    }

}
