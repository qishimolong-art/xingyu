package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceDocumentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * ERP 采购调价�?Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustMapper extends BaseMapperX<ErpPurchasePriceAdjustDO> {

    default PageResult<ErpPurchasePriceAdjustDO> selectInvoiceSourcePage(
            ErpPurchaseInvoiceSourceDocumentPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> query = new MPJLambdaWrapperX<>();
        query.eq(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                .likeIfPresent(ErpPurchasePriceAdjustDO::getNo, reqVO.getKeyword())
                .apply("EXISTS (SELECT 1 FROM erp_purchase_price_adjust_item invoice_source_item "
                        + "WHERE invoice_source_item.deleted=b'0' AND invoice_source_item.tenant_id=t.tenant_id "
                        + "AND invoice_source_item.adjust_id=t.id AND COALESCE(invoice_source_item.adjust_price,0)<>0 "
                        + "AND COALESCE(invoice_source_item.count,0)>COALESCE(("
                        + "SELECT SUM(COALESCE(invoice_allocation.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation invoice_allocation "
                        + "WHERE invoice_allocation.deleted=b'0' "
                        + "AND invoice_allocation.tenant_id=invoice_source_item.tenant_id "
                        + "AND invoice_allocation.source_type=20 "
                        + "AND invoice_allocation.source_item_id=invoice_source_item.id),0))")
                .orderByDesc(ErpPurchasePriceAdjustDO::getAdjustTime)
                .orderByDesc(ErpPurchasePriceAdjustDO::getId);
        return selectJoinPage(reqVO, ErpPurchasePriceAdjustDO.class, query);
    }

    default PageResult<ErpPurchasePriceAdjustDO> selectPage(ErpPurchasePriceAdjustPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> query = buildQuery(reqVO);
        orderByIfPresent(query, reqVO);
        return selectJoinPage(reqVO, ErpPurchasePriceAdjustDO.class, query);
    }

    default ErpPurchasePriceAdjustSummaryRespVO selectSummary(ErpPurchasePriceAdjustPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> query = buildQuery(reqVO)
                .selectAs("COALESCE(SUM(" + totalPriceSql("old_price") + "), 0)",
                        ErpPurchasePriceAdjustSummaryRespVO::getOriginalTotalPrice)
                .selectAs("COALESCE(SUM(" + totalPriceSql("new_price") + "), 0)",
                        ErpPurchasePriceAdjustSummaryRespVO::getAdjustedTotalPrice)
                .selectAs("COALESCE(SUM(t.total_adjust_price), 0)",
                        ErpPurchasePriceAdjustSummaryRespVO::getTotalAdjustPrice)
                .selectAs("COALESCE(SUM(" + paymentPriceSql() + "), 0)",
                        ErpPurchasePriceAdjustSummaryRespVO::getPaymentPrice);
        return selectJoinOne(ErpPurchasePriceAdjustSummaryRespVO.class, query);
    }

    static MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> buildQuery(ErpPurchasePriceAdjustPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> query = new MPJLambdaWrapperX<ErpPurchasePriceAdjustDO>()
                .likeIfPresent(ErpPurchasePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getAdjustType, reqVO.getAdjustType())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getSupplierId, reqVO.getSupplierId())
                .betweenIfPresent(ErpPurchasePriceAdjustDO::getAdjustTime, reqVO.getAdjustTime())
                .likeIfPresent(ErpPurchasePriceAdjustDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPurchasePriceAdjustDO::getAdjuster, reqVO.getAdjuster());
        ErpKeywordQuery.appendProductItemFilter(query, "erp_purchase_price_adjust_item", "adjust_id",
                reqVO.getProductId(), null, null, null, reqVO.getProductKeyword(), true,
                "product_code", "product_name", "product_unit_name", "vehicle_model", "standard",
                "feature_code", "brand", "drawing_no");
        if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_NONE)) {
            query.apply(paymentPriceSql() + " = 0");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_PART)) {
            query.apply(paymentPriceSql() + " <> 0")
                    .apply("ABS(" + paymentPriceSql() + ") < ABS(t.total_adjust_price)");
        } else if (Objects.equals(reqVO.getPaymentStatus(), ErpPurchasePriceAdjustPageReqVO.PAYMENT_STATUS_ALL)) {
            query.apply("ABS(" + paymentPriceSql() + ") = ABS(t.total_adjust_price)");
        }
        if (Boolean.TRUE.equals(reqVO.getPaymentEnable())) {
            query.eq(ErpPurchasePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("ABS(" + paymentPriceSql() + ") < ABS(t.total_adjust_price)");
        }
        ErpKeywordQuery.appendWithDeptNameAndPurchaseSupplierAndProductItemTokensByProductUnit(query, reqVO.getKeyword(),
                "erp_purchase_price_adjust_item", "adjust_id",
                ErpPurchasePriceAdjustDO::getNo, ErpPurchasePriceAdjustDO::getRemark);
        return query;
    }

    static String paymentPriceSql() {
        return "(SELECT COALESCE(SUM(item.payment_price), 0) FROM erp_finance_payment_item item "
                + "WHERE item.deleted = 0 AND item.tenant_id = t.tenant_id "
                + "AND item.biz_type = " + ErpBizTypeEnum.PURCHASE_PRICE_ADJUST.getType() + " "
                + "AND item.biz_id = t.id)";
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpPurchasePriceAdjustDO> wrapper,
                                 ErpPurchasePriceAdjustPageReqVO reqVO) {
        String direction = normalizeOrderDirection(reqVO.getOrderDirection());
        if (direction == null) {
            wrapper.orderByDesc(ErpPurchasePriceAdjustDO::getId);
            return;
        }
        String expression = getOrderExpression(reqVO.getOrderField());
        if (expression != null) {
            wrapper.last("ORDER BY " + expression + " " + direction + ", t.id ASC");
            return;
        }
        SFunction<ErpPurchasePriceAdjustDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpPurchasePriceAdjustDO::getId);
            return;
        }
        if ("ASC".equals(direction)) {
            wrapper.orderByAsc(orderColumn).orderByAsc(ErpPurchasePriceAdjustDO::getId);
        } else {
            wrapper.orderByDesc(orderColumn).orderByAsc(ErpPurchasePriceAdjustDO::getId);
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
            case "originalTotalPrice":
                return totalPriceSql("old_price");
            case "adjustedTotalPrice":
                return totalPriceSql("new_price");
            default:
                return null;
        }
    }

    static String totalPriceSql(String priceColumn) {
        return "(SELECT COALESCE(SUM(item." + priceColumn + " * item.`count`), 0) "
                + "FROM erp_purchase_price_adjust_item item "
                + "WHERE item.deleted = 0 AND item.tenant_id = t.tenant_id AND item.adjust_id = t.id)";
    }

    static SFunction<ErpPurchasePriceAdjustDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpPurchasePriceAdjustDO::getNo;
            case "status":
                return ErpPurchasePriceAdjustDO::getStatus;
            case "adjustTime":
                return ErpPurchasePriceAdjustDO::getAdjustTime;
            case "createTime":
                return ErpPurchasePriceAdjustDO::getCreateTime;
            case "supplierId":
            case "supplierName":
                return ErpPurchasePriceAdjustDO::getSupplierId;
            case "totalAdjustPrice":
                return ErpPurchasePriceAdjustDO::getTotalAdjustPrice;
            case "adjuster":
            case "adjusterName":
                return ErpPurchasePriceAdjustDO::getAdjuster;
            case "updater":
            case "approverName":
                return ErpPurchasePriceAdjustDO::getUpdater;
            case "approveTime":
                return ErpPurchasePriceAdjustDO::getApproveTime;
            case "remark":
                return ErpPurchasePriceAdjustDO::getRemark;
            default:
                return null;
        }
    }

    default ErpPurchasePriceAdjustDO selectByNo(String no) {
        return selectOne(ErpPurchasePriceAdjustDO::getNo, no);
    }

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPurchasePriceAdjustDO::getSupplierId, supplierId);
    }

    default String selectFirstNoBySupplierId(Long supplierId) {
        ErpPurchasePriceAdjustDO adjust = selectOne(new MPJLambdaWrapperX<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getSupplierId, supplierId)
                .orderByDesc(ErpPurchasePriceAdjustDO::getId)
                .last("LIMIT 1"));
        return adjust == null ? null : adjust.getNo();
    }

    /**
     * 基于 id + 原状态的乐观锁更新，避免并发审批 / 反审�?     *
     * @param id        调价�?id
     * @param status    期望的原状态（作为乐观�?where 条件�?
     * @param updateObj 更新内容
     * @return 实际影响行数
     */
    default int updateByIdAndStatus(Long id, Integer status, ErpPurchasePriceAdjustDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPurchasePriceAdjustDO>()
                .eq(ErpPurchasePriceAdjustDO::getId, id)
                .eq(ErpPurchasePriceAdjustDO::getStatus, status));
    }

}
