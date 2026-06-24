package cn.iocoder.yudao.module.erp.dal.mysql.purchase;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Objects;

/**
 * ERP 采购入库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInMapper extends BaseMapperX<ErpPurchaseInDO> {

    default PageResult<ErpPurchaseInDO> selectPage(ErpPurchaseInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseInDO> query = new MPJLambdaWrapperX<ErpPurchaseInDO>()
                .likeIfPresent(ErpPurchaseInDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseInDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpPurchaseInDO::getInTime, reqVO.getInTime())
                .eqIfPresent(ErpPurchaseInDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseInDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseInDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchaseInDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpPurchaseInDO::getOrderNo, reqVO.getOrderNo());
        // 付款状态。为什么需要 t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_NONE)) {
            query.eq(ErpPurchaseInDO::getPaymentPrice, 0);
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_PART)) {
            query.gt(ErpPurchaseInDO::getPaymentPrice, 0).apply("t.payment_price < t.total_price");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_ALL)) {
            query.apply("t.payment_price = t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getPaymentEnable())) {
            query.eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.payment_price < t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getInvoiceEnable()) || Boolean.TRUE.equals(reqVO.getExcludeInvoiced())) {
            query.eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .and(wrapper -> wrapper.isNull(ErpPurchaseInDO::getHasInvoice)
                            .or().eq(ErpPurchaseInDO::getHasInvoice, false))
                    .apply("NOT EXISTS (SELECT 1 FROM erp_purchase_invoice_item pii "
                            + "INNER JOIN erp_purchase_invoice pi ON pi.id = pii.invoice_id "
                            + "AND pi.deleted = 0 AND pi.tenant_id = t.tenant_id "
                            + "WHERE pii.deleted = 0 AND pii.tenant_id = t.tenant_id "
                            + "AND pii.source_in_id = t.id AND pi.status = {0})",
                            ErpAuditStatus.APPROVE.getStatus());
        }
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpPurchaseInItemDO.class, ErpPurchaseInItemDO::getInId, ErpPurchaseInDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpPurchaseInItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpPurchaseInItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpPurchaseInDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpPurchaseInDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpPurchaseInDO> wrapper, ErpPurchaseInPageReqVO reqVO) {
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (direction == null) {
            wrapper.orderByDesc(ErpPurchaseInDO::getId);
            return;
        }
        String expression = getOrderExpression(reqVO.getOrderField());
        if (expression != null) {
            wrapper.last("ORDER BY " + expression + " " + direction + ", t.id DESC");
            return;
        }
        SFunction<ErpPurchaseInDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpPurchaseInDO::getId);
            return;
        }
        if ("ASC".equals(direction)) {
            wrapper.orderByAsc(orderColumn);
        } else {
            wrapper.orderByDesc(orderColumn);
        }
    }

    static String normalizeOrderDirection(String orderDirection) {
        if (orderDirection == null) {
            return null;
        }
        if ("asc".equalsIgnoreCase(orderDirection.trim())) {
            return "ASC";
        }
        if ("desc".equalsIgnoreCase(orderDirection.trim())) {
            return "DESC";
        }
        return null;
    }

    static String getOrderExpression(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "paymentStatus":
                return "(CASE WHEN COALESCE(t.payment_price, 0) = 0 THEN 0 "
                        + "WHEN t.payment_price = t.total_price THEN 2 ELSE 1 END)";
            case "itemCount":
                return "(SELECT COUNT(1) FROM erp_purchase_in_items pii "
                        + "WHERE pii.deleted = 0 AND pii.tenant_id = t.tenant_id AND pii.in_id = t.id)";
            default:
                return null;
        }
    }

    static SFunction<ErpPurchaseInDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpPurchaseInDO::getNo;
            case "factoryOrderNo":
                return ErpPurchaseInDO::getFactoryOrderNo;
            case "createTime":
                return ErpPurchaseInDO::getCreateTime;
            case "status":
                return ErpPurchaseInDO::getStatus;
            case "supplierId":
            case "supplierName":
                return ErpPurchaseInDO::getSupplierId;
            case "totalProductPrice":
                return ErpPurchaseInDO::getTotalProductPrice;
            case "discountPrice":
                return ErpPurchaseInDO::getDiscountPrice;
            case "totalPrice":
                return ErpPurchaseInDO::getTotalPrice;
            case "totalCount":
                return ErpPurchaseInDO::getTotalCount;
            case "creator":
            case "creatorName":
                return ErpPurchaseInDO::getCreator;
            case "purchaser":
            case "purchaserName":
                return ErpPurchaseInDO::getPurchaser;
            case "deptId":
            case "deptName":
                return ErpPurchaseInDO::getDeptId;
            case "orderMethod":
                return ErpPurchaseInDO::getOrderMethod;
            case "settleMethod":
                return ErpPurchaseInDO::getSettleMethod;
            case "invoiceType":
                return ErpPurchaseInDO::getInvoiceType;
            case "orderNo":
                return ErpPurchaseInDO::getOrderNo;
            case "remark":
                return ErpPurchaseInDO::getRemark;
            case "transportMethod":
                return ErpPurchaseInDO::getTransportMethod;
            case "freightType1":
                return ErpPurchaseInDO::getFreightType1;
            case "totalFreight1":
                return ErpPurchaseInDO::getTotalFreight1;
            case "freightType2":
                return ErpPurchaseInDO::getFreightType2;
            case "totalFreight2":
                return ErpPurchaseInDO::getTotalFreight2;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPurchaseInDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getId, id).eq(ErpPurchaseInDO::getStatus, status));
    }

    default ErpPurchaseInDO selectByNo(String no) {
        return selectOne(ErpPurchaseInDO::getNo, no);
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPurchaseInDO::getSupplierId, supplierId);
    }

    default String selectFirstNoBySupplierId(Long supplierId) {
        ErpPurchaseInDO in = selectOne(new MPJLambdaWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getSupplierId, supplierId)
                .orderByDesc(ErpPurchaseInDO::getId)
                .last("LIMIT 1"));
        return in == null ? null : in.getNo();
    }

    default List<ErpPurchaseInDO> selectListByOrderId(Long orderId) {
        return selectList(ErpPurchaseInDO::getOrderId, orderId);
    }

    default List<ErpPurchaseInDO> selectListByOrderIdAndStatus(Long orderId, Integer status) {
        return selectList(new MPJLambdaWrapperX<ErpPurchaseInDO>()
                .eq(ErpPurchaseInDO::getOrderId, orderId)
                .eq(ErpPurchaseInDO::getStatus, status));
    }

}
