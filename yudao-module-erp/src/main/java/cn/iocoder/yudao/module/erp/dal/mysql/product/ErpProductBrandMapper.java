package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand.ErpProductBrandPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductBrandDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 配件品牌 Mapper
 */
@Mapper
public interface ErpProductBrandMapper extends BaseMapperX<ErpProductBrandDO> {

    default PageResult<ErpProductBrandDO> selectPage(ErpProductBrandPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductBrandDO> wrapper = new LambdaQueryWrapperX<ErpProductBrandDO>()
                .likeIfPresent(ErpProductBrandDO::getName, fuzzyKeyword(reqVO.getName()))
                .eqIfPresent(ErpProductBrandDO::getStatus, reqVO.getStatus());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(), ErpProductBrandDO::getName);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpProductBrandDO> wrapper, ErpProductBrandPageReqVO reqVO) {
        SFunction<ErpProductBrandDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByAsc(ErpProductBrandDO::getSort).orderByDesc(ErpProductBrandDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByAsc(ErpProductBrandDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByDesc(ErpProductBrandDO::getId);
            return;
        }
        wrapper.orderByAsc(ErpProductBrandDO::getSort).orderByDesc(ErpProductBrandDO::getId);
    }

    static SFunction<ErpProductBrandDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpProductBrandDO::getId;
            case "name":
                return ErpProductBrandDO::getName;
            case "status":
                return ErpProductBrandDO::getStatus;
            case "sort":
                return ErpProductBrandDO::getSort;
            case "createTime":
                return ErpProductBrandDO::getCreateTime;
            case "updateTime":
                return ErpProductBrandDO::getUpdateTime;
            default:
                return null;
        }
    }

    static String fuzzyKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        return keyword.trim().replaceAll("\\s+", "%");
    }

    default ErpProductBrandDO selectByName(String name) {
        return selectOne(ErpProductBrandDO::getName, name);
    }

    default List<ErpProductBrandDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpProductBrandDO>()
                .eqIfPresent(ErpProductBrandDO::getStatus, status)
                .orderByAsc(ErpProductBrandDO::getSort)
                .orderByDesc(ErpProductBrandDO::getId));
    }

    default List<ErpProductBrandDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<ErpProductBrandDO>()
                .orderByAsc(ErpProductBrandDO::getSort)
                .orderByDesc(ErpProductBrandDO::getId));
    }

}
