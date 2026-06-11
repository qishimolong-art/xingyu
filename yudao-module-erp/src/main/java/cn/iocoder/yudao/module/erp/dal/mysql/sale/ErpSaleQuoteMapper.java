package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 报价订单 Mapper
 */
@Mapper
public interface ErpSaleQuoteMapper extends BaseMapperX<ErpSaleQuoteDO> {

    default PageResult<ErpSaleQuoteDO> selectPage(ErpSaleQuotePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleQuoteDO> wrapper = new LambdaQueryWrapperX<ErpSaleQuoteDO>()
                .likeIfPresent(ErpSaleQuoteDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleQuoteDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleQuoteDO::getSaleUserId, reqVO.getSaleUserId())
                .eqIfPresent(ErpSaleQuoteDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleQuoteDO::getQuoteTime, reqVO.getQuoteTime())
                .eqIfPresent(ErpSaleQuoteDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleQuoteDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSaleQuoteDO::getId, reqVO.getIds());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpSaleQuoteDO> wrapper, ErpSaleQuotePageReqVO reqVO) {
        SFunction<ErpSaleQuoteDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSaleQuoteDO::getId);
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
        wrapper.orderByDesc(ErpSaleQuoteDO::getId);
    }

    static SFunction<ErpSaleQuoteDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleQuoteDO::getNo;
            case "quoteTime":
                return ErpSaleQuoteDO::getQuoteTime;
            case "status":
                return ErpSaleQuoteDO::getStatus;
            case "customerId":
            case "customerName":
                return ErpSaleQuoteDO::getCustomerId;
            case "totalProductPrice":
                return ErpSaleQuoteDO::getTotalProductPrice;
            case "discountPrice":
                return ErpSaleQuoteDO::getDiscountPrice;
            case "totalPrice":
                return ErpSaleQuoteDO::getTotalPrice;
            case "saleUserId":
            case "saleUserName":
                return ErpSaleQuoteDO::getSaleUserId;
            case "deptId":
            case "deptName":
                return ErpSaleQuoteDO::getDeptId;
            case "deliveryAddress":
                return ErpSaleQuoteDO::getDeliveryAddress;
            case "expectedDeliveryTime":
                return ErpSaleQuoteDO::getExpectedDeliveryTime;
            case "remark":
                return ErpSaleQuoteDO::getRemark;
            case "internalRemark":
                return ErpSaleQuoteDO::getInternalRemark;
            case "creator":
            case "creatorName":
                return ErpSaleQuoteDO::getCreator;
            case "createTime":
                return ErpSaleQuoteDO::getCreateTime;
            case "updater":
            case "updaterName":
                return ErpSaleQuoteDO::getUpdater;
            case "updateTime":
                return ErpSaleQuoteDO::getUpdateTime;
            default:
                return null;
        }
    }

    default ErpSaleQuoteDO selectByNo(String no) {
        return selectOne(ErpSaleQuoteDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleQuoteDO::getCustomerId, customerId);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleQuoteDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleQuoteDO>()
                .eq(ErpSaleQuoteDO::getId, id).eq(ErpSaleQuoteDO::getStatus, status));
    }

}
