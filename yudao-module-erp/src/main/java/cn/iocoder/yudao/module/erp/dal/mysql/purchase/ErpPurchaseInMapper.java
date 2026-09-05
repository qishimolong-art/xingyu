package cn.iocoder.yudao.module.erp.dal.mysql.purchase;


import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinancePaymentItemMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * ERP 采购入库 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInMapper extends BaseMapperX<ErpPurchaseInDO> {

    String EFFECTIVE_PAYMENT_PRICE_EXPRESSION = ErpFinancePaymentItemMapper.effectivePaymentPriceSql(
            ErpBizTypeEnum.PURCHASE_IN.getType());

    String ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION = "(CASE WHEN EXISTS (SELECT 1 FROM erp_purchase_in_items psi "
            + "WHERE psi.deleted = 0 AND psi.tenant_id = t.tenant_id AND psi.in_id = t.id "
            + "AND psi.original_product_price IS NOT NULL) THEN "
            + "(COALESCE((SELECT SUM(ROUND(COALESCE(psi.original_product_price, psi.product_price) * psi.count, 2)) "
            + "FROM erp_purchase_in_items psi WHERE psi.deleted = 0 AND psi.tenant_id = t.tenant_id "
            + "AND psi.in_id = t.id), 0) - ROUND(COALESCE((SELECT SUM(ROUND(COALESCE(psi.original_product_price, "
            + "psi.product_price) * psi.count, 2)) FROM erp_purchase_in_items psi WHERE psi.deleted = 0 "
            + "AND psi.tenant_id = t.tenant_id AND psi.in_id = t.id), 0) * COALESCE(t.discount_percent, 0) / 100, 2) "
            + "+ COALESCE(t.fee_amount, t.other_price, 0)) ELSE t.total_price END)";

    default PageResult<ErpPurchaseInDO> selectPage(ErpPurchaseInPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchaseInDO> query = new MPJLambdaWrapperX<ErpPurchaseInDO>()
                .likeIfPresent(ErpPurchaseInDO::getNo, reqVO.getNo())
                .likeIfPresent(ErpPurchaseInDO::getFactoryOrderNo, reqVO.getFactoryOrderNo())
                .eqIfPresent(ErpPurchaseInDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseInDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpPurchaseInDO::getInTime, reqVO.getInTime())
                .eqIfPresent(ErpPurchaseInDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPurchaseInDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseInDO::getPurchaser, reqVO.getPurchaser())
                .eqIfPresent(ErpPurchaseInDO::getHandler, reqVO.getHandler())
                .eqIfPresent(ErpPurchaseInDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchaseInDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpPurchaseInDO::getOrderNo, reqVO.getOrderNo());
        // 付款状态。为什么需�?t. 的原因，是因为联表查询时，需要指定表名，不然会报字段不存在的错误
        if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_NONE)) {
            query.apply(EFFECTIVE_PAYMENT_PRICE_EXPRESSION + " = 0");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_PART)) {
            query.apply(EFFECTIVE_PAYMENT_PRICE_EXPRESSION + " > 0")
                    .apply(EFFECTIVE_PAYMENT_PRICE_EXPRESSION + " < " + ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION);
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchaseInPageReqVO.PAYMENT_STATUS_ALL)) {
            query.apply(EFFECTIVE_PAYMENT_PRICE_EXPRESSION + " >= " + ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION);
        }
        if (Boolean.TRUE.equals(reqVO.getPaymentEnable())) {
            query.eq(ErpPurchaseInDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply(EFFECTIVE_PAYMENT_PRICE_EXPRESSION + " < " + ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION);
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
        if (reqVO.getWarehouseId() != null || reqVO.getProductId() != null
                || StringUtils.hasText(reqVO.getProductKeyword())) {
            query.leftJoin(ErpPurchaseInItemDO.class, ErpPurchaseInItemDO::getInId, ErpPurchaseInDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpPurchaseInItemDO::getProductId)
                    .eq(reqVO.getWarehouseId() != null, ErpPurchaseInItemDO::getWarehouseId, reqVO.getWarehouseId())
                    .eq(reqVO.getProductId() != null, ErpPurchaseInItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()), w -> {
                        String productKeyword = ErpKeywordQuery.normalize(reqVO.getProductKeyword());
                        w.like(ErpProductDO::getCode, productKeyword)
                                .or().like(ErpProductDO::getName, productKeyword)
                                .or().like(ErpProductDO::getPinyinCode, productKeyword)
                                .or().like(ErpProductDO::getWubiCode, productKeyword)
                                .or().like(ErpProductDO::getBarCode, productKeyword)
                                .or().like(ErpProductDO::getVehicleModel, productKeyword)
                                .or().like(ErpProductDO::getFactoryCode, productKeyword)
                                .or().like(ErpProductDO::getStandard, productKeyword)
                                .or().like(ErpProductDO::getBrand, productKeyword)
                                .or().like(ErpProductDO::getDrawingNo, productKeyword)
                                .or().like(ErpPurchaseInItemDO::getBarCode, productKeyword)
                                .or().like(ErpPurchaseInItemDO::getVehicleModel, productKeyword)
                                .or().like(ErpPurchaseInItemDO::getDrawingNo, productKeyword)
                                .or().like(ErpPurchaseInItemDO::getBrand, productKeyword);
                    })
                    .groupBy(ErpPurchaseInDO::getId); // 避免 1 对多查询，产生相同的 1
        }
        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplier(query, reqVO.getKeyword(),
                ErpPurchaseInDO::getNo, ErpPurchaseInDO::getOrderNo,
                ErpPurchaseInDO::getRemark, ErpPurchaseInDO::getPurchaser,
                ErpPurchaseInDO::getInvoiceType, ErpPurchaseInDO::getTransportMethod,
                ErpPurchaseInDO::getSettleMethod, ErpPurchaseInDO::getPurchaseArea,
                ErpPurchaseInDO::getAccountant, ErpPurchaseInDO::getFactoryOrderNo,
                ErpPurchaseInDO::getOrderMethod, ErpPurchaseInDO::getFreightType1,
                ErpPurchaseInDO::getFreightType2, ErpPurchaseInDO::getFreightObject1,
                ErpPurchaseInDO::getFreightObject2, ErpPurchaseInDO::getLogisticsCompany,
                ErpPurchaseInDO::getHandler, ErpPurchaseInDO::getPriority,
                ErpPurchaseInDO::getUnloader, ErpPurchaseInDO::getFloatRecord,
                ErpPurchaseInDO::getReceiveUnit, ErpPurchaseInDO::getBusinessEntity);
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
                        + "WHEN t.payment_price >= " + ORIGINAL_SETTLEMENT_TOTAL_EXPRESSION + " THEN 2 ELSE 1 END)";
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

    default List<ErpPurchaseInDO> selectListByFactoryOrderNos(Collection<String> factoryOrderNos) {
        return selectList(new MPJLambdaWrapperX<ErpPurchaseInDO>()
                .in(ErpPurchaseInDO::getFactoryOrderNo, factoryOrderNos)
                .orderByAsc(ErpPurchaseInDO::getFactoryOrderNo)
                .orderByDesc(ErpPurchaseInDO::getId));
    }

}
