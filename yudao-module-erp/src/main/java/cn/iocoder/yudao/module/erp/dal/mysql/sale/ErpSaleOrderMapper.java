package cn.iocoder.yudao.module.erp.dal.mysql.sale;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Objects;

/**
 * ERP 销售订�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOrderMapper extends BaseMapperX<ErpSaleOrderDO> {

    default PageResult<ErpSaleOrderDO> selectPage(ErpSaleOrderPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSaleOrderDO> query = new MPJLambdaWrapperX<ErpSaleOrderDO>()
                .likeIfPresent(ErpSaleOrderDO::getNo, normalizeLikeValue(reqVO.getNo()))
                .eqIfPresent(ErpSaleOrderDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleOrderDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleOrderDO::getOrderTime, reqVO.getOrderTime())
                .eqIfPresent(ErpSaleOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleOrderDO::getRemark, normalizeLikeValue(reqVO.getRemark()))
                .eqIfPresent(ErpSaleOrderDO::getCreator, reqVO.getCreator());
        // 入库状态。为什么需�?t. 的原因，是因为联表查询时，需要指定表名，不然会报 out_count 错误
        if (Objects.equals(reqVO.getOutStatus(), ErpSaleOrderPageReqVO.OUT_STATUS_NONE)) {
            query.eq(ErpSaleOrderDO::getOutCount, 0);
        } else if (Objects.equals(reqVO.getOutStatus(), ErpSaleOrderPageReqVO.OUT_STATUS_PART)) {
            query.gt(ErpSaleOrderDO::getOutCount, 0).apply("t.out_count < t.total_count");
        } else if (Objects.equals(reqVO.getOutStatus(), ErpSaleOrderPageReqVO.OUT_STATUS_ALL)) {
            query.apply("t.out_count = t.total_count");
        }
        // Return status.
        if (Objects.equals(reqVO.getReturnStatus(), ErpSaleOrderPageReqVO.RETURN_STATUS_NONE)) {
            query.eq(ErpSaleOrderDO::getReturnCount, 0);
        } else if (Objects.equals(reqVO.getReturnStatus(), ErpSaleOrderPageReqVO.RETURN_STATUS_PART)) {
            query.gt(ErpSaleOrderDO::getReturnCount, 0).apply("t.return_count < t.total_count");
        } else if (Objects.equals(reqVO.getReturnStatus(), ErpSaleOrderPageReqVO.RETURN_STATUS_ALL)) {
            query.apply("t.return_count = t.total_count");
        }
        // Sale outbound enabled.
        if (Boolean.TRUE.equals(reqVO.getOutEnable())) {
            query.eq(ErpSaleOrderDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.out_count < t.total_count");
        }
        // Sale return enabled.
        if (Boolean.TRUE.equals(reqVO.getReturnEnable())) {
            query.eq(ErpSaleOrderDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.return_count < t.out_count");
        }
        if (reqVO.getProductId() != null) {
            query.leftJoin(ErpSaleOrderItemDO.class, ErpSaleOrderItemDO::getOrderId, ErpSaleOrderDO::getId)
                    .eq(reqVO.getProductId() != null, ErpSaleOrderItemDO::getProductId, reqVO.getProductId())
                    .groupBy(ErpSaleOrderDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpSaleOrderDO::getNo, ErpSaleOrderDO::getRemark);
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpSaleOrderDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpSaleOrderDO> wrapper, ErpSaleOrderPageReqVO reqVO) {
        SFunction<ErpSaleOrderDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (orderColumn == null || direction == null) {
            wrapper.orderByDesc(ErpSaleOrderDO::getId);
            return;
        }
        if ("ASC".equals(direction)) {
            wrapper.orderByAsc(orderColumn);
        } else {
            wrapper.orderByDesc(orderColumn);
        }
        wrapper.orderByDesc(ErpSaleOrderDO::getId);
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

    static SFunction<ErpSaleOrderDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleOrderDO::getNo;
            case "orderType":
                return ErpSaleOrderDO::getOrderType;
            case "customerId":
                return ErpSaleOrderDO::getCustomerId;
            case "orderTime":
                return ErpSaleOrderDO::getOrderTime;
            case "creator":
                return ErpSaleOrderDO::getCreator;
            case "totalCount":
                return ErpSaleOrderDO::getTotalCount;
            case "outCount":
                return ErpSaleOrderDO::getOutCount;
            case "returnCount":
                return ErpSaleOrderDO::getReturnCount;
            case "totalProductPrice":
                return ErpSaleOrderDO::getTotalProductPrice;
            case "totalPrice":
                return ErpSaleOrderDO::getTotalPrice;
            case "depositPrice":
                return ErpSaleOrderDO::getDepositPrice;
            case "status":
                return ErpSaleOrderDO::getStatus;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleOrderDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleOrderDO>()
                .eq(ErpSaleOrderDO::getId, id).eq(ErpSaleOrderDO::getStatus, status));
    }

    default ErpSaleOrderDO selectByNo(String no) {
        return selectOne(ErpSaleOrderDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleOrderDO::getCustomerId, customerId);
    }

    static String normalizeLikeValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", "%");
        return normalized.isEmpty() ? null : normalized;
    }

}
