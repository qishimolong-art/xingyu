package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * ERP 采购订单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseOrderMapper extends BaseMapperX<ErpPurchaseOrderDO> {

    default PageResult<ErpPurchaseOrderDO> selectPage(ErpPurchaseOrderPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseOrderDO> query = new MPJLambdaWrapperX<ErpPurchaseOrderDO>()
                .likeIfPresent(ErpPurchaseOrderDO::getNo, normalizeLikeValue(reqVO.getNo()))
                .likeIfPresent(ErpPurchaseOrderDO::getFactoryOrderNo, normalizeLikeValue(reqVO.getFactoryOrderNo()))
                .eqIfPresent(ErpPurchaseOrderDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseOrderDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpPurchaseOrderDO::getOrderTime, reqVO.getOrderTime())
                .eqIfPresent(ErpPurchaseOrderDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseOrderDO::getRemark, normalizeLikeValue(reqVO.getRemark()))
                .eqIfPresent(ErpPurchaseOrderDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchaseOrderDO::getPurchaser, reqVO.getPurchaser());
        // 入库状态。为什么需�?t. 的原因，是因为联表查询时，需要指定表名，不然会报 in_count 错误
        if (Objects.equals(reqVO.getInStatus(), ErpPurchaseOrderPageReqVO.IN_STATUS_NONE)) {
            query.eq(ErpPurchaseOrderDO::getInCount, 0);
        } else if (Objects.equals(reqVO.getInStatus(), ErpPurchaseOrderPageReqVO.IN_STATUS_PART)) {
            query.gt(ErpPurchaseOrderDO::getInCount, 0).apply("t.in_count < t.total_count");
        } else if (Objects.equals(reqVO.getInStatus(), ErpPurchaseOrderPageReqVO.IN_STATUS_ALL)) {
            query.apply("t.in_count = t.total_count");
        }
        // Return status.
        if (Objects.equals(reqVO.getReturnStatus(), ErpPurchaseOrderPageReqVO.RETURN_STATUS_NONE)) {
            query.eq(ErpPurchaseOrderDO::getReturnCount, 0);
        } else if (Objects.equals(reqVO.getReturnStatus(), ErpPurchaseOrderPageReqVO.RETURN_STATUS_PART)) {
            query.gt(ErpPurchaseOrderDO::getReturnCount, 0).apply("t.return_count < t.total_count");
        } else if (Objects.equals(reqVO.getReturnStatus(), ErpPurchaseOrderPageReqVO.RETURN_STATUS_ALL)) {
            query.apply("t.return_count = t.total_count");
        }
        // Purchasable inbound.
        if (Boolean.TRUE.equals(reqVO.getInEnable())) {
            query.eq(ErpPurchaseOrderDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.in_count < t.total_count");
        }
        // Purchasable return.
        if (Boolean.TRUE.equals(reqVO.getReturnEnable())) {
            query.eq(ErpPurchaseOrderDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("t.return_count < t.in_count");
        }
        if (reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpPurchaseOrderItemDO.class, ErpPurchaseOrderItemDO::getOrderId, ErpPurchaseOrderDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpPurchaseOrderItemDO::getProductId)
                    .eq(reqVO.getProductId() != null, ErpPurchaseOrderItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> {
                        String productKeyword = normalizeLikeValue(reqVO.getProductKeyword());
                        w.like(ErpProductDO::getCode, productKeyword)
                                .or().like(ErpProductDO::getName, productKeyword)
                                .or().like(ErpProductDO::getBarCode, productKeyword)
                                .or().like(ErpProductDO::getVehicleModel, productKeyword)
                                .or().like(ErpProductDO::getFactoryCode, productKeyword)
                                .or().like(ErpProductDO::getStandard, productKeyword)
                                .or().like(ErpPurchaseOrderItemDO::getVehicleModel, productKeyword)
                                .or().like(ErpPurchaseOrderItemDO::getFactoryCode, productKeyword)
                                .or().like(ErpPurchaseOrderItemDO::getDrawingNo, productKeyword)
                                .or().like(ErpPurchaseOrderItemDO::getBrand, productKeyword);
                    })
                    .groupBy(ErpPurchaseOrderDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptName(query, reqVO.getKeyword(),
                ErpPurchaseOrderDO::getNo, ErpPurchaseOrderDO::getFactoryOrderNo,
                ErpPurchaseOrderDO::getRemark, ErpPurchaseOrderDO::getDeliveryMethod,
                ErpPurchaseOrderDO::getPurchaseType, ErpPurchaseOrderDO::getOrderFormula,
                ErpPurchaseOrderDO::getReceiveAddress, ErpPurchaseOrderDO::getInvoiceType,
                ErpPurchaseOrderDO::getSettleMethod, ErpPurchaseOrderDO::getOrderCompany,
                ErpPurchaseOrderDO::getDocumentType);
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpPurchaseOrderDO.class, query);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpPurchaseOrderDO> wrapper, ErpPurchaseOrderPageReqVO reqVO) {
        SFunction<ErpPurchaseOrderDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpPurchaseOrderDO::getId);
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
        wrapper.orderByDesc(ErpPurchaseOrderDO::getId);
    }

    static SFunction<ErpPurchaseOrderDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpPurchaseOrderDO::getNo;
            case "createTime":
                return ErpPurchaseOrderDO::getCreateTime;
            case "factoryOrderNo":
                return ErpPurchaseOrderDO::getFactoryOrderNo;
            case "supplierId":
            case "supplierName":
                return ErpPurchaseOrderDO::getSupplierId;
            case "status":
                return ErpPurchaseOrderDO::getStatus;
            case "inStatus":
                return ErpPurchaseOrderDO::getInCount;
            case "orderDate":
                return ErpPurchaseOrderDO::getOrderDate;
            case "arrivalDate":
                return ErpPurchaseOrderDO::getArrivalDate;
            case "totalCount":
                return ErpPurchaseOrderDO::getTotalCount;
            case "totalProductPrice":
                return ErpPurchaseOrderDO::getTotalProductPrice;
            case "remark":
                return ErpPurchaseOrderDO::getRemark;
            case "deptId":
            case "deptName":
                return ErpPurchaseOrderDO::getDeptId;
            case "creator":
            case "creatorName":
                return ErpPurchaseOrderDO::getCreator;
            case "purchaser":
            case "purchaserName":
                return ErpPurchaseOrderDO::getPurchaser;
            case "printFrequency":
                return ErpPurchaseOrderDO::getId;
            default:
                return null;
        }
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPurchaseOrderDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchaseOrderDO>()
                .eq(ErpPurchaseOrderDO::getId, id).eq(ErpPurchaseOrderDO::getStatus, status));
    }

    default ErpPurchaseOrderDO selectByNo(String no) {
        return selectOne(ErpPurchaseOrderDO::getNo, no);
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPurchaseOrderDO::getSupplierId, supplierId);
    }

    default String selectFirstNoBySupplierId(Long supplierId) {
        ErpPurchaseOrderDO order = selectOne(new MPJLambdaWrapperX<ErpPurchaseOrderDO>()
                .eq(ErpPurchaseOrderDO::getSupplierId, supplierId)
                .orderByDesc(ErpPurchaseOrderDO::getId)
                .last("LIMIT 1"));
        return order == null ? null : order.getNo();
    }

    static String normalizeLikeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().replaceAll("\\s+", "%");
    }

}
