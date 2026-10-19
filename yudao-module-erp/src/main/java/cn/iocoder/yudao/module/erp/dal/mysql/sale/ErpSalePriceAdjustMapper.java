package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceReceiptItemMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 销售调价单 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustMapper extends BaseMapperX<ErpSalePriceAdjustDO> {

    String EFFECTIVE_RECEIPT_PRICE_EXPRESSION = ErpFinanceReceiptItemMapper.effectiveReceiptPriceSql(
            ErpBizTypeEnum.SALE_PRICE_ADJUST.getType());

    default PageResult<ErpSalePriceAdjustDO> selectPage(ErpSalePriceAdjustPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSalePriceAdjustDO> wrapper = buildQuery(reqVO);
        orderByIfPresent(wrapper, reqVO);
        return selectJoinPage(reqVO, ErpSalePriceAdjustDO.class, wrapper);
    }

    default ErpSalePriceAdjustSummaryRespVO selectSummary(ErpSalePriceAdjustPageReqVO reqVO) {
        String originalPrice = "COALESCE(SUM((SELECT SUM(COALESCE(i.out_count, 0) * COALESCE(i.old_price, 0)) "
                + "FROM erp_sale_price_adjust_item i WHERE i.adjust_id = t.id AND i.deleted = b'0' "
                + "AND i.tenant_id = t.tenant_id)), 0)";
        String adjustedPrice = "COALESCE(SUM((SELECT SUM(COALESCE(i.out_count, 0) * COALESCE(i.new_price, 0)) "
                + "FROM erp_sale_price_adjust_item i WHERE i.adjust_id = t.id AND i.deleted = b'0' "
                + "AND i.tenant_id = t.tenant_id)), 0)";
        MPJLambdaWrapperX<ErpSalePriceAdjustDO> query = buildQuery(reqVO)
                .selectAs(originalPrice, ErpSalePriceAdjustSummaryRespVO::getTotalOriginalPrice)
                .selectAs(adjustedPrice, ErpSalePriceAdjustSummaryRespVO::getTotalAdjustedPrice)
                .selectAs("COALESCE(SUM(t.total_adjust_price), 0)", ErpSalePriceAdjustSummaryRespVO::getTotalAdjustPrice)
                .selectAs("COALESCE(SUM(t.total_adjust_price), 0)",
                        ErpSalePriceAdjustSummaryRespVO::getTaxExclusiveAdjustPrice)
                .selectAs("CAST(0 AS DECIMAL(18, 2))", ErpSalePriceAdjustSummaryRespVO::getTaxPrice);
        return selectJoinOne(ErpSalePriceAdjustSummaryRespVO.class, query);
    }

    static MPJLambdaWrapperX<ErpSalePriceAdjustDO> buildQuery(ErpSalePriceAdjustPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSalePriceAdjustDO> wrapper = new MPJLambdaWrapperX<ErpSalePriceAdjustDO>()
                .likeIfPresent(ErpSalePriceAdjustDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSalePriceAdjustDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpSalePriceAdjustDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSalePriceAdjustDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpSalePriceAdjustDO::getAdjustUserId, reqVO.getAdjustUserId())
                .eqIfPresent(ErpSalePriceAdjustDO::getAdjustType, reqVO.getAdjustType())
                .betweenIfPresent(ErpSalePriceAdjustDO::getAdjustDate, reqVO.getAdjustDate())
                .likeIfPresent(ErpSalePriceAdjustDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSalePriceAdjustDO::getId, reqVO.getIds());
        if (Boolean.TRUE.equals(reqVO.getReceiptEnable())) {
            wrapper.eq(ErpSalePriceAdjustDO::getStatus, ErpAuditStatus.APPROVE.getStatus())
                    .apply("((t.total_adjust_price > 0 AND " + EFFECTIVE_RECEIPT_PRICE_EXPRESSION
                            + " >= 0 AND " + EFFECTIVE_RECEIPT_PRICE_EXPRESSION + " < t.total_adjust_price) OR "
                            + "(t.total_adjust_price < 0 AND " + EFFECTIVE_RECEIPT_PRICE_EXPRESSION
                            + " <= 0 AND " + EFFECTIVE_RECEIPT_PRICE_EXPRESSION + " > t.total_adjust_price))");
        }
        ErpKeywordQuery.appendProductItemFilter(wrapper, "erp_sale_price_adjust_item", "adjust_id",
                reqVO.getProductId(), null, null, null, reqVO.getProductKeyword(), true);
        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokensByProductUnit(wrapper, reqVO.getKeyword(),
                "erp_sale_price_adjust_item", "adjust_id",
                ErpSalePriceAdjustDO::getNo, ErpSalePriceAdjustDO::getRemark,
                ErpSalePriceAdjustDO::getOriginalSaleOutNo, ErpSalePriceAdjustDO::getNewSaleOutNo,
                ErpSalePriceAdjustDO::getSettleMethod, ErpSalePriceAdjustDO::getDeliveryMethod,
                ErpSalePriceAdjustDO::getLogisticsCompany);
        return wrapper;
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpSalePriceAdjustDO> wrapper,
                                 ErpSalePriceAdjustPageReqVO reqVO) {
        SFunction<ErpSalePriceAdjustDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSalePriceAdjustDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn).orderByAsc(ErpSalePriceAdjustDO::getId);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn).orderByAsc(ErpSalePriceAdjustDO::getId);
            return;
        }
        wrapper.orderByDesc(ErpSalePriceAdjustDO::getId);
    }

    static SFunction<ErpSalePriceAdjustDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSalePriceAdjustDO::getNo;
            case "customerId":
            case "customerName":
                return ErpSalePriceAdjustDO::getCustomerId;
            case "adjustDate":
                return ErpSalePriceAdjustDO::getAdjustDate;
            case "status":
                return ErpSalePriceAdjustDO::getStatus;
            case "totalAdjustPrice":
                return ErpSalePriceAdjustDO::getTotalAdjustPrice;
            case "creator":
            case "creatorName":
                return ErpSalePriceAdjustDO::getCreator;
            case "createTime":
                return ErpSalePriceAdjustDO::getCreateTime;
            case "updater":
            case "updaterName":
                return ErpSalePriceAdjustDO::getUpdater;
            case "updateTime":
                return ErpSalePriceAdjustDO::getUpdateTime;
            case "adjustUserId":
            case "adjustUserName":
                return ErpSalePriceAdjustDO::getAdjustUserId;
            default:
                return null;
        }
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSalePriceAdjustDO::getCustomerId, customerId);
    }

    default ErpSalePriceAdjustDO selectByNo(String no) {
        return selectOne(ErpSalePriceAdjustDO::getNo, no);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSalePriceAdjustDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSalePriceAdjustDO>()
                .eq(ErpSalePriceAdjustDO::getId, id)
                .eq(ErpSalePriceAdjustDO::getStatus, status));
    }

}
