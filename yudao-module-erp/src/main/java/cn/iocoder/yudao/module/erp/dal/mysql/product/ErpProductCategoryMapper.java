package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.category.ErpProductCategoryListReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 产品分类 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProductCategoryMapper extends BaseMapperX<ErpProductCategoryDO> {

    default List<ErpProductCategoryDO> selectList(ErpProductCategoryListReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductCategoryDO> wrapper = new LambdaQueryWrapperX<ErpProductCategoryDO>()
                .likeIfPresent(ErpProductCategoryDO::getName, reqVO.getName())
                .eqIfPresent(ErpProductCategoryDO::getStatus, reqVO.getStatus());
        orderByIfPresent(wrapper, reqVO);
        return selectList(wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpProductCategoryDO> wrapper, ErpProductCategoryListReqVO reqVO) {
        SFunction<ErpProductCategoryDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpProductCategoryDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByAsc(ErpProductCategoryDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByDesc(ErpProductCategoryDO::getId);
            return;
        }
        wrapper.orderByDesc(ErpProductCategoryDO::getId);
    }

    static SFunction<ErpProductCategoryDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "name":
                return ErpProductCategoryDO::getName;
            case "code":
                return ErpProductCategoryDO::getCode;
            case "sort":
                return ErpProductCategoryDO::getSort;
            case "status":
                return ErpProductCategoryDO::getStatus;
            case "createTime":
                return ErpProductCategoryDO::getCreateTime;
            case "updateTime":
                return ErpProductCategoryDO::getUpdateTime;
            default:
                return null;
        }
    }

	default ErpProductCategoryDO selectByParentIdAndName(Long parentId, String name) {
	    return selectOne(ErpProductCategoryDO::getParentId, parentId, ErpProductCategoryDO::getName, name);
	}

    default ErpProductCategoryDO selectByCode(String code) {
        return selectOne(ErpProductCategoryDO::getCode, code);
    }

    default ErpProductCategoryDO selectByCodeExcludeId(String code, Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpProductCategoryDO>()
                .eq(ErpProductCategoryDO::getCode, code)
                .neIfPresent(ErpProductCategoryDO::getId, id));
    }

    default List<ErpProductCategoryDO> selectListByParentId(Long parentId) {
        return selectList(ErpProductCategoryDO::getParentId, parentId);
    }

    default Long selectCountByParentId(Long parentId) {
        return selectCount(ErpProductCategoryDO::getParentId, parentId);
    }

}
