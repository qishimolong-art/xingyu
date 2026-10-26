package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickQuantityAggregateDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSalePickDeliveryItemMapper extends BaseMapperX<ErpSalePickDeliveryItemDO> {

    default List<ErpSalePickDeliveryItemDO> selectDeliveryUsersBySaleOutId(Long saleOutId) {
        return selectList(new QueryWrapper<ErpSalePickDeliveryItemDO>()
                .select("delivery_user_id").eq("sale_out_id", saleOutId)
                .isNotNull("delivery_user_id").groupBy("delivery_user_id").orderByAsc("MIN(id)"));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByOrderId(Long orderId) {
        return selectList(buildWarehousePositionOrderWrapper().eq("order_id", orderId));
    }

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eq(ErpSalePickDeliveryItemDO::getOrderId, orderId));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByPickTaskId(Long pickTaskId) {
        return selectList(buildWarehousePositionOrderWrapper().eq("pick_task_id", pickTaskId));
    }

    default List<ErpSalePickDeliveryItemDO> selectListByPickTaskIds(Collection<Long> pickTaskIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(pickTaskIds)) {
            return java.util.Collections.emptyList();
        }
        return selectList(buildWarehousePositionOrderWrapper().in("pick_task_id", pickTaskIds));
    }

    default PageResult<ErpSalePickDeliveryItemDO> selectPageByPickTaskId(PageParam pageParam, Long pickTaskId) {
        return selectPage(pageParam, buildWarehousePositionOrderWrapper().eq("pick_task_id", pickTaskId));
    }

    default PageResult<ErpSalePickDeliveryItemDO> selectPageByOrderId(PageParam pageParam, Long orderId) {
        return selectPage(pageParam, buildWarehousePositionOrderWrapper().eq("order_id", orderId));
    }

    @Select({
            "SELECT pick_task_id AS pickTaskId, COUNT(1) AS totalItemCount,",
            "COALESCE(SUM(`count`), 0) AS totalQuantity,",
            "COALESCE(SUM(CASE WHEN picked_count IS NULL AND pick_status = 30",
            "THEN `count` ELSE COALESCE(picked_count, 0) END), 0) AS pickedQuantity",
            "FROM erp_sale_pick_delivery_item",
            "WHERE order_id = #{orderId} AND deleted = 0",
            "GROUP BY pick_task_id"
    })
    List<ErpSalePickQuantityAggregateDO> selectPickQuantityAggregatesByOrderId(
            @Param("orderId") Long orderId);

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

    default boolean hasPickProgress(Long taskId, Long orderId) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eqIfPresent(ErpSalePickDeliveryItemDO::getPickTaskId, taskId)
                .eqIfPresent(ErpSalePickDeliveryItemDO::getOrderId, orderId)
                .gt(ErpSalePickDeliveryItemDO::getPickedCount, 0)) > 0;
    }

    default boolean hasDeliveryProgress(Long orderId) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliveryItemDO>()
                .eq(ErpSalePickDeliveryItemDO::getOrderId, orderId)
                .and(wrapper -> wrapper.gt(ErpSalePickDeliveryItemDO::getDeliveredCount, 0)
                        .or().in(ErpSalePickDeliveryItemDO::getDeliveryStatus,
                                ErpSaleDeliveryStatusEnum.PARTIAL.getStatus(),
                                ErpSaleDeliveryStatusEnum.DONE.getStatus()))) > 0;
    }

    default Long selectCountByOrderIdWithAvailableDelivery(Long orderId) {
        return selectCount(new QueryWrapper<ErpSalePickDeliveryItemDO>()
                .eq("order_id", orderId)
                .apply("(CASE WHEN picked_count IS NULL AND pick_status = 30 THEN `count` ELSE COALESCE(picked_count, 0) END) "
                        + "> (CASE WHEN delivered_count IS NULL AND delivery_status = 30 THEN `count` ELSE COALESCE(delivered_count, 0) END)"));
    }

    default boolean hasAvailableDelivery(Long orderId) {
        return selectCountByOrderIdWithAvailableDelivery(orderId) > 0;
    }

    static QueryWrapper<ErpSalePickDeliveryItemDO> buildWarehousePositionOrderWrapper() {
        return new QueryWrapper<ErpSalePickDeliveryItemDO>()
                .orderByAsc("CASE WHEN warehouse_position IS NULL OR warehouse_position = '' THEN 1 ELSE 0 END")
                .orderByAsc("warehouse_position")
                .orderByAsc("id");
    }

}
