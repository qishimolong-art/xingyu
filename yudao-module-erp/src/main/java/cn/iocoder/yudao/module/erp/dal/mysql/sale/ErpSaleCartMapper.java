package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * ERP 销售手推车 Mapper
 */
@Mapper
public interface ErpSaleCartMapper extends BaseMapperX<ErpSaleCartDO> {

    default PageResult<ErpSaleCartDO> selectPage(ErpSaleCartPageReqVO reqVO) {
        MPJLambdaWrapperX<ErpSaleCartDO> queryWrapper = new MPJLambdaWrapperX<ErpSaleCartDO>()
                .likeIfPresent(ErpSaleCartDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleCartDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleCartDO::getSaleUserId, reqVO.getSaleUserId())
                .eqIfPresent(ErpSaleCartDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleCartDO::getCartTime, reqVO.getCartTime())
                .likeIfPresent(ErpSaleCartDO::getContactPerson, reqVO.getContactPerson())
                .eqIfPresent(ErpSaleCartDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleCartDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSaleCartDO::getId, reqVO.getIds());
        if (reqVO.getStatus() == null && Boolean.TRUE.equals(reqVO.getIncludeCompleted())) {
            queryWrapper.in(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        } else if (reqVO.getStatus() == null) {
            queryWrapper.notIn(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        }
        if (reqVO.getProductId() != null || StringUtils.hasText(reqVO.getProductKeyword())) {
            queryWrapper.leftJoin(ErpSaleCartItemDO.class, ErpSaleCartItemDO::getCartId, ErpSaleCartDO::getId)
                    .leftJoin(ErpProductDO.class, ErpProductDO::getId, ErpSaleCartItemDO::getProductId)
                    .leftJoin(ErpProductUnitDO.class, ErpProductUnitDO::getId, ErpSaleCartItemDO::getProductUnitId)
                    .eq(reqVO.getProductId() != null, ErpSaleCartItemDO::getProductId, reqVO.getProductId())
                    .and(StringUtils.hasText(reqVO.getProductKeyword()),
                            w -> ErpKeywordQuery.appendProductKeyword(w, reqVO.getProductKeyword()))
                    .groupBy(ErpSaleCartDO::getId);
        }
        ErpKeywordQuery.appendWithDeptNameAndSaleCustomerAndProductItemTokens(queryWrapper, reqVO.getKeyword(),
                "erp_sale_cart_items", "cart_id",
                ErpSaleCartDO::getNo, ErpSaleCartDO::getSourceNo,
                ErpSaleCartDO::getRemark, ErpSaleCartDO::getBusinessType,
                ErpSaleCartDO::getOrderType, ErpSaleCartDO::getBillingMethod,
                ErpSaleCartDO::getSettleMethod, ErpSaleCartDO::getInvoiceType,
                ErpSaleCartDO::getDeliveryMethod, ErpSaleCartDO::getFreightType,
                ErpSaleCartDO::getVin,
                ErpSaleCartDO::getPriority, ErpSaleCartDO::getPriceType,
                ErpSaleCartDO::getLogisticsCompany, ErpSaleCartDO::getContactPerson,
                ErpSaleCartDO::getContactPhone, ErpSaleCartDO::getDeliveryAddress,
                ErpSaleCartDO::getBusinessEntity, ErpSaleCartDO::getOrderMethod,
                ErpSaleCartDO::getSourceType2, ErpSaleCartDO::getRemark2);
        orderByIfPresent(queryWrapper, reqVO);
        return selectJoinPage(reqVO, ErpSaleCartDO.class, queryWrapper);
    }

    static void orderByIfPresent(MPJLambdaWrapperX<ErpSaleCartDO> wrapper, ErpSaleCartPageReqVO reqVO) {
        SFunction<ErpSaleCartDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSaleCartDO::getId);
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
        wrapper.orderByDesc(ErpSaleCartDO::getId);
    }

    static SFunction<ErpSaleCartDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleCartDO::getNo;
            case "cartTime":
                return ErpSaleCartDO::getCartTime;
            case "customerId":
            case "customerName":
                return ErpSaleCartDO::getCustomerId;
            case "creator":
            case "creatorName":
                return ErpSaleCartDO::getCreator;
            case "totalProductPrice":
                return ErpSaleCartDO::getTotalProductPrice;
            case "discountPrice":
                return ErpSaleCartDO::getDiscountPrice;
            case "totalPrice":
                return ErpSaleCartDO::getTotalPrice;
            case "totalFreight":
                return ErpSaleCartDO::getTotalFreight;
            case "settleMethod":
                return ErpSaleCartDO::getSettleMethod;
            case "deliveryMethod":
                return ErpSaleCartDO::getDeliveryMethod;
            case "vin":
                return ErpSaleCartDO::getVin;
            case "logisticsCompany":
                return ErpSaleCartDO::getLogisticsCompany;
            case "saleUserId":
            case "saleUserName":
                return ErpSaleCartDO::getSaleUserId;
            case "deptId":
            case "deptName":
                return ErpSaleCartDO::getDeptId;
            case "priority":
                return ErpSaleCartDO::getPriority;
            case "invoiceType":
                return ErpSaleCartDO::getInvoiceType;
            case "status":
                return ErpSaleCartDO::getStatus;
            case "createTime":
                return ErpSaleCartDO::getCreateTime;
            default:
                return null;
        }
    }

    default ErpSaleCartDO selectByNo(String no) {
        return selectOne(ErpSaleCartDO::getNo, no);
    }

    default ErpSaleCartDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapper<ErpSaleCartDO>()
                .eq(ErpSaleCartDO::getId, id)
                .last("FOR UPDATE"));
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleCartDO::getCustomerId, customerId);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleCartDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleCartDO>()
                .eq(ErpSaleCartDO::getId, id).eq(ErpSaleCartDO::getStatus, status));
    }

    /**
     * 撤销初审，并显式清空初审信息。
     *
     * <p>使用 {@link LambdaUpdateWrapper#set} 设置 null，避免实体更新策略忽略 null 字段。</p>
     */
    default int cancelFirstApprove(Long id) {
        return update(null, new LambdaUpdateWrapper<ErpSaleCartDO>()
                .eq(ErpSaleCartDO::getId, id)
                .eq(ErpSaleCartDO::getStatus, ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus())
                .set(ErpSaleCartDO::getStatus, ErpSaleCartStatusEnum.SUBMITTED.getStatus())
                .set(ErpSaleCartDO::getFirstAuditUserId, null)
                .set(ErpSaleCartDO::getFirstAuditTime, null));
    }

}
