package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryOrderDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliveryOrderMapper extends BaseMapperX<ErpSalePickDeliveryOrderDO> {

    default ErpSalePickDeliveryOrderDO selectBySaleOutId(Long saleOutId) {
        return selectOne(ErpSalePickDeliveryOrderDO::getSaleOutId, saleOutId);
    }

    default List<ErpSalePickDeliveryOrderDO> selectListBySaleOutIds(Collection<Long> saleOutIds) {
        return selectList(ErpSalePickDeliveryOrderDO::getSaleOutId, saleOutIds);
    }

    default ErpSalePickDeliveryOrderDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO>()
                .eq(ErpSalePickDeliveryOrderDO::getId, id).last("FOR UPDATE"));
    }

    default PageResult<ErpSalePickDeliveryOrderDO> selectPage(ErpSaleDeliveryPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO> wrapper = new LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO>()
                .inIfPresent(ErpSalePickDeliveryOrderDO::getId, reqVO.getIds())
                .likeIfPresent(ErpSalePickDeliveryOrderDO::getSaleOutNo, reqVO.getSaleOutNo())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getPickStatus, reqVO.getPickStatus())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getDeliveryStatus, reqVO.getDeliveryStatus())
                .betweenIfPresent(ErpSalePickDeliveryOrderDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(ErpSalePickDeliveryOrderDO::getDeliveryCompleteTime, reqVO.getDeliveryCompleteTime())
                .orderByDesc(ErpSalePickDeliveryOrderDO::getId);
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpSalePickDeliveryOrderDO::getSaleOutNo,
                ErpSalePickDeliveryOrderDO::getCustomerName);
        return selectPage(reqVO, wrapper);
    }

}
