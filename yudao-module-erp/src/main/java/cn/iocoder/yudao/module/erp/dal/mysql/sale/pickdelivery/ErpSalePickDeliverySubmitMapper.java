package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickDeliverySubmitTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ErpSalePickDeliverySubmitMapper extends BaseMapperX<ErpSalePickDeliverySubmitDO> {

    default List<ErpSalePickDeliverySubmitDO> selectDeliveryUsersBySaleOutId(Long saleOutId) {
        return selectList(new QueryWrapper<ErpSalePickDeliverySubmitDO>()
                .select("submit_user_id").eq("sale_out_id", saleOutId)
                .eq("type", ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType())
                .isNotNull("submit_user_id").groupBy("submit_user_id")
                .orderByAsc("MIN(submit_time)", "MIN(id)"));
    }

    default ErpSalePickDeliverySubmitDO selectByRequestId(Long orderId, Integer type, String requestId) {
        return selectOne(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getOrderId, orderId)
                .eq(ErpSalePickDeliverySubmitDO::getType, type)
                .eq(ErpSalePickDeliverySubmitDO::getRequestId, requestId).last("FOR UPDATE"));
    }

    default List<ErpSalePickDeliverySubmitDO> selectListByOrderId(Long orderId) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getOrderId, orderId)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default List<ErpSalePickDeliverySubmitDO> selectListByOrderIdAndType(Long orderId, Integer type) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getOrderId, orderId)
                .eq(ErpSalePickDeliverySubmitDO::getType, type)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default List<ErpSalePickDeliverySubmitDO> selectListBySaleOutId(Long saleOutId) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getSaleOutId, saleOutId)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default List<ErpSalePickDeliverySubmitDO> selectListBySaleOutIdAndType(Long saleOutId, Integer type) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getSaleOutId, saleOutId)
                .eqIfPresent(ErpSalePickDeliverySubmitDO::getType, type)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default PageResult<ErpSalePickDeliverySubmitDO> selectPageByOrderIdAndType(PageParam pageParam,
                                                                               Long orderId,
                                                                               Integer type) {
        return selectPage(pageParam, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getOrderId, orderId)
                .eq(ErpSalePickDeliverySubmitDO::getType, type)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default List<ErpSalePickDeliverySubmitDO> selectListByPickTaskIdAndType(Long pickTaskId, Integer type) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getPickTaskId, pickTaskId)
                .eq(ErpSalePickDeliverySubmitDO::getType, type)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

    default PageResult<ErpSalePickDeliverySubmitDO> selectPageByPickTaskIdAndType(PageParam pageParam,
                                                                                  Long pickTaskId,
                                                                                  Integer type) {
        return selectPage(pageParam, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitDO>()
                .eq(ErpSalePickDeliverySubmitDO::getPickTaskId, pickTaskId)
                .eq(ErpSalePickDeliverySubmitDO::getType, type)
                .orderByDesc(ErpSalePickDeliverySubmitDO::getId));
    }

}
