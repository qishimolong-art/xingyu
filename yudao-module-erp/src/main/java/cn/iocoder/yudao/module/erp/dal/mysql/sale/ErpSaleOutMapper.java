package cn.iocoder.yudao.module.erp.dal.mysql.sale;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ERP 销售出库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOutMapper extends BaseMapperX<ErpSaleOutDO> {

    default PageResult<ErpSaleOutDO> selectPage(ErpSaleOutPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSaleOutDO> query = new MPJLambdaWrapperX<ErpSaleOutDO>()
                .likeIfPresent(ErpSaleOutDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleOutDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleOutDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleOutDO::getOutTime, reqVO.getOutTime())
                .eqIfPresent(ErpSaleOutDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleOutDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpSaleOutDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpSaleOutDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpSaleOutDO::getOrderNo, reqVO.getOrderNo())
                .eqIfPresent(ErpSaleOutDO::getSourceType, reqVO.getSourceType())
                .likeIfPresent(ErpSaleOutDO::getSourceNo, reqVO.getSourceNo())
                .eqIfPresent(ErpSaleOutDO::getSaleUserId, reqVO.getSaleUserId())
                .inIfPresent(ErpSaleOutDO::getId, reqVO.getIds());
        // 收款状态。为什么需要 t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_NONE)) {
            query.eq(ErpSaleOutDO::getReceiptPrice, 0);
        } else if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_PART)) {
            query.gt(ErpSaleOutDO::getReceiptPrice, 0).apply("t.receipt_price < t.total_price");
        } else if (Objects.equals(reqVO.getReceiptStatus(), ErpSaleOutPageReqVO.RECEIPT_STATUS_ALL)) {
            query.apply("t.receipt_price = t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getReceiptEnable())) {
            query.eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.receipt_price < t.total_price");
        }
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpSaleOutItemDO.class, ErpSaleOutItemDO::getOutId, ErpSaleOutDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpSaleOutItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpSaleOutItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpSaleOutDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpSaleOutDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpSaleOutDO> wrapper, ErpSaleOutPageReqVO reqVO) {
        SFunction<ErpSaleOutDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSaleOutDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
            return;
        }
        wrapper.orderByDesc(ErpSaleOutDO::getId);
    }

    static SFunction<ErpSaleOutDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleOutDO::getNo;
            case "settleStatus":
                return ErpSaleOutDO::getSettleStatus;
            case "outTime":
                return ErpSaleOutDO::getOutTime;
            case "status":
                return ErpSaleOutDO::getStatus;
            case "customerId":
            case "customerName":
                return ErpSaleOutDO::getCustomerId;
            case "deliveryMethod":
                return ErpSaleOutDO::getDeliveryMethod;
            case "settleMethod":
                return ErpSaleOutDO::getSettleMethod;
            case "totalProductPrice":
                return ErpSaleOutDO::getTotalProductPrice;
            case "reductionAmount":
                return ErpSaleOutDO::getReductionAmount;
            case "afterReductionAmount":
                return ErpSaleOutDO::getAfterReductionAmount;
            case "billAmount":
                return ErpSaleOutDO::getBillAmount;
            case "freight":
                return ErpSaleOutDO::getFreight;
            case "creator":
            case "creatorName":
                return ErpSaleOutDO::getCreator;
            case "auditorId":
            case "auditorName":
                return ErpSaleOutDO::getAuditorId;
            case "cancelCount":
                return ErpSaleOutDO::getCancelCount;
            case "cancelAmount":
                return ErpSaleOutDO::getCancelAmount;
            case "afterCancelAmount":
                return ErpSaleOutDO::getAfterCancelAmount;
            case "shipper":
                return ErpSaleOutDO::getShipper;
            case "deliveryNo":
                return ErpSaleOutDO::getDeliveryNo;
            case "logisticsNo":
                return ErpSaleOutDO::getLogisticsNo;
            case "logisticsCompany":
                return ErpSaleOutDO::getLogisticsCompany;
            case "senderName":
                return ErpSaleOutDO::getSenderName;
            case "receiverName":
                return ErpSaleOutDO::getReceiverName;
            case "receiverPhone":
                return ErpSaleOutDO::getReceiverPhone;
            case "insuranceCompany":
                return ErpSaleOutDO::getInsuranceCompany;
            case "thirdPartyNo":
                return ErpSaleOutDO::getThirdPartyNo;
            case "thirdPartyUpstreamNo":
                return ErpSaleOutDO::getThirdPartyUpstreamNo;
            case "remark":
                return ErpSaleOutDO::getRemark;
            case "internalNote":
                return ErpSaleOutDO::getInternalNote;
            case "saleUserId":
            case "saleUserName":
                return ErpSaleOutDO::getSaleUserId;
            case "deptId":
            case "deptName":
                return ErpSaleOutDO::getDeptId;
            case "totalWeight":
                return ErpSaleOutDO::getTotalWeight;
            case "approveTime":
                return ErpSaleOutDO::getApproveTime;
            case "priority":
                return ErpSaleOutDO::getPriority;
            case "signStatus":
                return ErpSaleOutDO::getSignStatus;
            case "billNo":
                return ErpSaleOutDO::getBillNo;
            case "sourceNo":
                return ErpSaleOutDO::getSourceNo;
            case "sourceCreateTime":
                return ErpSaleOutDO::getSourceCreateTime;
            case "printTime":
                return ErpSaleOutDO::getPrintTime;
            case "confirmTime":
                return ErpSaleOutDO::getConfirmTime;
            case "vin":
                return ErpSaleOutDO::getVin;
            case "extraFee":
                return ErpSaleOutDO::getExtraFee;
            case "orderType":
                return ErpSaleOutDO::getOrderType;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleOutDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getId, id).eq(ErpSaleOutDO::getStatus, status));
    }

    default ErpSaleOutDO selectByNo(String no) {
        return selectOne(ErpSaleOutDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleOutDO::getCustomerId, customerId);
    }

    default List<ErpSaleOutDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSaleOutDO::getOrderId, orderId);
    }

    default ErpSaleOutDO selectBySourceTypeAndSourceId(Integer sourceType, Long sourceId) {
        return selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getSourceType, sourceType)
                .eq(ErpSaleOutDO::getSourceId, sourceId)
                .last("LIMIT 1"));
    }

    default List<ErpSaleOutDO> selectListBySourceTypeAndSourceIds(Integer sourceType, Collection<Long> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ErpSaleOutDO>()
                .eq(ErpSaleOutDO::getSourceType, sourceType)
                .in(ErpSaleOutDO::getSourceId, sourceIds)
                .orderByDesc(ErpSaleOutDO::getId));
    }

    @org.apache.ibatis.annotations.Select({
        "<script>",
        "SELECT c.id AS customerId,",
        "       so.lastSaleTime AS lastSaleTime,",
        "       IFNULL(so.totalSaleAmount, 0) AS totalSaleAmount,",
        "       IFNULL(ro.otherReceivableAmount, 0) AS otherReceivableAmount,",
        "       IFNULL(so.receivableBalance, 0) + IFNULL(ro.otherReceivableAmount, 0) AS receivableBalance",
        "  FROM erp_customer c",
        "  LEFT JOIN (",
        "       SELECT customer_id, MAX(out_time) AS lastSaleTime, SUM(total_price) AS totalSaleAmount,",
        "              SUM(total_price - IFNULL(receipt_price, 0)) AS receivableBalance",
        "         FROM erp_sale_out",
        "        WHERE deleted = 0 AND status = 20",
        "          AND customer_id IN",
        "          <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "        GROUP BY customer_id",
        "  ) so ON so.customer_id = c.id",
        "  LEFT JOIN (",
        "       SELECT customer_id, SUM(receivable_amount) AS otherReceivableAmount",
        "         FROM erp_receivable_other",
        "        WHERE deleted = 0 AND status = 20",
        "          AND customer_id IN",
        "          <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "        GROUP BY customer_id",
        "  ) ro ON ro.customer_id = c.id",
        " WHERE c.deleted = 0",
        "   AND c.id IN",
        "   <foreach collection='customerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
        "</script>"
    })
    java.util.List<cn.iocoder.yudao.module.erp.service.sale.bo.ErpCustomerSaleStatsBO> selectSaleStatsByCustomerIds(
            @org.apache.ibatis.annotations.Param("customerIds") java.util.Collection<Long> customerIds);

}
