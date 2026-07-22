package cn.iocoder.yudao.module.erp.dal.mysql.sale;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Objects;

/**
 * ERP 销售退�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleReturnMapper extends BaseMapperX<ErpSaleReturnDO> {

    default PageResult<ErpSaleReturnDO> selectPage(ErpSaleReturnPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSaleReturnDO> query = new MPJLambdaWrapperX<ErpSaleReturnDO>()
                .likeIfPresent(ErpSaleReturnDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleReturnDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleReturnDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleReturnDO::getReturnTime, reqVO.getReturnTime())
                .eqIfPresent(ErpSaleReturnDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleReturnDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpSaleReturnDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpSaleReturnDO::getHandler, reqVO.getHandler())
                .eqIfPresent(ErpSaleReturnDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpSaleReturnDO::getOrderNo, reqVO.getOrderNo())
                .inIfPresent(ErpSaleReturnDO::getId, reqVO.getIds());
        // 退款状态。为什么需�?t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getRefundStatus(), ErpSaleReturnPageReqVO.REFUND_STATUS_NONE)) {
            query.eq(ErpSaleReturnDO::getRefundPrice, 0);
        } else if (Objects.equals(reqVO.getRefundStatus(), ErpSaleReturnPageReqVO.REFUND_STATUS_PART)) {
            query.gt(ErpSaleReturnDO::getRefundPrice, 0).apply("t.refund_price < t.total_price");
        } else if (Objects.equals(reqVO.getRefundStatus(), ErpSaleReturnPageReqVO.REFUND_STATUS_ALL)) {
            query.apply("t.refund_price = t.total_price");
        }
        if (Boolean.TRUE.equals(reqVO.getRefundEnable())) {
            query.eq(ErpSaleOutDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.refund_price < t.total_price");
        }
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null) {
            query.leftJoin(ErpSaleReturnItemDO.class, ErpSaleReturnItemDO::getReturnId, ErpSaleReturnDO::getId)
                    .eq(reqVO.getWarehouseId() != null, ErpSaleReturnItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpSaleReturnItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpSaleReturnDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpSaleReturnDO::getNo, ErpSaleReturnDO::getOrderNo,
                ErpSaleReturnDO::getSourceOutNo, ErpSaleReturnDO::getRemark,
                ErpSaleReturnDO::getPriority, ErpSaleReturnDO::getInvoiceType,
                ErpSaleReturnDO::getBillNo, ErpSaleReturnDO::getDeliveryMethod,
                ErpSaleReturnDO::getFreightType, ErpSaleReturnDO::getSettleMethod,
                ErpSaleReturnDO::getLogisticsCompany, ErpSaleReturnDO::getVehicleNo,
                ErpSaleReturnDO::getBranchStore, ErpSaleReturnDO::getPurchaseArea,
                ErpSaleReturnDO::getBusinessType, ErpSaleReturnDO::getOrderMethod);
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpSaleReturnDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpSaleReturnDO> wrapper, ErpSaleReturnPageReqVO reqVO) {
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (direction == null) {
            wrapper.orderByDesc(ErpSaleReturnDO::getId);
            return;
        }
        String expression = getOrderExpression(reqVO.getOrderField());
        if (expression != null) {
            wrapper.last("ORDER BY " + expression + " " + direction + ", t.id DESC");
            return;
        }
        SFunction<ErpSaleReturnDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSaleReturnDO::getId);
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
            case "refundStatus":
            case "settlementStatus":
            case "settlementStatusText":
                return "(CASE WHEN COALESCE(t.refund_price, 0) = 0 THEN 0 "
                        + "WHEN t.refund_price = t.total_price THEN 2 ELSE 1 END)";
            default:
                return null;
        }
    }

    static SFunction<ErpSaleReturnDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleReturnDO::getNo;
            case "returnTime":
                return ErpSaleReturnDO::getReturnTime;
            case "status":
                return ErpSaleReturnDO::getStatus;
            case "customerId":
            case "customerName":
                return ErpSaleReturnDO::getCustomerId;
            case "returnMode":
                return ErpSaleReturnDO::getReturnMode;
            case "orderMethod":
                return ErpSaleReturnDO::getOrderMethod;
            case "totalCount":
                return ErpSaleReturnDO::getTotalCount;
            case "settleMethod":
                return ErpSaleReturnDO::getSettleMethod;
            case "deliveryMethod":
                return ErpSaleReturnDO::getDeliveryMethod;
            case "totalProductPrice":
                return ErpSaleReturnDO::getTotalProductPrice;
            case "discountPrice":
                return ErpSaleReturnDO::getDiscountPrice;
            case "totalPrice":
                return ErpSaleReturnDO::getTotalPrice;
            case "invoiceType":
                return ErpSaleReturnDO::getInvoiceType;
            case "billNo":
                return ErpSaleReturnDO::getBillNo;
            case "logisticsCompany":
                return ErpSaleReturnDO::getLogisticsCompany;
            case "deptId":
            case "deptName":
                return ErpSaleReturnDO::getDeptId;
            case "saleUserId":
            case "saleUserName":
                return ErpSaleReturnDO::getSaleUserId;
            case "creator":
            case "creatorName":
                return ErpSaleReturnDO::getCreator;
            case "handler":
            case "handlerName":
                return ErpSaleReturnDO::getHandler;
            case "priority":
                return ErpSaleReturnDO::getPriority;
            case "remark":
                return ErpSaleReturnDO::getRemark;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleReturnDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleReturnDO>()
                .eq(ErpSaleReturnDO::getId, id).eq(ErpSaleReturnDO::getStatus, status));
    }

    default ErpSaleReturnDO selectByNo(String no) {
        return selectOne(ErpSaleReturnDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleReturnDO::getCustomerId, customerId);
    }

    default List<ErpSaleReturnDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSaleReturnDO::getOrderId, orderId);
    }

    default List<ErpSaleReturnDO> selectListBySourceOutId(Long sourceOutId) {
        return selectList(ErpSaleReturnDO::getSourceOutId, sourceOutId);
    }

}
