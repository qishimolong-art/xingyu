package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.*;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliverySubmitItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;

@Mapper
public interface ErpSalePickDeliverySubmitItemMapper extends BaseMapperX<ErpSalePickDeliverySubmitItemDO> {
    default PageResult<ErpSalePickDeliverySubmitItemDO> selectPageBySubmitId(PageParam page, Long submitId) {
        return selectPage(page, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .eq(ErpSalePickDeliverySubmitItemDO::getSubmitId, submitId)
                .orderByAsc(ErpSalePickDeliverySubmitItemDO::getId));
    }

    default PageResult<ErpSalePickDeliverySubmitItemDO> selectPageBySubmitIds(
            PageParam page, Collection<Long> submitIds) {
        return selectPage(page, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .in(ErpSalePickDeliverySubmitItemDO::getSubmitId, submitIds)
                .orderByDesc(ErpSalePickDeliverySubmitItemDO::getSubmitId)
                .orderByAsc(ErpSalePickDeliverySubmitItemDO::getId));
    }

    default PageResult<ErpSalePickDeliverySubmitItemDO> selectPageByOrderIdAndType(
            PageParam page, Long orderId, Integer type) {
        return selectPage(page, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .apply("EXISTS (SELECT 1 FROM erp_sale_pick_delivery_submit s "
                                + "WHERE s.id = erp_sale_pick_delivery_submit_item.submit_id "
                                + "AND s.tenant_id = erp_sale_pick_delivery_submit_item.tenant_id "
                                + "AND s.deleted = 0 AND s.order_id = {0} AND s.type = {1} "
                                + "AND s.quantity_details = 1)", orderId, type)
                .orderByDesc(ErpSalePickDeliverySubmitItemDO::getSubmitId)
                .orderByAsc(ErpSalePickDeliverySubmitItemDO::getId));
    }

    default PageResult<ErpSalePickDeliverySubmitItemDO> selectPageBySubmitIdAndPickTaskId(
            PageParam page, Long submitId, Long taskId) {
        return selectPage(page, new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .eq(ErpSalePickDeliverySubmitItemDO::getSubmitId, submitId)
                .apply("EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item i "
                                + "WHERE i.id = erp_sale_pick_delivery_submit_item.item_id "
                                + "AND i.tenant_id = erp_sale_pick_delivery_submit_item.tenant_id "
                                + "AND i.deleted = 0 AND i.pick_task_id = {0})", taskId)
                .orderByAsc(ErpSalePickDeliverySubmitItemDO::getId));
    }

    default long selectCountBySubmitIdAndPickTaskId(Long submitId, Long taskId) {
        return selectCount(new LambdaQueryWrapperX<ErpSalePickDeliverySubmitItemDO>()
                .eq(ErpSalePickDeliverySubmitItemDO::getSubmitId, submitId)
                .apply("EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item i "
                                + "WHERE i.id = erp_sale_pick_delivery_submit_item.item_id "
                                + "AND i.tenant_id = erp_sale_pick_delivery_submit_item.tenant_id "
                                + "AND i.deleted = 0 AND i.pick_task_id = {0})", taskId));
    }
}
