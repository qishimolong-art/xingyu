package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliveryItemMapper extends BaseMapperX<ErpSalePickDeliveryItemDO> {

    default List<ErpSalePickDeliveryItemDO> selectListByOrderId(Long orderId) {
        return selectList(buildWarehousePositionOrderWrapper().eq("order_id", orderId));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByPickTaskId(Long pickTaskId) {
        return selectList(buildWarehousePositionOrderWrapper().eq("pick_task_id", pickTaskId));
    }

    default PageResult<ErpSalePickDeliveryItemDO> selectPageByPickTaskId(PageParam pageParam, Long pickTaskId) {
        return selectPage(pageParam, buildWarehousePositionOrderWrapper().eq("pick_task_id", pickTaskId));
    }

    default PageResult<ErpSalePickDeliveryItemDO> selectPageByOrderId(PageParam pageParam, Long orderId) {
        return selectPage(pageParam, buildWarehousePositionOrderWrapper().eq("order_id", orderId));
    }

    default PageResult<ErpSalePickDeliveryItemDO> selectPageBySaleOutId(PageParam pageParam, Long saleOutId) {
        return selectPage(pageParam, buildWarehousePositionOrderWrapper().eq("sale_out_id", saleOutId));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByTransferOutIds(Collection<Long> transferOutIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(transferOutIds)) {
            return java.util.Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .in(ErpSalePickDeliveryItemDO::getTransferOutId, transferOutIds));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByIdsForUpdate(Collection<Long> ids) {
        return selectList(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .in(ErpSalePickDeliveryItemDO::getId, ids).last("FOR UPDATE"));
    }

    default Long selectCountByPickTaskIdAndPickStatus(Long pickTaskId, Integer pickStatus) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eq(ErpSalePickDeliveryItemDO::getPickTaskId, pickTaskId)
                .eq(ErpSalePickDeliveryItemDO::getPickStatus, pickStatus));
    }

    default Long selectCountByOrderIdAndPickStatus(Long orderId, Integer pickStatus) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eq(ErpSalePickDeliveryItemDO::getOrderId, orderId)
                .eq(ErpSalePickDeliveryItemDO::getPickStatus, pickStatus));
    }

    default Long selectCountByOrderIdAndDeliveryStatus(Long orderId, Integer deliveryStatus) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eq(ErpSalePickDeliveryItemDO::getOrderId, orderId)
                .eq(ErpSalePickDeliveryItemDO::getDeliveryStatus, deliveryStatus));
    }

    static QueryWrapper<ErpSalePickDeliveryItemDO> buildWarehousePositionOrderWrapper() {
        return new QueryWrapper<ErpSalePickDeliveryItemDO>()
                .orderByAsc("CASE WHEN warehouse_position IS NULL OR warehouse_position = '' THEN 1 ELSE 0 END")
                .orderByAsc("warehouse_position")
                .orderByAsc("id");
    }

}
