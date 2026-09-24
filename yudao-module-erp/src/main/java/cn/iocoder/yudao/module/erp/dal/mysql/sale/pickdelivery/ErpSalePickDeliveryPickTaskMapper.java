package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliveryPickTaskMapper extends BaseMapperX<ErpSalePickDeliveryPickTaskDO> {

    default ErpSalePickDeliveryPickTaskDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO>()
                .eq(ErpSalePickDeliveryPickTaskDO::getId, id).last("FOR UPDATE"));
    }

    default List<ErpSalePickDeliveryPickTaskDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSalePickDeliveryPickTaskDO::getOrderId, orderId);
    }

    default PageResult<ErpSalePickDeliveryPickTaskDO> selectPage(ErpSalePickPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO> wrapper = new LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO>()
                .inIfPresent(ErpSalePickDeliveryPickTaskDO::getId, reqVO.getIds())
                .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getStatus, reqVO.getStatus())
                .inIfPresent(ErpSalePickDeliveryPickTaskDO::getWarehouseId, reqVO.getWarehouseIds())
                .betweenIfPresent(ErpSalePickDeliveryPickTaskDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(ErpSalePickDeliveryPickTaskDO::getCompleteTime, reqVO.getCompleteTime())
                .orderByDesc(ErpSalePickDeliveryPickTaskDO::getId);
        if (StringUtils.hasText(reqVO.getSaleOutNo())) {
            wrapper.and(query -> query.like(ErpSalePickDeliveryPickTaskDO::getSaleOutNo, reqVO.getSaleOutNo())
                    .or().like(ErpSalePickDeliveryPickTaskDO::getSourceNo, reqVO.getSaleOutNo()));
        }
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpSalePickDeliveryPickTaskDO::getSaleOutNo,
                ErpSalePickDeliveryPickTaskDO::getSourceNo,
                ErpSalePickDeliveryPickTaskDO::getCustomerName,
                ErpSalePickDeliveryPickTaskDO::getWarehouseName);
        return selectPage(reqVO, wrapper);
    }

}
