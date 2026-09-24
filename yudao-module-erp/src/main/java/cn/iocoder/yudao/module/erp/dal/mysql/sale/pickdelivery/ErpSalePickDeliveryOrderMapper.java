package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryOrderDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliveryOrderMapper extends BaseMapperX<ErpSalePickDeliveryOrderDO> {

    default ErpSalePickDeliveryOrderDO selectBySaleOutId(Long saleOutId) {
        if (saleOutId == null) {
            return null;
        }
        return selectOne(ErpSalePickDeliveryOrderDO::getSaleOutId, saleOutId);
    }

    default List<ErpSalePickDeliveryOrderDO> selectListBySaleOutIds(Collection<Long> saleOutIds) {
        return selectList(ErpSalePickDeliveryOrderDO::getSaleOutId, saleOutIds);
    }

    default ErpSalePickDeliveryOrderDO selectBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        return selectOne(ErpSalePickDeliveryOrderDO::getSourceType, sourceType,
                ErpSalePickDeliveryOrderDO::getSourceId, sourceId);
    }

    default ErpSalePickDeliveryOrderDO selectBySourceForUpdate(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO>()
                .eq(ErpSalePickDeliveryOrderDO::getSourceType, sourceType)
                .eq(ErpSalePickDeliveryOrderDO::getSourceId, sourceId)
                .last("FOR UPDATE"));
    }

    default ErpSalePickDeliveryOrderDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO>()
                .eq(ErpSalePickDeliveryOrderDO::getId, id).last("FOR UPDATE"));
    }

    default PageResult<ErpSalePickDeliveryOrderDO> selectPage(ErpSaleDeliveryPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO> wrapper = new LambdaQueryWrapperX<ErpSalePickDeliveryOrderDO>()
                .inIfPresent(ErpSalePickDeliveryOrderDO::getId, reqVO.getIds())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getPickStatus, reqVO.getPickStatus())
                .eqIfPresent(ErpSalePickDeliveryOrderDO::getDeliveryStatus, reqVO.getDeliveryStatus())
                .betweenIfPresent(ErpSalePickDeliveryOrderDO::getCreateTime, reqVO.getCreateTime())
                .betweenIfPresent(ErpSalePickDeliveryOrderDO::getDeliveryCompleteTime, reqVO.getDeliveryCompleteTime())
                .orderByDesc(ErpSalePickDeliveryOrderDO::getId);
        if (StringUtils.hasText(reqVO.getSaleOutNo())) {
            wrapper.and(query -> query.like(ErpSalePickDeliveryOrderDO::getSaleOutNo, reqVO.getSaleOutNo())
                    .or().like(ErpSalePickDeliveryOrderDO::getSourceNo, reqVO.getSaleOutNo()));
        }
        ErpKeywordQuery.append(wrapper, reqVO.getKeyword(),
                ErpSalePickDeliveryOrderDO::getSaleOutNo,
                ErpSalePickDeliveryOrderDO::getSourceNo,
                ErpSalePickDeliveryOrderDO::getCustomerName);
        return selectPage(reqVO, wrapper);
    }

}
