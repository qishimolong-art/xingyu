package cn.iocoder.yudao.module.crm.dal.mysql.product;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.product.vo.category.CrmProductCategoryListReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.product.CrmProductCategoryDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * CRM 产品分类 Mapper
 *
 * @author ZanGe丶
 */
@Mapper
public interface CrmProductCategoryMapper extends BaseMapperX<CrmProductCategoryDO> {

    default List<CrmProductCategoryDO> selectList(CrmProductCategoryListReqVO reqVO) {
        LambdaQueryWrapperX<CrmProductCategoryDO> wrapper = new LambdaQueryWrapperX<CrmProductCategoryDO>()
                .likeIfPresent(CrmProductCategoryDO::getName, reqVO.getName())
                .eqIfPresent(CrmProductCategoryDO::getParentId, reqVO.getParentId());
        orderByIfPresent(wrapper, reqVO);
        return selectList(wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<CrmProductCategoryDO> wrapper, CrmProductCategoryListReqVO reqVO) {
        SFunction<CrmProductCategoryDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(CrmProductCategoryDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByAsc(CrmProductCategoryDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByDesc(CrmProductCategoryDO::getId);
            return;
        }
        wrapper.orderByDesc(CrmProductCategoryDO::getId);
    }

    static SFunction<CrmProductCategoryDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "name":
                return CrmProductCategoryDO::getName;
            case "id":
                return CrmProductCategoryDO::getId;
            case "createTime":
                return CrmProductCategoryDO::getCreateTime;
            case "updateTime":
                return CrmProductCategoryDO::getUpdateTime;
            default:
                return null;
        }
    }

    default CrmProductCategoryDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(CrmProductCategoryDO::getParentId, parentId, CrmProductCategoryDO::getName, name);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(CrmProductCategoryDO::getParentId, parentId);
    }

}
