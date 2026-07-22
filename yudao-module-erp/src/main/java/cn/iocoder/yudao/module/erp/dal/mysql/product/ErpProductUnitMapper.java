package cn.iocoder.yudao.module.erp.dal.mysql.product;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit.ErpProductUnitPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 产品单位 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpProductUnitMapper extends BaseMapperX<ErpProductUnitDO> {

    default PageResult<ErpProductUnitDO> selectPage(ErpProductUnitPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpProductUnitDO> wrapper = new LambdaQueryWrapperX<ErpProductUnitDO>()
                .likeIfPresent(ErpProductUnitDO::getName, reqVO.getName())
                .eqIfPresent(ErpProductUnitDO::getStatus, reqVO.getStatus());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(), ErpProductUnitDO::getName);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpProductUnitDO> wrapper, ErpProductUnitPageReqVO reqVO) {
        SFunction<ErpProductUnitDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpProductUnitDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByAsc(ErpProductUnitDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByDesc(ErpProductUnitDO::getId);
            return;
        }
        wrapper.orderByDesc(ErpProductUnitDO::getId);
    }

    static SFunction<ErpProductUnitDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpProductUnitDO::getId;
            case "name":
                return ErpProductUnitDO::getName;
            case "status":
                return ErpProductUnitDO::getStatus;
            case "createTime":
                return ErpProductUnitDO::getCreateTime;
            case "updateTime":
                return ErpProductUnitDO::getUpdateTime;
            default:
                return null;
        }
    }

    default ErpProductUnitDO selectByName(String name) {
        return selectOne(ErpProductUnitDO::getName, name);
    }

    default List<ErpProductUnitDO> selectListByStatus(Integer status) {
        return selectList(ErpProductUnitDO::getStatus, status);
    }

    default List<ErpProductUnitDO> selectListAll() {
        return selectList(new LambdaQueryWrapperX<ErpProductUnitDO>()
                .orderByDesc(ErpProductUnitDO::getId));
    }

}
