package cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSaleDeliveryPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.ErpSalePickDeliveryPickTaskDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
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

    default int deleteByOrderId(Long orderId) {
        return delete(new LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO>()
                .eq(ErpSalePickDeliveryPickTaskDO::getOrderId, orderId));
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

    default PageResult<ErpSalePickDeliveryPickTaskDO> selectDeliveryPage(ErpSaleDeliveryPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO> wrapper =
                new LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO>()
                        .inIfPresent(ErpSalePickDeliveryPickTaskDO::getId, reqVO.getIds())
                        .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getCustomerId, reqVO.getCustomerId())
                        .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getWarehouseId, reqVO.getWarehouseId())
                        .eqIfPresent(ErpSalePickDeliveryPickTaskDO::getStatus, reqVO.getPickStatus())
                        .betweenIfPresent(ErpSalePickDeliveryPickTaskDO::getCreateTime, reqVO.getCreateTime())
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
        appendDeliveryStatus(wrapper, reqVO.getDeliveryStatus());
        if (reqVO.getDeliveryCompleteTime() != null && reqVO.getDeliveryCompleteTime().length == 2) {
            wrapper.apply("(SELECT MAX(i.delivery_time) FROM erp_sale_pick_delivery_item i "
                            + "WHERE i.pick_task_id = erp_sale_pick_delivery_pick_task.id "
                            + "AND i.tenant_id = erp_sale_pick_delivery_pick_task.tenant_id AND i.deleted = 0) "
                            + "BETWEEN {0} AND {1}",
                    reqVO.getDeliveryCompleteTime()[0], reqVO.getDeliveryCompleteTime()[1]);
        }
        return selectPage(reqVO, wrapper);
    }

    static void appendDeliveryStatus(LambdaQueryWrapperX<ErpSalePickDeliveryPickTaskDO> wrapper,
                                     Integer deliveryStatus) {
        if (deliveryStatus == null) {
            return;
        }
        String itemScope = "i.pick_task_id = erp_sale_pick_delivery_pick_task.id "
                + "AND i.tenant_id = erp_sale_pick_delivery_pick_task.tenant_id AND i.deleted = 0";
        String delivered = "(CASE WHEN i.delivered_count IS NULL AND i.delivery_status = 30 "
                + "THEN i.`count` ELSE COALESCE(i.delivered_count, 0) END)";
        String picked = "(CASE WHEN i.picked_count IS NULL AND i.pick_status = 30 "
                + "THEN i.`count` ELSE COALESCE(i.picked_count, 0) END)";
        String hasIncomplete = "EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item i WHERE "
                + itemScope + " AND " + delivered + " < i.`count`)";
        String hasProgress = "EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item i WHERE "
                + itemScope + " AND " + delivered + " > 0)";
        String hasAvailable = "EXISTS (SELECT 1 FROM erp_sale_pick_delivery_item i WHERE "
                + itemScope + " AND " + picked + " > " + delivered + ")";
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)) {
            wrapper.apply("NOT " + hasIncomplete);
        } else if (ErpSaleDeliveryStatusEnum.PARTIAL.getStatus().equals(deliveryStatus)) {
            wrapper.apply(hasProgress).apply(hasIncomplete);
        } else if (ErpSaleDeliveryStatusEnum.WAITING.getStatus().equals(deliveryStatus)) {
            wrapper.apply("NOT " + hasProgress).apply(hasAvailable);
        } else if (ErpSaleDeliveryStatusEnum.NOT_READY.getStatus().equals(deliveryStatus)) {
            wrapper.apply("NOT " + hasProgress).apply("NOT " + hasAvailable);
        } else {
            wrapper.apply("1 = 0");
        }
    }

}
