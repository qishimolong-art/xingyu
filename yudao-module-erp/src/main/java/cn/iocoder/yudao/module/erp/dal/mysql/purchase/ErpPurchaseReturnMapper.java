package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Objects;

/**
 * ERP 采购退�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseReturnMapper extends BaseMapperX<ErpPurchaseReturnDO> {

    default PageResult<ErpPurchaseReturnDO> selectPage(ErpPurchaseReturnPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseReturnDO> query = new MPJLambdaWrapperX<ErpPurchaseReturnDO>()
                .likeIfPresent(ErpPurchaseReturnDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseReturnDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseReturnDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpPurchaseReturnDO::getReturnTime, reqVO.getReturnTime())
                .eqIfPresent(ErpPurchaseReturnDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseReturnDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseReturnDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchaseReturnDO::getPurchaser, reqVO.getPurchaser())
                .eqIfPresent(ErpPurchaseReturnDO::getHandler, reqVO.getHandler())
                .eqIfPresent(ErpPurchaseReturnDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpPurchaseReturnDO::getOrderNo, reqVO.getOrderNo());
        // 退款状态。为什么需�?t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getRefundStatus(), ErpPurchaseReturnPageReqVO.REFUND_STATUS_NONE)) {
            query.eq(ErpPurchaseReturnDO::getRefundPrice, 0);
        } else if (Objects.equals(reqVO.getRefundStatus(), ErpPurchaseReturnPageReqVO.REFUND_STATUS_PART)) {
            query.gt(ErpPurchaseReturnDO::getRefundPrice, 0).apply("t.refund_price < t.total_price");
        } else if (Objects.equals(reqVO.getRefundStatus(), ErpPurchaseReturnPageReqVO.REFUND_STATUS_ALL)) {
            query.apply("t.refund_price = t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getRefundEnable())) {
            query.eq(ErpPurchaseReturnDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.refund_price < t.total_price");
        }
        boolean hasSourceInNo = reqVO.getSourceInNo() != null && !reqVO.getSourceInNo().trim().isEmpty();
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null || hasSourceInNo) {
            query.leftJoin(ErpPurchaseReturnItemDO.class, ErpPurchaseReturnItemDO::getReturnId, ErpPurchaseReturnDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpPurchaseReturnItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpPurchaseReturnItemDO::getProductId, reqVO.getProductId())
                    .likeIfPresent(ErpPurchaseReturnItemDO::getSourceInNo, reqVO.getSourceInNo())
                    .groupBy(ErpPurchaseReturnDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpPurchaseReturnDO::getNo, ErpPurchaseReturnDO::getOrderNo,
                ErpPurchaseReturnDO::getRemark, ErpPurchaseReturnDO::getReturnType,
                ErpPurchaseReturnDO::getPurchaser, ErpPurchaseReturnDO::getInvoiceType,
                ErpPurchaseReturnDO::getTransportMethod, ErpPurchaseReturnDO::getSettleMethod,
                ErpPurchaseReturnDO::getLogisticsCompany, ErpPurchaseReturnDO::getDocSource,
                ErpPurchaseReturnDO::getFactoryOrderNo, ErpPurchaseReturnDO::getMaker,
                ErpPurchaseReturnDO::getDept, ErpPurchaseReturnDO::getShippingArea,
                ErpPurchaseReturnDO::getWarehouseType, ErpPurchaseReturnDO::getFreightType,
                ErpPurchaseReturnDO::getLogisticsNo, ErpPurchaseReturnDO::getPriority,
                ErpPurchaseReturnDO::getOrderMethod);
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpPurchaseReturnDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpPurchaseReturnDO> wrapper, ErpPurchaseReturnPageReqVO reqVO) {
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (direction == null) {
            wrapper.orderByDesc(ErpPurchaseReturnDO::getId);
            return;
        }
        String expression = getOrderExpression(reqVO.getOrderField());
        if (expression != null) {
            wrapper.last("ORDER BY " + expression + " " + direction + ", t.id DESC");
            return;
        }
        SFunction<ErpPurchaseReturnDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpPurchaseReturnDO::getId);
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
            case "settlementStatus":
                return "(CASE WHEN t.status = " + ErpAuditStatus.APPROVE.getStatus() + " THEN 0 ELSE 1 END), "
                        + "(CASE WHEN t.status <> " + ErpAuditStatus.APPROVE.getStatus() + " THEN 3 "
                        + "WHEN COALESCE(t.refund_price, 0) = 0 THEN 0 "
                        + "WHEN t.refund_price = t.total_price THEN 2 ELSE 1 END)";
            case "itemCount":
                return "(SELECT COUNT(1) FROM erp_purchase_return_items pri "
                        + "WHERE pri.deleted = 0 AND pri.tenant_id = t.tenant_id AND pri.return_id = t.id)";
            default:
                return null;
        }
    }

    static SFunction<ErpPurchaseReturnDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpPurchaseReturnDO::getNo;
            case "returnTime":
                return ErpPurchaseReturnDO::getReturnTime;
            case "supplierId":
            case "supplierName":
                return ErpPurchaseReturnDO::getSupplierId;
            case "totalCount":
                return ErpPurchaseReturnDO::getTotalCount;
            case "totalPrice":
                return ErpPurchaseReturnDO::getTotalPrice;
            case "purchaser":
                return ErpPurchaseReturnDO::getPurchaser;
            case "status":
                return ErpPurchaseReturnDO::getStatus;
            case "deptId":
            case "deptName":
                return ErpPurchaseReturnDO::getDeptId;
            case "remark":
                return ErpPurchaseReturnDO::getRemark;
            case "creator":
            case "creatorName":
                return ErpPurchaseReturnDO::getCreator;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPurchaseReturnDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getId, id).eq(ErpPurchaseReturnDO::getStatus, status));
    }

    default ErpPurchaseReturnDO selectByNo(String no) {
        return selectOne(ErpPurchaseReturnDO::getNo, no);
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPurchaseReturnDO::getSupplierId, supplierId);
    }

    default String selectFirstNoBySupplierId(Long supplierId) {
        ErpPurchaseReturnDO purchaseReturn = selectOne(new MPJLambdaWrapperX<ErpPurchaseReturnDO>()
                .eq(ErpPurchaseReturnDO::getSupplierId, supplierId)
                .orderByDesc(ErpPurchaseReturnDO::getId)
                .last("LIMIT 1"));
        return purchaseReturn == null ? null : purchaseReturn.getNo();
    }

    default List<ErpPurchaseReturnDO> selectListByOrderId(Long orderId) {
        return selectList(ErpPurchaseReturnDO::getOrderId, orderId);
    }

}
